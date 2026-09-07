/*
 * Copyright 2015 Sven Meier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package svenmeier.coxswain;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.io.ImportIntention;
import svenmeier.coxswain.view.PerformanceFragment;
import svenmeier.coxswain.view.ProgramsFragment;
import svenmeier.coxswain.view.WorkoutsFragment;


public class MainActivity extends AbstractActivity {

    public static String TAG = "coxswain";

    private static final int REQUEST_IMPORT = 42;

    private Gym gym;

    private ViewPager2 pager;
    private BottomNavigationView bottomNav;

    private AppBarLayout appBar;

    private ViewGroup programView;

    private TextView programNameView;

    private TextView programCurrentView;

    private Gym.Listener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
            }
        }

        gym = Gym.instance(this);

        setContentView(R.layout.layout_main);

        onNewIntent(getIntent());

        pager = findViewById(R.id.main_pager2);
        pager.setAdapter(new MainAdapter(this));

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_programs) {
                    pager.setCurrentItem(0, false);
                    return true;
                } else if (id == R.id.nav_workouts) {
                    pager.setCurrentItem(1, false);
                    return true;
                } else if (id == R.id.nav_performance) {
                    pager.setCurrentItem(2, false);
                    return true;
                }
                return false;
            }
        });

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNav.setSelectedItemId(R.id.nav_programs);
                        break;
                    case 1:
                        bottomNav.setSelectedItemId(R.id.nav_workouts);
                        break;
                    case 2:
                        bottomNav.setSelectedItemId(R.id.nav_performance);
                        break;
                }
            }
        });

        appBar = (AppBarLayout) findViewById(R.id.main_appbar);

        programView = (ViewGroup) findViewById(R.id.main_program);
        programView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WorkoutActivity.restart(MainActivity.this);
            }
        });

        programNameView = (TextView) findViewById(R.id.main_program_name);
        programCurrentView = (TextView) findViewById(R.id.main_program_current);

        findViewById(R.id.main_current_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Gym.instance(MainActivity.this).deselect();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        listener = new Gym.Listener() {
            @Override
            public void changed(Object scope) {
                if (scope == null) {
                    updateProgram();
                }
            }
        };
        listener.changed(null);
        gym.addListener(listener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        gym.removeListener(listener);
    }

    private void updateProgram() {
        // AppBarLayout messes up its layout when a child changes to
        // Visibility.GONE, so we remove programView instead

        Program program = gym.program;
        if (program == null) {
            if (programView.getParent() != null) {
                appBar.removeView(programView);;
            }
        } else {
            if (programView.getParent() == null) {
                appBar.addView(programView, 1); //
            }

            programNameView.setText(gym.program.name.get());

            String description = getString(R.string.gym_ready);
            if (gym.progress != null) {
                description = gym.progress.describe();
            }
            programCurrentView.setText(description);
        }
    }

    /**
     * Check whether the intent contains a {@link UsbDevice}, and pass it to {@link GymService}.
     *
     * @param intent possible USB device connect
     */
    private boolean checkUsbDevice(Intent intent) {
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null) {
                GymService.start(this, device);

                if (gym.program == null) {
                    if (pager != null) {
                        // views are already created, so switch to workouts
                        pager.setCurrentItem(0, true);
                    }
                    // try to unlock device - has no effect if this activity is already running :/
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                } else {
                    // program is already selected so restart workout
                    WorkoutActivity.start(this);
                }
            }

            return true;
        }

        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        checkUsbDevice(intent);

        new ImportIntention(this).onIntent(intent);

        // consume intent
        intent.setAction(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMPORT && data != null) {
            // data intent does not have an action
            new ImportIntention(this).importFrom(data.getData());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (BuildConfig.DEBUG == false) {
            menu.findItem(R.id.action_mock).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_mock) {
            GymService.start(this, GymService.CONNECTOR_MOCK);

            return true;
        } else if (id == R.id.action_help) {
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.help_url)));
            startActivity(browser);

            return true;
        } else if (id == R.id.action_bluetooth) {
            GymService.start(this, GymService.CONNECTOR_BLUETOOTH);

            return true;
        } else if (id == R.id.action_import) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            try {
                startActivityForResult(Intent.createChooser(intent, getString(R.string.action_import)), REQUEST_IMPORT);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, getString(R.string.import_chooser), Toast.LENGTH_LONG).show();
            }

            return true;
        } else if (id == R.id.action_settings) {
            startActivity(SettingsActivity.createIntent(this));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class MainAdapter extends FragmentStateAdapter {

        public MainAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public int getItemCount() {
            return 3;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new ProgramsFragment();
            } else if (position == 1) {
                return new WorkoutsFragment();
            } else {
                return new PerformanceFragment();
            }
        }
    }
}
