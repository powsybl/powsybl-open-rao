# RAO Logs Example

Here is a full RAO logs example divided into multiple steps.  
_Note: for the sake of being exhaustive, DEBUG level is shown. But information at this level is not always interesting 
to the end user._

## Open RAO version

The first log that is outputed by Open RAO is about the version of Open RAO which is used.
~~~
WARN  c.p.o.commons.logs.RaoBusinessWarns - Running RAO using Open RAO version 5.6.0-SNAPSHOT from git commit 45eb3cf9d26a3509eeeb260bd9fe157ff006e1c2.
~~~
This shows the version of the project (which snapshot or which release is used), and the exact git commit on which Open RAO was compiled.

This can be useful when trying to find which version had a bug, or for keeping track of the version deployed.

## Initial sensitivity analysis

In this step, the RAO assesses the initial network against the different constraints in the CRAC.  
It then prints the two CNECs with the smallest margins (i.e. "most limiting elements"), along with their margins.
~~~
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Initial sensitivity analysis: cost = 182.55 (functional: 182.55, virtual: 0.00)
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #01: margin = -182.55 MW, element NNL1AA1  NNL3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "NL1-NL3-D - outage"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #02: margin = -166.08 MW, element FFR2AA1  FFR3AA1  1 at state preventive, CNEC ID = "FR2-FR3-O - preventive"
~~~
In this case, the most limiting CNEC is "NL1-NL3-D - outage" (this is its unique ID in the [OpenRAO CRAC object](../../input-data/crac/json.md)).  
It monitors network element "NNL1AA1  NNL3AA1  1", after contingency "Contingency_FR1_FR3" (unique ID of the contingency 
in the OpenRAO CRAC), at the "outage" instant (i.e. before applying auto & curative actions).
The margin on this CNEC is -182.55MW, so the branch is actually over-charged by 182MW.

The second most-limiting CNEC is "FR2-FR3-O - preventive" with a margin of -166 MW.

## First preventive RAO
Next comes the [first preventive RAO](../../algorithms/castor/rao-steps.md#preventive-perimeter).

> 💡  **NOTE**
> This step is delimited by the two following lines:
> ~~~
> INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- Preventive perimeter optimization [start]
> ...
> INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- Preventive perimeter optimization [end]
> ~~~

As the two most limiting elements are either monitored at the preventive or the outage instant, the RAO will try to 
improve their margins using preventive remedial actions.  

First the RAO logs the beginning of the step:

~~~
INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- Preventive perimeter optimization [start]
~~~

### Root leaf

Then it starts the [search-tree](../../algorithms/castor.md#search-tree-algorithm) algorithm, starting by evaluating the 
"root leaf": it assesses CNEC constraints on the network, considering only CNECs that belong to the 
[preventive perimeter](../../algorithms/castor/rao-steps.md#preventive-perimeter), 
before applying any preventive remedial action.  
It needs not re-run a sensitivity analysis (or load-flow computation), as this has already been done. It just needs to 
restrict constraints assessment on the perimeter's CNECs.  
The RAO then logs the most limiting elements of the preventive perimeter. In this case, they are the same as the RAO's 
overall most limiting elements for the reasons explained above.

~~~
INFO  c.p.openrao.commons.logs.TechnicalLogs - Evaluating root leaf
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Leaf has already been evaluated
INFO  c.p.openrao.commons.logs.TechnicalLogs - Root leaf, range actions have not been optimized, cost: 182.55 (functional: 182.55, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #01: margin = -182.55 MW, element NNL1AA1  NNL3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "NL1-NL3-D - outage"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #02: margin = -166.08 MW, element FFR2AA1  FFR3AA1  1 at state preventive, CNEC ID = "FR2-FR3-O - preventive"
~~~

After root leaf evaluation, the RAO conducts [range action linear optimisation](../../algorithms/castor/linear-problem.md) 
before applying any [network action](../../input-data/crac.md#network-action). 
This step is usually quick and allows the RAO to try to secure the network / improve margins using only remedial actions 
with a linear impact on the network ([range actions](../../input-data/crac.md#range-action)).  
Multiple "MILP -> sensitivity analysis" iterations can be needed until the optimisation converges to an optimal set of 
set-point for range actions (in the example, 2 iterations are needed at the root leaf).

~~~
INFO  c.p.openrao.commons.logs.TechnicalLogs - Linear optimization on root leaf
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Optimizing leaf...
INFO  c.p.openrao.commons.logs.TechnicalLogs - Loading library 'jniortools'
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: linear optimization [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: linear optimization [end]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: better solution found with a cost of 179.10 (functional: 179.10)
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: linear optimization [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: linear optimization [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: same results as previous iterations, optimal solution found
INFO  c.p.o.commons.logs.RaoBusinessLogs - Root leaf, 1 range action(s) activated, cost: 179.10 (functional: 179.10, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - range action(s): PRA_CRA_PST_BE: 1
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #01: margin = -179.10 MW, element NNL1AA1  NNL3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "NL1-NL3-D - outage"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #02: margin = -173.31 MW, element FFR2AA1  FFR3AA1  1 at state preventive, CNEC ID = "FR2-FR3-O - preventive"
~~~

The RAO successfully decreased the objection function value to 179.1 by setting the tap position of PST "PRA_CRA_PST_BE" to 1.
(Note that the objective function seen by the RAO is the opposite of the minimum margin).  
So it increased the margin on the most limiting element from -182MW to -179MW.  
This is not a lot (but it's a good start); you can limit using range actions for small margin improvements using 
[the dedicated parameters](../../parameters/business-parameters.md#range-actions-optimisation-parameters).

### Network actions optimisation

After getting the most out of range actions, the RAO then goes on to choosing the best network actions.  
It does so by choosing the single best network action (or [pre-defined network action combination](../../parameters/implementation-specific-parameters.md#predefined-combinations)) 
first ("search depth 1"), then trying to combine it with the remaining actions to get a two-actions combo 
("search depth 2"), ... until the minimum margin cannot be improved anymore, or until there are no remaining network 
actions to try.  

The search-tree RAO works by generating "leaves" for every depth: every leaf represents the situation of the network with 
an applied network action (or network action combination).  
Then every leaf is treated like the root leaf: it is evaluated, then optimised.  

#### Search depth 1

Here is an example of a "search depth 1" RAO.

~~~
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluating 1 leaves in parallel
INFO  c.p.openrao.commons.logs.TechnicalLogs - Using base network 'TestCase12Nodes2PSTs' on variant 'PreventiveScenario'
INFO  c.p.openrao.commons.logs.TechnicalLogs - Search depth 1 [start]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Leaves to evaluate: 2
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluating network action(s): Open line FR1-FR2
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluated network action(s): Open line FR1-FR2, range actions have not been optimized, cost: 1307.99 (functional: 1307.99, virtual: 0.00)
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Optimizing leaf...
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: linear optimization [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: linear optimization [end]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: better solution found with a cost of 1266.73 (functional: 1266.73)
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: linear optimization [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: linear optimization [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: same results as previous iterations, optimal solution found
INFO  c.p.o.commons.logs.RaoBusinessLogs - Optimized network action(s): Open line FR1-FR2, 1 range action(s) activated, cost: 1266.73 (functional: 1266.73, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Remaining leaves to evaluate: 1
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluating network action(s): Open line NL1-NL2
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluated network action(s): Open line NL1-NL2, range actions have not been optimized, cost: 181.93 (functional: 181.93, virtual: 0.00)
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Optimizing leaf...
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: linear optimization [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: linear optimization [end]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: better solution found with a cost of 140.65 (functional: 140.65)
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: linear optimization [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: linear optimization [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: same results as previous iterations, optimal solution found
INFO  c.p.o.commons.logs.RaoBusinessLogs - Optimized network action(s): Open line NL1-NL2, 1 range action(s) activated, cost: 140.65 (functional: 140.65, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Remaining leaves to evaluate: 0
INFO  c.p.openrao.commons.logs.TechnicalLogs - Search depth 1 [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Search depth 1 best leaf: network action(s): Open line NL1-NL2, 1 range action(s) activated, cost: 140.65 (functional: 140.65, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Search depth 1 best leaf: range action(s): PRA_CRA_PST_BE: -6
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #01: margin = -140.65 MW, element FFR2AA1  FFR3AA1  1 at state preventive, CNEC ID = "FR2-FR3-O - preventive"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #02: margin = 0.22 MW, element NNL1AA1  NNL3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "NL1-NL3-D - outage"
~~~

The RAO has two preventive network actions to assess:
- Open line FR1-FR2: after applying this action on the original network (leaf evaluation), the minimum margin is 
  decreased from -182 to -1308 MW! Even after optimising range actions (leaf optimisaton), minimum margin only reaches -1267MW.
- Open line NL1-NL2: after applying this action on the original network, the minimum margin is almost not changed (-182 MW). 
  However, once it is associated to optimal range action ste-points, the minimum margin reaches -141 MW!  

After evaluating and optimising these two leaves, the search-tree decides to keep the combination of network action 
"Open line NL1-NL2" with PST action "PRA_CRA_PST_BE" at tap position -6. This optimal combination changes the most 
limiting elements in the preventive RAO:
- CNEC "NL1-NL3-D - outage" is now secure with a margin of +0.22MW, and is the second most-limiting element
- CNEC "FR2-FR3-O - preventive" became the most limiting one, even though it has seen its margin increased from -166 to -141 MW.


#### Search depth 2

The RAO then goes on to try combining "Open line NL1-NL2" with the remaining network actions (i.e. "Open line FR1-FR2").

~~~
INFO  c.p.openrao.commons.logs.TechnicalLogs - Search depth 2 [start]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Leaves to evaluate: 1
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluating network action(s): Open line NL1-NL2, Open line FR1-FR2
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluated network action(s): Open line NL1-NL2, Open line FR1-FR2, range actions have not been optimized, cost: 1339.40 (functional: 1339.40, virtual: 0.00)
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Optimizing leaf...
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: linear optimization [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: linear optimization [end]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: better solution found with a cost of 1280.34 (functional: 1280.34)
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: linear optimization [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: linear optimization [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: same results as previous iterations, optimal solution found
INFO  c.p.o.commons.logs.RaoBusinessLogs - Optimized network action(s): Open line NL1-NL2, Open line FR1-FR2, 1 range action(s) activated, cost: 1280.34 (functional: 1280.34, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Remaining leaves to evaluate: 0
INFO  c.p.o.commons.logs.RaoBusinessLogs - No better result found in search depth 2, exiting search tree
~~~

This combination is almost as catastrophic as using "Open line FR1-FR2" alone:
- before range actions optimisation, minimum margin is decreased to -1339MW
- even after range actions optimisation, minimum margin only reaches -1280MW
  
The RAO scraps the combination.

#### Final result

The RAO re-log the optimal remedial action combination it found (search depth 1 result) then logs the most limiting 
elements of the preventive perimeter.

~~~
INFO  c.p.openrao.commons.logs.TechnicalLogs - Search-tree RAO completed with status DEFAULT
INFO  c.p.openrao.commons.logs.TechnicalLogs - Best leaf: network action(s): Open line NL1-NL2, 1 range action(s) activated, cost: 140.65 (functional: 140.65, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Best leaf: range action(s): PRA_CRA_PST_BE: -6
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #01: margin = -140.65 MW, element FFR2AA1  FFR3AA1  1 at state preventive, CNEC ID = "FR2-FR3-O - preventive"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #02: margin = 0.22 MW, element NNL1AA1  NNL3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "NL1-NL3-D - outage"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #03: margin = 38.37 MW, element FFR2AA1  DDE3AA1  1 at state preventive, CNEC ID = "FR2-DE3-D - preventive"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #04: margin = 82.50 MW, element FFR2AA1  FFR3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "FR2-FR3-OO - outage"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #05: margin = 582.50 MW, element FFR2AA1  DDE3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "FR2-DE3-DO - outage"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Scenario "preventive": initial cost = 182.55 (functional: 182.55, virtual: 0.00), 1 network action(s) and 1 range action(s) activated, cost after PRA = 140.65 (functional: 140.65, virtual: 0.00)
INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- Preventive perimeter optimization [end]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Systematic sensitivity analysis after preventive remedial actions: cost = 790.43 (functional: 140.65, virtual: 649.78)
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #01: margin = -140.65 MW, element FFR2AA1  FFR3AA1  1 at state preventive, CNEC ID = "FR2-FR3-O - preventive"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #02: margin = -32.49 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - auto, CNEC ID = "NL2-BE3-O - auto"
~~~

This concludes the preventive RAO.  
The RAO applies the optimal preventive remedial actions on the network then re-assesses it against all CNEC constraints 
in the CRAC.  
In this case the most limiting element is still the preventive CNEC detected by the RAO (so we don't have any hope of 
improving its margin anymore), but the second most limiting element is an auto CNEC that was not in the preventive 
perimeter: "NL2-BE3-O - auto".  
Most importantly, a virtual cost is created: the preventive remedial actions must have violated a soft constraint (e.g. 
MNEC or loop-flow constraint) without noticing so, because the constraint was on a CNEC of the AUTO instant.  
The following steps will have to take care of this.

## Contingency scenarios

Every contingency is treated separately:
- Each auto instant is simulated to select triggered automatons (if AUTO CNECs and remedial actions exist)
- Each curative instant is optimised in a search-tree to select best curative actions

In this case, the CRAC only defines one contingency: "Contingency_FR1_FR3".
As many contingency scenarios are created as there are contingencies in the CRAC (with associated [remedial actions](../../algorithms/castor/rao-steps.md#extension-of-the-preventive-perimeter-to-the-curative-situation)).

~~~
INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- Post-contingency perimeters optimization [start]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Using base network 'TestCase12Nodes2PSTs' on variant 'ContingencyScenario944e605c-e5df-4d31-a06e-7df90fa4517c'
INFO  c.p.openrao.commons.logs.TechnicalLogs - Optimizing scenario post-contingency Contingency_FR1_FR3.
~~~

### Automatons simulation

Automatons are triggered with regard to their usage rules.

~~~
INFO  c.p.openrao.commons.logs.TechnicalLogs - Optimizing automaton state Contingency_FR1_FR3 - auto.
INFO  c.p.openrao.commons.logs.TechnicalLogs - Initial situation:
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #01: margin = -32.49 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - auto, CNEC ID = "NL2-BE3-O - auto"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Topological automaton state Contingency_FR1_FR3 - auto has been skipped as no topological automatons were activated.
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Shifting setpoint from 0.0 to 1.5583491300758083 on range action(s) ARA_PST_DE to improve margin on cnec NL2-BE3-O - auto on side ONE (initial margin : -32.489056200328605 MW).
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #01: margin = 6.96 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - auto, CNEC ID = "NL2-BE3-O - auto"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Running pre curative sensi after auto state Contingency_FR1_FR3 - auto.
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #01: margin = 6.96 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - auto, CNEC ID = "NL2-BE3-O - auto"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Automaton state Contingency_FR1_FR3 - auto has been optimized.
INFO  c.p.o.commons.logs.RaoBusinessLogs - Scenario "Contingency_FR1_FR3": 1 range action(s) activated, cost after ARA = -1549.84 (functional: -1549.84, virtual: 0.00)
~~~

In this case, automatic PST "ARA_PST_DE" is triggered in order to secure its associated CNEC "NL2-BE3-O - auto", that 
has a negative margin of -32MW initially.  
The OpenRAO automaton simulator shifts the PST's tap (using sensitivity values) until the CNEC is secured or the PST 
reaches its limit tap. In this case, shifting the PST's set-point from 0.0 to 1.56 (in radians) is enough to increase 
the margin on the CNEC from -32 to +7 MW.  

### Curative remedial actions optimisation

The curative search-tree starts by assessing the most limiting element of the curative perimeter.  
Here, it is the same network element as in the auto perimeter with a margin of +7MW.  
The search-tree behaves exactly like the preventive search-tree, but using curative remedial actions defined in the CRAC. 

~~~
INFO  c.p.openrao.commons.logs.TechnicalLogs - Optimizing curative state Contingency_FR1_FR3 - curative.
INFO  c.p.openrao.commons.logs.TechnicalLogs - Evaluating root leaf
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Leaf has already been evaluated
INFO  c.p.openrao.commons.logs.TechnicalLogs - Root leaf, range actions have not been optimized, cost: -6.96 (functional: -6.96, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #01: margin = 6.96 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - curative, CNEC ID = "NL2-BE3-O - curative"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Linear optimization on root leaf
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Optimizing leaf...
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: linear optimization [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: linear optimization [end]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: better solution found with a cost of -223.77 (functional: -223.77)
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: linear optimization [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: linear optimization [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: same results as previous iterations, optimal solution found
INFO  c.p.openrao.commons.logs.TechnicalLogs - Root leaf, 1 range action(s) activated, cost: -223.77 (functional: -223.77, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - range action(s): PRA_CRA_PST_BE: 16
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #01: margin = 223.77 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - curative, CNEC ID = "NL2-BE3-O - curative"
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluating 1 leaves in parallel
INFO  c.p.openrao.commons.logs.TechnicalLogs - Using base network 'TestCase12Nodes2PSTs' on variant 'OpenRaoNetworkPool working variant c43f9fb6-2502-40d8-b99f-63c4598954c6'
INFO  c.p.openrao.commons.logs.TechnicalLogs - Search depth 1 [start]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Leaves to evaluate: 1
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluating network action(s): Open line FR1-FR2
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluated network action(s): Open line FR1-FR2, range actions have not been optimized, cost: -7.61 (functional: -7.61, virtual: 0.00)
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Optimizing leaf...
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: linear optimization [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: linear optimization [end]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Iteration 1: better solution found with a cost of -224.34 (functional: -224.34)
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: linear optimization [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: linear optimization [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Iteration 2: same results as previous iterations, optimal solution found
INFO  c.p.openrao.commons.logs.TechnicalLogs - Optimized network action(s): Open line FR1-FR2, 1 range action(s) activated, cost: -224.34 (functional: -224.34, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Remaining leaves to evaluate: 0
INFO  c.p.openrao.commons.logs.TechnicalLogs - No better result found in search depth 1, exiting search tree
INFO  c.p.openrao.commons.logs.TechnicalLogs - Search-tree RAO completed with status DEFAULT
INFO  c.p.openrao.commons.logs.TechnicalLogs - Best leaf: Root leaf, 1 range action(s) activated, cost: -223.77 (functional: -223.77, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Best leaf: range action(s): PRA_CRA_PST_BE: 16
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #01: margin = 223.77 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - curative, CNEC ID = "NL2-BE3-O - curative"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Scenario "Contingency_FR1_FR3": initial cost = -6.96 (functional: -6.96, virtual: 0.00), 1 range action(s) activated, cost after CRA = -223.77 (functional: -223.77, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Curative state Contingency_FR1_FR3 - curative has been optimized.
INFO  c.p.openrao.commons.logs.TechnicalLogs - Remaining post-contingency scenarios to optimize: 0
INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- Post-contingency perimeters optimization [end]
~~~

In this case, setting "PRA_CRA_PST_BE" to tap +16 produces the best result with a minimum margin, on this curative 
perimeter, increased from +7 to +224 MW.

## Second preventive RAO

After optimising preventive remedial actions, simulating automatons, and optimising curative remedial actions, the 
network situation is re-assessed after applying these actions on it.  
Then a second preventive RAO is executed.  
First, the RAO applies only automatons and curative actions on the network and assesses the constraints.

~~~
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis with applied RA [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - ... (1/3) 2 state(s) without RA 
DEBUG c.p.openrao.commons.logs.TechnicalLogs - ... (2/3) state with RA Contingency_FR1_FR3 - auto
DEBUG c.p.openrao.commons.logs.TechnicalLogs - ... (3/3) state with RA Contingency_FR1_FR3 - curative
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis with applied RA [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Systematic sensitivity analysis after curative remedial actions before second preventive optimization: cost = 592.14 (functional: 203.22, virtual: 388.93)
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #01: margin = -203.22 MW, element NNL1AA1  NNL3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "NL1-NL3-D - outage"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #02: margin = -122.73 MW, element FFR2AA1  FFR3AA1  1 at state preventive, CNEC ID = "FR2-FR3-O - preventive"
~~~

It finds that applying these RAs only results in a minimum margin of -203.22 MW on "NL1-NL3-D - outage", but also in a 
virtual cost (i.e. soft constraint violation) of 389 (equivalent) megawatts.  

A new preventive search-tree is executed, like the first one, but taking into account all CNECs as well as applied 
automatic and curative actions.  

(Note that the original virtual cost is due to applied preventive PST that is not re-optimised, because it is also 
curative, and re-optimising curative PSTs is not enabled in this instance)

~~~
INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- Second preventive perimeter optimization [start]
WARN  c.p.o.commons.logs.RaoBusinessWarns - Range action PRA_CRA_PST_BE will not be considered in 2nd preventive RAO as it is also curative (or its network element has an associated CRA)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Evaluating root leaf
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Leaf has already been evaluated
INFO  c.p.openrao.commons.logs.TechnicalLogs - Root leaf, range actions have not been optimized, cost: 592.14 (functional: 203.22, virtual: 388.93)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #01: margin = -203.22 MW, element NNL1AA1  NNL3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "NL1-NL3-D - outage"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #02: margin = -122.73 MW, element FFR2AA1  FFR3AA1  1 at state preventive, CNEC ID = "FR2-FR3-O - preventive"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Linear optimization on root leaf
INFO  c.p.openrao.commons.logs.TechnicalLogs - No range actions to optimize
INFO  c.p.o.commons.logs.RaoBusinessLogs - Root leaf, range actions have not been optimized, cost: 592.14 (functional: 203.22, virtual: 388.93)
INFO  c.p.openrao.commons.logs.TechnicalLogs - range action(s): 
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #01: margin = -203.22 MW, element NNL1AA1  NNL3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "NL1-NL3-D - outage"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #02: margin = -122.73 MW, element FFR2AA1  FFR3AA1  1 at state preventive, CNEC ID = "FR2-FR3-O - preventive"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Root leaf, limiting "mnec-cost" constraint #01: flow = -1610.04 MW, threshold = -1590.02 MW, margin = -20.02 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "NL2-BE3-O - outage", CNEC name = "NL2-BE3"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Root leaf, limiting "mnec-cost" constraint #02: flow = -1568.71 MW, threshold = -1549.84 MW, margin = -18.87 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - auto, CNEC ID = "NL2-BE3-O - auto", CNEC name = "NL2-BE3"
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluating 1 leaves in parallel
INFO  c.p.openrao.commons.logs.TechnicalLogs - Using base network 'TestCase12Nodes2PSTs' on variant 'SecondPreventiveScenario'
INFO  c.p.openrao.commons.logs.TechnicalLogs - Search depth 1 [start]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Leaves to evaluate: 2
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluating network action(s): Open line FR1-FR2
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis with applied RA [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - ... (1/3) 2 state(s) without RA 
DEBUG c.p.openrao.commons.logs.TechnicalLogs - ... (2/3) state with RA Contingency_FR1_FR3 - auto
DEBUG c.p.openrao.commons.logs.TechnicalLogs - ... (3/3) state with RA Contingency_FR1_FR3 - curative
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis with applied RA [end]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluated network action(s): Open line FR1-FR2, range actions have not been optimized, cost: 1577.69 (functional: 1246.11, virtual: 331.58)
INFO  c.p.openrao.commons.logs.TechnicalLogs - No range actions to optimize
INFO  c.p.o.commons.logs.RaoBusinessLogs - Optimized network action(s): Open line FR1-FR2, range actions have not been optimized, cost: 1577.69 (functional: 1246.11, virtual: 331.58)
INFO  c.p.o.commons.logs.RaoBusinessLogs - Optimized network action(s): Open line FR1-FR2, limiting "mnec-cost" constraint #01: flow = -1565.86 MW, threshold = -1549.84 MW, margin = -16.02 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - auto, CNEC ID = "NL2-BE3-O - auto", CNEC name = "NL2-BE3"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Optimized network action(s): Open line FR1-FR2, limiting "mnec-cost" constraint #02: flow = -1607.16 MW, threshold = -1590.02 MW, margin = -17.14 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "NL2-BE3-O - outage", CNEC name = "NL2-BE3"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Remaining leaves to evaluate: 1
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluating network action(s): Open line NL1-NL2
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis with applied RA [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - ... (1/3) 2 state(s) without RA 
DEBUG c.p.openrao.commons.logs.TechnicalLogs - ... (2/3) state with RA Contingency_FR1_FR3 - auto
DEBUG c.p.openrao.commons.logs.TechnicalLogs - ... (3/3) state with RA Contingency_FR1_FR3 - curative
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis with applied RA [end]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluated network action(s): Open line NL1-NL2, range actions have not been optimized, cost: 140.65 (functional: 140.65, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - No range actions to optimize
INFO  c.p.o.commons.logs.RaoBusinessLogs - Optimized network action(s): Open line NL1-NL2, range actions have not been optimized, cost: 140.65 (functional: 140.65, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Remaining leaves to evaluate: 0
INFO  c.p.openrao.commons.logs.TechnicalLogs - Search depth 1 [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Search depth 1 best leaf: network action(s): Open line NL1-NL2, range actions have not been optimized, cost: 140.65 (functional: 140.65, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Search depth 1 best leaf: range action(s): 
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #01: margin = -140.65 MW, element FFR2AA1  FFR3AA1  1 at state preventive, CNEC ID = "FR2-FR3-O - preventive"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #02: margin = 0.22 MW, element NNL1AA1  NNL3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "NL1-NL3-D - outage"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Search depth 2 [start]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Leaves to evaluate: 1
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluating network action(s): Open line NL1-NL2, Open line FR1-FR2
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis with applied RA [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - ... (1/3) 2 state(s) without RA 
DEBUG c.p.openrao.commons.logs.TechnicalLogs - ... (2/3) state with RA Contingency_FR1_FR3 - auto
DEBUG c.p.openrao.commons.logs.TechnicalLogs - ... (3/3) state with RA Contingency_FR1_FR3 - curative
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis with applied RA [end]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Evaluated network action(s): Open line NL1-NL2, Open line FR1-FR2, range actions have not been optimized, cost: 1280.34 (functional: 1280.34, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - No range actions to optimize
INFO  c.p.o.commons.logs.RaoBusinessLogs - Optimized network action(s): Open line NL1-NL2, Open line FR1-FR2, range actions have not been optimized, cost: 1280.34 (functional: 1280.34, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Remaining leaves to evaluate: 0
INFO  c.p.o.commons.logs.RaoBusinessLogs - No better result found in search depth 2, exiting search tree
INFO  c.p.openrao.commons.logs.TechnicalLogs - Search-tree RAO completed with status DEFAULT
INFO  c.p.openrao.commons.logs.TechnicalLogs - Best leaf: network action(s): Open line NL1-NL2, range actions have not been optimized, cost: 140.65 (functional: 140.65, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Best leaf: range action(s): PRA_CRA_PST_BE: -6
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #01: margin = -140.65 MW, element FFR2AA1  FFR3AA1  1 at state preventive, CNEC ID = "FR2-FR3-O - preventive"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #02: margin = 0.22 MW, element NNL1AA1  NNL3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "NL1-NL3-D - outage"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #03: margin = 38.37 MW, element FFR2AA1  DDE3AA1  1 at state preventive, CNEC ID = "FR2-DE3-D - preventive"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #04: margin = 82.50 MW, element FFR2AA1  FFR3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "FR2-FR3-OO - outage"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #05: margin = 223.77 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - curative, CNEC ID = "NL2-BE3-O - curative"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Scenario "preventive": initial cost = 592.14 (functional: 203.22, virtual: 388.93), 1 network action(s) activated, cost after PRA = 140.65 (functional: 140.65, virtual: 0.00)
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- Second preventive perimeter optimization [end]
~~~

The search-tree now finds that the optimal preventive remedial action combination is still the same.  
Note that when the search-tree RAO detects a virtual, it logs details about its cause. 

## Second automaton simulation

As automatons are only activated if associated CNECs are constrained, and since second preventive RAO might have changed 
preventive actions, automaton simulation is re-run.  
In this case, second preventive RAO did not change preventive actions, so the second automaton simulation behaves in 
the same way as the first.

~~~
INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- Second automaton simulation [start]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Using base network 'TestCase12Nodes2PSTs' on variant 'ContingencyScenarioaa4a095c-b400-4008-a9f0-894073339fd3'
INFO  c.p.openrao.commons.logs.TechnicalLogs - Optimizing scenario post-contingency Contingency_FR1_FR3.
INFO  c.p.openrao.commons.logs.TechnicalLogs - Optimizing automaton state Contingency_FR1_FR3 - auto.
INFO  c.p.openrao.commons.logs.TechnicalLogs - Initial situation:
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #01: margin = -32.49 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - curative, CNEC ID = "NL2-BE3-O - curative"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Topological automaton state Contingency_FR1_FR3 - auto has been skipped as no topological automatons were activated.
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Shifting setpoint from 0.0 to 1.5583491300758083 on range action(s) ARA_PST_DE to improve margin on cnec NL2-BE3-O - auto on side ONE} (initial margin : -32.489056200328605 MW).
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #01: margin = 6.96 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - curative, CNEC ID = "NL2-BE3-O - curative"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Running pre curative sensi after auto state Contingency_FR1_FR3 - auto.
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis [end]
INFO  c.p.openrao.commons.logs.TechnicalLogs - Limiting element #01: margin = 6.96 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - curative, CNEC ID = "NL2-BE3-O - curative"
INFO  c.p.openrao.commons.logs.TechnicalLogs - Automaton state Contingency_FR1_FR3 - auto has been optimized.
INFO  c.p.o.commons.logs.RaoBusinessLogs - Scenario "Contingency_FR1_FR3": 1 range action(s) activated, cost after ARA = -1549.84 (functional: -1549.84, virtual: 0.00)
INFO  c.p.openrao.commons.logs.TechnicalLogs - Remaining post-contingency scenarios to optimize: 0
INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- Second automaton simulation [end]
~~~

## Final results

Finally, the results from all the steps are merged and most limiting elements after remdial actions are logged.  
In this case, as expected, the most limiting CNEC is "FR2-FR3-O - preventive" with a margin of -140MW. 

~~~
INFO  c.p.o.commons.logs.RaoBusinessLogs - Merging first, second preventive and post-contingency RAO results:
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis with applied RA [start]
DEBUG c.p.openrao.commons.logs.TechnicalLogs - ... (1/3) 2 state(s) without RA 
DEBUG c.p.openrao.commons.logs.TechnicalLogs - ... (2/3) state with RA Contingency_FR1_FR3 - auto
DEBUG c.p.openrao.commons.logs.TechnicalLogs - ... (3/3) state with RA Contingency_FR1_FR3 - curative
DEBUG c.p.openrao.commons.logs.TechnicalLogs - Systematic sensitivity analysis with applied RA [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #01: margin = -140.65 MW, element FFR2AA1  FFR3AA1  1 at state preventive, CNEC ID = "FR2-FR3-O - preventive"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #02: margin = 0.22 MW, element NNL1AA1  NNL3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "NL1-NL3-D - outage"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #03: margin = 38.37 MW, element FFR2AA1  DDE3AA1  1 at state preventive, CNEC ID = "FR2-DE3-D - preventive"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #04: margin = 82.50 MW, element FFR2AA1  FFR3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "FR2-FR3-OO - outage"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #05: margin = 223.77 MW, element NNL2AA1  BBE3AA1  1 at state Contingency_FR1_FR3 - curative, CNEC ID = "NL2-BE3-O - curative"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #06: margin = 582.50 MW, element FFR2AA1  DDE3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "FR2-DE3-DO - outage"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #07: margin = 1000.22 MW, element NNL1AA1  NNL3AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "NL1-NL3-O - outage"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #08: margin = 1153.77 MW, element DDE1AA1  DDE2AA1  1 at state preventive, CNEC ID = "DE1-DE2-O - preventive"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #09: margin = 1639.35 MW, element DDE1AA1  DDE2AA1  1 at state Contingency_FR1_FR3 - outage, CNEC ID = "DE1-DE2-OO - outage"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #10: margin = 1846.14 MW, element DDE1AA1  DDE2AA1  1 at state preventive, CNEC ID = "DE1-DE2-D - preventive"
INFO  c.p.o.commons.logs.RaoBusinessLogs - Cost before RAO = 182.55 (functional: 182.55, virtual: 0.00), cost after RAO = 140.65 (functional: 140.65, virtual: 0.00)
~~~
