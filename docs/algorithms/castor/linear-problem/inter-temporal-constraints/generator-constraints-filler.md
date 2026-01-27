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
| Timestamps                      | $\mathcal{T}$              | Set of all timestamps on which the optimization is performed.                                                                               |
| Time gap                        | $\Delta_{i \rightarrow j}$ | Time gap between timestamps $i$ and $j$ (with $i < j$).                                                                                     |
| Generator states                | $\Omega_{generator}$       | Set of all possible states o generator can be in: $\lbrace \textcolor{green}{\text{ON}}, \textcolor{red}{\text{OFF}}, \textcolor{blue}{\text{START UP}}, \textcolor{orange}{\text{SHUT DOWN}} \rbrace$                |

## Defined optimization variables

| Name                               | Symbol                          | Details                                                                                                                           | Type       | Index                                                                                                                                                                                              | Unit | Lower bound | Upper bound |
|------------------------------------|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------|-------------|-------------|
| Generator power                    | $P(g,s,t)$                      | the power of generator $g$ at grid state $s$ of timestamp $t$                                                                     | Real value | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$                                                                                                           | MW   | $-\infty$   | $+\infty$   |
| Generator state                    | $\delta_{\omega}^{gen}(g,s,t)$  | whether generator $g$'s state is $\omega$ at grid state $s$ of timestamp $t$ or not                                               | Binary     | one per generator defined in $\Gamma$, per generator state $\omega \in \Omega_{generator}$, per grid state and per timestamp of $\mathcal{T}$                                                      | -    | 0           | 1           |
| Generator state transition         | $T_{\omega \to \omega'}(g,s,t)$ | whether generator $g$'s state has transitioned from $\omega$ to $\omega'$ at grid state $s$ of timestamp $t$ or not               | Binary     | one per generator defined in $\Gamma$, per generator state $\omega \in \Omega_{generator}$, per generator state $\omega' \in \Omega_{generator}$ per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |

## Used optimization variables

| Name                   | Symbol     | Defined in                                                                    |
|------------------------|------------|-------------------------------------------------------------------------------|
| Range action set-point | $A(r,s,t)$ | [CoreProblemFiller](../core-problem-filler.md#defined-optimization-variables) |

## Defined constraints

### Only one state

At each timestamp and for each grid state, each generator can only be in one of the four states among ON, OFF, SHUT DOWN
and START UP. Mathematically, this is written as:

$$\forall g \in \Gamma, \forall t \in \mathcal{T}, \forall s, \; \sum_{\omega \in \Omega_{generator}} \delta_{\omega}^{gen}(g,s,t) = 1$$

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

##### *State-From* constraints

The state at the end of the previous timestamp must be the starting state of the transition that occurred during the
timestamp, no matter the final state.

$$\forall g \in \Gamma, \forall t \in \mathcal{T}, \forall s, \forall \omega \in \Omega_{generator}, \; \delta_{\omega}^{gen}(g,s,t) = \sum_{\omega' \in \Omega_{generator}} T_{\omega \to \omega'}(g,s,t)$$

##### *State-To* constraints

The state at the end of the timestamp must be the final state of the transition that occurred during the timestamp, no
matter the state of origin.

$$\forall g \in \Gamma, \forall t \in \mathcal{T}, \forall s, \forall \omega \in \Omega_{generator}, \; \delta_{\omega}^{gen}(g,s,t + 1) = \sum_{\omega' \in \Omega_{generator}} T_{\omega' \to \omega}(g,s,t)$$

### Ramp constraints

#### Ramp-Up state

If the generator starts ramping up at a given timestamp, it is thus constrained to the Ramp-Up state for a duration that
covers its lead time. Thus:

$$\forall t' > t \text{ such that } \Delta_{t \rightarrow t'} < LEAD(g), \; T_{\textcolor{red}{\text{OFF}} \to \textcolor{blue}{\text{START UP}}}(g,s,t) \leq \delta_{\textcolor{blue}{\text{START UP}}}^{gen}(g,s,t')$$

#### Ramp-Down state

Similarly, if the generator stops ramping down at a given timestamp, it must have been constrained to the Ramp-Down
state for a duration that covers its lag time. Thus:

$$\forall t' \leq t \text{ such that } \Delta_{t' \rightarrow t + 1} < LAG(g), \; T_{\textcolor{orange}{\text{SHUT DOWN}} \to \textcolor{red}{\text{OFF}}}(g,s,t) \leq \delta_{\textcolor{orange}{\text{SHUT DOWN}}}^{gen}(g,s,t')$$

### Power constraints

#### Off state

By definition, $\delta_{\textcolor{red}{\text{OFF}}}^{gen}(g,s,t) = 1 \Leftrightarrow P(g,s,t) = 0$. However, to account
for issues that can stem from number rounding, we define a **minimal power variation deadband** $\Delta P_{\min}$ such
that if the power of the generator is lower that $\Delta P_{\min}$, the generator is considered as off. Thus,
$\delta_{\textcolor{red}{\text{OFF}}}^{gen}(g,s,t) = 1 \Leftrightarrow P(g,s,t) \leq \Delta P_{\min}$.

$$\Delta P_{\min} (1 - \delta_{\textcolor{red}{\text{OFF}}}^{gen}(g,s,t)) \leq P(g,s,t) \leq P_{\max}(g) (1 - \delta_{\textcolor{red}{\text{OFF}}}^{gen}(g,s,t)) + \Delta P_{\min} \delta_{\textcolor{red}{\text{OFF}}}^{gen}(g,s,t)$$

#### On state

By definition, $\delta_{\textcolor{green}{\text{ON}}}^{gen}(g,s,t) = 1 \Leftrightarrow P(g,s,t) \geq P_{\min}(g)$ which can be linearized using the two following
equations:

$$P_{\min} \delta_{\textcolor{green}{\text{ON}}}^{gen}(g,s,t) \leq P(g,s,t) \leq P_{\min}(g) (1 - \delta_{\textcolor{green}{\text{ON}}}^{gen}(g,s,t)) + P_{\max}(g) \delta_{\textcolor{green}{\text{ON}}}^{gen}(g,s,t)$$

#### Power variation

The power of the generator determines the state of the generator. The two previous sections showed how to identify the
on and off state. In between these are the ramping states which can be determined based on the previous state and/or
whether the power is increasing (ramp-up) or decreasing (ramp-down).

More generally, the power variation and the state transitions are strongly entangled and constrain one another.
Depending on the state transition, the power variation $P(g,s,t+1) - P(g,s,t)$ is bounded differently.

##### Off to Off transition

The power remains constant and null between the two timestamp:

$$P(g,s,t) = P(g,s,t+1) = 0$$

##### Off to Ramp-Up transition

> The following holds only if $\Delta_{t \rightarrow t+1} < LEAD(g)$.

The power increase is constrained by the upward power ramp:

$$P(g,s,t+1) - P(g,s,t) = \frac{P_{\min}(g)}{LEAD(g)}\Delta_{t \rightarrow t+1}$$

> 💡 **Participation to the global constraint**
>
> When including this equation in the global constraint, both bounds must be multiplied by the binary variable
> $T_{OFF \to RU}(g,s,t)$.

##### Off to On transition

> The following holds only if $\Delta_{t \rightarrow t+1} \geq LEAD(g)$.

The power jumps from 0 to $P_{\min}(g)$ at least. Then it is free to vary in a range that is bounded by $P_{\max}(g)$ or
the upward power gradient (considered infinite if not defined). Note that the power gradient only holds for the time
when the generator power is _free_, which happens at the end of the ramp and before the end of the timestamp, i.e. for a
duration of $\Delta_{t \rightarrow t+1} - LEAD(g)$.

$$P_{\min}(g) \leq P(g,s,t+1) - P(g,s,t) \leq P_{\min}(g) + \left ( \Delta_{t \rightarrow t+1} - LEAD(g) \right )
\nabla^{+}(g)$$

> 💡 **Participation to the global constraint**
>
> When including this equation in the global constraint, both bounds must be multiplied by the binary variable
> $T_{OFF \to ON}(g,s,t)$.

##### Ramp-Up to Ramp-Up transition

The power increase is constrained by the upward power ramp:

$$P(g,s,t+1) - P(g,s,t) = \frac{P_{\min}(g)}{LEAD(g)}\Delta_{t \rightarrow t+1}$$

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

$$\tau_{\infty}^{\nearrow}(t) = \min \lbrace t' \geq t \; | \; \Delta_{t \rightarrow t'} \geq LEAD(g) \rbrace$$

Note that several timestamps can have the same value of $\tau_{\infty}^{\nearrow}$. Then, $\forall t' < t$ such that
$\tau_{\infty}^{\nearrow}(t') = t$, considering that the ramping up started at $t'$, the power variation is constrained
as:

$$(LEAD(g) - \Delta_{t' \rightarrow t-1}) \frac{P_{\min}(g)}{LEAD(g)} \leq P(g,s,t+1) - P(g,s,t) \leq (LEAD(g) -
\Delta_{t' \rightarrow t-1}) \frac{P_{\min}(g)}{LEAD(g)} +(\Delta_{t' \rightarrow t} - LEAD(g)) \nabla^{+}(g)$$

> 💡 **Participation to the global constraint**
>
> When including this equation in the global constraint, both bounds must be multiplied by the binary variable
> $T_{OFF \to RU}(g,s,t')$, for all $t'$ such that $\tau_{\infty}^{\nearrow}(t') = t$.

##### On to Off transition

> The following holds only if $\Delta_{t \rightarrow t+1} \geq LAG(g)$.

The power decreases from $P_{\min}(g)$ to 0 at least. Before the ramp, the power is free to vary in a range that is
bounded by $P_{\max}(g)$ or the downward power gradient (considered negatively infinite if not defined). Note that the
power gradient only holds for the time when the generator power is _free_, which happens before the ramp and after the
beginning of the timestamp, i.e. for a duration of $\Delta_{t \rightarrow t+1} - LAG(g)$.

$$-P_{\min}(g) + \left ( \Delta_{t \rightarrow t+1} - LAG(g) \right ) \nabla^{-}(g) \leq P(g,s,t) - P(g,s,t-1) \leq
-P_{\min}(g)$$

> 💡 **Participation to the global constraint**
>
> When including this equation in the global constraint, both bounds must be multiplied by the binary variable
> $T_{ON \to OFF}(g,s,t)$.

##### On to On transition

The power is simply bounded by the power gradients:

$$\Delta_{t \rightarrow t+1} \nabla^{-}(g) \leq P(g,s,t+1) - P(g,s,t) \leq \Delta_{t\rightarrow t+1} \nabla^{+}(g)$$

> 💡 **Participation to the global constraint**
>
> When including this equation in the global constraint, both bounds must be multiplied by the binary variable
> $T_{ON \to ON}(g,s,t)$.

##### On to Ramp-Down transition

This transition is decomposed into two parts: a first part corresponding to a time when the generator power is _free_
and the beginning of the downward power ramp. However, if the different timestamps are not evenly distributed over time,
it may not be possible to identify the precise timestamp at which the ramping-down will end.

For a given timestamp $t$, we define $\tau_{0}^{\searrow}(t)$ as the unique timestamp at which the ramping down began
given that it ends at $t$ (i.e. when the generator will transition to the Off state). Mathematically:

$$\tau_{0}^{\searrow}(t) = \max \lbrace t' \leq t \; | \; \Delta_{t' \rightarrow t} \geq LAG(g) \rbrace$$

Note that several timestamps can have the same value of $\tau_{0}^{\searrow}$. Then, $\forall t' > t$ such that
$\tau_{0}^{\searrow}(t') = t$, considering that the ramping down started at $t$ and ends at $t'$, the power variation is
constrained as:

$$- (LAG(g) - \Delta_{t+1 \rightarrow t'}) \frac{P_{\min}(g)}{LAG(g)} + (\Delta_{t \rightarrow t'} - LAG(g))
\nabla^{-}(g) \leq P(g,s,t+1) - P(g,s,t) \leq - (LAG(g) - \Delta_{t+1 \rightarrow t'}) \frac{P_{\min}(g)}{LAG(g)}$$

> 💡 **Participation to the global constraint**
>
> When including this equation in the global constraint, both bounds must be multiplied by the binary variable
> $T_{RD \to OFF}(g,s,t'-1)$, for all $t'$ such that $\tau_{0}^{\searrow}(t') = t$.

##### Ramp-Down to Off transition

The power decrease is constrained by the downward power ramp:

$$P(g,s,t+1) - P(g,s,t) = - \frac{P_{\min}(g)}{LAG(g)}\Delta_{t \rightarrow t+1}$$

> 💡 **Participation to the global constraint**
>
> When including this equation in the global constraint, both bounds must be multiplied by the binary variable
> $T_{RD \to OFF}(g,s,t)$.

##### Ramp-Down to Ramp-Down transition

The power decrease is constrained by the downward power ramp:

$$P(g,s,t+1) - P(g,s,t) = - \frac{P_{\min}(g)}{LAG(g)}\Delta_{t \rightarrow t+1}$$

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

$$\begin{align*}
0 \times T_{\textcolor{red}{\text{OFF}} \to \textcolor{red}{\text{OFF}}}(g,s,t) & & 0 \times T_{\textcolor{red}{\text{OFF}} \to \textcolor{red}{\text{OFF}}}(g,s,t) \\
+ \frac{P_{\min}}{LEAD(g)} \Delta_{t \to t + 1} T_{\textcolor{red}{\text{OFF}} \to \textcolor{blue}{\text{START UP}}}(g,s,t) & & + \frac{P_{\min}}{LEAD(g)} \Delta_{t \to t + 1} T_{\textcolor{red}{\text{OFF}} \to \textcolor{blue}{\text{START UP}}}(g,s,t) \\
+ P_{\min} T_{\textcolor{red}{\text{OFF}} \to \textcolor{green}{\text{ON}}}(g,s,t) & & + \left [ P_{\min} + (\Delta_{t \to t + 1} - LEAD(g)) \nabla^{+}(g) \right ] T_{\textcolor{red}{\text{OFF}} \to \textcolor{green}{\text{ON}}}(g,s,t) \\
+ \frac{P_{\min}}{LEAD(g)} \Delta_{t \to t + 1} T_{\textcolor{blue}{\text{START UP}} \to \textcolor{blue}{\text{START UP}}}(g,s,t) & & + \frac{P_{\min}}{LEAD(g)} \Delta_{t \to t + 1} T_{\textcolor{blue}{\text{START UP}} \to \textcolor{blue}{\text{START UP}}}(g,s,t) \\
+ T_{\textcolor{blue}{\text{START UP}} \to \textcolor{green}{\text{ON}}}(g,s,t) & & + T_{\textcolor{blue}{\text{START UP}} \to \textcolor{green}{\text{ON}}}(g,s,t) \\
+ \nabla^{-}(g) \Delta_{t \to t + 1} T_{\textcolor{green}{\text{ON}} \to \textcolor{green}{\text{ON}}}(g,s,t) & \leq P(g,s,t + 1) - P(g,s,t) \leq & + \nabla^{+}(g) \Delta_{t \to t + 1} T_{\textcolor{green}{\text{ON}} \to \textcolor{green}{\text{ON}}}(g,s,t) \\
- T_{\textcolor{green}{\text{ON}} \to \textcolor{orange}{\text{SHUT DOWN}}}(g,s,t) & & + T_{\textcolor{green}{\text{ON}} \to \textcolor{orange}{\text{SHUT DOWN}}}(g,s,t) \\
- \frac{P_{\min}}{LAG(g)} \Delta_{t \to t + 1} T_{\textcolor{orange}{\text{SHUT DOWN}} \to \textcolor{orange}{\text{SHUT DOWN}}}(g,s,t) & & - \frac{P_{\min}}{LAG(g)} \Delta_{t \to t + 1} T_{\textcolor{orange}{\text{SHUT DOWN}} \to \textcolor{orange}{\text{SHUT DOWN}}}(g,s,t) \\
- \frac{P_{\min}}{LAG(g)} \Delta_{t \to t + 1} T_{\textcolor{orange}{\text{SHUT DOWN}} \to \textcolor{red}{\text{OFF}}}(g,s,t) & & - \frac{P_{\min}}{LAG(g)} \Delta_{t \to t + 1} T_{\textcolor{orange}{\text{SHUT DOWN}} \to \textcolor{red}{\text{OFF}}}(g,s,t) \\
- \left [ P_{\min} - (\Delta_{t \to t + 1} - LAG(g)) \nabla^{-}(g) \right ] T_{\textcolor{green}{\text{ON}} \to \textcolor{red}{\text{OFF}}}(g,s,t) & & - P_{\min} T_{\textcolor{green}{\text{ON}} \to \textcolor{red}{\text{OFF}}}(g,s,t)
\end{align*}$$

### Injection to generator power constraint

The power of the generator can be linked to the set-point of the injection range action $r$ it is involved in:

$$P(g,s,t) = \frac{\sigma(g)}{k(g,r)} A(r,s,t)$$

where $k(g,r)$ is the injection key of $g$ in $r$ and $\sigma(g) = 1$ if $g$ is a generator or $-1$ if it is a load.
