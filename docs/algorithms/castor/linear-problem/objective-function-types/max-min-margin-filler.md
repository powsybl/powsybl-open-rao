# Modelling the maximum minimum margin objective

## Used input data

| Name               | Symbol                   | Details                                                                                                                                                                                                                                           |
|--------------------|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| OptimisedFlowCnecs | $c \in \mathcal{C} ^{o}$ | Set of FlowCnecs[^1] which are ['optimised'](../../../../input-data/crac/json.md#optimised-and-monitored-cnecs). OptimisedFlowCnecs is a subset of [FlowCnecs](../core-problem-filler.md#used-input-data): $\mathcal{C} ^{o} \subset \mathcal{C}$ |
| upper threshold    | $f^{+}_{threshold} (c)$  | Upper threshold of FlowCnec $c$, in [flow unit](../../../../parameters/business-parameters.md#objective-function-parameters), as defined in the CRAC                                                                                              |
| lower threshold    | $f^{-}_{threshold} (c)$  | Lower threshold of FlowCnec $c$, in [flow unit](../../../../parameters/business-parameters.md#objective-function-parameters), defined in the CRAC                                                                                                 |
| nominal voltage    | $U_{nom}(c)$             | Nominal voltage of OptimizedFlowCnec $c$                                                                                                                                                                                                          |

[^1]: CNECs that belong to a state for which sensitivity computations failed are ignored in the MILP

## Defined optimization variables

| Name           | Symbol | Details                                        | Type       | Index                                     | Unit                                                                                                                 | Lower bound | Upper bound |
|----------------|--------|------------------------------------------------|------------|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------|-------------|-------------|
| Minimum margin | $MM$   | the minimum margin over all OptimizedFlowCnecs | Real value | one scalar variable for the whole problem | MW or AMPERE (depending on [flow unit](../../../../parameters/business-parameters.md#objective-function-parameters ) | $-\infty$   | $+\infty$   |

## Used optimization variables

| Name | Symbol | Defined in                                                                    |
|------|--------|-------------------------------------------------------------------------------|
| Flow | $F(c)$ | [CoreProblemFiller](../core-problem-filler.md#defined-optimization-variables) |

## Defined constraints

> 💡 Max Min Margin constraints are considered in the [flow unit](../../../../parameters/business-parameters.md#objective-function-parameters) in the MIP. 

### Define the minimum margin variable

$$
\begin{equation}
MM \leq f^{+}_{threshold} (c) - F(c), \forall c \in \mathcal{C} ^{o}
\end{equation}
$$  

$$
\begin{equation}
MM \leq F(c) - f^{-}_{threshold} (c), \forall c \in \mathcal{C} ^{o}
\end{equation}
$$  

Note that OptimizedFlowCnec might have only one threshold (upper or lower), in that case, only one of the two above constraints is defined.
<br>

## Contribution to the objective function

The minimum margin should be maximised:  

$$
\begin{equation}
\min (-MM)
\end{equation}
$$