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

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import propoid.db.Reference;
import propoid.db.aspect.Row;
import propoid.ui.list.GenericRecyclerAdapter;
import svenmeier.coxswain.gym.Difficulty;
import com.google.android.material.chip.Chip;
import svenmeier.coxswain.gym.Difficulty;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.view.AbstractValueFragment;
import svenmeier.coxswain.view.BindingView;
import svenmeier.coxswain.view.LevelView;
import svenmeier.coxswain.view.MaterialLimitPickerDialog;
import svenmeier.coxswain.view.MaterialTargetPickerDialog;
import svenmeier.coxswain.view.ValueBinding;


public class ProgramActivity extends AbstractActivity implements AbstractValueFragment.Callback {

    private Gym gym;

    private EditText nameView;

    private RecyclerView segmentsView;
    private SegmentsAdapter segmentsAdapter;

    private Program program;
    private ItemTouchHelper touchHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

        setContentView(R.layout.layout_program);

        nameView = findViewById(R.id.toolbar_edit);

        segmentsView = findViewById(R.id.program_segments);
        segmentsView.setLayoutManager(new LinearLayoutManager(this));
        segmentsView.setHasFixedSize(true);

        Reference<Program> reference = Reference.from(getIntent());

        program = gym.getProgram(reference);
        if (program == null) {
            finish();
        } else {
            nameView.setText(program.name.get());

            segmentsView.setAdapter(segmentsAdapter = new SegmentsAdapter());

            touchHelper = new ItemTouchHelper(new SegmentsMover());
            touchHelper.attachToRecyclerView(segmentsView);

            View addButton = findViewById(R.id.segment_add);
            if (addButton != null) {
                addButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Segment newSegment = new Segment(Difficulty.EASY);
                        newSegment.setDistance(500);
                        program.addSegment(newSegment);
                        gym.mergeProgram(program);
                        segmentsAdapter.notifyItemInserted(program.getSegments().size() - 1);
                        segmentsView.smoothScrollToPosition(program.getSegments().size() - 1);
                    }
                });
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (program != null) {
            program.name.set(nameView.getText().toString());
            Gym.instance(ProgramActivity.this).mergeProgram(program);
        }
    }

    private class SegmentsMover extends ItemTouchHelper.SimpleCallback {

        public SegmentsMover() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder from, @NonNull RecyclerView.ViewHolder to) {
            int fromPosition = from.getAdapterPosition();
            int toPosition = to.getAdapterPosition();

            Collections.swap(program.getSegments(), fromPosition, toPosition);

            segmentsAdapter.notifyItemMoved(fromPosition, toPosition);

            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();

            program.removeSegment(program.getSegment(position));

            Gym.instance(ProgramActivity.this).mergeProgram(program);

            segmentsAdapter.notifyItemRemoved(position);
        }
    }

    private class SegmentsAdapter extends GenericRecyclerAdapter<Segment> {

        public SegmentsAdapter() {
            super(R.layout.layout_segments_item, program.getSegments());
        }

        @Override
        protected GenericHolder createHolder(View v) {
            return new SegmentHolder(v);
        }
    }

    private class SegmentHolder extends GenericRecyclerAdapter.GenericHolder<Segment> {

        private final View intensityStrip;
        private final View targetContainer;
        private final TextView targetValue;
        private final TextView targetSubtitle;
        private final Chip goalChip;
        private final TextView difficultyBadge;
        private final ImageButton menuButton;
        private final ImageButton deleteButton;

        public SegmentHolder(View v) {
            super(v);

            intensityStrip = v.findViewById(R.id.segment_intensity_strip);
            targetContainer = v.findViewById(R.id.segment_target_container);
            targetValue = v.findViewById(R.id.segment_target_value);
            targetSubtitle = v.findViewById(R.id.segment_target_subtitle);
            goalChip = v.findViewById(R.id.segment_goal_chip);
            difficultyBadge = v.findViewById(R.id.segments_difficulty);
            deleteButton = v.findViewById(R.id.segment_delete);
            menuButton = v.findViewById(R.id.segment_menu);

            if (deleteButton != null) {
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            program.removeSegment(item);
                            gym.mergeProgram(program);
                            segmentsAdapter.notifyItemRemoved(pos);
                        }
                    }
                });
            }

            if (menuButton != null) {
                menuButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Segment duplicate = program.duplicateSegment(item);
                        gym.mergeProgram(program);
                        segmentsAdapter.notifyItemInserted(program.getSegments().indexOf(duplicate));
                    }
                });
                menuButton.setOnTouchListener(new View.OnTouchListener() {
                    private float y;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP) {
                            y = event.getY();
                        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                            if (Math.abs(event.getY() - y) > 5) {
                                touchHelper.startDrag(SegmentHolder.this);
                                return true;
                            }
                        }
                        return false;
                    }
                });
            }
        }

        @Override
        protected void onBind() {
            // 1. Target Value & Subtitle
            if (item.duration.get() > 0) {
                int secs = item.duration.get();
                targetValue.setText(String.format("%02d:%02d", secs / 60, secs % 60));
                targetSubtitle.setText(R.string.duration_label);
            } else if (item.distance.get() > 0) {
                targetValue.setText(String.format("%,d m", item.distance.get()));
                targetSubtitle.setText(R.string.distance_label);
            } else if (item.strokes.get() > 0) {
                targetValue.setText(String.format("%,d", item.strokes.get()));
                targetSubtitle.setText(R.string.strokes_label);
            } else if (item.energy.get() > 0) {
                targetValue.setText(String.format("%,d kcal", item.energy.get()));
                targetSubtitle.setText(R.string.energy_label);
            }

            targetContainer.setOnClickListener(v -> MaterialTargetPickerDialog.create(item).show(getSupportFragmentManager(), "target_dialog"));

            // 2. Goal / Limit Chip
            if (item.strokeRate.get() > 0) {
                goalChip.setText(item.strokeRate.get() + " spm");
            } else if (item.pulse.get() > 0) {
                goalChip.setText(item.pulse.get() + " bpm");
            } else if (item.speed.get() > 0) {
                goalChip.setText(String.format("%.2f m/s", item.speed.get() / 100f));
            } else if (item.power.get() > 0) {
                goalChip.setText(item.power.get() + " W");
            } else {
                goalChip.setText("+ Set goal");
            }

            goalChip.setOnClickListener(v -> MaterialLimitPickerDialog.create(item).show(getSupportFragmentManager(), "limit_dialog"));

            // 3. Difficulty Badge & Left Strip
            Difficulty diff = item.difficulty.get();
            difficultyBadge.setText(diff.name());

            int colorRes = R.color.intensity_easy;
            switch (diff) {
                case REST:
                    colorRes = R.color.intensity_rest;
                    break;
                case EASY:
                    colorRes = R.color.intensity_easy;
                    break;
                case MEDIUM:
                    colorRes = R.color.intensity_medium;
                    break;
                case HARD:
                    colorRes = R.color.intensity_hard;
                    break;
                case PEAK:
                    colorRes = R.color.intensity_peak;
                    break;
            }

            int stripColor = ContextCompat.getColor(ProgramActivity.this, colorRes);
            intensityStrip.setBackgroundColor(stripColor);
            difficultyBadge.setBackgroundTintList(ColorStateList.valueOf(stripColor));

            difficultyBadge.setOnClickListener(v -> {
                item.difficulty.set(item.difficulty.get().increase());
                gym.mergeProgram(program);
                segmentsAdapter.notifyItemChanged(getAdapterPosition());
            });
        }
    }

    @Override
    public void onChanged(Segment segment) {
        List<Segment> segments = program.getSegments();

        int index = 0;
        for (Segment candidate : segments) {
            if (Row.getID(candidate) == Row.getID(segment)) {
                segments.set(index, segment);
                break;
            }
            index++;
        }

        segmentsAdapter.notifyDataSetChanged();
    }

    public static Intent createIntent(Context context, Program program) {
        Intent intent = new Intent(context, ProgramActivity.class);

        intent.setData(new Reference<Program>(program).toUri());

        return intent;
    }
}