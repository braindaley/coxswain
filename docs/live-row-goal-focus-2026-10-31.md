# Live rowing target feedback — October 31, 2026

The six metric bindings belong to the user. Goals never insert, replace, reorder, or persist a different binding. A chosen metric that matches the active goal now retains its actual value and gains target coloring and a short target label. Stroke rate and power goal tiles use the same five-second display average as their goal comparison so the number and color agree. Missing measurements remain neutral. Heart-rate zone coloring stays independent.

A recorded race keeps its large ahead/behind comparison with only the two short rail labels underneath. The duplicate target/current-feedback lines and saved-best result were removed from that footer; the best result remains on program details before starting. Prescribed pace coaching uses the large readout for seconds faster/slower rather than a virtual meter gap, with the target split below it. A non-race goal whose metric is absent from the selected grid also uses this prominent directional readout. A chosen goal tile on a regular non-race row leaves the footer available for row progress.

The Pause button now explicitly uses white text and icon color against blue in both themes.

Validation: app and instrumentation builds; 207 unit tests; six emulator UI checks covering saved race + selected cadence, selected/missing split, limited coaching phases, and heart-rate zone behavior. The emulator uses a separate application ID and data store; production phone data is untouched.
