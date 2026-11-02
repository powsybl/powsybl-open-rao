# Frequently Asked Questions

Answers to practical questions frequently asked by teams building applications on top of OpenRAO, grounded in the documentation of this site and in the repository.

## Objective function and RAO parameters

### Which objective function should I use?

It depends on the goal of the application around the RAO (see [`objective-function.type`](parameters/business-parameters.md#type)):

- if your application runs an **iterative search in an external loop** around the RAO — e.g. an NTC calculation, where a dichotomy tests several **border-exchange values** — use `SECURE_FLOW`: each RAO run then provides a clean "secured / not secured" outcome for the search to consume. `SECURE_FLOW` is also the natural choice for a pure capacity validation ("can the network be secured?");

The objective function's unit follows the computation mode: AMPERE in AC, MEGAWATT in DC ([objective function parameters](parameters/business-parameters.md#objective-function-parameters)).

### What is a good starting point for the RAO parameters file?

Start from the [full JSON/YAML examples on the Parameters page](parameters.md#examples) (or the parameters of the [tutorial](getting-started/tutorial.md)) and adapt line by line. Beyond this base, most of the parameter surface (loop-flow parameters, MNEC, relative margins, predefined combinations, second preventive RAO, ...) is "advanced" and should only be activated once the base case is reproduced and understood.

Note: the RAO parameters JSON version is **strictly checked** by the deserializer — only the current version is accepted.

### In the RAO parameters, does the penalty applied to range actions make them costly?

No — do not confuse two distinct concepts:

- the [`*-ra-min-impact-threshold` parameters](parameters/business-parameters.md#pst-ra-min-impact-threshold) are **penalties, not economic costs**: they are small terms added to the linear problem for moving PST/HVDC/injection set-points, used to break ties — when several solutions give the same objective value, they make the optimizer keep the set-points closest to the initial situation, i.e. **use the least remedial action movement among equivalent solutions**;
- the actual monetary costs of remedial actions (**activation costs** for all RAs, and **variation costs** for range actions — the cost of shifting the set-point by 1 MW, or passing one tap for a PST, per direction) are **declared in the CRAC**, not in the RAO parameters, and are only taken into account when the objective function is `MIN_COST` (costly mode) — see [optimization of costly remedial actions](algorithms/castor/linear-problem/objective-function-types/costly-optimization.md).

## Sensitivities and algorithms

### Does OpenRAO compute the sensitivities internally, or do I have to supply them?

**Internally.** CASTOR performs the sensitivity analysis itself at each step of the search tree and at each linear-optimization iteration, through the PowSyBl sensitivity provider configured in `load-flow-and-sensitivity-computation` (by default, OpenLoadFlow). Users never supply precomputed sensitivities — only the provider and its parameters ([load flow and sensitivity computation parameters](parameters/implementation-specific-parameters.md#load-flow-and-sensitivity-computation-parameters)).

### How are sensitivities handled for PSTs versus topological actions (switching, disconnections)?

Remedial actions come in two families, and the sensitivity mechanism only applies to one of them ([CASTOR algorithm details](algorithms/castor.md#algorithm-details)):

- **continuous range actions** (PSTs, HVDC links, injections) — the "linear remedial actions" (LRAs): for a given topology, CASTOR computes the linear sensitivity of each LRA on each CNEC of the studied perimeter (PSDFs for PSTs) and solves a linear problem `F_i = F_ref,i + Σ_j Δα_j · S_j,i` against the objective `OF = min_i(ρ_i · (F_max,i − F_i))`;

The two are combined: the LRA optimization is performed **separately for each topology** — the sensitivities are recomputed in the new topology at every search-tree leaf, which captures the coupled effects (topology + PSTs). See also the [search tree algorithm example](algorithms/castor.md#search-tree-algorithm-example).

### How can I inspect the raw sensitivity coefficients?

They are not exposed in the RAO result. If you use OpenLoadFlow, they can be dumped by enabling the debug mode, i.e. setting the [`debugDir` parameter](https://powsybl.readthedocs.io/projects/powsybl-open-loadflow/en/latest/sensitivity/parameters.html#debugdir) of the OpenLoadFlow sensitivity parameters to a given directory. At the business level, the logs give functional/virtual costs, limiting elements and margins before/after for each state, and the RaoResult gives flows and margins per CNEC.

## CRAC: content and import validation

### What is the minimum CRAC content for a RAO execution?

- **at least two instants**: the first of kind `PREVENTIVE`, the second of kind `OUTAGE` (`AUTO` and `CURATIVE` instants are optional) — the importer warns otherwise ([JSON CRAC → Instants and States](input-data/crac/json.md#instants-and-states));

### How do I check that my CRAC file was imported correctly?

Check the **CRAC creation report**: `Crac.readWithContext(...)` returns a `CracCreationContext` whose `getCreationReport()` is a textual report of the elements that were **ignored or modified** during the import, with tagged lines — `[ERROR]`, `[REMOVED]`, `[ADDED]`, `[ALTERED]`, `[WARN]`, `[INFO]` (e.g. `[ERROR]` when a CRAC element references a network element that was not found in the network, `[REMOVED]` when a contingency or CNEC is dropped). Log and check it to make sure the CRAC file is well-defined. Details: [CRAC creation context](input-data/crac/creation-context.md#non-specific-information).

## Curative actions and contingencies

### How do I associate a curative action with one or several contingencies?

Add to the action **one `OnContingencyState` usage rule per contingency** (each rule = a curative `instant` + the `contingencyId`) — see [remedial actions and usage rules](input-data/crac/json.md#remedial-actions-and-usages-rules).

### If a curative switching group is associated with several contingencies, is it eligible independently for each?

Yes. A **NetworkAction (group of elementary actions) is indivisible** — it is applied as a whole or not at all — and each contingency defines its **own curative perimeter**, optimized separately and sequentially ([curative perimeters](algorithms/castor/rao-steps.md#curative-perimeters)). The same action group, eligible in the curative states of several contingencies, is therefore a candidate independently for each one: the optimizer may activate it for contingency A and not for contingency B. Do not confuse this with `predefined-combinations`, which forces combinations of NetworkActions in the search space but does not tie states together.

## Reading and diagnosing results

### How do I inspect the final contingency → remedial action mapping used by the optimizer?

The [`RaoResult`](output-data/rao-result.md) is keyed by **state** (the preventive state, or a (contingency, instant) pair): see the [Network actions results](output-data/rao-result.md#network-actions-results), [Standard range actions results](output-data/rao-result.md#standard-range-actions-results) and [PST range actions results](output-data/rao-result.md#pst-range-actions-results) sections for the API methods (activated actions per state, optimized set-points / taps before and after) and the equivalent JSON content. The business logs summarize the activated actions per scenario.

### Can I distinguish initial N-0, initial N-1, after preventive optimization and after curative optimization?

Yes — the granularity of the results is the **state**: the RaoResult provides flows and margins per CNEC per state (initial N-1 = the outage state; preventive actions are reported in the post-contingency states via `wasActivatedBeforeState`), the `costResults` distinguish `initial` / `preventive` / `auto` / `curative`, and the logs are segmented the same way ("Preventive perimeter optimization", then "Post-contingency perimeters optimization").

### Why can the overload count increase after preventive actions, while the objective improved?

Two reasons, both expected:

1. the objective is the **minimum margin**, not the *number* of violations — improving the worst margin can legitimately degrade other CNECs;
2. in the preventive perimeter, the post-contingency CNECs are evaluated against their **TATL**, not their PATL ([curative perimeters](algorithms/castor/rao-steps.md#curative-perimeters)) — a preventive solution can "sacrifice" curative CNECs between PATL and TATL to improve the perimeter's minimum margin; the curative actions are supposed to close the gap.

To diagnose: look at the **margins per CNEC and per state** (PATL vs TATL) in the RaoResult, the `costResults` before/after, the limiting elements in the business logs, and the business warns (e.g. RA ignored for an initial set-point out of range, MIP sensitivity failure).

## Getting started

### Is there a small benchmark to reproduce before scaling up?

Yes: the [tutorial](getting-started/tutorial.md) — a 12-node UCTE network, a complete CRAC built from scratch (1 contingency, 3 instants, 3 flow CNECs with PATL/TATL, 1 preventive PST range action, 1 curative switching group), the RAO parameters, and the expected results **log by log**. Code and resources: [farao-community/rao-example](https://github.com/farao-community/rao-example). A [time-coupled variant](getting-started/time-coupled-tutorial.md) exists as well, and the `ra-optimisation` module contains integration tests with expected RaoResults, usable as numerical oracles.
