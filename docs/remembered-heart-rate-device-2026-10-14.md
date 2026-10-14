# Remembered Bluetooth heart-rate device

The heart-rate GATT fallback previously cleared the saved address on disconnect, timeout and missing service discovery. The picker then always reopened with Remember unchecked. The phone’s current preferences contained a saved heart-rate device, but the available log buffer did not retain the earlier disconnect sequence; the issue was confirmed in code and simulated connection tests.

The saved address now survives connection failures and cancellation. A remembered device uses Android’s auto-connect mode, with a sixty-second connection/reconnection window before reopening the picker. A newly selected unsaved device retains the existing ten-second fallback. On temporary disconnect the existing connection attempts reconnection instead of immediately forgetting the watch and opening the picker. Fallback selection explicitly bypasses the stored address, so preserving it cannot create a retry loop. The picker inherits Remember checked when a saved device exists. Selecting a device with Remember unchecked explicitly clears the preference.

Closed connections cancel their timeout and ignore late callbacks. Successful heart-rate service discovery cancels the connection timeout. The watch must still make its Bluetooth heart-rate service available. Physical Pixel Watch reconnection has not been exercised by these simulated tests; phone history and settings were not modified.

Android API reference: [BluetoothDevice.connectGatt](https://developer.android.com/reference/android/bluetooth/BluetoothDevice#connectGatt(android.content.Context,%20boolean,%20android.bluetooth.BluetoothGattCallback)) documents the auto-connect mode used for remembered devices.

Validation: debug build and six Robolectric tests passed, covering saved-device timeout, temporary disconnect, picker cancellation, explicit checked/unchecked selections, shutdown/recreation, late callbacks after teardown, and the actual picker checkbox state.
