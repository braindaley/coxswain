package svenmeier.coxswain.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import propoid.db.Reference;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Segment;

public class MaterialLimitPickerDialog extends DialogFragment {

    private static final int TYPE_NONE = 0;
    private static final int TYPE_STROKE_RATE = 1;
    private static final int TYPE_PULSE = 2;
    private static final int TYPE_SPEED = 3;
    private static final int TYPE_POWER = 4;

    private Gym gym;
    private Segment segment;

    private int currentType = TYPE_NONE;
    private int currentValue = 0;

    private View controlsContainer;
    private View presetsScroll;
    private EditText valueInput;
    private TextView unitLabel;
    private ChipGroup presetChips;
    private boolean updatingText = false;

    public static MaterialLimitPickerDialog create(Segment segment) {
        MaterialLimitPickerDialog dialog = new MaterialLimitPickerDialog();
        Bundle args = new Bundle();
        new Reference<>(segment).to(args);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gym = Gym.instance(getContext());
        segment = gym.get(Reference.<Segment>from(getArguments()));

        if (segment != null) {
            if (segment.strokeRate.get() > 0) {
                currentType = TYPE_STROKE_RATE;
                currentValue = segment.strokeRate.get();
            } else if (segment.pulse.get() > 0) {
                currentType = TYPE_PULSE;
                currentValue = segment.pulse.get();
            } else if (segment.speed.get() > 0) {
                currentType = TYPE_SPEED;
                currentValue = segment.speed.get();
            } else if (segment.power.get() > 0) {
                currentType = TYPE_POWER;
                currentValue = segment.power.get();
            } else {
                currentType = TYPE_NONE;
                currentValue = 0;
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_limit_picker, null);

        TabLayout tabLayout = view.findViewById(R.id.limit_picker_tabs);
        controlsContainer = view.findViewById(R.id.limit_controls_container);
        presetsScroll = view.findViewById(R.id.limit_presets_scroll);
        valueInput = view.findViewById(R.id.limit_value_input);
        unitLabel = view.findViewById(R.id.limit_unit_label);
        presetChips = view.findViewById(R.id.limit_preset_chips);

        MaterialButton btnMinus = view.findViewById(R.id.limit_btn_minus);
        MaterialButton btnPlus = view.findViewById(R.id.limit_btn_plus);

        // Configure Tabs
        tabLayout.addTab(tabLayout.newTab().setText(R.string.none_label), currentType == TYPE_NONE);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.strokeRate_label), currentType == TYPE_STROKE_RATE);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.pulse_label), currentType == TYPE_PULSE);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.speed_label), currentType == TYPE_SPEED);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.power_label), currentType == TYPE_POWER);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentType = tab.getPosition();
                switch (currentType) {
                    case TYPE_NONE:
                        currentValue = 0;
                        break;
                    case TYPE_STROKE_RATE:
                        currentValue = 26;
                        break;
                    case TYPE_PULSE:
                        currentValue = 140;
                        break;
                    case TYPE_SPEED:
                        currentValue = 250; // 2.50 m/s
                        break;
                    case TYPE_POWER:
                        currentValue = 150; // 150 W
                        break;
                }
                updateUI();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Stepper Buttons
        btnMinus.setOnClickListener(v -> adjustValue(-getStep()));
        btnPlus.setOnClickListener(v -> adjustValue(getStep()));

        // Direct Text input watcher
        valueInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (updatingText) return;
                try {
                    int val = Integer.parseInt(s.toString().replaceAll("[^0-9]", ""));
                    if (val > 0) {
                        currentValue = val;
                    }
                } catch (NumberFormatException ignored) {}
            }
        });

        updateUI();

        return new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> applyLimit())
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    private int getStep() {
        switch (currentType) {
            case TYPE_STROKE_RATE:
                return 1;
            case TYPE_PULSE:
                return 5;
            case TYPE_SPEED:
                return 10; // 0.10 m/s
            case TYPE_POWER:
                return 10;
            default:
                return 1;
        }
    }

    private void adjustValue(int delta) {
        currentValue = Math.max(getStep(), currentValue + delta);
        updateUI();
    }

    private void updateUI() {
        if (currentType == TYPE_NONE) {
            controlsContainer.setVisibility(View.GONE);
            presetsScroll.setVisibility(View.GONE);
            unitLabel.setText(R.string.none_label);
            return;
        }

        controlsContainer.setVisibility(View.VISIBLE);
        presetsScroll.setVisibility(View.VISIBLE);

        updatingText = true;
        valueInput.setText(String.valueOf(currentValue));
        updatingText = false;

        switch (currentType) {
            case TYPE_STROKE_RATE:
                unitLabel.setText(R.string.strokeRate_label);
                setupPresets(new int[]{20, 24, 26, 28, 30, 32});
                break;
            case TYPE_PULSE:
                unitLabel.setText(R.string.pulse_label);
                setupPresets(new int[]{120, 135, 150, 165, 180});
                break;
            case TYPE_SPEED:
                unitLabel.setText(R.string.speed_label);
                setupPresets(new int[]{200, 250, 300, 350});
                break;
            case TYPE_POWER:
                unitLabel.setText(R.string.power_label);
                setupPresets(new int[]{100, 150, 200, 250, 300});
                break;
        }
    }

    private void setupPresets(int[] presets) {
        presetChips.removeAllViews();
        Context context = getContext();
        if (context == null) return;

        for (int preset : presets) {
            Chip chip = new Chip(context);
            String label;
            if (currentType == TYPE_STROKE_RATE) {
                label = preset + " spm";
            } else if (currentType == TYPE_PULSE) {
                label = preset + " bpm";
            } else if (currentType == TYPE_SPEED) {
                label = String.format("%.2f m/s", preset / 100.0);
            } else if (currentType == TYPE_POWER) {
                label = preset + " W";
            } else {
                label = String.valueOf(preset);
            }

            chip.setText(label);
            chip.setCheckable(false);
            chip.setOnClickListener(v -> {
                currentValue = preset;
                updateUI();
            });
            presetChips.addView(chip);
        }
    }

    private void applyLimit() {
        if (segment == null) return;

        segment.clearLimit();

        switch (currentType) {
            case TYPE_NONE:
                break;
            case TYPE_STROKE_RATE:
                segment.setStrokeRate(currentValue);
                break;
            case TYPE_PULSE:
                segment.setPulse(currentValue);
                break;
            case TYPE_SPEED:
                segment.setSpeed(currentValue);
                break;
            case TYPE_POWER:
                segment.setPower(currentValue);
                break;
        }

        gym.mergeSegment(segment);
        Utils.getCallback(this, AbstractValueFragment.Callback.class).onChanged(segment);
    }
}
