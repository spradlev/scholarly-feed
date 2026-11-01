# Accessibility requirements and test plan

Accessibility is a product requirement, not a visual polish pass. Compose does not automatically guarantee an accessible result; inspect and test actual semantics and interaction.

## Principles

- Use native controls and platform semantics by default.
- Support TalkBack from the first working screen.
- Provide meaningful labels, roles, state, and actions.
- Keep focus and traversal order logical.
- Provide visible keyboard focus when hardware keyboard navigation is supported.
- Announce meaningful refresh, import/export, validation, and error status changes without excessively interrupting speech.
- Do not communicate status using color alone.
- Avoid custom gestures as the only way to perform an action.
- Avoid third-party accessibility overlay widgets.
- Use adequate touch targets and readable content.
- Keep UI text simple and descriptive.
- Treat feed text as untrusted plain text.

## Initial acceptance checklist

### Navigation and focus
- [ ] Every visible control has a meaningful accessible name.
- [ ] Screen-reader focus order matches the visual/logical order.
- [ ] Focus Mode exposes its name and on/off state.
- [ ] Navigation destinations and current screen are understandable.
- [ ] Opening an article, including a similar article, returns to the top and announces its title.
- [ ] No keyboard trap; Back and Escape-like navigation behavior is predictable where applicable.
- [ ] With touch exploration off, Up/Down keys and mouse-wheel scrolling move long feed and article content.
- [ ] Navigation controls show their names on hover and keyboard focus; Focus Mode also shows its current state.
- [ ] Dialogs, menus, and loading states return focus appropriately.

### Feed and article reading
- [ ] Each entry exposes title, source, publication date when known, and read/saved state where present.
- [ ] Missing publication dates are not fabricated.
- [ ] Article text is readable in a sensible order.
- [ ] Links/actions are separately reachable and named.
- [ ] Excerpts are not represented as full article text.
- [ ] Long content does not require a custom gesture to scroll.
- [ ] Moving TalkBack focus through long article text brings the focused paragraph into the visible reading area.
- [ ] Article text size can be changed with an accessible settings control; pinch zoom is optional and does not replace single-finger scrolling.

### Status and errors
- [ ] Refresh success/failure is announced or otherwise discoverable.
- [ ] OPML import errors identify what needs correction.
- [ ] Suggested feed validation communicates pending, valid, and invalid states.
- [ ] AI network failures do not prevent ordinary reading.
- [ ] Speech-unavailable and permission-denied states provide a text alternative.

The Focus Mode switch exposes its label, switch role, and explicit on/off state in Compose semantics, and the UI test verifies those values. Confirm that TalkBack actually announces the state on an emulator or device before marking the checklist item complete.

The source-management screen uses labeled fields and named remove actions. Sources are session-only and are not validated or fetched yet.

## Test sequence

1. Run each primary flow with TalkBack enabled.
2. Verify the same flow with touch disabled where practical, using accessibility focus and actions.
3. Test external keyboard navigation if available.
4. Test text scaling and display-size changes.
5. Test loading, empty, error, offline, and success states.
6. Record device/Android version and actual results; do not claim WCAG or platform compliance without an appropriate evaluation.

The desktop web concepts originally mentioned JAWS, NVDA, and VoiceOver. The first implementation is native Android, so TalkBack is the primary initial screen-reader target. Revisit desktop and iOS assistive-technology testing if/when those clients are built.
