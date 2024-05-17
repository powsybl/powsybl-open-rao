# Modelling un-optimised CNECs (PSTs)

> ⚠️  **NOTE**  
> These constraints are not compatible with [Modelling un-optimised CNECs (CRAs)](unoptimized-cnec-filler-cra.md).  
> Only one of both features can be activated
> through [RAO parameters](/parameters/parameters.md#cnecs-that-should-not-be-optimised).

## Used input data

| Name            | Symbol                   | Details                                                                                                                                                                             |
|-----------------|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| FlowCnecs       | $c \in \mathcal{C}$      | Set of optimised FlowCnecs[^2]                                                                                                                                                      |
| Upper threshold | $f^{+}_{threshold} (c)$  | Upper threshold of FlowCnec $c$, in MW, defined in the CRAC                                                                                                                         |
| Lower threshold | $f^{-}_{threshold} (c)$  | Lower threshold of FlowCnec $c$, in MW, defined in the CRAC                                                                                                                         |
| RA upper bound  | $A^{+}(r,s)$             | Upper bound of allowed range for range action $r$ at state $s$[^1]                                                                                                                  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| RA lower bound  | $A^{-}(r,s)$             | Lower bound of allowed range for range action $r$ at state $s$[^1]                                                                                                                  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| Sensitivities   | $\sigma _{n}(r,c,s)$     | sensitivity of RangeAction $r$ on FlowCnec $c$ for state $s$                                                                                                                        |
| cnecPstPairs    | $(c, r)\in \mathcal{CP}$ | These are CNEC-PST combinations defined by the user in the [do-not-optimize-cnec-secured-by-its-pst](/parameters/parameters.md#do-not-optimize-cnec-secured-by-its-pst) parameter.  |

[^1]: Range actions' lower & upper bounds are computed using CRAC + network + previous RAO results, depending on the
types of their ranges: ABSOLUTE, RELATIVE_TO_INITIAL_NETWORK, RELATIVE_TO_PREVIOUS_INSTANT (more
information [here](/input-data/crac/json.md#range-actions))

[^2]: CNECs that belong to a state for which sensitivity computations failed are ignored during linear optimisation

## Used parameters

| Name                                                                                                         | Details                                                                    |
|--------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| [do-not-optimize-cnec-secured-by-its-pst](/parameters/parameters.md#do-not-optimize-cnec-secured-by-its-pst) | This filler is only used if this parameter contains CNEC-PST combinations. |

## Defined optimization variables

| Name       | Symbol          | Details                                                                                                            | Type   | Index                                                     | Unit    | Lower bound | Upper bound |
|------------|-----------------|--------------------------------------------------------------------------------------------------------------------|--------|-----------------------------------------------------------|---------|-------------|-------------|
| DoOptimize | $DoOptimize(c)$ | FlowCnec $c$ should be optimized. Equal to 0 if its associated PST has enough taps left to secure it, 1 otherwise. | Binary | One variable for every CNEC element $c$ in $\mathcal{CP}$ | no unit | 0           | 1           |

## Used optimization variables

| Name        | Symbol | Defined in                                                              |
|-------------|--------|-------------------------------------------------------------------------|
| Flow        | $F(c)$ | [CoreProblemFiller](core-problem-filler.md#defined-optimization-variables) |
| RA setpoint | $A(r)$ | [CoreProblemFiller](core-problem-filler.md#defined-optimization-variables) |

## Defined constraints

### Defining the "don't optimize CNEC" binary variable

It should be equal to 1 if the PST has enough taps left to secure the associated CNEC.  
This is estimated using sensitivity values.

$\forall (c, r)\in \mathcal{CP}$ let $s$ be the state on which $c$ is evaluated and $bigM(r, s) = A^{+}(r, s) - A^{-}(r, s)$

- **if $\sigma _{n}(r,c,s) \gt 0$**  

$$
\begin{equation}
A(r, s) \geq A^{-}(r, s) - \frac{f^{+}_{threshold} (c) - F(c)}{\sigma _{n}(r,c,s)} - bigM(r, s) \times DoOptimize(c)
\end{equation}
$$
$$
\begin{equation}
A(r, s) \leq A^{+}(r, s) + \frac{F(c) - f^{-}_{threshold} (c)}{\sigma _{n}(r,c,s)} + bigM(r, s) \times DoOptimize(c)
\end{equation}
$$

- **if $\sigma _{n}(r,c,s) \lt 0$**  
  
$$
\begin{equation}
A(r, s) \geq A^{-}(r, s) - \frac{f^{-}_{threshold} (c) - F(c)}{\sigma _{n}(r,c,s)} - bigM(r, s) \times DoOptimize(c)
\end{equation}
$$
$$
\begin{equation}
A(r, s) \leq A^{+}(r, s) + \frac{F(c) - f^{+}_{threshold} (c)}{\sigma _{n}(r,c,s)} + bigM(r, s) \times DoOptimize(c)
\end{equation}
$$

*Note that a FlowCnec might have only one threshold (upper or lower), in that case, only one of the two above
constraints is defined.*

<br>

### Updating the minimum margin constraints

(These are originally defined in [MaxMinMarginFiller](max-min-margin-filler.md#defined-constraints)
and [MaxMinRelativeMarginFiller](max-min-relative-margin-filler.md#defined-constraints))

For CNECs which should not be optimized, their RAM should not be taken into account in the minimum margin variable
unless their margin is decreased.

So we can release the minimum margin constraints if DoOptimize is equal to 0. In order to do this, we just need to add
the following term to these constraints' right side:

$$
\begin{equation}
(1 - DoOptimize(c)) \times 2 \times MaxRAM, \forall  (c, r) \in \mathcal{CP}
\end{equation}
$$

*Note that this term should be divided by the absolute PTDF sum for relative margins, but it is not done explicitly in
the code because this coefficient is brought to the left-side of the constraint.*

<br>

## Contribution to the objective function

Given the updated constraints above, the "un-optimised CNECs" will no longer count in the minimum margin (thus in the
objective function) unless they are overloaded and their associated PST cannot relieve the overload.
