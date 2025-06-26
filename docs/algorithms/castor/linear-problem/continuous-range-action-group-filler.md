# Modelling aligned range actions

## Used input data

| Name                        | Symbol                       | Details                                                                                                                                                                                                                                                       |
|-----------------------------|------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ContinuousRangeActionGroups | $g \in \mathcal{G}^{c}_{RA}$ | Set of continuous RangeActionGroups. <br> Each RangeActionGroup contains a set of remedial actions, the remedial actions of the group have to be "aligned" between each other. $r \in \mathcal{RA}(g)$ <br> with: <br> $\mathcal{RA}(g) \subset \mathcal{RA}$ |

## Used parameters

| Name                                                                             | Details                                                            |
|----------------------------------------------------------------------------------|--------------------------------------------------------------------|
| [pst-model](../../../parameters/implementation-specific-parameters.md#pst-model) | This filler is used only if this parameters is set to *CONTINUOUS* |

## Defined optimization variables

| Name           | Symbol         | Details                       | Type       | Index                                                           | Unit                                                                 | Lower bound | Upper bound |
|----------------|----------------|-------------------------------|------------|-----------------------------------------------------------------|----------------------------------------------------------------------|-------------|-------------|
| Group set-point | $A^{group}(g)$ | The set-point of the group $g$ | Real value | One variable for every element of (ContinuousRangeActionGroups) | Degrees for PST range action groups; MW for HVDC range action groups | $-\infty$   | $+\infty$   |

## Used optimization variables

| Name        | Symbol | Defined in                                                                 |
|-------------|--------|----------------------------------------------------------------------------|
| RA set-point | $A(r)$ | [CoreProblemFiller](core-problem-filler.md#defined-optimization-variables) |

## Defined constraints

### Equality of the set-points of the RangeActions of the same group

$$
\begin{equation}
A^{group}(g) = A(r), \forall r \in \mathcal{RA}(g), \forall g \in \mathcal{G}^{c}_{RA}
\end{equation}
$$