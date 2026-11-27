# 0001. Deep copy of RaoParameters through the JSON round trip

|             |                                                                                                                                                                                                                     |
|-------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Status**  | Accepted                                                                                                                                                                                                            |
| **Date**    | 2026-11-26                                                                                                                                                                                                          |
| **Domain**  | api                                                                                                                                                                                                                 |
| **Related** | PR [#2054](https://github.com/powsybl/powsybl-open-rao/pull/2054), issue [#2032](https://github.com/powsybl/powsybl-open-rao/issues/2032), [powsybl-core#4236](https://github.com/powsybl/powsybl-core/issues/4236) |

## Context

Castor needs to deep copy a `RaoParameters` instance (review of #2032), without mutating the shared instance. At the
time, the only copy mechanism was `MarmotUtils.cloneParameters()`, a JSON round trip hidden in a Marmot-specific utility
class.

The original branch of #2054 added copy constructors on the 21 sub-classes (8 parameter classes, 13 extensions) plus a
reflection lookup in `RaoParameters`. During review, a powsybl-openrao maintainer pointed out that powsybl-core#4236
clones `SensitivityAnalysisParameters` through JSON serialization; after checking with a powsybl maintainer, the branch
was rewritten around the JSON round trip.

## Decision

Deep copy of `RaoParameters` goes through the **JSON round trip**, exposed as the instance method `RaoParameters.copy()`
in `rao-api`: it writes the object with `JsonRaoParameters` and reads it back, returning a fully detached instance.
There are **no copy constructors** on parameter or extension classes, and no static copy utility: a copy constructor
would have been a standard pattern to offer alongside, but with the JSON round trip it forces an awkward state transfer,
since the round trip necessarily produces a different object whose extensions must be unbound from it before being
attached to the new instance, and no caller needs one (Marmot and Castor both call `raoParameters.copy()`).
`MarmotUtils.cloneParameters()` was removed; Marmot (and Castor from #2032) call `raoParameters.copy()`. The precedent
is the `SensitivityAnalysisParameters` cloning introduced in powsybl-core#4236.

## Consequences

- Positive:
    - the copy is a single mechanism with no second copy code to maintain: none of the 21 hand-written copy constructors
      of the original branch, so nothing to keep in sync and no drift from the real structure;
    - the cost per extension is zero: every extension that serializes is copied for free, while the copy-constructor
      version required a hand-written copy method on each of the 21 classes, and any future extension without one failed
      at runtime through the reflection lookup;
    - the round trip additionally validates the object against the persistence model, so a malformed extension is caught
      at copy time, not in the middle of an optimization;
    - one discoverable API for every caller: Marmot, Castor, and any future consumer.
- Negative / risks:
    - an extension type with **no JSON serializer is dropped from the copy**, not shared with the original; stated in
      the `copy()` Javadoc, but easy to miss when adding an extension;
    - one serialization round trip per copy, negligible in practice (a few kilobytes, cloned once per optimization in
      Castor or once per timestamp in Marmot, next to a MIP solve and sensitivity analyses).
- Impact:
    - this is the house pattern for parameters objects: any new `RaoParameters` sub-object or extension must ship with
      its JSON serialization to participate in the copy;
    - do not add copy constructors to parameter or extension classes; `copy()` is the only entry point.
