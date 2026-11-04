# Accessibility Statement

Kickoff is committed to ensuring digital accessibility for all developers, contributors, and end-users of apps built with our platform. We continually improve the user experience for everyone and apply relevant accessibility standards across our developer CLI and scaffolded project templates.

---

## Developer CLI & Tooling Accessibility

- **No Color Mode (`NO_COLOR`)**: The Kickoff CLI respects the `NO_COLOR` standard environment variable and `--no-color` flags, suppressing ANSI escape sequences for screen readers and high-contrast terminal environments.
- **Machine-Readable Outputs**: All commands support `--json` output, enabling seamless integration with assistive scripts, screen readers, and custom automation without parsing terminal formatting.
- **Semantic Text Output**: Error messages, warnings, and success notices include explicit text prefixes (`✔`, `Error:`, `Warning:`) rather than relying solely on color cues.

---

## Generated Android Application Archetypes

Scaffolded Compose templates (`now-in-android` and `android-sdk`) follow Android's [Material 3 Accessibility Guidelines](https://m3.material.io/foundations/accessible-design/overview):

- **Touch Target Sizes**: All interactive elements (buttons, icons, chips) meet the recommended minimum target size of 48x48 dp.
- **Screen Reader Support**: Scaffolded Jetpack Compose components include `contentDescription` properties on iconography and graphical elements for TalkBack.
- **Dynamic Text Scaling**: Typography scales dynamically with Android's system font size preferences without truncation or layout clipping.
- **Contrast Ratios**: Default Material 3 color palettes conform to WCAG 2.1 AA contrast ratios (minimum 4.5:1 for normal text, 3:1 for large text and UI components).

---

## Reporting Accessibility Barriers

If you encounter accessibility barriers when using Kickoff CLI, reading our documentation, or running scaffolded applications, please let us know:

- **GitHub Issue**: Open an issue labeled with `accessibility` via [our issue tracker](https://github.com/Andrer757/Kickoff-V2/issues).
- **Email**: Contact maintainer André Rosa at [andre.rosa@simdea.pt](mailto:andre.rosa@simdea.pt) with `[ACCESSIBILITY]` in the subject line.

We will review your feedback within **48 hours** and work toward a timely resolution.
