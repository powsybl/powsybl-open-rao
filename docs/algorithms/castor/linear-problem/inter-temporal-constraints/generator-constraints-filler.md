# Modelling the generator constraints

## Preliminary notations

A generator possesses mechanical characteristics that constrain the way it can be operated. Among these characteristics
are:

- the **minimum operating power** (or $P_{\min}$), which is the power threshold over which the generator can properly be
  operated;
- the **maximum operating power** (or $P_{\max}$), which is the highest power value the generator can reach at any time;
- the **lead time**, which is the time required by the generator to constantly ramp up from a null power to $P_{\min}$;
- the **lag time**, which is the time required by the generator to constantly ramp down from $P_{\min}$ to a complete
  shutdown.

The generator then operates in four distinct states depending on its current power, based on the aforementioned
characteristics:

- it is **off** whenever its power is null;
- it is **on** whenever its power is greater or equal than $P_{\min}$ (and lower or equal than $P_{\max}$). In that
  range, the power is somewhat free to change (with only possible power gradient constraints);
- it is **ramping up** if its power is increasing from zero to $P_{\min}$, at a fixed pace of $\frac{P_{\min}}{LEAD}$ (
  in MW/h);
- it is **ramping down** if its power is decreasing from $P_{\min}$ to zero, at a fixed pace of $\frac{P_{\min}}{LAG}$ (
  in MW/h).

<!-- Include a graph with the four different states -->

The life of a generator thus cycles through these four states, always in the same order: OFF → RAMP UP → ON → RAMP DOWN
→ OFF ...

## Used input data

| Name                            | Symbol               | Details                                                                                                                                                                 |
|---------------------------------|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Constrained generators set      | $\Gamma$             | Set of generators with constraints defined                                                                                                                              |
| PMin                            | $P_{\min}(g)$        | Minimum operating power of generator $g$. This value must be non-negative.                                                                                              |
| PMax                            | $P_{\max}(g)$        | Maximum operating power of generator $g$. This value must be non-negative.                                                                                              |
| Lead Time                       | $LEAD(g)$            | Maximum operating power of generator $g$. This value must be non-negative.                                                                                              |
| Lag Time                        | $LAG(g)$             | Maximum operating power of generator $g$. This value must be non-negative.                                                                                              |
| Upper power gradient constraint | $\nabla p^{+}(g)$    | Maximum upward power variation between two consecutive timestamps for generator $g$. This value must be non-negative.                                                   |
| Lower power gradient constraint | $\nabla p^{-}(g)$    | Maximum downward power variation (in absolute value) between two consecutive timestamps for generator $g$. This value must be non-positive.                             |
| Initial generator power         | $p_{0}(g,s,t)$       | Initial power of generator $g$ at state $s$ of timestamp $t$, as defined in the network.                                                                                |
| Timestamps                      | $\mathcal{T}$        | Set of all timestamps on which the optimization is performed.                                                                                                           |
| Time gap                        | $\Delta_t(t, t + 1)$ | Time gap between consecutive timestamps $t$ and $t + 1$. For simplicity's sake, **the time gap is considered constant between all consecutive timestamps**.             |
| Initial state                   | $S_{0}(g,s)$         | Initial state of generator $g$ at state $s$ of timestamp $t$. It is deduced from the initial power. For simplicity's sake, **the initial state can only be ON or OFF**. |

> We also introduce the **reduced lead and lag times**, respectively defined as
> $\lambda_{t}^{\nearrow}(g) \equiv LEAD [\Delta t]$ and $\lambda_{t}^{\searrow}(g) \equiv LAG [\Delta t]$, that simply
> are the reminders of the division of both characteristic times by the duration of the timestamps.

## Defined optimization variables

| Name                               | Symbol                   | Details                                                                                                                         | Type       | Index                                                                               | Unit | Lower bound | Upper bound |
|------------------------------------|--------------------------|---------------------------------------------------------------------------------------------------------------------------------|------------|-------------------------------------------------------------------------------------|------|-------------|-------------|
| Generator power                    | $P(g,s,t)$               | the power of generator $g$ at state at $s$ of timestamp $t$                                                                     | Real value | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | MW   | $-\infty$   | $+\infty$   |
| *On* State                         | $ON(g,s,t)$              | whether generator $g$ is on at state at $s$ of timestamp $t$ or not                                                             | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Off* State                        | $OFF(g,s,t)$             | whether generator $g$ is off at state at $s$ of timestamp $t$ or not                                                            | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Up* State                    | $RU(g,s,t)$              | whether generator $g$ is ramping up at state at $s$ of timestamp $t$ or not                                                     | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Down* State                  | $RD(g,s,t)$              | whether generator $g$ is ramping down at state at $s$ of timestamp $t$ or not                                                   | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *On → On* Transition               | $T_{ON \to ON}(g,s,t)$   | whether generator $g$ has transitioned from an "on" state to an "on" state, at state at $s$ of timestamp $t$ or not             | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *On → Off* Transition              | $T_{ON \to OFF}(g,s,t)$  | whether generator $g$ has transitioned from an "on" state to an "off" state, at state at $s$ of timestamp $t$ or not            | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *On → Ramp-Down* Transition        | $T_{ON \to RD}(g,s,t)$   | whether generator $g$ has transitioned from an "on" state to a "ramp up" state, at state at $s$ of timestamp $t$ or not         | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *On → Ramp-Up* Transition          | $T_{ON \to RU}(g,s,t)$   | whether generator $g$ has transitioned from an "on" state to a "ramp down" state, at state at $s$ of timestamp $t$ or not       | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Off → On* Transition              | $T_{OFF \to ON}(g,s,t)$  | whether generator $g$ has transitioned from an "off" state to an "on" state, at state at $s$ of timestamp $t$ or not            | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Off → Off* Transition             | $T_{OFF \to OFF}(g,s,t)$ | whether generator $g$ has transitioned from an "off" state to an "off" state, at state at $s$ of timestamp $t$ or not           | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Off → Ramp-Down* Transition       | $T_{OFF \to RD}(g,s,t)$  | whether generator $g$ has transitioned from an "off" state to a "ramp down" state, at state at $s$ of timestamp $t$ or not      | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Off → Ramp-Up* Transition         | $T_{OFF \to RU}(g,s,t)$  | whether generator $g$ has transitioned from an "off" state to a "ramp up" state, at state at $s$ of timestamp $t$ or not        | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Down → On* Transition        | $T_{RD \to ON}(g,s,t)$   | whether generator $g$ has transitioned from a "ramp down" state to an "on" state, at state at $s$ of timestamp $t$ or not       | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Down → Off* Transition       | $T_{RD \to OFF}(g,s,t)$  | whether generator $g$ has transitioned from a "ramp down" state to an "off" state, at state at $s$ of timestamp $t$ or not      | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Down → Ramp-Down* Transition | $T_{RD \to RD}(g,s,t)$   | whether generator $g$ has transitioned from a "ramp down" state to a "ramp down" state, at state at $s$ of timestamp $t$ or not | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Down → Ramp-Up* Transition   | $T_{RD \to RU}(g,s,t)$   | whether generator $g$ has transitioned from a "ramp down" state to a "ramp up" state, at state at $s$ of timestamp $t$ or not   | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Up → On* Transition          | $T_{RU \to ON}(g,s,t)$   | whether generator $g$ has transitioned from a "ramp up" state to an "on" state, at state at $s$ of timestamp $t$ or not         | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Up → Off* Transition         | $T_{RU \to OFF}(g,s,t)$  | whether generator $g$ has transitioned from a "ramp up" state to an "off" state, at state at $s$ of timestamp $t$ or not        | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Up → Ramp-Down* Transition   | $T_{RU \to RD}(g,s,t)$   | whether generator $g$ has transitioned from a "ramp up" state to a "ramp down" state, at state at $s$ of timestamp $t$ or not   | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Up → Ramp-Up* Transition     | $T_{RU \to RU}(g,s,t)$   | whether generator $g$ has transitioned from a "ramp up" state to a "ramp up" state, at state at $s$ of timestamp $t$ or not     | Binary     | one per generator defined in $\Gamma$, per state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |

## Defined constraints

### Only one state

At each timestamp and for each grid state, each generator can only be in one of the four states among ON, OFF, RAMP DOWN
and RAMP UP. Mathematically, this is written as:

$$\forall g \in \Gamma, \forall t \in \mathcal{T}, \forall s, \; ON(g,s,t) + OFF(g,s,t) + RD(g,s,t) + RU(g,s,t) = 1$$

### State transition constraints

#### Impossible transitions

Some transitions are impossible which means that the value of the associated binary variables is always forced to 0.
These impossible transitions are:

- Ramp Up → Ramp Down
- Ramp Up → Off
- Ramp Down → Ramp up
- Ramp Down → On

> 💡 In the code, the associated binary variables are simply not created in order not to overload the model.

Besides, when the lead/lag time is shorter than the duration of a timestamp, the generator can be completely switched on
or off during the timestamp. For simplicity's sake, we thus force the generator to be either on or off at the end of
each timestamp in such conditions. In that case, the ramping states (Ramp Up and Ramp Down) are unreachable and the
transitions to or from these states are straightforwardly impossible. All of these binary variables are forced to 0.

> 💡 Once again, in the code, and under such conditions, the binary variables associated to the ramping states and the
> transitions in which they are involved are not created.

#### Chaining

To ensure a continuity in the cycle of generator states, a _chaining_ need to be set up thanks to linear constraints
linking the state variables of two consecutive timestamps to the transition variables. Thus,
$\forall g \in \Gamma, \forall t \in \mathcal{T}, \forall s$:

##### *State-To* constraints

The state at the end of the timestamp must be the final state of the transition that occurred during the timestamp, no
matter the state of origin.

$$ON(g,t,s) = T_{ON \to ON}(g,s,t) + T_{OFF \to ON}(g,s,t) + T_{RD \to ON}(g,s,t) + T_{RU \to ON}(g,s,t)$$
$$OFF(g,t,s) = T_{ON \to OFF}(g,s,t) + T_{OFF \to OFF}(g,s,t) + T_{RD \to OFF}(g,s,t) + T_{RU \to OFF}(g,s,t)$$
$$RD(g,t,s) = T_{ON \to RD}(g,s,t) + T_{OFF \to RD}(g,s,t) + T_{RD \to RD}(g,s,t) + T_{RU \to RD}(g,s,t)$$
$$RU(g,t,s) = T_{ON \to RU}(g,s,t) + T_{OFF \to RU}(g,s,t) + T_{RD \to RU}(g,s,t) + T_{RU \to RU}(g,s,t)$$

##### *State-From* constraints

The state at the end of the previous timestamp must be the starting state of the transition that occurred during the
timestamp, no matter the final state.

$$ON(g,t-1,s) = T_{ON \to ON}(g,s,t) + T_{ON \to OFF}(g,s,t) + T_{ON \to RD}(g,s,t) + T_{ON \to RU}(g,s,t)$$
$$OFF(g,t-1,s) = T_{OFF \to ON}(g,s,t) + T_{OFF \to OFF}(g,s,t) + T_{OFF \to RD}(g,s,t) + T_{OFF \to RU}(g,s,t)$$
$$RD(g,t-1,s) = T_{RD \to ON}(g,s,t) + T_{RD \to OFF}(g,s,t) + T_{RD \to RD}(g,s,t) + T_{RD \to RU}(g,s,t)$$
$$RU(g,t-1,s) = T_{RU \to ON}(g,s,t) + T_{RU \to OFF}(g,s,t) + T_{RU \to RD}(g,s,t) + T_{RU \to RU}(g,s,t)$$

> Note that for the first timestamp, the initial state $S_{0}(g,s)$ is used as the initial condition of the chain:
> - if $S_{0}(g,s)$ is On: $T_{ON \to ON}(g,s,1) + T_{ON \to OFF}(g,s,1) + T_{ON \to RD}(g,s,1) + T_{ON \to RU}(g,s,1) =
    1$
> - if $S_{0}(g,s)$ is Off: $T_{OFF \to ON}(g,s,1) + T_{OFF \to OFF}(g,s,1) + T_{OFF \to RD}(g,s,1) + T_{OFF \to RU}(
    g,s,1) = 1$

### Power constraints

#### Off state

By definition, $OFF(g,s,t) = 1 \Rightarrow P(g,s,t) = 0$ which can be linearized as:

$$P(g,s,t) \leq P_{\max}(g) (1 - OFF(g,s,t))$$

#### On state

By definition, $ON(g,s,t) = 1 \Leftrightarrow P(g,s,t) \geq P_{\min}(g)$ which can be linearized using the two following
equations:

$$P(g,s,t) \leq P_{min}(g) (1 - ON(g,s,t)) + P_{\max}(g) ON(g,s,t)$$
$$P_{t} \geq P_{\min} ON_{t}$$

#### Power variation

The power of the generator determines the state of the generator. The two previous sections showed how to identify the
on and off state. In between these are the ramping states which can be determined based on the previous state and/or
whether the power is increasing (ramp up) or decreasing (ramp down).

More generally, the power variation and the state transitions are strongly entangled and constrain one another.
The following table displays the bounds of the variation $P(g,s,t) - P(g,s,t-1)$ based on the state transition:

> - A single value in a cell means that said value is both a lower and an upper bound.
> - A "$\times$" symbol denotes an [impossible transition](#impossible-transitions).

| From / To | $OFF$                                                                              | $RU$                             | $ON$                                                                                                                                                     | $RD$                                                                                                                                                     |
|-----------|------------------------------------------------------------------------------------|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| $OFF$     | 0                                                                                  | $\Delta t \frac{P_{\min}}{LEAD}$ | $P_{\min} \leq . \leq P_{\min} + (\Delta t - \lambda_{t}^{\nearrow}) \nabla^{+}$                                                                         | $\times$                                                                                                                                                 |
| $RU$      | $\times$                                                                           | $\Delta t \frac{P_{\min}}{LEAD}$ | $\lambda_{t}^{\nearrow} \frac{P_{\min}}{LEAD} \leq . \leq \lambda_{t}^{\nearrow} \frac{P_{\min}}{LEAD} + (\Delta t - \lambda_{t}^{\nearrow}) \nabla^{+}$ | $\times$                                                                                                                                                 |
| $ON$      | $-P_{\min} + (\Delta t - \lambda_{t}^{\searrow}) \nabla^{-} \leq . \leq -P_{\min}$ | $\times$                         | $\Delta t \nabla^{-} \leq . \leq \Delta t \nabla^{+}$                                                                                                    | $-\lambda_{t}^{\searrow} \frac{P_{\min}}{LAG} + (\Delta t - \lambda_{t}^{\searrow}) \nabla^{-} \leq . \leq -\lambda_{t}^{\searrow} \frac{P_{\min}}{LAG}$ |
| $RD$      | $- \Delta t \frac{P_{\min}}{LAG}$                                                  | $\times$                         | $\times$                                                                                                                                                 | $- \Delta t \frac{P_{\min}}{LAG}$                                                                                                                        |

All these equations can then be grouped into two inequalities, simply by multiplying the bounds by the associated binary
transition variable.

> **Additional remarks**
> - The constant power rate of the ramping states is materialized through the same values being used for the lower and
    upper bounds of the power variation
> - Note the use of power gradients whenever the On state is implicated
> - The reduced lead and lag times are used for the power gradients when the generator transitions from/to the On state
    to/from a ramping state. This denotes the actual time during which the power is "free" (or simply constrained by
    gradients).
