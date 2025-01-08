# Modelling the Power Gradient Constraints

## Used input data

| Name                            | Symbol               | Details                                                                                                                                     |
|---------------------------------|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| Power gradient constraints set  | $\Gamma$             | Set of power gradient constraints                                                                                                           |
| Upper power gradient constraint | $\nabla p^{+}(g)$    | Maximum upward power variation between two consecutive timestamps for generator $g$. This value must be non-negative.                       |
| Lower power gradient constraint | $\nabla p^{-}(g)$    | Maximum downward power variation (in absolute value) between two consecutive timestamps for generator $g$. This value must be non-positive. |
| Initial generator power         | $p_{0}(g,t)$         | Initial power of generator $g$ at timestamp $t$, as defined in the network.                                                                 |
| Timestamps                      | $\mathcal{T}$        | Set of all timestamps on which the optimization is performed.                                                                               |
| Time gap                        | $\Delta_t(t, t + 1)$ | Time gap between consecutive timestamps $t$ and $t + 1$.                                                                                    |

## Defined optimization variables

| Name            | Symbol   | Details                                                             | Type       | Index                                                                    | Unit | Lower bound | Upper bound |
|-----------------|----------|---------------------------------------------------------------------|------------|--------------------------------------------------------------------------|------|-------------|-------------|
| Generator power | $P(g,t)$ | the power of generator $g$ in the preventive state at timestamp $t$ | Real value | one per generator defined in $\Gamma$ and per timestamp of $\mathcal{T}$ | MW   | $-\infty$   | $+\infty$   |

## Used optimization variables

| Name                                      | Symbol              | Defined in                                                                 |
|-------------------------------------------|---------------------|----------------------------------------------------------------------------|
| Range Action upward set-point variation   | $\Delta^{+}(r,s,t)$ | [CoreProblemFiller](core-problem-filler.md#defined-optimization-variables) |
| Range Action downward set-point variation | $\Delta^{-}(r,s,t)$ | [CoreProblemFiller](core-problem-filler.md#defined-optimization-variables) |

## Defined constraints

### Define the impact of injections on generator

For a given generator $g$ at timestamp $t \in \mathcal{T}$, we denote $\mathcal{I}_{\text{prev}}(g,t)$ the set of preventive [injection range actions](/input-data/crac/json.html#injection-range-action) defined at timestamp $t$ that act on $g$. For each such injection range action $i$, $d_{i}(g)$ denotes the distribution key of $g$ for this very remedial action. The power of $g$ in the preventive state is:

$$P(g,t) = p_{0}(g,t) + \sum_{i \in \mathcal{I}_{\text{prev}}(g,t)} d_i(g) \left [ \Delta^{+}(r,s,t) - \Delta^{-}(r,s,t) \right ], \forall g, t$$

### Define the power gradient constraint

$$\nabla p^{-}(g) * \Delta_t(t, t + 1) \leq P(g, t + 1) - P(g, t)\leq \nabla p^{+}(g) * \Delta_t(t, t + 1)$$

> If the constraint is only defined with an upper (resp. lower) gradient then the lower (resp. upper) bound of the constraint if $-\infty$ (resp. $\infty$).