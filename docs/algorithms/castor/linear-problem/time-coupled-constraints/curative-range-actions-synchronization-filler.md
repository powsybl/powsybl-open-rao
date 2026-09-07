# Modeling the curative range actions synchronization constraints

In time-coupled optimization, when a global range action optimization is done, the linear problem is a single MIP that optimizes
the variables of all the timestamps at the same time. Therefore, a range action that is common between several timestamps has one
setpoint variable created per timestamp. By default, the solver is free to give each setpoint a different value, which leads
to the same range action activated at different setpoints from one timestamp to another.

When the same decision must be taken simultaneously on all the timestamps, i.e. when all the common range actions must have the same 
setpoint, the MIP must be forced to do so by adding synchronization constraints.

This synchronization is currently available for curative range actions only.

## Input data 

### Sets

| Name                              | Symbol           | Details                                                                             |
|-----------------------------------|------------------|-------------------------------------------------------------------------------------|
| Timestamps                        | $\mathcal{T}$    | Set of all timestamps on which the optimization is performed.                       |
| Synchronized contingencies        | $\mathcal{C}$    | Set of the contingency ids whose curative states hold range actions to synchronize. |
| Synchronized range actions        | $\mathcal{R}(c)} | Set of the range action ids available, for the contingency $c$                      |
| Timestamps sharing a range action | $\mathcal{T}(r)$ | Set of the timestamps in which the range action $r \in \mathcal{R}$ is available.   |


### Used optimization variables

| Name                  | Symbol       | Details                                                                       |
|-----------------------|--------------|-------------------------------------------------------------------------------|
| Range action setpoint | $A(r, s, t)$ | [CoreProblemFiller](../core-problem-filler.md#defined-optimization-variables) |   

with $A(r, s, t)$ the setpoint of the range action $r$, at its optimization state $s$, in timestamp $t$.

## Constraints 

### Setpoint equality constraint

For each contingency and each range action shared by the timestamps set, the setpoint of the range action in the reference timestamp 
$t_0(r)$ (the first one in chronological order) must be equal  to its setpoint in every other setpoint sharing it : 

$$\forall r \in \mathcal{R}, \forall t \in \mathcal{T}(r) \setminus \lbrace t_0(r) \rbrace, \; A(r, s, t_0(r)) - A(r, s, t) = 0$$

A range action shared by $n$ timestamps therefore creates $n-1$ constraints. The equalities between the non-reference timestamps are 
implied by transitivity, so this is the minimal set of constraints making the $n$ setpoints mutually equal.

**Note** : 
The range actions are matched across the timestamps by their CRAC IDs, their network elements are not compared.