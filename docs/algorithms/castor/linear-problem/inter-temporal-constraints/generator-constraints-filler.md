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

The life of a generator thus cycles through these four states, always in the same order: OFF → RAMP-UP → ON → RAMP-DOWN
→ OFF ...

## Hypotheses

To simplify the problem, we will consider that the orders to switch a generator on are given at the beginning of the
timestamps, and not some time in between two timestamps. Thus, the upward ramps always start at times that coincide with
the problem's timestamps.

Similarly, a symmetric hypothesis holds for downward ramps, with the constraint that the power of the generator must
reach zero exactly at the beginning of a timestamp. Thus, the downward ramps always end at times that coincide with the
problem's timestamps.

## Used input data

| Name                            | Symbol                     | Details                                                                                                                                     |
|---------------------------------|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| Constrained generators set      | $\Gamma$                   | Set of generators with constraints defined                                                                                                  |
| PMin                            | $P_{\min}(g)$              | Minimum operating power of generator $g$. This value must be non-negative.                                                                  |
| PMax                            | $P_{\max}(g)$              | Maximum operating power of generator $g$. This value must be non-negative.                                                                  |
| Lead Time                       | $LEAD(g)$                  | Maximum operating power of generator $g$. This value must be non-negative.                                                                  |
| Lag Time                        | $LAG(g)$                   | Maximum operating power of generator $g$. This value must be non-negative.                                                                  |
| Upper power gradient constraint | $\nabla^{+}(g)$            | Maximum upward power variation between two consecutive timestamps for generator $g$. This value must be non-negative.                       |
| Lower power gradient constraint | $\nabla^{-}(g)$            | Maximum downward power variation (in absolute value) between two consecutive timestamps for generator $g$. This value must be non-positive. |
| Initial generator power         | $p_{0}(g,s,t)$             | Initial power of generator $g$ at grid state $s$ of timestamp $t$, as defined in the network.                                               |
| Timestamps                      | $\mathcal{T}$              | Set of all timestamps on which the optimization is performed.                                                                               |
| Time gap                        | $\Delta_{i \rightarrow j}$ | Time gap between timestamps $i$ and $j$ (with $i < j$).                                                                                     |

## Defined optimization variables

| Name                               | Symbol                   | Details                                                                                                                           | Type       | Index                                                                                    | Unit | Lower bound | Upper bound |
|------------------------------------|--------------------------|-----------------------------------------------------------------------------------------------------------------------------------|------------|------------------------------------------------------------------------------------------|------|-------------|-------------|
| Generator power                    | $P(g,s,t)$               | the power of generator $g$ at grid state $s$ of timestamp $t$                                                                     | Real value | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | MW   | $-\infty$   | $+\infty$   |
| *On* State                         | $ON(g,s,t)$              | whether generator $g$ is on at grid state $s$ of timestamp $t$ or not                                                             | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Off* State                        | $OFF(g,s,t)$             | whether generator $g$ is off at grid state $s$ of timestamp $t$ or not                                                            | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Up* State                    | $RU(g,s,t)$              | whether generator $g$ is ramping up at grid state $s$ of timestamp $t$ or not                                                     | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Down* State                  | $RD(g,s,t)$              | whether generator $g$ is ramping down at grid state $s$ of timestamp $t$ or not                                                   | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *On → On* Transition               | $T_{ON \to ON}(g,s,t)$   | whether generator $g$ has transitioned from an "on" state to an "on" state, at grid state $s$ of timestamp $t$ or not             | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *On → Off* Transition              | $T_{ON \to OFF}(g,s,t)$  | whether generator $g$ has transitioned from an "on" state to an "off" state, at grid state $s$ of timestamp $t$ or not            | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *On → Ramp-Down* Transition        | $T_{ON \to RD}(g,s,t)$   | whether generator $g$ has transitioned from an "on" state to a "ramp-up" state, at grid state $s$ of timestamp $t$ or not         | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *On → Ramp-Up* Transition          | $T_{ON \to RU}(g,s,t)$   | whether generator $g$ has transitioned from an "on" state to a "ramp-down" state, at grid state $s$ of timestamp $t$ or not       | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Off → On* Transition              | $T_{OFF \to ON}(g,s,t)$  | whether generator $g$ has transitioned from an "off" state to an "on" state, at grid state $s$ of timestamp $t$ or not            | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Off → Off* Transition             | $T_{OFF \to OFF}(g,s,t)$ | whether generator $g$ has transitioned from an "off" state to an "off" state, at grid state $s$ of timestamp $t$ or not           | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Off → Ramp-Down* Transition       | $T_{OFF \to RD}(g,s,t)$  | whether generator $g$ has transitioned from an "off" state to a "ramp-down" state, at grid state $s$ of timestamp $t$ or not      | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Off → Ramp-Up* Transition         | $T_{OFF \to RU}(g,s,t)$  | whether generator $g$ has transitioned from an "off" state to a "ramp-up" state, at grid state $s$ of timestamp $t$ or not        | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Down → On* Transition        | $T_{RD \to ON}(g,s,t)$   | whether generator $g$ has transitioned from a "ramp-down" state to an "on" state, at grid state $s$ of timestamp $t$ or not       | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Down → Off* Transition       | $T_{RD \to OFF}(g,s,t)$  | whether generator $g$ has transitioned from a "ramp-down" state to an "off" state, at grid state $s$ of timestamp $t$ or not      | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Down → Ramp-Down* Transition | $T_{RD \to RD}(g,s,t)$   | whether generator $g$ has transitioned from a "ramp-down" state to a "ramp-down" state, at grid state $s$ of timestamp $t$ or not | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Down → Ramp-Up* Transition   | $T_{RD \to RU}(g,s,t)$   | whether generator $g$ has transitioned from a "ramp-down" state to a "ramp-up" state, at grid state $s$ of timestamp $t$ or not   | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Up → On* Transition          | $T_{RU \to ON}(g,s,t)$   | whether generator $g$ has transitioned from a "ramp-up" state to an "on" state, at grid state $s$ of timestamp $t$ or not         | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Up → Off* Transition         | $T_{RU \to OFF}(g,s,t)$  | whether generator $g$ has transitioned from a "ramp-up" state to an "off" state, at state  $s$ of timestamp $t$ or not            | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Up → Ramp-Down* Transition   | $T_{RU \to RD}(g,s,t)$   | whether generator $g$ has transitioned from a "ramp-up" state to a "ramp-down" state, at grid state $s$ of timestamp $t$ or not   | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |
| *Ramp-Up → Ramp-Up* Transition     | $T_{RU \to RU}(g,s,t)$   | whether generator $g$ has transitioned from a "ramp-up" state to a "ramp-up" state, at grid state $s$ of timestamp $t$ or not     | Binary     | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |

## Defined constraints

### Only one state

At each timestamp and for each grid state, each generator can only be in one of the four states among ON, OFF, RAMP-DOWN
and RAMP-UP. Mathematically, this is written as:

$$\forall g \in \Gamma, \forall t \in \mathcal{T}, \forall s, \; ON(g,s,t) + OFF(g,s,t) + RD(g,s,t) + RU(g,s,t) = 1$$

### State transition constraints

Some transitions are impossible which means that the value of the associated binary variables is always forced to 0.
These impossible transitions are:

- Ramp-Up →Ramp-Down
- Ramp-Up → Off
- Ramp-Down → Ramp-Up
- Ramp-Down → On

> 💡 In the code, the associated binary variables are simply not created in order not to overload the model.

Besides, when the lead/lag time is shorter than the duration of a timestamp, the generator can be completely switched on
or off during the timestamp. For simplicity's sake, we thus force the generator to be either on or off at the end of
each timestamp in such conditions. In that case, the ramping states (Ramp-Up and Ramp-Down) are unreachable and the
transitions to or from these states are straightforwardly impossible. All of these binary variables are forced to 0.

> 💡 Once again, in the code, and under such conditions, the binary variables associated to the ramping states and the
> transitions in which they are involved are not created.

The table belows gathers all the possible generator state transitions and their associated condition if any:

| ↓ From / To → | $OFF$                                       | $RU$                                      | $ON$                                         | $RD$                                     |
|---------------|---------------------------------------------|-------------------------------------------|----------------------------------------------|------------------------------------------|
| $OFF$         | ✅                                           | ✳️ if $\Delta_{t-1 \rightarrow t} < LEAD$ | ✳️ if $\Delta_{t-1 \rightarrow t} \geq LEAD$ | ❌                                        |
| $RU$          | ❌                                           | ✅                                         | ✅                                            | ❌                                        |
| $ON$          | ✳️ if $\Delta_{t-1 \rightarrow t} \geq LAG$ | ❌                                         | ✅                                            | ✳️ if $\Delta_{t-1 \rightarrow t} > LAG$ |
| $RD$          | ✅                                           | ❌                                         | ❌                                            | ✅                                        |

#### Chaining

To ensure a continuity in the cycle of generator states, a _chaining_ needs to be set up thanks to linear constraints
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

### Ramp constraints

#### Ramp-Up state

If the generator starts ramping up at a given timestamp, it is thus constrained to the Ramp-Up state for a duration that
covers its lead time. Thus:

$$\forall t' > t \text{ such that } \Delta_{t-1 \rightarrow t'} < LEAD(g), \; T_{OFF \to RU}(g,s,t) \leq RU(g,t',s)$$

#### Ramp-Down state

Similarly, if the generator stops ramping down at a given timestamp, it must have been constrained to the Ramp-Down
state for a duration that covers its lag time. Thus:

$$\forall t' < t \text{ such that } \Delta_{t' \rightarrow t} < LAG(g), \; T_{RD \to OFF}(g,s,t) \leq RD(g,t',s)$$

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
whether the power is increasing (ramp-up) or decreasing (ramp-down).

More generally, the power variation and the state transitions are strongly entangled and constrain one another.
Depending on the state transition, the power variation $P(g,s,t) - P(g,s,t-1)$ is bounded differently.

##### Off to Off transition

The power remains constant and null between the two timestamp:

$$P(g,s,t-1) = P(g,s,t) = 0$$

##### Off to Ramp-Up transition

> The following holds only if $\Delta_{t-1 \rightarrow t} < LEAD(g)$.

The power increase is constrained by the upward power ramp:

$$P(g,s,t) - P(g,s,t-1) = \frac{P_{\min}(g)}{LEAD(g)}$$

> 💡 **Participation to the global constraint**
>
> When including this equation in the global constraint, both bounds must be multiplied by the binary variable
> $T_{OFF \to RU}(g,s,t)$.

##### Off to On transition

> The following holds only if $\Delta_{t-1 \rightarrow t} \geq LEAD(g)$.

The power jumps from 0 to $P_{\min}(g)$ at least. Then it is free to vary in a range that is bounded by $P_{\max}(g)$ or
the upward power gradient (considered infinite if not defined). Note that the power gradient only holds for the time
when the generator power is _free_, which happens at the end of the ramp and before the end of the timestamp, i.e. for a
duration of $\Delta_{t-1 \rightarrow t} - LEAD(g)$.

$$P_{\min}(g) \leq P(g,s,t) - P(g,s,t-1) \leq P_{\min}(g) + \min
\left [ P_{\max}(g) - P_{\min}(g), \left ( \Delta_{t-1 \rightarrow t} - LEAD(g) \right ) \nabla^{+}(g) \right ]$$

> 💡 **Participation to the global constraint**
>
> When including this equation in the global constraint, both bounds must be multiplied by the binary variable
> $T_{OFF \to ON}(g,s,t)$.

##### Ramp-Up to Ramp-Up transition

The power increase is constrained by the upward power ramp:

$$P(g,s,t) - P(g,s,t-1) = \frac{P_{\min}(g)}{LEAD(g)}$$

> 💡 **Participation to the global constraint**
>
> When including this equation in the global constraint, both bounds must be multiplied by the binary variable
> $T_{RU \to RU}(g,s,t)$.

##### Ramp-Up to On transition

This transition is decomposed into two parts: a first part corresponding to the end of the upward power ramp and a
second one corresponding to the remaining time during which the power is _free_. However, if the different timestamps
are not evenly distributed over time, it may not be possible to identify the precise timestamp at which the ramping-up
began.

<!-- TODO: prove this with a simple situation 46 / 30 / 60 and lead time of 1,75h -->

For a given timestamp $t$, we define $\tau_{\infty}^{\nearrow}(t)$ as the timestamp during which the ramping up will
finish (i.e. when the generator will transition to the On state). Mathematically:

$$\tau_{\infty}^{\nearrow}(t) = \min \lbrace t' \geq t \; | \; \Delta_{t-1 \rightarrow t'} \geq LEAD(g) \rbrace$$

Note that several timestamps can have the same value of $\tau_{\infty}^{\nearrow}$. Then, $\forall t' < t$ such that
$\tau_{\infty}^{\nearrow}(t') = t$, considering that the ramping up started at $t'$, the power variation is constrained
as:

$$(LEAD(g) - \Delta_{t' \rightarrow t-1}) \frac{P_{\min}(g)}{LEAD(g)} \leq P(g,s,t) - P(g,s,t-1) \leq (LEAD(g) - \Delta_
{t' \rightarrow t-1}) \frac{P_{\min}(g)}{LEAD(g)} + \min
\left [ P_{\max}(g) - P_{\min}(g), (\Delta_{t' \rightarrow t} - LEAD(g)) \nabla^{+}(g) \right ]$$

> 💡 **Participation to the global constraint**
> 
> When including this equation in the global constraint, both bounds must be multiplied by the binary variable
> $T_{OFF \to RU}(g,s,t')$, for all $t'$ such that $\tau_{\infty}^{\nearrow}(t') = t$.

##### On to Off transition

> The following holds only if $\Delta_{t-1 \rightarrow t} \geq LAG(g)$.

The power decreases from $P_{\min}(g)$ to 0 at least. Before the ramp, the power is free to vary in a range that is
bounded by $P_{\max}(g)$ or the downward power gradient (considered negatively infinite if not defined). Note that the
power gradient only holds for the time when the generator power is _free_, which happens before the ramp and after the
beginning of the timestamp, i.e. for a duration of $\Delta_{t-1 \rightarrow t} - LAG(g)$.

$$-P_{\min}(g) + \max
\left [ P_{\min}(g) - P_{\max}(g), \left ( \Delta_{t-1 \rightarrow t} - LAG(g) \right ) \nabla^{-}(g) \right ] \leq P(
g,s,t) - P(g,s,t-1) \leq -P_{\min}(g)$$

> 💡 **Participation to the global constraint**
>
> When including this equation in the global constraint, both bounds must be multiplied by the binary variable
> $T_{ON \to OFF}(g,s,t)$.

##### On to On transition

The power is simply bounded by the power gradients:

$$\Delta_{t-1 \rightarrow t} \nabla^{-}(g) \leq P(g,s,t) - P(g,s,t-1) \leq \Delta_{t-1 \rightarrow t} \nabla^{+}(g)$$

> 💡 **Participation to the global constraint**
>
> When including this equation in the global constraint, both bounds must be multiplied by the binary variable
> $T_{ON \to ON}(g,s,t)$.

##### On to Ramp-Down transition

$$\tau_{0}^{\searrow}(t) = \max \lbrace t' \leq t \; | \; \Delta_{t'-1 \rightarrow t} \geq LAG(g) \rbrace$$

$\forall t' > t$ such that $\tau_{0}^{\searrow}(t) = t'$, considering that the ramping down started at $t$, the power
variation is constrained as:

$$- (LAG(g) - \Delta_{t \rightarrow t'}) \frac{P_{\min}(g)}{LAG(g)} \leq P(g,s,t) - P(g,s,t-1) \leq - (LAG(g) - \Delta_{t \rightarrow t'}) \frac{P_{\min}(g)}{LAG(g)} + \max \left [ P_{\min}(g) - P_{\max}(g), (\Delta_{t-1 \rightarrow t'} - LAG(g)) \nabla^{-}(g) \right ]$$

##### Ramp-Ramp to Off transition

The power decrease is constrained by the downward power ramp:

$$P(g,s,t) - P(g,s,t-1) = - \frac{P_{\min}(g)}{LAG(g)}$$

> 💡 **Participation to the global constraint**
>
> When including this equation in the global constraint, both bounds must be multiplied by the binary variable
> $T_{RD \to OFF}(g,s,t)$.

##### Ramp-Down to Ramp-Down transition

The power decrease is constrained by the downward power ramp:

$$P(g,s,t) - P(g,s,t-1) = - \frac{P_{\min}(g)}{LAG(g)}$$

> 💡 **Participation to the global constraint**
>
> When including this equation in the global constraint, both bounds must be multiplied by the binary variable
> $T_{RD \to RD}(g,s,t)$.

##### Summary

All these equations can then be grouped into two inequalities, simply by multiplying the bounds by the associated binary
transition variable.

> **Additional remarks**
> - The constant power rate of the ramping states is materialized through the same values being used for the lower and
    upper bounds of the power variation
> - Note the use of power gradients whenever the On state is implicated
