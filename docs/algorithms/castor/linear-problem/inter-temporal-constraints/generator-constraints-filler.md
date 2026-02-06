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
- it is **starting up** if its power is increasing from zero to $P_{\min}$, at a fixed pace of $\frac{P_{\min}}{LEAD}$ (
  in MW/h);
- it is **shutting down** if its power is decreasing from $P_{\min}$ to zero, at a fixed pace of $\frac{P_{\min}}{LAG}$ (
  in MW/h).

![Generator States](../../../../_static/img/generator-states.png){.forced-white-background}

The life of a generator thus cycles through these four states, always in the same order: OFF → START UP → ON → SHUT DOWN
→ OFF ...

## Hypotheses

To simplify the problem, we will consider that the orders to switch a generator on are given at the beginning of the
timestamps, and not some time in between two timestamps. Thus, the upward ramps always start at times that coincide with
the problem's timestamps.

Similarly, a symmetric hypothesis holds for downward ramps, with the constraint that the power of the generator must
reach zero exactly at the beginning of a timestamp. Thus, the downward ramps always end at times that coincide with the
problem's timestamps.

## Used input data

| Name                            | Symbol               | Details                                                                                                                                     |
|---------------------------------|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| Constrained generators set      | $\Gamma$             | Set of generators with constraints defined                                                                                                  |
| PMin                            | $P_{\min}(g)$        | Minimum operating power of generator $g$. This value must be non-negative.                                                                  |
| PMax                            | $P_{\max}(g)$        | Maximum operating power of generator $g$. This value must be non-negative.                                                                  |
| Lead Time                       | $LEAD(g)$            | Maximum operating power of generator $g$. This value must be non-negative.                                                                  |
| Lag Time                        | $LAG(g)$             | Maximum operating power of generator $g$. This value must be non-negative.                                                                  |
| Upper power gradient constraint | $\nabla^{+}(g)$      | Maximum upward power variation between two consecutive timestamps for generator $g$. This value must be non-negative.                       |
| Lower power gradient constraint | $\nabla^{-}(g)$      | Maximum downward power variation (in absolute value) between two consecutive timestamps for generator $g$. This value must be non-positive. |
| Timestamps                      | $\mathcal{T}$        | Set of all timestamps on which the optimization is performed.                                                                               |
| Time gap                        | $\Delta_{\tau}$      | Time gap between timestamps two consecutive timestamps. It is assumed constant for all pairs of consecutive timestamps.                     |
| Generator states                | $\Omega_{generator}$ | Set of all possible states a generator can be in: $\lbrace \textcolor{green}{\text{ON}}, \textcolor{red}{\text{OFF}} \rbrace$               |

## Defined optimization variables

| Name                       | Symbol                          | Details                                                                                                             | Type       | Index                                                                                                                                                                                              | Unit | Lower bound | Upper bound |
|----------------------------|---------------------------------|---------------------------------------------------------------------------------------------------------------------|------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------|-------------|-------------|
| Generator power            | $P(g,s,t)$                      | the power of generator $g$ at grid state $s$ of timestamp $t$                                                       | Real value | one per generator defined in $\Gamma$, per grid state and per timestamp of $\mathcal{T}$                                                                                                           | MW   | $-\infty$   | $+\infty$   |
| Generator state            | $\delta_{\omega}^{gen}(g,s,t)$  | whether generator $g$'s state is $\omega$ at grid state $s$ of timestamp $t$ or not                                 | Binary     | one per generator defined in $\Gamma$, per generator state $\omega \in \Omega_{generator}$, per grid state and per timestamp of $\mathcal{T}$                                                      | -    | 0           | 1           |
| Generator state transition | $T_{\omega \to \omega'}(g,s,t)$ | whether generator $g$'s state has transitioned from $\omega$ to $\omega'$ at grid state $s$ of timestamp $t$ or not | Binary     | one per generator defined in $\Gamma$, per generator state $\omega \in \Omega_{generator}$, per generator state $\omega' \in \Omega_{generator}$ per grid state and per timestamp of $\mathcal{T}$ | -    | 0           | 1           |

## Used optimization variables

| Name                   | Symbol     | Defined in                                                                    |
|------------------------|------------|-------------------------------------------------------------------------------|
| Range action set-point | $A(r,s,t)$ | [CoreProblemFiller](../core-problem-filler.md#defined-optimization-variables) |

## Defined constraints

### Only one state

At each timestamp and for each grid state, each generator can only be in one of the four states among ON, OFF, SHUT DOWN
and START UP. Mathematically, this is written as:

$$\forall g \in \Gamma, \forall t \in \mathcal{T}, \forall s, \; \delta_{\textcolor{green}{\text{ON}}}^{gen}(g,s,t) + \delta_{\textcolor{red}{\text{OFF}}}^{gen}(g,s,t) = 1$$

> $\delta_{\textcolor{red}{\text{OFF}}}^{gen}(g,s,t)$ is defined in the documentation for readability's sake. In the
> code, only $\delta_{\textcolor{green}{\text{ON}}}^{gen}(g,s,t)$ is being used not to create useless variables. Thus,
> all occurrences of $\delta_{\textcolor{red}{\text{OFF}}}^{gen}(g,s,t)$ are simply replaced by
> $1 - \delta_{\textcolor{green}{\text{ON}}}^{gen}(g,s,t)$.

### On or Off State

By definition, the generator is on if its power is greater than $P_{\min}$ and off if its power is null. To account for
issues that can stem from number rounding, we define a _minimal power variation deadband_ $\epsilon_{P}^{\text{OFF}}$
such that if the power of the generator is lower that $\epsilon_{P}^{\text{OFF}}$, the generator is considered off.

$$P_{\min} \delta_{\textcolor{green}{\text{ON}}}^{gen}(g,s,t) \leq P(g,s,t) \leq P_{\max}(g) \delta_{\textcolor{green}{\text{ON}}}^{gen}(g,s,t) + \epsilon_{P}^{\text{OFF}} \delta_{\textcolor{red}{\text{OFF}}}^{gen}(g,s,t)$$

### State transition constraints

To ensure a continuity in the cycle of generator states, a _chaining_ needs to be set up thanks to linear constraints
linking the state variables of two consecutive timestamps to the transition variables. Thus,
$\forall g \in \Gamma, \forall t \in \mathcal{T}, \forall s$:

#### *State-From* constraints

The state at the end of the previous timestamp must be the starting state of the transition that occurred during the
timestamp, no matter the final state. Thus, $\forall g \in \Gamma, \forall t \in \mathcal{T}, \forall s$:

$$\delta_{\textcolor{red}{\text{OFF}}}^{gen}(g,s,t) = T_{\textcolor{red}{\text{OFF}} \to \textcolor{red}{\text{OFF}}}(g,s,t) + T_{\textcolor{red}{\text{OFF}} \to \textcolor{green}{\text{ON}}}(g,s,t)$$

$$\delta_{\textcolor{green}{\text{ON}}}^{gen}(g,s,t) = T_{\textcolor{green}{\text{ON}} \to \textcolor{green}{\text{ON}}}(g,s,t) + T_{\textcolor{green}{\text{ON}} \to \textcolor{red}{\text{OFF}}}(g,s,t)$$

#### *State-To* constraints

The state at the end of the timestamp must be the final state of the transition that occurred during the timestamp, no
matter the state of origin. Thus, $\forall g \in \Gamma, \forall t \in \mathcal{T}, \forall s$:

$$\delta_{\textcolor{red}{\text{OFF}}}^{gen}(g,s,t + 1) = T_{\textcolor{red}{\text{OFF}} \to \textcolor{red}{\text{OFF}}}(g,s,t) + T_{\textcolor{green}{\text{ON}} \to \textcolor{red}{\text{OFF}}}(g,s,t)$$

$$\delta_{\textcolor{green}{\text{ON}}}^{gen}(g,s,t + 1) = T_{\textcolor{green}{\text{ON}} \to \textcolor{green}{\text{ON}}}(g,s,t) + T_{\textcolor{red}{\text{OFF}} \to \textcolor{green}{\text{ON}}}(g,s,t)$$

### Warm-Up

When the generator is turned on, it has a warm-up time called _lead time_ during which the power remains null before to
step up to $P_{\min}$.

$$\forall t' \leq t \text{ such that } \Delta_{t' \rightarrow t} < LEAD(g), \; T_{\textcolor{red}{\text{OFF}} \to \textcolor{green}{\text{ON}}}(g,s,t) \leq \delta_{\textcolor{red}{\text{OFF}}}^{gen}(g,s,t')$$

### Cool-Down

Similarly, when the generator is turned off, it has a cool-down time called _lag time_ during which the power remains
null and the generator cannot be tuned on again.

$$\forall t' \geq t \text{ such that } \Delta_{t \rightarrow t'} < LAG(g), \; T_{\textcolor{green}{\text{ON}} \to \textcolor{red}{\text{OFF}}}(g,s,t) \leq \delta_{\textcolor{red}{\text{OFF}}}^{gen}(g,s,t')$$

### Power variation constraint

The power variation and the state transitions are strongly entangled and constrain one another. Depending on the state
transition, the power variation $P(g,s,t+1) - P(g,s,t)$ is bounded differently.

$$\begin{align*}
- \epsilon_{P}^{\text{OFF}} T_{\textcolor{red}{\text{OFF}} \to \textcolor{red}{\text{OFF}}}(g,s,t) & & \epsilon_{P}^{\text{OFF}} T_{\textcolor{red}{\text{OFF}} \to \textcolor{red}{\text{OFF}}}(g,s,t) \\
+ \left ( P_{\min} - \epsilon_{P}^{\text{OFF}} \right ) T_{\textcolor{red}{\text{OFF}} \to \textcolor{green}{\text{ON}}}(g,s,t) & & P_{\min} T_{\textcolor{red}{\text{OFF}} \to \textcolor{green}{\text{ON}}}(g,s,t) \\
+ \nabla^{-}(g) \Delta_{\tau} T_{\textcolor{green}{\text{ON}} \to \textcolor{green}{\text{ON}}}(g,s,t) & \leq P(g,s,t + 1) - P(g,s,t) \leq & + \nabla^{+}(g) \Delta_{\tau} T_{\textcolor{green}{\text{ON}} \to \textcolor{green}{\text{ON}}}(g,s,t) \\
- P_{\min} T_{\textcolor{green}{\text{ON}} \to \textcolor{red}{\text{OFF}}}(g,s,t) & & - \left ( P_{\min} - \epsilon_{P}^{\text{OFF}} \right ) T_{\textcolor{green}{\text{ON}} \to \textcolor{red}{\text{OFF}}}(g,s,t)
\end{align*}$$

### Injection to generator power constraint

The power of the generator can be linked to the set-point of the injection range action $r$ it is involved in:

$$P(g,s,t) = \frac{\sigma(g)}{k(g,r)} A(r,s,t)$$

where $k(g,r)$ is the injection key of $g$ in $r$ and $\sigma(g) = 1$ if $g$ is a generator or $-1$ if it is a load.
