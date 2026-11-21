package svenmeier.coxswain.bluetooth;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.WorkerThread;

import java.util.ArrayDeque;

import propoid.util.content.Preference;
import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Heart;
import svenmeier.coxswain.DiagnosticsLog;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.util.PermissionBlock;

/**
 * {@link Heart} reading from a connected Bluetooth device.
 */
public class BluetoothHeart extends Heart {

	private static final int CONNECT_TIMEOUT_MILLIS = 10000;
    private static final int REMEMBERED_CONNECT_TIMEOUT_MILLIS = 15000;
    private static final int RETRY_MILLIS = 5000;
    private static final int READING_TIMEOUT_MILLIS = 15000;

	private static final int BATTERY_LEVEL_NOTIFICATION_THRESHOLD = 25;

	private Handler handler = new Handler();

	private ArrayDeque<Connection> connections = new ArrayDeque<>();

	private Preference<String> devicePreference;

	public BluetoothHeart(Context context, Measurement measurement, Callback callback) {
		super(context, measurement, callback);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
			toast(context.getString(R.string.bluetooth_heart_no_bluetooth));
			return;
		}

		devicePreference = Preference.getString(context, R.string.preference_bluetooth_heart_device);

		push(new Permissions());
	}

	private void fireDisconnected() {
	}

	@Override
	public void destroy() {
		while (connections.isEmpty() == false) {
			pop();
		}
	}

	private void toast(final String text) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context, text, Toast.LENGTH_LONG).show();
			}
		});
	}

	private void push(Connection connection) {
		this.connections.push(connection);

		connection.open();
	}

    private void pop() {
        this.connections.pop().close();
    }

    private interface Connection {

		void open();

		void close();
	}

	private class Permissions extends PermissionBlock implements Connection {

		public Permissions() {
			super(context);
		}

		@Override
		public void open() {
			if (Build.VERSION.SDK_INT >= 31) {
				acquirePermissions(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT);
			} else {
				acquirePermissions(Manifest.permission.ACCESS_FINE_LOCATION);
			}
		}

		@Override
		public void close() {
			abortPermissions();
		}

		@Override
		protected void onPermissionsApproved() {
			if (Build.VERSION.SDK_INT >= 31) {
				push(new Bluetooth());
			} else {
				push(new LocationServices());
			}
		}
	}

	private class LocationServices extends BroadcastReceiver implements Connection {

		private boolean registered;

		public void open() {
			if (isRequired()) {
				if (isEnabled() == false) {
					toast(context.getString(R.string.bluetooth_heart_no_location));

					Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);

					IntentFilter filter = new IntentFilter();
					filter.addAction(LocationManager.MODE_CHANGED_ACTION);
					if (Build.VERSION.SDK_INT >= 33) {
						context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED);
					} else {
						context.registerReceiver(this, filter);
					}
					registered = true;

					return;
				}
			}

			proceed();
		}

		/**
		 * Location services must be enabled for Apps built for M and running on M or later.
		 */
		private boolean isRequired() {
			boolean builtForM = context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.M;
			boolean runningOnM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

			return builtForM && runningOnM;
		}

		private boolean isEnabled() {
			LocationManager manager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			criteria.setAltitudeRequired(false);
			criteria.setBearingRequired(false);
			criteria.setCostAllowed(true);
			criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);

			String provider = manager.getBestProvider(criteria, true);

			return provider != null && LocationManager.PASSIVE_PROVIDER.equals(provider) == false;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (connections.peek() == this && isEnabled()) {
				proceed();
			}
		}

		private void proceed() {
			push(new Bluetooth());
		}

		@Override
		public void close() {
			if (registered) {
				context.unregisterReceiver(this);
				registered = false;
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private class Bluetooth extends BroadcastReceiver implements Connection {

		private BluetoothAdapter adapter;

		private boolean registered;

		@Override
		public void open() {
			BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
			adapter = manager.getAdapter();

			if (adapter == null) {
				toast(context.getString(R.string.bluetooth_heart_no_bluetooth));
				return;
			}

			IntentFilter filter = new IntentFilter();
			filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
			filter.addAction(LocationManager.MODE_CHANGED_ACTION);
			if (Build.VERSION.SDK_INT >= 33) {
				context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED);
			} else {
				context.registerReceiver(this, filter);
			}
			registered = true;

			if (adapter.isEnabled() == false) {
				adapter.enable();
			} else {
				proceed();
			}
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
			if (state == BluetoothAdapter.STATE_ON) {
				if (connections.peek() == this) {
					proceed();
				}
			} else if (state == BluetoothAdapter.STATE_OFF) {
				fireDisconnected();
			}
		}

		private void proceed() {
			push(new SelectionConnection());
		}

		@Override
		public void close() {
			if (registered) {
				context.unregisterReceiver(this);
				registered = false;
			}

			adapter = null;
		}
	}

	private class SelectionConnection extends BroadcastReceiver implements Connection {

		private boolean registered;
        private final boolean forceSelection;

        SelectionConnection() { this(false); }
        SelectionConnection(boolean forceSelection) { this.forceSelection = forceSelection; }

        @Override
        public void open() {
            String address = devicePreference.get();
            if (address != null && !forceSelection) {
				proceed(address);
				return;
			}

			String name = context.getString(R.string.bluetooth_heart);
			IntentFilter filter = BluetoothActivity.start(context, name, BlueWriter.SERVICE_HEART_RATE.toString(), address != null);
			if (Build.VERSION.SDK_INT >= 33) {
				context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED);
			} else {
				context.registerReceiver(this, filter);
			}
			registered = true;
		}

		@Override
		public final void onReceive(Context context, Intent intent) {
			if (connections.peek() == this) {
				String address = intent.getStringExtra(BluetoothActivity.DEVICE_ADDRESS);
				if (address == null) {
					fireDisconnected();
				} else {
					boolean remember = intent.getBooleanExtra(BluetoothActivity.DEVICE_REMEMBER, false);
					if (remember) {
						devicePreference.set(address);
					} else {
						devicePreference.set(null);
					}

					proceed(address);
				}
			}
		}

		private void proceed(String address) {
			push(new GattConnection(address));
		}

		@Override
		public void close()
		{
			if (registered) {
				context.unregisterReceiver(this);
				registered = false;
			}

			BluetoothActivity.cancel(context);
		}
	}

    private class GattConnection extends BlueWriter implements Connection, Runnable {
        private final String address;
        private BluetoothAdapter adapter;
        private BluetoothGatt connected;
        private BluetoothGattCharacteristic heartRateMeasurement;
        private BluetoothGattCharacteristic batteryLevel;
        private boolean receivedReading;
        private boolean retrying;
        private final Runnable retry = this::startAttempt;

        private GattConnection(String address) { this.address = address; }
        private boolean remembered() { return address.equals(devicePreference.get()); }
        private boolean active(BluetoothGatt gatt) {
            return adapter != null && !retrying && gatt == connected;
        }

        private void select() {
            handler.post(() -> {
                synchronized (GattConnection.this) {
                    if (connections.peek() != this || adapter == null) return;
                    pop();
                    pop();
                    push(new SelectionConnection(true));
                }
            });
        }

        private void scheduleTimeout(int millis) {
            handler.removeCallbacks(this);
            handler.postDelayed(this, millis);
        }
        private int connectionTimeout() {
            return remembered() ? REMEMBERED_CONNECT_TIMEOUT_MILLIS : CONNECT_TIMEOUT_MILLIS;
        }

        @Override public synchronized void open() {
            BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            adapter = manager.getAdapter();
            toast(context.getString(R.string.bluetooth_heart_connecting, address));
            startAttempt();
        }

        private synchronized void startAttempt() {
            if (adapter == null || connections.peek() != this) return;
            retrying = false;
            receivedReading = false;
            if (!adapter.isEnabled()) {
                failed("Bluetooth is switched off");
                return;
            }
            try {
                BluetoothDevice device = adapter.getRemoteDevice(address);
                Log.d(Coxswain.TAG, "bluetooth heart connecting over LE " + address);
                connected = device.connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE);
                if (connected == null) failed("Connection could not be started");
                else scheduleTimeout(connectionTimeout());
            } catch (IllegalArgumentException invalid) {
                failed("Saved heart-rate device address is invalid");
            }
        }

        private synchronized void failed(String reason) {
            if (adapter == null || retrying) return;
            retrying = true;
            handler.removeCallbacks(this);
            Log.w(Coxswain.TAG, "bluetooth heart: " + reason);
            DiagnosticsLog.recordHeart(context, reason + (remembered() ? " · Retrying saved sensor" : ""));
            onHeartRate(0); // Never keep showing a cached BPM after the stream stops.
            handler.post(() -> {
                synchronized (GattConnection.this) {
                    if (adapter == null || connections.peek() != this) return;
                    releaseGatt();
                    if (remembered()) handler.postDelayed(retry, RETRY_MILLIS);
                    else select();
                }
            });
        }

        private void releaseGatt() {
            if (connected != null) {
                connected.close();
                connected = null;
            }
            heartRateMeasurement = null;
            batteryLevel = null;
            clearRequests();
        }

        @Override public synchronized void close() {
            handler.removeCallbacks(this);
            handler.removeCallbacks(retry);
            adapter = null;
            releaseGatt();
            onHeartRate(0);
        }

        @Override public synchronized void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (!active(gatt)) return;
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failed("Heart-rate connection failed (status " + status + ")");
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(Coxswain.TAG, "bluetooth heart discovering services");
                scheduleTimeout(connectionTimeout());
                if (!gatt.discoverServices()) failed("Heart-rate service discovery could not start");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                failed("Heart-rate connection was lost");
            }
        }

        @Override public synchronized void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (!active(gatt)) return;
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failed("Heart-rate service discovery failed (status " + status + ")");
                return;
            }
            heartRateMeasurement = get(gatt, SERVICE_HEART_RATE, CHARACTERISTIC_HEART_RATE_MEASUREMENT);
            if (heartRateMeasurement == null) {
                failed("Heart-rate sharing service was not found");
                return;
            }
            batteryLevel = get(gatt, SERVICE_BATTERY, CHARACTERISTIC_BATTERY_LEVEL);
            BluetoothGattDescriptor descriptor = heartRateMeasurement.getDescriptor(CLIENT_CHARACTERISTIC_DESCRIPTOR);
            if (descriptor == null || !gatt.setCharacteristicNotification(heartRateMeasurement, true)) {
                failed("Heart-rate notifications could not be enabled");
                return;
            }
            scheduleTimeout(connectionTimeout());
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            if (!gatt.writeDescriptor(descriptor)) failed("Heart-rate notification subscription could not start");
            // Finding a service is not proof of a working data stream. Keep the timeout
            // until an actual heart-rate packet arrives, including after subscription succeeds.
        }

        @Override public synchronized void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (!active(gatt)) return;
            super.onDescriptorWrite(gatt, descriptor, status);
            if (heartRateMeasurement == null || descriptor.getCharacteristic() == null ||
                    !descriptor.getCharacteristic().getUuid().equals(CHARACTERISTIC_HEART_RATE_MEASUREMENT)) return;
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failed("Heart-rate notification subscription failed (status " + status + ")");
                return;
            }
            Log.d(Coxswain.TAG, "bluetooth heart subscribed; waiting for readings");
            if (!receivedReading) scheduleTimeout(connectionTimeout());
            if (batteryLevel != null) read(gatt, batteryLevel);
        }

        @Override @WorkerThread
        public synchronized void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            handleReading(gatt, characteristic, characteristic.getValue());
        }

        @Override @WorkerThread
        public synchronized void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            handleReading(gatt, characteristic, value);
        }

        private void handleReading(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            if (!active(gatt) || heartRateMeasurement == null ||
                    !characteristic.getUuid().equals(CHARACTERISTIC_HEART_RATE_MEASUREMENT)) return;
            if (value == null || value.length < 2) return;
            boolean wide = (value[0] & 1) != 0;
            if (wide && value.length < 3) return;
            int bpm = value[1] & 0xff;
            if (wide) bpm |= (value[2] & 0xff) << 8;
            if (bpm <= 0) return;
            if (!receivedReading) {
                receivedReading = true;
                DiagnosticsLog.recordHeart(context, "Heart-rate readings received");
                toast(context.getString(R.string.bluetooth_heart_connected, address));
            }
            onHeartRate(bpm);
            scheduleTimeout(READING_TIMEOUT_MILLIS);
        }

        @Override public synchronized void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (!active(gatt)) return;
            if (status == BluetoothGatt.GATT_SUCCESS && batteryLevel != null &&
                    characteristic.getUuid().equals(batteryLevel.getUuid()) && characteristic.getValue() != null &&
                    characteristic.getValue().length > 0) {
                onBatteryLevel(characteristic.getValue()[0] & 0xff);
            }
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override public synchronized void run() {
            if (adapter != null && !retrying) {
                failed(receivedReading ? "Heart-rate readings stopped arriving" :
                        "No heart-rate reading arrived before the connection timed out");
            }
        }
    }

	private void onBatteryLevel(int level) {
		if (level <= BATTERY_LEVEL_NOTIFICATION_THRESHOLD) {
			NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

			Notification.Builder builder = new Notification.Builder(context)
					.setContentText(context.getString(R.string.bluetooth_heart_battery_level, level));

			Coxswain.initNotification(context, builder, "Hardware");

			notificationManager.notify(R.string.bluetooth_heart_battery_level, builder.build());
		} else {
			Log.i(Coxswain.TAG, "bluetooth heart battery level " + level);
		}
	}
}
