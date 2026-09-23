# Coxswain dark theme

The dark theme keeps the same hierarchy and spacing as the light app. It changes the color roles rather than introducing separate dark layouts.

## Palette

| Role | Dark value | Use |
| --- | --- | --- |
| App background | `#08131F` | Screen background and scroll areas |
| Card surface | `#121F2C` | Primary cards and bottom navigation |
| Subtle surface | `#162534` | Inputs, chart fields, inactive controls |
| Raised surface | `#1D2D3D` | Elements that need stronger separation |
| Primary blue | `#72A7FF` | Main actions, selected icons, charts |
| Selected blue surface | `#173B68` | Selected chips and highlighted content |
| Primary text | `#F3F6FA` | Headlines, values, and card titles |
| Secondary text | `#AAB8C7` | Labels, captions, and supporting values |
| Border | `#405267` | Inputs and controls requiring a visible edge |
| Subtle border | `#2A3A4B` | Card and section separation |
| Error | `#FFB4AB` | Destructive actions and negative states |

## Component rules

- Cards use the card surface and rely on tonal separation instead of strong shadows.
- Filled blue buttons use dark navy text for accessible contrast. Outlined and text buttons use the brighter primary blue.
- Selected segmented controls use the card surface inside a subtle surface track. Selected presets and goal cards use the selected blue surface.
- Charts use primary blue for recorded activity and the subtle surface for empty bars. Axes and supporting statistics use secondary text.
- The bottom navigation uses the card surface, selected blue surface, primary blue, and secondary text.
- System status and navigation bars follow the active theme and switch icon contrast with it.
- Status colors remain semantic. Positive race and connection states stay green; negative and disconnected states stay red.
- Live Row remains a fixed high contrast cockpit in both app themes. Its deep teal background prevents a visual change during an active workout and preserves the large metric display.

## Theme behavior

The existing **Settings > Dark theme** switch controls both the legacy Android screens and all Compose screens. Compose listens for preference changes, so the open app updates without needing a restart. If the setting has never been stored, the app follows the system theme.
