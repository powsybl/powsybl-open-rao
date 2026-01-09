# Modelling loop-flows and their virtual cost

## Used input data

| Name                      | Symbol                    | Details                                                                                                                                                                                                                                                  |
|---------------------------|---------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| LoopFlowCnecs             | $c \in \mathcal{C} ^{lf}$ | Set of FlowCnecs[^1] with a loop-flow threshold. (for example, in CORE CC, loop-flows are monitored on cross-border CNECs). LoopFlowCnecs is a subset of [FlowCnecs](../core-problem-filler.md#used-input-data): $\mathcal{C} ^{lf} \subset \mathcal{C}$ |
| Reference commercial flow | $f^{commercial} (c)$      | Commercial flow[^2], of LoopFlowCnec $c$, at the beginning of the optimization, in [flow's unit](../../../../parameters/business-parameters.md#objective-function-parameters).                                                                           |
| initial loop-flow         | $f^{loop} _ {0} (c)$      | loop-flow before RAO of LoopFlowCnec $c$, in [flow's unit](../../../../parameters/business-parameters.md#objective-function-parameters)                                                                                                                  |
| loop-flow threshold       | $lf^{threshold} (c)$      | loop-flow threshold of the LoopFlowCnec $c$, in [flow's unit](../../../../parameters/business-parameters.md#objective-function-parameters), as defined in the CRAC.                                                                                      |

[^1]: CNECs that belong to a state for which sensitivity computations failed are ignored in the MILP  
[^2]: The commercial flow is computed oustide the MILP, see [loop-flow computation](../../special-features/loop-flows.md#computation)

## Used parameters

| Name                                                                                                                                | Symbol                  | Details                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|-------------------------------------------------------------------------------------------------------------------------------------|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [acceptable-increase](../../../../parameters/business-parameters.md#acceptable-increase)                                            | $c^{acc-increase}_{lf}$ | The increase of the initial loop-flow that is allowed by the optimisation, see [loop-flow-acceptable-increase](../../../../parameters/business-parameters.md#acceptable-increase).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| [constraint-adjustment-coefficient](../../../../parameters/implementation-specific-parameters.md#constraint-adjustment-coefficient) | $c^{adj-coeff}_{lf}$    | This parameter acts as a margin that tightens the loop-flow constraints bounds in the linear problem. It conceptually behaves as the coefficient $c^{adjustment}$ from the constraint below: <br> $abs(F_{loop-flow}(c)) <= lf^{threshold} (c) - c^{adjustment}$ <br> This parameter is a safety margin which can absorb some of the approximations  made in the linear optimization problem such as non integer PST taps, flows approximated by sensitivity coefficients, etc. It therefore increases the probability that the loop-flow constraints respected in the linear optimisation problem, remain respected once the loop-flows are re-computed without the linear approximations. |
| [violation-cost](../../../../parameters/implementation-specific-parameters.md#violation-cost)                                       | $c^{penalty}_{lf}$      | penalisation, in the objective function, of the excess of 1 MW or A of loop-flow                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |

## Defined optimization variables

| Name             | Symbol       | Details                                                                                                                                                                                      | Type       | Index                                                  | Unit                                                                                        | Lower bound | Upper bound |
|------------------|--------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|--------------------------------------------------------|---------------------------------------------------------------------------------------------|-------------|-------------|
| loop-flow excess | $S^{lf} (c)$ | Slack variable for loop-flow constraint of FlowCnec c. <br> Defines the amount of MW or A by which a loop-flow constraint has been violated. <br> This makes the loop-flow constraints soft. | Real value | One variable for every element of  $\mathcal{C} ^{lf}$ | [flow's unit](../../../../parameters/business-parameters.md#objective-function-parameters)  | 0           | $+\infty$   |

## Used optimization variables

| Name | Symbol | Defined in                                                                    |
|------|--------|-------------------------------------------------------------------------------|
| Flow | $F(c)$ | [CoreProblemFiller](../core-problem-filler.md#defined-optimization-variables) |

## Defined constraints

> 💡 Loop-flows constraints are considered in the same unit as the [flow's unit](../../../../parameters/business-parameters.md#objective-function-parameters) in the MIP

### Keeping the loop-flows within their bounds

<br>

$$
\begin{equation}
F(c) - f^{commercial} (c) \leq \overline{f^{loop} (c)} + S^{lf} (c), \forall c \in \mathcal{C} ^{lf}
\end{equation}
$$

$$
\begin{equation}
F(c) - f^{commercial} (c) \geq - \overline{f^{loop} (c)} - S^{lf} (c), \forall c \in \mathcal{C} ^{lf}
\end{equation}
$$

<br>

With $\overline{f^{loop} (c)}$ the loop-flow threshold, constant defined as:

$$
\begin{matrix}
\overline{f^{loop} (c)} = \max(lf^{threshold} (c) - c^{adj-coeff}_{lf} \:, \: \: |f^{loop} _ {0} (c)| +
c^{acc-increase}_{lf} - c^{adj-coeff}_{lf} \:, \: \: |f^{loop} _ {0} (c)|)
\end{matrix}
$$

The two first terms of the max define the actual loop-flow upper bound:

- either as the threshold defined in the CRAC,
- or as the initial loop-flow value of the FlowCnec, on which the acceptable increase coefficient is added

The last term ensures that the initial situation is always feasible, whatever the configuration parameters.

## Contribution to the objective function

Penalisation of the loop-flow excess in the objective function:

$$
\begin{equation}
\min (c^{penalty}_{lf} \sum_{c \in \mathcal{C} ^{lf}} S^{lf} (c))
\end{equation}
$$

This penalisation is part of the virtual cost.