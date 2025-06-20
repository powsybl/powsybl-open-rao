# Using integer variables for PST taps

## Used input data

| Name                             | Symbol                        | Details                                                                                                   |
|----------------------------------|-------------------------------|-----------------------------------------------------------------------------------------------------------|
| PstRangeActions                  | $r \in \mathcal{RA}^{PST}$    | Set of PST RangeActions                                                                                   |
| PstRangeActions                  | $r,s \in \mathcal{RA}^{PST}$  | set of PST RangeActions and state on which they are applied                                               |
| reference angle                  | $\alpha _n(r, s)$             | angle of PstRangeAction $r$ at state $s$, at the beginning of the current iteration of the MILP           |
| reference tap position           | $t_{n}(r, s)$                 | tap of PstRangeAction $r$ at state $s$, at the beginning of the current iteration of the MILP             |
| PstRangeAction tap bounds        | $t^-(r) \: , \: t^+(r)$       | min and max tap[^1] of PstRangeAction $r$                                                                 |
| tap-to-angle conversion function | $f_r(t) = \alpha$             | Discrete function $f$, which gives, for a given tap of the PstRangeAction $r$, its associated angle value |

[^1]: PST range actions' lower & upper bounds are computed using CRAC + network + previous RAO results, depending on the
types of their ranges: ABSOLUTE, RELATIVE_TO_INITIAL_NETWORK, RELATIVE_TO_PREVIOUS_INSTANT (more
information [here](../../../input-data/crac/json.md#range-actions))

## Used parameters

| Name                                                                             | Details                                                                       |
|----------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| [pst-model](../../../parameters/implementation-specific-parameters.md#pst-model) | This filler is used only if this parameters is set to *APPROXIMATED_INTEGERS* |

## Defined optimization variables

| Name                                         | Symbol                        | Details                                                                                                                 | Type    | Index                                                                                         | Unit                      | Lower bound | Upper bound |
|----------------------------------------------|-------------------------------|-------------------------------------------------------------------------------------------------------------------------|---------|-----------------------------------------------------------------------------------------------|---------------------------|-------------|-------------|
| PstRangeAction tap upward variation          | $\Delta t^{+} (r, s)$         | upward tap variation of PstRangeAction $r$, at state $s$, between two iterations of the optimisation                    | Integer | One variable for every element of PstRangeActions and for evey state in which it is optimized | No unit (number of taps)  | 0           | $+\infty$   |
| PstRangeAction tap downward variation        | $\Delta t^{-} (r, s)$         | downward tap variation of PstRangeAction $r$, at state $s$, between two iterations of the optimisation                  | Integer | One variable for every element of PstRangeActions and for evey state in which it is optimized | No unit (number of taps)  | 0           | $+\infty$   |
| PstRangeAction tap upward variation binary   | $\delta ^{+} (r, s)$          | indicates whether the tap of PstRangeAction $r$ has increased, at state $s$, between two iterations of the optimisation | Binary  | One variable for every element of PstRangeActions and for evey state in which it is optimized | No unit                   | 0           | 1           |
| PstRangeAction tap downward variation binary | $\delta ^{-} (r, s)$          | indicates whether the tap of PstRangeAction $r$ has decreased, at state $s$, between two iterations of the optimisation | Binary  | One variable for every element of PstRangeActions and for evey state in which it is optimized | No unit                   | 0           | 1           |
| PstRangeAction tap                           | $\tau (r, s)$                 | tap position of the PST of range action $r$ at state $s$                                                                | Integer | One variable for every element of PstRangeActions and for evey state in which it is optimized | No unit                   | min PST tap | max PST tap |
| Total PstRangeAction upward tap variation    | $\Delta_{total} t^{+} (r, s)$ | total upward tap variation of PstRangeAction $r$, at state $s$, from the pre-perimeter tap position                     | Integer | One variable for every element of PstRangeActions and for evey state in which it is optimized | No unit (number of taps)  | 0           | $+\infty$   |
| Total PstRangeAction downward tap variation  | $\Delta_{total} t^{-} (r, s)$ | total downward tap variation of PstRangeAction $r$, at state $s$, from the pre-perimeter tap position                   | Integer | One variable for every element of PstRangeActions and for evey state in which it is optimized | No unit (number of taps)  | 0           | $+\infty$   |

## Used optimization variables

| Name        | Symbol | Defined in                                                                 |
|-------------|--------|----------------------------------------------------------------------------|
| RA setpoint | $A(r, s)$ | [CoreProblemFiller](core-problem-filler.md#defined-optimization-variables) |

## Defined constraints

### Tap to angle conversion constraint

$$
\begin{equation}
A(r, s) = \alpha_{n}(r, s) + c^{+}_{tap \rightarrow a}(r, s) * \Delta t^{+} (r, s) - 
c^{-}_{tap \rightarrow a}(r, s) * \Delta t^{-} (r, s)
, \forall (r,s) \in \mathcal{RA}^{PST}
\end{equation}
$$

<br>

Where the computation of the conversion depends on the context in which the optimization problem is solved.

For the **first solve**, the coefficients are calibrated on the maximum possible variations of the PST:

$$
\begin{equation}
c^{+}_{tap \rightarrow a}(r, s) = \frac{f_r(t^+(r)) - f_r(t_{n}(r, s))}{t^+(r) - t_{n}(r, s)}
\end{equation}
$$

$$
\begin{equation}
c^{-}_{tap \rightarrow a}(r, s) = \frac{f_r(t_{n}(r, s)) - f_r(t^-(r))}{t_{n}(r, s) - t^-(r)}
\end{equation}
$$

For the **second and next solves** (during the iteration of the linear optimization), the coefficients are calibrated on
a small variation of 1 tap:

$$
\begin{equation}
c^{+}_{tap \rightarrow a}(r, s) = f_r(t_{n}(r, s) + 1) - f_r(t_{n}(r, s))
\end{equation}
$$

$$
\begin{equation}
c^{-}_{tap \rightarrow a}(r, s) = f_r(t_{n}(r, s)) - f_r(t_{n}(r, s) - 1)
\end{equation}
$$

<br>

*Note that if $t_n(r, s)$ is equal to its bound $t^+(r)$ (resp. $t^-(r)$), then the coefficient
$c^{+}_{tap \rightarrow a}(r, s)$ (resp. $c^{-}_{tap \rightarrow a}(r, s)$) is set equal to 0 instead.*

<br>

### Tap variable

$$\tau(r, s) = \Delta t^{+} - \Delta t^{-} + t_{n}(r, s)$$

### Total tap variation

$$\Delta_{total} t^{+} (r, s) - \Delta_{total} t^{-} (r, s) = \tau(r, s) -
\begin{cases}
    \tau(r, s') & \text{if $r$ was previously available at state $s'$}\\
    t_{0}(r, s) & \text{otherwise}
\end{cases}$$

### Tap variation can only be in one direction, upward or downward

$$
\begin{equation}
\Delta t^{+} (r) \leq \delta ^{+} (r, s) [t^+(r) - t_{n}(r, s)] , \forall (r, s) \in \mathcal{RA}^{PST}
\end{equation}
$$

$$
\begin{equation}
\Delta t^{-} (r) \leq \delta ^{-} (r, s) [t_{n}(r, s) - t^-(r)] , \forall (r, s) \in \mathcal{RA}^{PST}
\end{equation}
$$

$$
\begin{equation}
\delta ^{+} (r, s) + \delta ^{-} (r, s)  \leq 1 , \forall (r, s) \in \mathcal{RA}^{PST}
\end{equation}
$$

<br>

### RangeActions relative tap variations

If PST $r$ has a `RELATIVE_TO_PREVIOUS_INSTANT` range constraints, between $t^-_{rel}(r)$ and $t^+_{rel}(r)$, the
following constraint is added:  

$$
\begin{equation}
t^-_{rel}(r) \leq (t_{n}(r, s) + \Delta t^+(r, s) - \Delta t^-(r, s)) - (t_{n}(r, s') + \Delta t^+(r, s') - \Delta t^-(r, s')) \leq t^+_{rel}(r)
\end{equation}
$$

where $s'$ is the last state preceding $s$ where $r$ is also optimized.