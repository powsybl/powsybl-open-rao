# Modelling the minimum cost objective

## Used input data


| Name               | Symbol                   | Details                                                                                                                                                                                                                             |
|--------------------|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| OptimisedFlowCnecs | $c \in \mathcal{C} ^{o}$ | Set of FlowCnecs[^1] which are ['optimised'](/input-data/crac/json.md#optimised-and-monitored-cnecs). OptimisedFlowCnecs is a subset of [FlowCnecs](core-problem-filler.md#used-input-data): $\mathcal{C} ^{o} \subset \mathcal{C}$ |
| RangeActions       | $r \in \mathcal{RA(s)}$  | set of RangeActions available on state $s$, could be PSTs, HVDCs, or injection range actions                                                                                                                                        |
| upper threshold    | $f^{+}_{threshold} (c)$  | Upper threshold of FlowCnec $c$, in MW, as defined in the CRAC                                                                                                                                                                      |
| lower threshold    | $f^{-}_{threshold} (c)$  | Lower threshold of FlowCnec $c$, in MW, defined in the CRAC                                                                                                                                                                         |
| activation cost    | $ac(r)$                  | Activation cost of RangeAction $r$, defined in the CRAC                                                                                                                                                                             |

[^1]: CNECs that belong to a state for which sensitivity computations failed are ignored in the MILP

## Used parameters

| Name                         | Details                                                                                                     |
|------------------------------|-------------------------------------------------------------------------------------------------------------|
| [type](/parameters.md#type)  | Used to set the unit (AMPERE/MW) of the flow values. The unit is only relevent for logging the worst CNECs. |

## Defined optimization variables

| Name             | Symbol   | Details                                                                                                                      | Type        | Index                                            | Unit                                                                                                | Lower bound | Upper bound |
|------------------|----------|------------------------------------------------------------------------------------------------------------------------------|-------------|--------------------------------------------------|-----------------------------------------------------------------------------------------------------|-------------|-------------|
| Minimum margin   | $MM$     | The minimum margin over all OptimizedFlowCnecs. Serve as a penalty if margin is negative. Set to 0 if all Cnecs are secure.  | Real value  | one scalar variable for the whole problem        | MW or AMPERE (depending on [objective-function](/parameters.md#objective-function-parameters) unit) | $-\infty$   | 0           |
| Total cost       | $TC$     | The total cost for all RangeActions.                                                                                         | Real value  | one scalar variable for the whole problem        |                                                                                                     | 0           | $+\infty$   |
| RangeAction cost | $C(r)$   | The cost for one RangeAction.                                                                                                | Real value  | one variable for every element of (RangeActions) |                                                                                                     | 0           | $+\infty$   |

## Used optimization variables

| Name                           | Symbol          | Defined in                                                                  |
|--------------------------------|-----------------|-----------------------------------------------------------------------------|
| Flow                           | $F(c)$          | [CoreProblemFiller](core-problem-filler.md#defined-optimization-variables)  |
| RA setpoint absolute variation | $\Delta A(r,s)$ | [CoreProblemFiller](core-problem-filler.md#defined-optimization-variables)  |

## Defined constraints

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

$MM$ is non-positive, so if all CNECs are safe, the value of $MM$ will be $0$

### Define the RangeAction cost

$$
\begin{equation}
C(r) = ac(r) * \Delta A(r,s)
\end{equation}
$$

The cost of a RangeAction depends on the AbsoluteVariation of the setpoint.  

### Define the total cost

$$
\begin{equation}
TC = \sum_{r \in \mathcal{RA(s)}}C(r)
\end{equation}
$$

## Contribution to the objective function

The total cost should be minimized, with a penalty added if min margin is negative:  

$$
\begin{equation}
\min (TC - 1000 * MM) 
\end{equation}
$$

The penalty coefficient was arbitrary chosen to be $1000$. The coefficient needs to be high enough so that any solution that secures the network would have a lower cost that an unsecured network.   