# Business parameters

## Rao parameters

RAO parameters to tune the RAO (not implementation specific).

### Objective function parameters

These parameters (objective-function) configure the remedial action optimisation's objective function.  

#### type
- **Expected value**: one of the following:
  - "SECURE_FLOW"
  - "MAX_MIN_MARGIN"
  - "MAX_MIN_RELATIVE_MARGIN"
  - "MIN_COST"
- **Default value**: "SECURE_FLOW"
- **Usage**: this parameter sets the objective function of the RAO. For now, the existing objective function are:
  - **SECURE_FLOW**: The search-tree will stop as soon as it finds a solution where the minimum margin is positive.
  - **MAX_MIN_MARGIN**: the search-tree will maximize the minimum margin until it converges to a
    maximum value, or until another stop criterion has been reached (e.g. [max-preventive-search-tree-depth](implementation-specific-parameters.md#max-preventive-search-tree-depth)).
  - **MAX_MIN_RELATIVE_MARGIN**: same as MAX_MIN_MARGIN, but the margins will be relative
    (divided by the absolute sum of PTDFs) when they are positive.
  - **MIN_COST**: the search-tree will minimize minimal margin violation and remedial action expenses

#### unit
- **Expected value**: one of the following:
  - "MEGAWATT"
  - "AMPERE"
- **Default value**: "MEGAWATT"
- **Usage**: this parameter sets the objective function unit of the RAO. For now, the existing objective function units are:
  - **MEGAWATT**: the margins to maximize are considered in MW.
    - **AMPERE**: the margins to maximize are considered in A.
      Note that CNECs from different voltage levels will not have the same weight in the objective function depending on the unit
      considered (MW or A). Ampere unit only works in AC-load-flow mode (see [sensitivity-parameters](implementation-specific-parameters.md#sensitivity-parameters)).

#### enforce-curative-security
- **Expected value**: true/false
- **Default value**: false
- **Usage**: if this parameter is set to true, OpenRAO will continue optimizing curative states even if preventive state
  is unsecure.
  If this parameter is set to false, OpenRAO will stop after preventive if preventive state is unsecure and won't try to 
  improve curative states.
  
  *Note: Only applied when ["type"](#type) is set to SECURE_FLOW. In this case, if preventive was unsecure,
second preventive won't be run, even if curative cost is higher, in order to save computation time* 

### Range actions optimisation parameters
These parameters (range-actions-optimization) tune the [linear optimiser](../algorithms/castor/linear-problem.md) used to optimise range actions.  
(See [Modelling CNECs and range actions](../algorithms/castor/linear-problem/core-problem-filler.md))

#### pst-ra-min-impact-threshold
- **Expected value**: numeric value, unit: unit of the objective function / ° (per degree)
- **Default value**: 0.01
- **Usage**: the pst-ra-min-impact-threshold represents the cost of changing the PST set-points, it is used within the linear
  optimisation problem of the RAO, where, for each PST, the following term is added to the objective function: 
  $pst\text{-}ra\text{-}min\text{-}impact\text{-}threshold \times |\alpha - \alpha_{0}|$, where $\alpha$ is the optimized angle of the PST, and 
  $\alpha_{0}$ the angle in its initial position.  
  If several solutions are equivalent (e.g. with the same min margin), a strictly positive pst-ra-min-impact-threshold will favour
  the ones with the PST taps the closest to the initial situation.  

#### hvdc-ra-min-impact-threshold
- **Expected value**: numeric value, unit: unit of the objective function / MW
- **Default value**: 0.001
- **Usage**: the hvdc-ra-min-impact-threshold represents the cost of changing the HVDC set-points, it is used within the linear
  optimisation problem of the RAO, where, for each HVDC, the following term is added to the objective function: 
  $hvdc\text{-}ra\text{-}min\text{-}impact\text{-}threshold \times |P - P_{0}|$, where $P$ is the optimized target power of the HVDC, and $P_{0}$ the initial target
  power.  
  If several solutions are equivalent (e.g. with the same min margin), a strictly positive hvdc-ra-min-impact-threshold will favour
  the ones with the HVDC set-points the closest to the initial situation.

#### injection-ra-min-impact-threshold
- **Expected value**: numeric value, unit: unit of the objective function / MW
- **Default value**: 0.001
- **Usage**: the injection-ra-min-impact-threshold represents the cost of changing the injection set-points, it is used within the linear
  optimisation problem of the RAO, in the same way as the two types of RangeAction above.

### Network actions optimisation parameters
These parameters (topological-actions-optimization) tune the [search-tree algorithm](../algorithms/castor.md#search-tree-algorithm) 
when searching for the best network actions.

#### absolute-minimum-impact-threshold
- **Expected value**: numeric value, where the unit is that of the objective function
- **Default value**: 0.0
- **Usage**: if a topological action improves the objective function by x, and x is smaller than this parameter, the 
  effectiveness of this topological action will be considered inconsequential, and it will not be retained by the 
  search-tree.  
  The absolute-minimum-impact-threshold can therefore fill two purposes:
  - do not retain in the optimal solution of the RAO remedial actions with a negligible impact
  - speed up the computation by avoiding the few final depths which only slightly improve the solution.

#### relative-minimum-impact-threshold
- **Expected value**: numeric value, percentage defined between 0 and 1 (1 = 100%)
- **Default value**: 0.0
- **Usage**: behaves like [absolute-minimum-impact-threshold](#absolute-minimum-impact-threshold), but the
  threshold here is defined as a coefficient of the objective function value of the previous depth. In depth (n+1), if a
  topological action improves the objective function by $x$, with $x < solution(depth(n)) \times relative\text{-}minimum\text{-}impact\text{-}threshold$, 
  it will not be retained by the search-tree.

### CNECs that should not be optimised
These parameters (not-optimized-cnecs) allow the activation of region-specific features, that de-activate the
optimisation of specific CNECs in specific conditions.

#### do-not-optimize-curative-cnecs-for-tsos-without-cras
- **Expected value**: true/false
- **Default value**: false
- **Usage**: if this parameter is set to true, the RAO will detect TSOs not sharing any curative remedial actions (in
  the CRAC). During the curative RAO, these TSOs' CNECs will not be taken into account in the minimum margin objective
  function, unless the applied curative remedial actions decrease their margins (compared to their margins before 
  applying any curative action).  
  If it is set to false, all CNECs are treated equally in the curative RAO.  
  This parameter has no effect on the preventive RAO.  
  This parameter should be set to true for CORE CC.

### Loop-flow optional parameter
Adding a LoopFlowParameters to RaoParameters will activate [loop-flow constraints](../algorithms/castor/special-features/loop-flows.md).  
(The RAO will monitor the loop-flows on CNECs that have a LoopFlowThreshold extension.)  
The following parameters tune some of these constraints, the one which are not implementation specific. 
See also: [Modelling loop-flows and their virtual cost](../algorithms/castor/linear-problem/special-features/max-loop-flow-filler.md)

#### acceptable-increase
- **Expected value**: numeric values, in MEGAWATT unit
- **Default value**: 0.0 MW
- **Usage**: the increase of the initial loop-flow that is allowed by the optimisation. That is to say, the optimisation
  bounds the loop-flow on CNECs by:  
  *LFcnec ≤ max(MaxLFcnec , InitLFcnec + acceptableAugmentation)*  
  With *LFcnec* the loop-flow on the CNEC after optimisation, *MaxLFcnec* is the CNEC loop-flow threshold, *InitLFcnec*
  the initial loop-flow on the cnec, and *acceptableAugmentation* the so-called "loop-flow-acceptable-augmentation"
  coefficient.  
  If this constraint cannot be respected and the loop-flow exceeds the aforementioned threshold, the objective function
  associated to this situation will be penalized (see also [violation-cost](implementation-specific-parameters.md#violation-cost))

#### countries
- **Expected value**: array of country codes "XX"
- **Default value**: all countries encountered
- **Usage**: list of countries for which loop-flows should be limited accordingly to the specified constraints. If not
  present, all countries encountered in the input files will be considered. Note that a cross-border line will have its
  loop-flows monitored if at least one of its two sides is in a country from this list.  
  Example of this parameter : [ "BE", "NL" ] if you want to monitor loop-flows in and out of Belgium and the
  Netherlands.

### MNEC optional parameter

Adding a MnecParameters to RaoParameters will activate [MNEC constraints](../algorithms/castor/linear-problem/special-features/mnec-filler.md).  
(The RAO will only monitor CNECs that are only ["monitored"](../input-data/crac/json.md#cnecs)).
The following parameters tune some of these constraints, the one which are not implementation specific.

#### acceptable-margin-decrease

- **Expected value**: numeric values, in MEGAWATT unit
- **Default value**: 50 MW (required by CORE CC methodology)
- **Usage**: the decrease of the initial margin that is allowed by the optimisation on MNECs.  
  In other words, it defines the bounds for the margins on the MNECs by  
  *Mcnec ≥ max(0, m0cnec − acceptableDiminution)*  
  With *Mcnec* the margin on the cnec after optimisation, *m0cnec* the initial margin on the cnec, and
  *acceptableDiminution* the so-called "acceptable-margin-decrease" coefficient.  
  For the CORE CC calculation, the ACER methodology fixes this coefficient at 50 MW.  
  For CSE CC calculation, setting this parameter to -99999 allows the MNEC constraints to consider
  the thresholds in the CRAC only.

### Relative margins optional parameter
Adding a RelativeMarginsParameters is mandatory when [objective function is relative](#type).  
The following parameters tune some constraints, the one which are not implementation specific.
See also: [Modelling the maximum minimum relative margin objective function](../algorithms/castor/linear-problem/objective-function-types/max-min-relative-margin-filler.md)

#### ptdf-boundaries
- **Expected value**: array of zone-to-zone PTDF computation definition, expressed as an equation.  
  Zones are defined by their 2-character code or their 16-character EICode, inside **{ }** characters.  
  Zones are seperated by + or -.  
  All combinations are allowed: country codes, EIC, a mix.
- **Default value**: empty array
- **Usage**: contains the boundaries on which the PTDF absolute sums should be computed (and added to the denominator of
  the relative RAM).  
  For example, in the SWE case, it should be equal to [ "{FR}-{ES}", "{ES}-{PT}" ].  
  For CORE, we should use all the CORE region boundaries (all countries seperated by a - sign) plus Alegro's special
  equation: "{BE}-{22Y201903144---9}-{DE}+{22Y201903145---4}"

## Examples
> ⚠️  **NOTE**  
> The following examples in json and yaml are not equivalent

::::{tabs}
:::{group-tab} JSON
~~~json
{
  "version" : "3.1",
  "objective-function" : {
    "type" : "SECURE_FLOW",
    "unit" : "A",
    "enforce-curative-security" : true
  },
  "range-actions-optimization" : {
    "pst-ra-min-impact-threshold" : 0.01,
    "hvdc-ra-min-impact-threshold" : 0.001,
    "injection-ra-min-impact-threshold" : 0.001
  },
  "topological-actions-optimization" : {
    "relative-minimum-impact-threshold" : 0.0,
    "absolute-minimum-impact-threshold" : 1.0
  },
  "not-optimized-cnecs" : {
    "do-not-optimize-curative-cnecs-for-tsos-without-cras" : false
  }
}
~~~
:::
:::{group-tab} iTools
Based on PowSyBl's [configuration mechanism](inv:powsyblcore:std:doc#user/configuration/index).
~~~yaml
rao-objective-function:
  type: SECURE_FLOW
  unit: AMPERE

rao-range-actions-optimization:
  pst-ra-min-impact-threshold: 0.01

rao-topological-actions-optimization:
  relative-minimum-impact-threshold: 0.0
  absolute-minimum-impact-threshold: 2.0

search-tree-multi-threading:
  available-cpus: 4

search-tree-second-preventive-rao:
  execution-condition: POSSIBLE_CURATIVE_IMPROVEMENT
  re-optimize-curative-range-actions: true
  hint-from-first-preventive-rao: true

rao-not-optimized-cnecs:
  do-not-optimize-curative-cnecs-for-tsos-without-cras: false
~~~
:::
::::
