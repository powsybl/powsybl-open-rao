# Modelling the Power Gradient Constraints

## Used input data

| Name                            | Symbol               | Details                                                                                                                                     |
|---------------------------------|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| Power gradient constraints set  | $\Gamma$             | Set of power gradient constraints                                                                                                           |
| Upper power gradient constraint | $\nabla p^{+}(g)$    | Maximum upward power variation between two consecutive timestamps for generator $g$. This value must be non-negative.                       |
| Lower power gradient constraint | $\nabla p^{-}(g)$    | Maximum downward power variation (in absolute value) between two consecutive timestamps for generator $g$. This value must be non-positive. |
| Initial generator power         | $p_{0}(g,s,t)$       | Initial power of generator $g$ at state $s$ of timestamp $t$, as defined in the network.                                                    |
| Timestamps                      | $\mathcal{T}$        | Set of all timestamps on which the optimization is performed.                                                                               |
| Time gap                        | $\Delta_t(t, t + 1)$ | Time gap between consecutive timestamps $t$ and $t + 1$.                                                                                    |

## Defined optimization variables

| Name            | Symbol     | Details                                                     | Type       | Index                                                                               | Unit | Lower bound | Upper bound |
|-----------------|------------|-------------------------------------------------------------|------------|-------------------------------------------------------------------------------------|------|-------------|-------------|
| Generator power | $P(g,s,t)$ | the power of generator $g$ at state at $s$ of timestamp $t$ | Real value | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | MW   | $-\infty$   | $+\infty$   |

## Used optimization variables

| Name                                      | Symbol              | Defined in                                                                 |
|-------------------------------------------|---------------------|----------------------------------------------------------------------------|
| Range Action upward set-point variation   | $\Delta^{+}(r,s,t)$ | [CoreProblemFiller](core-problem-filler.md#defined-optimization-variables) |
| Range Action downward set-point variation | $\Delta^{-}(r,s,t)$ | [CoreProblemFiller](core-problem-filler.md#defined-optimization-variables) |

## Defined constraints

### Define the impact of injections on generator

For a given generator $g$ at state $s$ of timestamp $t \in \mathcal{T}$, we denote $\mathcal{I}(g,s,t)$ the set of [injection range actions](/input-data/crac/json.html#injection-range-action) defined at state $s$ for timestamp $t$ that act on $g$. For each such injection range action $i$, $d_{i}(g)$ denotes the distribution key of $g$ for this very remedial action. The power of $g$ at state $s$ is:

$$\forall g, t \quad  P(g,s,t) = p_{0}(g,s,t) + \sum_{i \in \mathcal{I}(g,s,t)} d_i(g) \left [ \Delta^{+}(r,s,t) - \Delta^{-}(r,s,t) \right ]$$

> This is only implemented for preventive injection range actions for the moment

### Define the power gradient constraint

$$\nabla p^{-}(g) * \Delta_t(t, t + 1) \leq P(g,s,t + 1) - P(g,s',t)\leq \nabla p^{+}(g) * \Delta_t(t, t + 1)$$

> - If the constraint is only defined with an upper (resp. lower) gradient then the lower (resp. upper) bound of the constraint is $-\infty$ (resp. $\infty$).
> - $s'$ is always the preventive state of timestamp $t$ as curative states of different timestamps are independent