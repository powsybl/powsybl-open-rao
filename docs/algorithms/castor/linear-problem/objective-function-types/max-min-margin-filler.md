# Modelling the maximum minimum margin objective

## Used input data

| Name               | Symbol                   | Details                                                                                                                                                                                                                                           |
|--------------------|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| OptimisedFlowCnecs | $c \in \mathcal{C} ^{o}$ | Set of FlowCnecs[^1] which are ['optimised'](../../../../input-data/crac/json.md#optimised-and-monitored-cnecs). OptimisedFlowCnecs is a subset of [FlowCnecs](../core-problem-filler.md#used-input-data): $\mathcal{C} ^{o} \subset \mathcal{C}$ |
| upper threshold    | $f^{+}_{threshold} (c)$  | Upper threshold of FlowCnec $c$, in MW, as defined in the CRAC                                                                                                                                                                                    |
| lower threshold    | $f^{-}_{threshold} (c)$  | Lower threshold of FlowCnec $c$, in MW, defined in the CRAC                                                                                                                                                                                       |
| nominal voltage    | $U_{nom}(c)$             | Nominal voltage of OptimizedFlowCnec $c$                                                                                                                                                                                                          |

[^1]: CNECs that belong to a state for which sensitivity computations failed are ignored in the MILP

## Used parameters

| Name                                                       | Details                                                    |
|------------------------------------------------------------|------------------------------------------------------------|
| [type](../../../../parameters/business-parameters.md#type) | Used to set the unit (AMPERE/MW) of the objective function |

## Defined optimization variables

| Name           | Symbol | Details                                        | Type       | Index                                     | Unit                                                                                                                               | Lower bound | Upper bound |
|----------------|--------|------------------------------------------------|------------|-------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|-------------|-------------|
| Minimum margin | $MM$   | the minimum margin over all OptimizedFlowCnecs | Real value | one scalar variable for the whole problem | MW or AMPERE (depending on [objective-function](../../../../parameters/business-parameters.md#objective-function-parameters) unit) | $-\infty$   | $+\infty$   |

## Used optimization variables

| Name | Symbol | Defined in                                                                    |
|------|--------|-------------------------------------------------------------------------------|
| Flow | $F(c)$ | [CoreProblemFiller](../core-problem-filler.md#defined-optimization-variables) |

## Defined constraints

### Define the minimum margin variable

#### If [objective-function](../../../../parameters/business-parameters.md#objective-function-parameters) is in MW

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

#### If [objective-function](../../../../parameters/business-parameters.md#objective-function-parameters) is in AMPERE

$$
\begin{equation}
MM \leq \frac{f^{+}_{threshold} (c) - F(c)}{c^{A->MW}(c)}, \forall c \in \mathcal{C} ^{o}
\end{equation}
$$  

$$
\begin{equation}
MM \leq \frac{F(c) - f^{-}_{threshold} (c)}{c^{A->MW}(c)}, \forall c \in \mathcal{C} ^{o}
\end{equation}
$$  

where $c^{A->MW}(c)$ is the conversion factor from AMPERE to MW of the FlowCnec c, calculated as below:  

$$
\begin{equation}
c^{A->MW}(c) = \frac{U_{nom}(c) \sqrt{3}}{1000}
\end{equation}
$$

There is a real difference between the two objective functions, as the most limiting OptimizedFlowCnec (i.e. the one which defines the minimum margin), can be different in MW and in A, depending on the nominal voltages Unom.
<br>


## Contribution to the objective function

The minimum margin should be maximised:  

$$
\begin{equation}
\min (-MM)
\end{equation}
$$