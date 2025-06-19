# Multi-step Optimisation

## Introduction

To be able to face the combinatorial complexity of the problem, without having to simplify / linearize the impact of the
available remedial actions available, the problem is divided into preventive and curative perimeters.  
The definition of CNEC and associated usage rules allows to distinguish four temporal situations or instants in the
problem:

- **Before outage situation**: immediately before the occurrence of a fault.
- **After outage situation**: immediately after the occurrence of a fault and prior to the potential application
  of post-fault actions (if applicable).
- **After automatons**: grid situations which follow the application of automatic remedial actions (automatons)
  (if applicable) in the case of a particular contingency. *Note that this perimeter is simulated, not optimised*
- **After curative situations**: grid situations which follow the application of curative actions
  (if applicable) in the case of a particular contingency.

This leads to the consideration of different **optimisation perimeters**. Each perimeter calls for the application of one
set of remedial actions. The preventive perimeter is the first computed. Optimisation will try to maximise the objective
function value on this first perimeter (or just secure it, depending on the stop criterion). Then, each contingency in
curative defines one perimeter of curative optimisation. Optimisation will try to maximise the objective function value
of each of these perimeters (or just secure it, depending on the stop criterion).

## Preventive perimeter

The preventive perimeter is made up of:

- CNECs of the “before outage situation” defined with a limit of type PATL (**P**ermanent limit). This corresponds to
  the so-called “N state” situation.
- CNECs of the “after outage situation” defined with a limit of type **T**ATL (**T**emporary limit).

Overloads potentially reached on this perimeter are to be solved with preventive actions (PRA).

## Automaton perimeters

An automaton perimeter can be defined for each defined contingency. Each automaton perimeter is formed by CNECs of the
“after automatons” defined with a limit of type TATL. All CNECs are defined upon the same contingency.

Optimal preventive actions selected by the preventive RAO are applied in this perimeter.

While preventive and curative remedial actions are always optimized to prevent overloads in their respective perimeters,
auto remedial actions are actually forced in the automaton perimeter, depending on the usage method. Indeed, in real
life an "automated protection" is triggered when an outage takes place, even if its triggering causes another constraint
somewhere else in the network.
That's why, regardless of the impacts on CNECs, forced automatic remedial actions are activated if their specific
activation conditions are met (e.g. after a given contingency).
In CASTOR, this simulation is carried out in two stages, the first handling specifically the automatic remedial
actions for which there is not set-point to compute. These remedial actions are
called [network actions](/input-data/crac/json.md#network-actions). They include topological actions, injection
set-points, PST set-points, switch pairs.

#### Automatic network actions simulation

First, all automatic network actions with a FORCED usage method are applied on the network. FORCED means that no optimization is carried out and that as long as the triggering conditions are met, the remedial action is applied.

#### Automatic range actions simulation

Then, automatic range actions are applied one by one, as long as some of the perimeter's CNECs are overloaded. Range
actions include PSTs and HVDCs. The remedial actions' speed determines the order in which they are applied: the fastest
range actions are simulated first. Aligned range actions are simulated simultaneously.

> Note that auto range actions must have a FORCED usage method too.  

Automatic range actions' set-point is computed using the results of a sensitivity analysis computation, during which the
sensitivity $\sigma$ of a range action on a CNEC is computed. By focusing on the worse overloaded CNEC, we can compute
the automatic range action's optimal set-point to relieve that CNEC with the following formula:

$$\begin{equation}
A_{optimal} = A_{current} + {sign}{(F(c)}) * \frac{\min(0, margin(c))}{\sigma}
\end{equation}$$

| Name        | Symbol | Details                 | Type       | Index                                            | Unit                                                      | Lower bound           | Upper bound           |
|-------------|--------|-------------------------|------------|--------------------------------------------------|-----------------------------------------------------------|-----------------------|-----------------------|
| Flow        | $F(c)$ | flow of FlowCnec $c$    | Real value | One variable for every element of (FlowCnecs)    | MW                                                        | $-\infty$             | $+\infty$             |
| RA setpoint | $A$    | setpoint of RangeAction | Real value | One variable for every element of (RangeActions) | Degrees for PST range actions; MW for other range actions | Range lower bound[^1] | Range upper bound[^1] |

This formula is capped by the range actions' min and max setpoints: if the range action we're simulating doesn't achieve
relieving the worst CNEC, we'll push it to its min or max tap and then continue the simulation with the next fastest
ARA.

Once the worse overloaded CNEC is relieved, we carry on the simulation focusing on the next worse overloaded CNEC. As
soon as all CNECs are secure, we stop applying range actions.

N.B: a range action can only be shifted in one direction. If the previous formula implies moving a range action in the
opposite direction (compared to the one it's already been shifted in), we'll carry on the simulation with the next
fastest ARA.

The simulation can therefore stop for the following reasons:

- there are no more overloaded CNECs in the perimeter
- all ARAs have been made the most of
    - either because they have reached their min/max set-points
    - either because they have already been shifted in one direction and relieving remaining overloaded CNECs would
      require shifting these ARAs in the opposite direction
- a sensitivity analysis computation failed
- too many iterations have been performed (security stop criterion)

Unlike automatic network actions, automatic range actions can only be applied as long as CNECs are overloaded because we
need to determine the set-point on which they are applied.

Here is an example of the simulation of automatic range actions:

![Simulation_Of_Auto_Range_Actions](/_static/img/simulation_ara.png)

## Curative perimeters

Finally, a curative perimeter can be defined for each defined contingency. Each curative perimeter is formed by:

- CNECs of the “after curative situation” defined with a limit of type PATL. All CNECs defined upon the same
  contingency.

Overloads potentially reached on this perimeter are to be solved with curative actions.
Preventive and automatic actions already selected in the “after automatons” situation by the first two steps are applied
in this perimeter.

The separation between preventive and curative perimeters is justified by the fact that curative remedial actions
often suffice to solve the constraints which may appear in the curative situation (but were not already treated at
preventive stage due to a TATL higher than PATL).

Only TSOs owning curative actions allow CNECs to have a TATL which exceeds the PATL value. Operating the system with
curative actions allows to overload a line temporarily, thus allowing more flexibility in grid operations.

Operationally, curative actions are sized in order to overcome the difference between these two limits (TATL-PATL).  
While during preventive actions, only the TATL has to be respected (active flow should remain below this value),
curative actions (if properly sized), should be able to reduce the active flow in order to respect the PATL value.

![PATL vs TATL](/_static/img/patl-tatl.png){.forced-white-background}

When curative actions are not sized properly and are not sufficient to respect the two limits, CASTOR will then
investigate additional preventive remedial actions, by running
a [second preventive optimisation](#second-preventive-rao).  
When no curative actions are available to secure a given CNEC, CASTOR will investigate additional preventive remedial
actions by [extending the preventive perimeter](#extension-of-the-preventive-perimeter-to-the-curative-situation).

Therefore, preventive optimisation is more permissive with CNECs defined with a TATL larger than the PATL: this allows
preventive optimisation to focus preferably on CNECs with a low TATL (in particular where TSOs do not define curative
actions).  
When switching to the curative problem, the PATL applies, and the potential negative impact of previously found
preventive actions (limited by the consideration of TATL) should be overcome by the available curative actions.

In the example below, preventive actions applied might have a negative impact on a particular CNEC (if the objective
function value can be increased through the application of this action) when compared with the initial situation:

- The flow in “1- initial situation” is lower than in “2- after outage”, the margin is positive in both situations (PATL
  for 1, TATL for 2). But flow in situation 2 is higher than in situation 1 on this particular CNEC.
- When studying the flow in curative (“3- After curative”), PATL applies, and the CNEC is initially seen in overload
  against this limit due to applied preventive actions. Curative actions should then normally make the flow go below the
  PATL value.

![Different thresholds for different instants](/_static/img/curative1.png)

### Extension of the preventive perimeter to the curative situation

When a CNEC is monitored in a curative state for which no remedial action exists, this CNEC has no chance of being
explicitly secured by remedial actions other than preventive.  
The availability of curative actions is first assessed prior to the preventive optimisation:  
In the case where no curative action is available for a given CNEC which is to be optimised in the curative problem,
the curative CNEC will be treated directly within the preventive optimisation perimeter (outage 2 in figure below).  
As a result, the current limit considered in the preventive optimisation of this particular CNEC will be the PATL
and not the TATL.

This ensures that the optimisation avoids finding a preventive solution where the flow of this CNEC would lie between
the PATL and TATL value. If such preventive solution were selected, due to the unavailability of curative actions,
the flow in curative would remain higher than the PATL value, which would correspond to an overload on the grid element
(situation for Outage 2 in the figure below).

![Curative CNEC in preventive example](/_static/img/curative2.png)

### Multiple curative instants

TODO + schema or link towards schema in NC 


## Second preventive RAO

Given the sequential nature of CASTOR's search-tree algorithm, some constraints on curative CNECs that cannot be solved
by curative remedial actions are not seen by the preventive RAO.  
After the curative RAO is complete, the user has the possibility
to [run a second preventive RAO](/parameters.md#second-preventive-rao-parameters).  
The differences with the first preventive RAO are that, in this run:

- **all CNECs** are optimised, whatever their state;
- **automatic remedial actions** that were selected in the automaton perimeter are supposed activated, in order for
  the preventive RAO to focus on constraints that cannot be solved by automatons;
- **curative remedial actions** that were selected in the curative perimeter are supposed activated*, in order for
  the preventive RAO to focus on constraints that cannot be solved by curative actions.

_* It is possible to re-optimise curative range actions during second preventive RAO
using [this parameter](/parameters.md#re-optimize-curative-range-actions)._

![PATL vs TATL](/_static/img/rao_steps.png){.forced-white-background} 
