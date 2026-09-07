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

public class MaterialTargetPickerDialog extends DialogFragment {

    private static final int TYPE_DISTANCE = 0;
    private static final int TYPE_DURATION = 1;
    private static final int TYPE_STROKES = 2;
    private static final int TYPE_ENERGY = 3;

    private Gym gym;
    private Segment segment;

    private int currentType = TYPE_DISTANCE;
    private int currentValue = 500;

    private EditText valueInput;
    private TextView unitLabel;
    private ChipGroup presetChips;
    private boolean updatingText = false;

    public static MaterialTargetPickerDialog create(Segment segment) {
        MaterialTargetPickerDialog dialog = new MaterialTargetPickerDialog();
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
            if (segment.duration.get() > 0) {
                currentType = TYPE_DURATION;
                currentValue = segment.duration.get();
            } else if (segment.strokes.get() > 0) {
                currentType = TYPE_STROKES;
                currentValue = segment.strokes.get();
            } else if (segment.energy.get() > 0) {
                currentType = TYPE_ENERGY;
                currentValue = segment.energy.get();
            } else {
                currentType = TYPE_DISTANCE;
                currentValue = Math.max(100, segment.distance.get());
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_target_picker, null);

        TabLayout tabLayout = view.findViewById(R.id.picker_tabs);
        valueInput = view.findViewById(R.id.value_input);
        unitLabel = view.findViewById(R.id.unit_label);
        presetChips = view.findViewById(R.id.preset_chips);

        MaterialButton btnMinus = view.findViewById(R.id.btn_minus_large);
        MaterialButton btnPlus = view.findViewById(R.id.btn_plus_large);

        // Configure Tabs
        tabLayout.addTab(tabLayout.newTab().setText(R.string.distance_label), currentType == TYPE_DISTANCE);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.duration_label), currentType == TYPE_DURATION);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.strokes_label), currentType == TYPE_STROKES);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.energy_label), currentType == TYPE_ENERGY);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentType = tab.getPosition();
                // Set default sensible starting value for each type
                switch (currentType) {
                    case TYPE_DISTANCE:
                        currentValue = 500;
                        break;
                    case TYPE_DURATION:
                        currentValue = 120; // 2 minutes
                        break;
                    case TYPE_STROKES:
                        currentValue = 50;
                        break;
                    case TYPE_ENERGY:
                        currentValue = 50;
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
                .setPositiveButton(R.string.action_ok, (dialog, which) -> applyTarget())
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    private int getStep() {
        switch (currentType) {
            case TYPE_DISTANCE:
                return 100;
            case TYPE_DURATION:
                return 30; // 30 seconds
            case TYPE_STROKES:
                return 10;
            case TYPE_ENERGY:
                return 10;
            default:
                return 10;
        }
    }

    private void adjustValue(int delta) {
        currentValue = Math.max(getStep(), currentValue + delta);
        updateUI();
    }

    private void updateUI() {
        updatingText = true;
        valueInput.setText(String.valueOf(currentValue));
        updatingText = false;

        switch (currentType) {
            case TYPE_DISTANCE:
                unitLabel.setText(R.string.distance_label);
                setupPresets(new int[]{250, 500, 1000, 2000, 5000});
                break;
            case TYPE_DURATION:
                unitLabel.setText(R.string.duration_label);
                setupPresets(new int[]{60, 120, 300, 600, 1200});
                break;
            case TYPE_STROKES:
                unitLabel.setText(R.string.strokes_label);
                setupPresets(new int[]{20, 50, 100, 200, 500});
                break;
            case TYPE_ENERGY:
                unitLabel.setText(R.string.energy_label);
                setupPresets(new int[]{25, 50, 100, 200, 500});
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
            if (currentType == TYPE_DURATION) {
                int mins = preset / 60;
                int secs = preset % 60;
                label = secs == 0 ? (mins + "m") : (mins + "m " + secs + "s");
            } else if (currentType == TYPE_DISTANCE) {
                label = preset + "m";
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

    private void applyTarget() {
        if (segment == null) return;

        segment.setDistance(0);
        segment.setDuration(0);
        segment.setStrokes(0);
        segment.setEnergy(0);

        switch (currentType) {
            case TYPE_DISTANCE:
                segment.setDistance(currentValue);
                break;
            case TYPE_DURATION:
                segment.setDuration(currentValue);
                break;
            case TYPE_STROKES:
                segment.setStrokes(currentValue);
                break;
            case TYPE_ENERGY:
                segment.setEnergy(currentValue);
                break;
        }

        gym.mergeSegment(segment);
        Utils.getCallback(this, AbstractValueFragment.Callback.class).onChanged(segment);
    }
}
