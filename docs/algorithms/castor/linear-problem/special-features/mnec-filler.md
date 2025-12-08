# Modelling MNECs and their virtual cost

## Used input data

| Name               | Symbol                   | Details                                                                                                                                                                                                                                           |
|--------------------|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| MonitoredFlowCnecs | $c \in \mathcal{C} ^{m}$ | Set of FlowCnecs[^1] which are ['monitored'](../../../../input-data/crac/json.md#optimised-and-monitored-cnecs). MonitoredFlowCnecs is a subset of [FlowCnecs](../core-problem-filler.md#used-input-data): $\mathcal{C} ^{o} \subset \mathcal{C}$ |
| Initial flow       | $f_{0} (c)$              | flow before RAO of MonitoredFlowCnec $c$, in objective function's unit                                                                                                                                                                            |
| Upper threshold    | $f^{+}_{threshold} (c)$  | Upper threshold of FlowCnec $c$, in objective function's unit, defined in the CRAC                                                                                                                                                                |
| Lower threshold    | $f^{-}_{threshold} (c)$  | Lower threshold of FlowCnec $c$, in objective function's unit, defined in the CRAC                                                                                                                                                                |

[^1]: CNECs that belong to a state for which sensitivity computations failed are ignored in the MILP

## Used parameters

| Name                                                                                                                                                    | Symbol              | Details                                                                                                                                                                                                                                                                                                                                                                             |
|---------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [mnec-acceptable-margin-decrease](../../../../parameters/business-parameters.md#acceptable-margin-decrease)                                             | $c^{acc-augm}_{m}$  | The decrease of the initial margin that is allowed by the optimisation on MNECs.                                                                                                                                                                                                                                                                                                    |
| [mnec-constraint-adjustment-coefficient](../../../../parameters/implementation-specific-parameters.md#mnec-violation-constraint-adjustment-coefficient) | $c^{adj-coeff}_{m}$ | This coefficient is here to mitigate the approximation made by the linear optimization (approximation = use of sensitivities to linearize the flows, rounding of the PST taps). <br> It tightens the MNEC constraint, in order to take some margin for that constraint to stay respected once the approximations are removed (i.e. taps have been rounded and real flow calculated) |
| [mnec-violation-cost](../../../../parameters/implementation-specific-parameters.md#mnec-violation-cost)                                                 | $c^{penalty}_{lf}$  | penalisation, in the objective function, of the excess of 1 MW or A of a MNEC flow                                                                                                                                                                                                                                                                                                  |

## Defined optimization variables

| Name        | Symbol      | Details                                                                                                                                                                      | Type       | Index                                                  | Unit                      | Lower bound | Upper bound |
|-------------|-------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|--------------------------------------------------------|---------------------------|-------------|-------------|
| MNEC excess | $S^{m} (c)$ | Slack variable for the MNEC constraint of FlowCnec c. <br> Defines the amount of MW by which a MNEC constraint has been violated. <br> This makes the MNEC constraints soft. | Real value | One variable for every element of (MonitoredFlowCnecs) | objective function's unit | 0           | $+\infty$   |

## Used optimization variables

| Name | Symbol | Defined in                                                                 |
|------|--------|----------------------------------------------------------------------------|
| Flow | $F(c)$ | [CoreProblemFiller](../core-problem-filler.md#defined-optimization-variables) |

## Defined constraints

### Keeping the MNEC margin positive or above its initial value

$$
\begin{equation}
F(c) \leq \overline{f(c)} + S^{m} (c) , \forall c \in \mathcal{C} ^{m}
\end{equation}
$$

$$
\begin{equation}
F(c) \geq \underline{f(c)} - S^{m} (c), \forall c \in \mathcal{C} ^{m}
\end{equation}
$$

*Note that MonitoredFlowCnec might have only one threshold (upper or lower), in that case, only one of the two above
constraints is defined.*

<br>

With $\overline{f(c)}$ and $\underline{f(c)}$ the bounds of the previous constraints, defined a below:

$$
\begin{matrix}
\overline{f(c)} = \max(f^{+}_{threshold} (c) - c^{adj-coeff}_{m} \:, \: \:
f_{0} (c) + c^{acc-augm}_{m} - c^{adj-coeff}_{m} \:)
\end{matrix}
$$

$$
\begin{matrix}
\underline{f(c)} = \min(f^{-}_{threshold} (c) + c^{adj-coeff}_{m} \:, \: \:
f_{0} (c) - c^{acc-augm}_{m} + c^{adj-coeff}_{m} \:)
\end{matrix}
$$

The first terms of the bounds define the actual MNEC flow limit:

- either equal to the threshold defined in the CRAC,
- or to the initial flow value of the FlowCnec, added to the acceptable margin diminution coefficient

<br>

## Contribution to the objective function

Penalisation of the MNEC excess in the objective function:

$$
\begin{equation}
\min (c^{penalty}_{m} \sum_{c \in \mathcal{C} ^{m}} S^{m} (c))
\end{equation}
$$