# Modelling CNECs and range actions

## Used input data

| Name                        | Symbol                     | Details                                                                                                                                                                                                                                                                                                                     |
|-----------------------------|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| FlowCnecs                   | $c \in \mathcal{C}$        | set of FlowCnecs[^1]. Note that FlowCnecs are all the CBCO for which we compute the flow in the MILP, either: <br> - because we are optimizing their flow (optimized flowCnec = CNEC) <br> - because we are monitoring their flow, and ensuring it does not exceed its threshold (monitored flowCnec = MNEC) <br> - or both |
| RangeActions                | $r,s \in \mathcal{RA}$     | set of RangeActions and state on which they are applied, could be PSTs, HVDCs, or injection range actions                                                                                                                                                                                                                   |
| RangeActions                | $r \in \mathcal{RA(s)}$    | set of RangeActions available at state $s$, could be PSTs, HVDCs, or injection range actions                                                                                                                                                                                                                                |
| InjectionRangeActions       | $r \in \mathcal{IRA}(s)$   | set of InjectionRangeActions available at state $s$                                                                                                                                                                                                                                                                         |
| Injection Distribution keys | $d \in \mathcal{DK(r)}$    | set of distribution keys of InjectionRangeAction $r$. Each distribution key is linked to a NetworkElement.                                                                                                                                                                                                                  |
| Iteration number            | $n$                        | number of current iteration                                                                                                                                                                                                                                                                                                 |
| ReferenceFlow               | $f_{n}(c)$                 | reference flow, for FlowCnec $c$. <br>The reference flow is the flow at the beginning of the current iteration of the MILP, around which the sensitivities are computed                                                                                                                                                     |
| PrePerimeterSetpoints       | $\alpha _0(r)$             | setpoint of RangeAction $r$ at the beginning of the optimization                                                                                                                                                                                                                                                            |
| ReferenceSetpoints          | $\alpha _n(r)$             | setpoint of RangeAction $r$ at the beginning of the current iteration of the MILP, around which the sensitivities are computed                                                                                                                                                                                              |
| Sensitivities               | $\sigma _{n}(r,c,s)$       | sensitivity of RangeAction $r$ on FlowCnec $c$ for state $s$                                                                                                                                                                                                                                                                |
| Previous RA setpoint        | $A_{n-1}(r,s)$             | optimal setpoint of RangeAction $r$ on state $s$ in previous iteration ($n-1$)                                                                                                                                                                                                                                              |

[^1]: CNECs that belong to a state for which sensitivity computations failed are ignored in the MILP

## Used parameters

| Name                 | Symbol             | Details                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | Source                                                                                                                                                                                                                                                                                                                           |
|----------------------|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| sensitivityThreshold |                    | Set to zero the sensitivities of RangeActions below this threshold; thus avoiding the activation of RangeActions which have too small an impact on the flows (can also be achieved with penaltyCost). This simplifies & speeds up the resolution of the optimization problem (can be necessary when the problem contains integer variables). However, it also adds an approximation in the computation of the flows within the MILP, which can be tricky to handle when the MILP contains hard constraints on loop-flows or monitored FlowCnecs. | Equal to [pst-sensitivity-threshold](/parameters.md#pst-sensitivity-threshold) for PSTs, [hvdc-sensitivity-threshold](/parameters.md#hvdc-sensitivity-threshold) for HVDCs, and [injection-ra-sensitivity-threshold](/parameters.md#injection-ra-sensitivity-threshold) for injection range actions |
| penaltyCost          | $c^{penalty}_{ra}$ | Supposedly a small penalization, in the use of the RangeActions. When several solutions are equivalent, this favours the one with the least change in the RangeActions' setpoints (compared to the initial situation). It also avoids the activation of RangeActions which have to small an impact on the objective function.                                                                                                                                                                                                                    | Equal to [pst-penalty-cost](/parameters.md#pst-penalty-cost) for PSTs, [hvdc-penalty-cost](/parameters.md#hvdc-penalty-cost) for HVDCs, and [injection-ra-penalty-cost](/parameters.md#injection-ra-penalty-cost) for injection range actions                                                      |

## Defined optimization variables

| Name                            | Symbol            | Details                                                                                                               | Type                | Index                                            | Unit                                                      | Lower bound           | Upper bound           |
|---------------------------------|-------------------|-----------------------------------------------------------------------------------------------------------------------|---------------------|--------------------------------------------------|-----------------------------------------------------------|-----------------------|-----------------------|
| Flow                            | $F(c)$            | flow of FlowCnec $c$                                                                                                  | Real value          | One variable for every element of (FlowCnecs)    | MW                                                        | $-\infty$             | $+\infty$             |
| RA set-point                    | $A(r,s)$          | set-point of RangeAction $r$ on state $s$                                                                             | Real value          | One variable for every element of (RangeActions) | Degrees for PST range actions; MW for other range actions | Range lower bound[^2] | Range upper bound[^2] |
| RA set-point absolute variation | $\Delta A(r,s)$   | The absolute set-point variation of RangeAction $r$ on state $s$, from setpoint on previous state to "RA setpoint"    | Real positive value | One variable for every element of (RangeActions) | Degrees for PST range actions; MW for other range actions | 0                     | $+\infty$             |
| RA upward set-point variation   | $\Delta^{+}(r,s)$ | The upward set-point variation of RangeAction $r$ on state $s$, from set-point on previous state to "RA set-point"    | Real positive value | One variable for every element of (RangeActions) | Degrees for PST range actions; MW for other range actions | 0                     | $+\infty$             |
| RA downward set-point variation | $\Delta^{-}(r,s)$ | The downward set-point variation of RangeAction $r$ on state $s$, from set-point on previous state to "RA set-point"  | Real positive value | One variable for every element of (RangeActions) | Degrees for PST range actions; MW for other range actions | 0                     | $+\infty$             |

[^2]: Range actions' lower & upper bounds are computed using CRAC + network + previous RAO results, depending on the
types of their ranges: ABSOLUTE, RELATIVE_TO_INITIAL_NETWORK, RELATIVE_TO_PREVIOUS_INSTANT (more
information [here](/input-data/crac/json.md#range-actions))

## Defined constraints

### Impact of rangeActions on FlowCnecs flows

$$
\begin{equation}
F(c) = f_{n}(c) + \sum_{r \in \mathcal{RA(s)}} \sigma_n (r,c,s) * [A(r,s) - \alpha_{n}(r,s)] , \forall (c) \in
\mathcal{C}
\end{equation}
$$

with $s$ the state on $c$ which is evaluated

<br>

### Definition of the setpoint variations of the RangeActions

$$
\begin{equation}
A(r,s) = A(r,s') + \Delta^{+}(r,s) - \Delta^{-}(r,s) , \forall (r,s) \in \mathcal{RA}
\end{equation}
$$

with $A(r,s')$ the setpoint of the last range action on the same element as $r$ but a state preceding $s$. If none such
range actions exists, then $A(r,s') = \alpha_{0}(r)$

This equation always applies, except for PSTs when modeling them as [APPROXIMATED_INTEGERS](/parameters.md#pst-model).
If so, this constraint will be modeled directly via PST taps, see [here](/castor/linear-problem/discrete-pst-tap-filler.md#rangeactions-relative-tap-variations) as it gives better results [^3].

<br>

[^3]: Relative bounds are originally in tap.
When converting them in angle, we take the smallest angle step between two tap changes.
Despite artificially tightening the bounds, this is the only way to ensure we respect the tap limits as the tap to angle map is non linear.  
With PST modeled as APPROXIMATED_INTEGERS, we can directly use the taps and therefore avoid this approximation.

### Definition of the absolute variations of the RangeActions

$$
\begin{equation}
\Delta A(r,s) = \Delta^{+}(r,s) + \Delta^{-}(r,s) , \forall (r,s) \in \mathcal{RA}
\end{equation}
$$

### Shrinking the allowed range

If parameter [ra-range-shrinking](/parameters.md#ra-range-shrinking) is enabled, the allowed range for range actions
is shrunk after each iteration according to the following constraints:

$$
\begin{equation}
t = UB(A(r,s)) - LB(A(r,s))
\end{equation}
$$

$$
\begin{equation}
A(r,s) \geq A_{n-1}(r,s) - (\frac{2}{3})^{n}*t
\end{equation}
$$

$$
\begin{equation}
A(r,s) \leq A_{n-1}(r,s) + (\frac{2}{3})^{n}*t
\end{equation}
$$

The value $\frac{2}{3}$ has been chosen to force the linear problem convergence while allowing the RA to go
back to its initial solution if needed.

### Injection balance constraint

The network must remain balanced in terms of production and consumption after injection variations:

$$\sum_{r \in \mathcal{IRA}(s)} \left ( \Delta^{+} (r, s) - \Delta^{-} (r, s) \right ) \times \sum_{d \in \mathcal{DK(r)}} d = 0, \forall s$$

## Contribution to the objective function

Small penalisation for the use of RangeActions:

$$
\begin{equation}
\min \sum_{r,s \in \mathcal{RA}} (c^{penalty}_{ra}(r) \Delta A(r,s))
\end{equation}
$$
