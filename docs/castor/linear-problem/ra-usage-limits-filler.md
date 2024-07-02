# Limiting the number of range actions to use

## Used input data

| Name                           | Symbol                                | Details                                                                                                                                                                                                                                                  |
|--------------------------------|---------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RangeActions                   | $r,s \in \mathcal{RA}$                | Set of optimised RangeActions and state on which they are applied (PSTs, HVDCs, InjectionRangeActions...)                                                                                                                                                |
| States                         | $s \in \mathcal{S}$                   | Set of states on which RangeActions are optimized                                                                                                                                                                                                        |
| PrePerimeterSetpoints          | $\alpha _0(r)$                        | Setpoint of RangeAction $r$ at the beginning of the optimisation                                                                                                                                                                                         |
| Range upper bound              | $\underline{\alpha(r)(\alpha _0(r))}$ | Highest allowed setpoint for a range action $r$, given its pre-perimeter setpoint $\alpha _0(r)$                                                                                                                                                         |
| Range lower bound              | $\overline{\alpha(r)(\alpha _0(r))}$  | Lowest allowed setpoint for a range action $r$, given its pre-perimeter setpoint $\alpha _0(r)$                                                                                                                                                          |
| Maximum number of RAs          | $nRA^{max}(s)$                        | Maximum number of range actions that can be used on state $s$                                                                                                                                                                                            |
| Maximum number of TSOs         | $nTSO^{max}(s)$                       | Maximum number of TSOs that can use at least one range action (those in "TSO exclusions" do not count) on state $s$                                                                                                                                      |
| TSO exclusions                 | $tso \in \mathcal{TSO_{ex}}(s)$       | TSOs that do not count in the "Maximum number of TSOs" constraint on state $s$ (typically because they already have an activated network action outside the MILP, and that maxTso has been decremented, so using range actions for these TSOs is "free") |
| Maximum number of PSTs per TSO | $nPST^{max}(tso,s)$                   | Maximum number of PSTs that can be used by a given TSO on state $s$                                                                                                                                                                                      |
| Maximum number of RAs per TSO  | $nRA^{max}(tso,s)$                    | Maximum number of range actions to use by a given TSO on state $s$                                                                                                                                                                                       |
| TSOs                           | $tso \in \mathcal{TSO}$               | Set of all TSOs operating a range action in RangeActions                                                                                                                                                                                                 |

*Note that this filler uses crac-creation-parameters
defined [here](/input-data/crac/creation-parameters.md#ra-usage-limits-per-instant). Nonetheless, they are modified to
take into account applied topological actions first.*

## Defined optimization variables

| Name                | Symbol              | Details                                                                                                                                                                                             | Type       | Index                                                                  | Unit    | Lower bound | Upper bound |
|---------------------|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|------------------------------------------------------------------------|---------|-------------|-------------|
| RA usage binary     | $\delta(r,s)$       | binary indicating if a range action is used                                                                                                                                                         | Binary     | One variable for every element of (RangeActions)                       | no unit | 0           | 1           |
| TSO RA usage binary | $\delta^{TSO}(tso)$ | binary indicating for a given TSO if it has any range action used. <br> Note that it is defined as a real value to speed up resolution, but it will act as a binary given the following constraints | Real value | One variable for every element of $\mathcal{TSO} - \mathcal{TSO_{ex}}$ | no unit | 0           | 1           |

## Used optimization variables

| Name                           | Symbol          | Defined in                                                                 |
|--------------------------------|-----------------|----------------------------------------------------------------------------|
| RA setpoint absolute variation | $\Delta A(r,s)$ | [CoreProblemFiller](core-problem-filler.md#defined-optimization-variables) |

## Defined constraints

Let the following symbol indicate the subset of RangeActions belonging to TSO (tso): $\mathcal{RA}(tso)$

### Define the binary variable

Force the binary to 1 if optimal setpoint should be different from pre-perimeter setpoint:

$$
\begin{equation}
\Delta A(r,s) \geq \delta (r,s) * (\overline{\alpha(r)(\alpha _0(r))} - \underline{\alpha(r)(\alpha _0(r))})  ,
\forall (r,s) \in \mathcal{RA}
\end{equation}
$$

<br>

*⚠️ In order to mitigate rounding issues, and ensure that the max and min setpoints are feasible, a small "epsilon" (
1e-4) is added to max / subtracted to min setpoint.*

*⚠️ In order to mitigate PST tap ↔ angle approximation in "[APPROXIMATED_INTEGERS](/parameters.md#pst-model)"
mode, and ensure that the initial setpoint is feasible, a correction factor is added or subtracted from the initial
setpoint in the constraints above. This coefficient is computed as 30% of the average tap to angle conversion factor:*  
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
\sum_{r,s \in \mathcal{RA}(tso), r \ is \ PST} \delta (r,s) \leq nPST^{max}(tso,s), \forall tso \in \mathcal{TSO},
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