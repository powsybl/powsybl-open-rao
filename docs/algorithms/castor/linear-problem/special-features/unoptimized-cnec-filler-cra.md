# Modelling un-optimised CNECs (CRAs)

## Used input data

| Name                   | Symbol                  | Details                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|------------------------|-------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| FlowCnecs              | $c \in \mathcal{C}$     | Set of optimised FlowCnecs[^1]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| Upper threshold        | $f^{+}_{threshold} (c)$ | Upper threshold of FlowCnec $c$, in objective function's unit, defined in the CRAC                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| Lower threshold        | $f^{-}_{threshold} (c)$ | Lower threshold of FlowCnec $c$, in objective function's unit, defined in the CRAC                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| PrePerimeterMargin     | $RAM_{preperim}(c)$     | Pre-perimeter margin, for FlowCnec $c$. <br> The pre-perimeter margin is the margin before optimising (topo + range) RAs, which constitutes a threshold for the constraints of this filler. <br> Used in absolute MW or A (depending on objective function'unit) in this filler                                                                                                                                                                                                                                                                                                                     |
| operatorsNotToOptimize | $o\in \mathcal{UO}$     | These are the operators for which CNECs should not be "optimized". It means that those of these CNECs for which the margin improves (compared to the pre-perimeter margin) are not taken into account in the minimum margin maximization, and those for which the margin decreases are taken into account in the minimum margin maximization. <br> Note that this set is computed by OpenRAO for curative RAO only, by detecting operators that do not share any curative RA. <br> FlowCnecs belonging to these operators constitute a subset of FlowCnecs: $\mathcal{C} ^{uo} \subset \mathcal{C}$ |
| higestThresholdValue   | $MaxRAM$                | A "bigM" which is computed (by OpenRAO) as the greatest absolute possible value of the CNEC threshold, among all CNECs in the CRAC. <br> It represents the common greatest possible value for a given CNEC's margin (exception made of CNECs only constrained in one direction, but this value should be high enough not to have any effect on those).                                                                                                                                                                                                                                              |

[^1]: CNECs that belong to a state for which sensitivity computations failed are ignored in the MILP

## Used parameters

| Name                                                                                                                                                       | Details                                                                             |
|------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| [do-not-optimize-curative-cnecs-for-tsos-without-cras](../../../../parameters/business-parameters.md#do-not-optimize-curative-cnecs-for-tsos-without-cras) | This filler is only used if this parameter is activated, and only for curative RAO. |

## Defined optimization variables

| Name       | Symbol         | Details                                                                                                                                        | Type   | Index                                                                                                                              | Unit    | Lower bound | Upper bound |
|------------|----------------|------------------------------------------------------------------------------------------------------------------------------------------------|--------|------------------------------------------------------------------------------------------------------------------------------------|---------|-------------|-------------|
| DoOptimize | DoOptimize(c)$ | FlowCnec $c$ should be optimized. Equal to 1 if the margin is decreased compared to the pre-perimeter value (PrePerimeterMargin), 0 otherwise. | Binary | One variable for every element of (FlowCnecs) whose operator is in (operatorsNotToOptimize) <br> $\forall c \in \mathcal{C} ^{uo}$ | no unit | 0           | 1           |

## Used optimization variables

| Name                    | Symbol | Defined in                                                                                  |
|-------------------------|--------|---------------------------------------------------------------------------------------------|
| Flow                    | $F(c)$ | [CoreProblemFiller](../core-problem-filler.md#defined-optimization-variables)                  |
| Minimum margin          | $MM$   | [MaxMinMarginFiller](../objective-function-types/max-min-margin-filler.md#defined-optimization-variables)                  |
| Minimum relative margin | $MRM$  | [MaxMinRelativeMarginFiller](../objective-function-types/max-min-relative-margin-filler.md#defined-optimization-variables) |

## Defined constraints

> 💡 Unoptimized CNEC constraints are considered in the same unit as the objective function's unit in the MIP

### Defining the margin decrease variable

It should be equal to 1 if the optimizer wants to degrade the margin of a given CNEC.

$$
\begin{equation}
F(c) - f^{-}_{threshold} (c) \geq RAM_{preperim}(c) - worstMarginDecrease \times DoOptimize(c), \forall c \in
\mathcal{C} ^{uo}
\end{equation}
$$

$$
\begin{equation}
f^{+}_{threshold} (c) - F(c) \geq RAM_{preperim}(c) - worstMarginDecrease \times DoOptimize(c), \forall c \in
\mathcal{C} ^{uo}
\end{equation}
$$

Where $worstMarginDecrease$ represents the worst possible margin decrease, estimated as follows:

$$
\begin{equation}
worstMarginDecrease = 20 \times MaxRAM
\end{equation}
$$

*Note that no margin should be smaller than the worst margin computed above, otherwise it means the linear optimizer or
the search tree rao is degrading the situation. So we can safely use this to estimate the worst decrease possible of the
margins on cnecs.*

*Note that OptimizedFlowCnec might have only one threshold (upper or lower), in that case, only one of the two above
constraints is defined.*

<br>

### Updating the minimum margin constraints

(These are originally defined in [MaxMinMarginFiller](../objective-function-types/max-min-margin-filler.md#defined-constraints)
and [MaxMinRelativeMarginFiller](../objective-function-types/max-min-relative-margin-filler.md#defined-constraints))

For CNECs which should not be optimized, their RAM should not be taken into account in the minimum margin variable
unless their margin is decreased.

So we can release the minimum margin constraints if MarginDecrease is equal to 0. In order to do this, we just need to
add the following term to these constraints' right side:

$$
\begin{equation}
(1 - DoOptimize(c)) \times 2 \times MaxRAM, \forall c \in \mathcal{C} ^{uo}
\end{equation}
$$

*Note that this term should be divided by the absolute PTDF sum for relative margins, but it is not done explicitly in
the code because this coefficient is brought to the left-side of the constraint.*

<br>

## Contribution to the objective function

Given the updated constraints above, the "un-optimised CNECs" will no longer count in the minimum margin (thus in the
objective function) unless their margin is decreased.