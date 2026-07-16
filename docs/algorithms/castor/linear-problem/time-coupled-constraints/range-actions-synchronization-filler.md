# Modeling the range actions synchronization constraints

This filler forces the setpoint of a range action to be equal across all the timestamps that share it. It is useful in time-coupled optimization if the same decision must be taken simultaneously on all timestamps.

When several timestamps are optimized together, the linear problem is a single MIP that holds the variables of all the timestamps at the same time. A range action that exists in several timestamps has therefore one 
setpoint variable per timestamp, and by default the solver is free to give each of them a different value. But a range action that appears in several timestamps under the same CRAC id represents te same physical 
remedial action : it is decided once and applied identically on every timestamp. This filter adds the constraints that force its per-timestamp setpoint variables to be equal.

## Input data 

### Sets

|  Name                             | Symbol           | Details                                                                            |
|-----------------------------------|------------------|------------------------------------------------------------------------------------|
| Timestamps                        | $\mathcal{T}$    | Set of all timestamps on which the optimization is performed.                      |
| Synchronized range actions        | $\mathcal{R}$    | Set of the range action ids that are common between at least two timestamps.       |
| Timestamps sharing a range action | $\mathcal{T}(r)$ | Set of the timestamps in which the range action $r \in \mathcal{R}$ is available.  |


### Used optimization variables

| Name                  | Symbol       | Details                                                                       |
|-----------------------|--------------|-------------------------------------------------------------------------------|
| Range action setpoint | $A(r, s, t)$ | [CoreProblemFiller](../core-problem-filler.md#defined-optimization-variables) |   

with $A(r, s, t)$ the setpoint of the range action $r$, at its optimization state $s$, in timestamp $t$.
## Defined Constraints 

### Setpoint equality constraints 

For each synchronized range action, its setpoint in the reference timestamp $t_0(r)$ (the first one in chronological order) must be equal  to its setpoint in every other setpoint sharing it : 

$$\forall r \in \mathcal{R}, \forall t \in \mathcal{T}(r) \setminus \lbrace t_0(r) \rbrace, \; A(r, s, t_0(r)) - A(r, s, t) = 0$$

A range action shared by $n$ timestamps therefore yields $n-1$ constraints arranged in a star around the reference timestamp. The equalities between the other timestamps are implied by transitivity, so this is 
the minimal set of constraints making the $n$ setpoints mutually equal.