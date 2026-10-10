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
package svenmeier.coxswain.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.R;
import svenmeier.coxswain.view.preference.ResultPreference;

public class SettingsFragment extends PreferenceFragmentCompat {

    private Map<String, Integer> requestCodes = new HashMap<>();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        if (savedInstanceState != null) {
            Bundle routes = savedInstanceState.getBundle("picker_routes");
            if (routes != null) for (String key : routes.keySet()) requestCodes.put(key, routes.getInt(key));
        }

        Preference bindings = findPreference(getString(R.string.preference_workout_bindings_reset));
        bindings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                propoid.util.content.Preference.getEnum(getActivity(), ValueBinding.class, R.string.preference_workout_binding).setList(new ArrayList<ValueBinding>());
                propoid.util.content.Preference.getEnum(getActivity(), ValueBinding.class, R.string.preference_workout_binding_pace).setList(new ArrayList<ValueBinding>());

                requireContext().getSharedPreferences("live_row_display", android.content.Context.MODE_PRIVATE)
                        .edit().clear().apply();
                return true;
            }
        });

        // App-private diagnostic files do not require shared-storage permission.
        Preference log = findPreference(getString(R.string.preference_hardware_log));
        log.setOnPreferenceClickListener(preference -> {
            exportLog();
            return true;
        });

        // Storage switching currently selects a separate database without migration.
        Preference external = findPreference(getString(R.string.preference_data_external));
        external.setEnabled(false);
        external.setSummary(R.string.settings_storage_unavailable);

        // Legacy controls have no consumer in the current Compose application.
        int[] unsupported = { R.string.preference_picture_in_picture,
            R.string.preference_integration_intent, R.string.preference_integration_intent_uri,
            R.string.preference_end_workout_result, R.string.preference_distance_unit,
            R.string.preference_energy_unit, R.string.preference_split_distance,
            R.string.preference_numbers_arabic };
        for (int key : unsupported) {
            Preference obsolete = findPreference(getString(key));
            if (obsolete != null) obsolete.setVisible(false);
        }

        int[] reconnect = { R.string.preference_adjust_energy, R.string.preference_weight,
                R.string.preference_adjust_speed, R.string.preference_hardware_trace,
                R.string.preference_hardware_legacy, R.string.preference_hardware_heart_sensor };
        for (int key : reconnect) {
            Preference setting = findPreference(getString(key));
            if (setting != null) {
                CharSequence original = setting.getSummary();
                setting.setSummary((original == null ? "" : original + "\n") + getString(R.string.settings_reconnect));
            }
        }

        Preference devices = findPreference(getString(R.string.preference_devices));
        devices.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.settings_fragment, new DevicesFragment());
                transaction.addToBackStack(null);
                transaction.commit();
                return true;
            }
        });

        findPreference("heart_rate_zones").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireContext(), svenmeier.coxswain.HeartRateZonesActivity.class));
            return true;
        });

        Preference healthConnect = findPreference("preference_health_connect");
        if (healthConnect != null) {
            healthConnect.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(requireContext(),
                            svenmeier.coxswain.google.HealthConnectManageActivity.class));
                    return true;
                }
            });
        }
    }

    public static final String LOG_FILE = "coxswain.log";

    private void exportLog() {
        final android.content.Context context = requireContext();
        new Thread(() -> {
            int message = R.string.preference_hardware_log_failed;
            Process process = null;
            try {
                File file = new File(Coxswain.getExternalFilesDir(context), LOG_FILE);
                file.getParentFile().mkdirs();
                process = new ProcessBuilder("logcat", "-d", "-f", file.getAbsolutePath())
                        .redirectErrorStream(true).start();
                if (process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
                        && process.exitValue() == 0 && file.exists()) {
                    message = R.string.preference_hardware_log_finished;
                }
            } catch (Exception e) {
                Log.e(Coxswain.TAG, "Export log failed", e);
            } finally {
                if (process != null) process.destroy();
            }
            final int result = message;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, result, Toast.LENGTH_LONG).show());
        }, "settings-log-export").start();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof ResultPreference) {
            ResultPreference resultPreference = (ResultPreference) preference;

            Intent intent = resultPreference.getRequest();

            int requestCode = requestCode(resultPreference);
            startActivityForResult(intent, requestCode);

            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        Bundle routes = new Bundle();
        for (Map.Entry<String, Integer> route : requestCodes.entrySet()) routes.putInt(route.getKey(), route.getValue());
        state.putBundle("picker_routes", routes);
    }

    private int requestCode(ResultPreference preference) {
        Integer code = requestCodes.get(preference.getKey());
        if (code == null) {
            code = requestCodes.size();
            requestCodes.put(preference.getKey(), code);
        }

        return code;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != 0) {
            for (Map.Entry<String, Integer> entry : requestCodes.entrySet()) {
                if (entry.getValue() == requestCode) {
                    ResultPreference target = findPreference(entry.getKey());
                    if (target != null && intent != null) target.onResult(intent);
                    return;
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }
}
