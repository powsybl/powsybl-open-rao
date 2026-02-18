# Limiting the number of range actions to use

## Used input data

| Name                                         | Symbol                           | Details                                                                                                                                                                                                                                                  |
|----------------------------------------------|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RangeActions                                 | $r,s \in \mathcal{RA}$           | Set of optimised RangeActions and state on which they are applied (PSTs, HVDCs, InjectionRangeActions...)                                                                                                                                                |
| PstRangeActions                              | $r \in \mathcal{RA}^{PST}$       | Set of PST RangeActions                                                                                                                                                                                                                                  |
| States                                       | $s \in \mathcal{S}$              | Set of states on which RangeActions are optimized                                                                                                                                                                                                        |
| PrePerimeterSetpoints                        | $\alpha _0(r)$                   | Set-point of RangeAction $r$ at the beginning of the optimisation                                                                                                                                                                                        |
| Range upper bound                            | $\alpha_{\max}(r)(\alpha _0(r))$ | Highest allowed set-point for a range action $r$, given its pre-perimeter set-point $\alpha _0(r)$                                                                                                                                                         |
| Range lower bound                            | $\alpha_{\min}(r)(\alpha _0(r))$ | Lowest allowed set-point for a range action $r$, given its pre-perimeter set-point $\alpha _0(r)$                                                                                                                                                          |
| Maximum number of RAs                        | $nRA^{max}(s)$                   | Maximum number of range actions that can be used on state $s$                                                                                                                                                                                            |
| Maximum number of TSOs                       | $nTSO^{max}(s)$                  | Maximum number of TSOs that can use at least one range action (those in "TSO exclusions" do not count) on state $s$                                                                                                                                      |
| TSO exclusions                               | $tso \in \mathcal{TSO_{ex}}(s)$  | TSOs that do not count in the "Maximum number of TSOs" constraint on state $s$ (typically because they already have an activated network action outside the MILP, and that maxTso has been decremented, so using range actions for these TSOs is "free") |
| Maximum number of PSTs per TSO               | $nPST^{max}(tso,s)$              | Maximum number of PSTs that can be used by a given TSO on state $s$                                                                                                                                                                                      |
| Maximum number of RAs per TSO                | $nRA^{max}(tso,s)$               | Maximum number of range actions to use by a given TSO on state $s$                                                                                                                                                                                       |
| Maximum number of elementary actions per TSO | $nEA^{max}(tso)$                 | Maximum number of elementary actions to use by a given TSO at a given instant                                                                                                                                                                            |
| TSOs                                         | $tso \in \mathcal{TSO}$          | Set of all TSOs operating a range action in RangeActions                                                                                                                                                                                                 |
| reference tap position                       | $t_{n}(r)$                       | tap of PstRangeAction $r$ at the beginning of the current iteration of the MILP                                                                                                                                                                          |
| pre-perimeter tap position                   | $t_{0}(r)$                       | tap of PstRangeAction $r$ at the end of the previous optimization perimeter (or initial tap if preventive perimeter)                                                                                                                                     |

> 💡 Note
> 
> This filler uses:
> - crac-creation-parameters defined [here](../../../input-data/crac/creation-parameters.md#ra-usage-limits-per-instant) 
> - or CRAC JSON's ra-usage-limits-per-instant field defined [here](../../../input-data/crac/json.md#ras-usage-limitations). 
> 
> Nonetheless, they are modified to take into account :
> - applied topological actions at each leaf of a search tree 
> - the cumulative effect of the limits in multi-curative at the beginning of each contingency scenario

## Defined optimization variables

| Name                       | Symbol              | Details                                                                                                                                                                                             | Type          | Index                                                                  | Unit               | Lower bound | Upper bound |
|----------------------------|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|------------------------------------------------------------------------|--------------------|-------------|-------------|
| RA usage binary            | $\delta(r,s)$       | binary indicating if a range action is used                                                                                                                                                         | Binary        | One variable for every element of $\mathcal{RA}$                       | no unit            | 0           | 1           |
| TSO RA usage binary        | $\delta^{TSO}(tso)$ | binary indicating for a given TSO if it has any range action used. <br> Note that it is defined as a real value to speed up resolution, but it will act as a binary given the following constraints | Real value    | One variable for every element of $\mathcal{TSO} - \mathcal{TSO_{ex}}$ | no unit            | 0           | 1           |
| PST absolute tap variation | $\Delta t(r)$       | integer computing the number of taps that were moved on the PST at a given instant                                                                                                                  | Integer value | One variable for every element of $\mathcal{RA}^\mathcal{PST}$         | no unit (PST taps) | 0           | $+ \infty$  |

## Used optimization variables

| Name                                     | Symbol             | Defined in                                                                        |
|------------------------------------------|--------------------|-----------------------------------------------------------------------------------|
| RangeAction set-point upward variation   | $\Delta^{+}(r,s)$  | [CoreProblemFiller](core-problem-filler.md#defined-optimization-variables)        |
| RangeAction set-point downward variation | $\Delta^{-}(r,s)$  | [CoreProblemFiller](core-problem-filler.md#defined-optimization-variables)        |
| PstRangeAction tap upward variation      | $\Delta t^{+} (r)$ | [DiscretePstTapFiller](discrete-pst-tap-filler.md#defined-optimization-variables) |
| PstRangeAction tap downward variation    | $\Delta t^{-} (r)$ | [DiscretePstTapFiller](discrete-pst-tap-filler.md#defined-optimization-variables) |

## Defined constraints

Let the following symbol indicate the subset of RangeActions belonging to TSO (tso): $\mathcal{RA}(tso)$

### Define the binary variable

Force the binary to 1 if optimal set-point should be different from pre-perimeter set-point:

$$
\begin{equation}
\Delta^{+}(r,s) + \Delta^{-}(r,s) \geq \delta (r,s) * (\alpha_{\max}(r)(\alpha _0(r)) - \alpha_{\min}(r)(\alpha _0(r)))  ,
\forall (r,s) \in \mathcal{RA}
\end{equation}
$$

<br>

*⚠️ In order to mitigate rounding issues, and ensure that the max and min set-points are feasible, a small "epsilon" (
1e-4) is added to max / subtracted to min set-point.*

*⚠️ In order to mitigate PST tap ↔ angle approximation in "[APPROXIMATED_INTEGERS](../../../parameters/implementation-specific-parameters.md#pst-model)"
mode, and ensure that the initial set-point is feasible, a correction factor is added or subtracted from the initial
set-point in the constraints above. This coefficient is computed as 30% of the average tap to angle conversion factor:*  
*correction = 0.3 x abs((max angle - min angle) / (max tap - min tap))*

### Maximum number of remedial actions

$$
\begin{equation}
\sum_{r \in \mathcal{RA(s)}} \delta (r,s) \leq nRA^{max}(s), \forall s \in \mathcal{S}
\end{equation}
$$

<br>

### Maximum number of TSOs

$$
\begin{equation}
\delta^{TSO}(tso,s) \geq \delta (r,s), \forall tso \in \mathcal{TSO - TSO_{ex}}, \forall r,s \in \mathcal{RA}(tso)
\end{equation}
$$

<br>

$$
\begin{equation}
\sum_{tso \in \mathcal{TSO - TSO_{ex}}} \delta^{tso} (tso,s) \leq nTSO^{max}(s), \forall s \in \mathcal{S}
\end{equation}
$$

<br>

### Maximum number of PSTs per TSO

$$
\begin{equation}
\sum_{\substack{r,s \in \mathcal{RA}(tso) \\ r \ is \ PST}} \delta (r,s) \leq nPST^{max}(tso,s), \forall tso \in
\mathcal{TSO},
\forall s \in \mathcal{S}
\end{equation}
$$

<br>

### Maximum number of RAs per TSO

$$
\begin{equation}
\sum_{r,s \in \mathcal{RA}(tso)} \delta (r,s) \leq nRA^{max}(tso,s), \forall tso \in \mathcal{TSO}, \forall s \in
\mathcal{S}
\end{equation}
$$

### Define the absolute tap variation variable

_️⚠️ Only if [APPROXIMATED_INTEGERS](../../../parameters/implementation-specific-parameters.md#pst-model) mode._

_💡 Ideally, we would like to define $\Delta t (r) = | t_{n}(r) + \Delta t^{+} (r) - \Delta t^{-} (r) - t_{0}(r) |$
but this is not linear. Instead, a classical workaround is done as follows:_

$$
\begin{equation}
\begin{cases}
\Delta t (r) \geq t_{n}(r) + \Delta t^{+} (r) - \Delta t^{-} (r) - t_{0}(r)\\
\Delta t (r) \geq t_{0}(r) - t_{n}(r) - \Delta t^{+} (r) + \Delta t^{-} (r)
\end{cases}
, \forall r \in \mathcal{RA}^{PST}
\end{equation}
$$

### Maximum number of elementary actions per TSO

_️⚠️ Only if [APPROXIMATED_INTEGERS](../../../parameters/implementation-specific-parameters.md#pst-model) mode._

$$
\begin{equation}
\sum_{\substack{\forall r \in \mathcal{RA}^{PST} \\ r \text{'s operator is } tso}} \Delta t (r) \leq nEA^{max}(tso)
, \forall tso \in \mathcal{TSO}
\end{equation}
$$