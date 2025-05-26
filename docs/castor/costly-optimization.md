# Optimization of costly remedial actions

While some processes aim at freeing up as much margin as possible, others attempt to secure the network by spending as
little as possible on remedial actions. OpenRAO and its CASTOR implementation offer the possibility to optimize the
selection of remedial actions cost-wise.

> ⚙️ **Objective function setting**
>
> To run OpenRAO in costly mode, the [objective function type](../parameters/business-parameters.md#type) must be set
> to `MIN_COST`.

The costly mode induces some changes in the optimization model by adding new variables and new constraints that are not
required in the margin maximization mode. The objective function itself is also different to account for the monetary
cost of remedial actions.

Indeed, in costly mode, the goal of the RAO is two-fold:

1. it must secure the network (i.e. secure the base-case and all contingency scenarios)
2. this must come at the lowest expenses as possible

<!-- TODO: better rephrase below -->
In case the network cannot be secured, a penalty has to be taken in account.

## Remedial action cost

Remedial actions have inherent costs that represent the expenses for operators when using such remedial actions. There
are two categories of such costs:

1. **activation costs** which are only taken in account if the remedial action is chosen by the RAO;
2. **variation costs** that are only defined for [range actions](../input-data/crac/json.md#range-actions) and which
   represent the cost for shifting the set-point of 1 MW (for HVDC or redispatching actions) or passing one tap for
   PSTs. Note that the variation costs can be different based on the variation direction (upward or downward).

Using the same notations as in
the [section dedicated to the linear problem](linear-problem/core-problem-filler.md#remedial-actions-cost-optimization),
the participation of the costs of remedial actions to the objective function is simply the sum of the activation and
variation costs of remedial actions for all optimization states. It represents the functional (i.e. _real_) cost of the
objective function.

$$\text{Minimize } \sum_{r \in \mathcal{RA}} \sum_{s \in \mathcal{S}} c(r, s)$$

$$c(r, s) = \begin{cases}
c^{act}(r) \delta(r,s) & \text{if $r$ is a network action}\\
c^{act}(r) \delta(r,s) + c_{\Delta}^{+}(r) \Delta^{+}(r,s) + c_{\Delta}^{-}(r) \Delta^{-}(r,s)
& \text{if $r$ is a range action}\\
\end{cases}$$

## Security status

### Network security

In costly mode, the aim is no longer to maximize the minimum margin, but simply to make it secure. Thus, the minimum
margin can be broken down into two parts:
- the minimum margin ($MM$) which is forced to be negative ($MM \leq 0$)
- the minimum margin violation ($MMV$) which represents the number of MW or A of violation on the worst margin, i.e.
  the smallest overload on the network. This value is always positive ($MMV \geq 0$) and is equal to 0 in situations
  where the network can be secured.

$$MM + MMV \geq 0 \text{, with } MM \leq 0 \text{ and } MMV \geq 0$$

> TODO: link to parameter

### Shifting the security domain

In order to prevent the RAO from choosing set-points to make the minimum margin perfectly equal to 0, which could lead
to future rounding errors, one solution is to shift the security domain of the lines by a few MW or A to keep a margin
of error. This security shift is noted as $\Delta_{secure}$.

The equations of the previous section remain valid, with the simple difference that the 0 used in some of the bounds is
replaced by $\Delta_{secure}$ to take into account the shift in the security domain.

$$MM + MMV \geq \Delta_{secure} \text{, with } MM \leq \Delta_{secure} \text{ and } MMV \geq 0$$

### Participation to the objective function

The RAO must secure the network if possible. In case such a result in not feasible, it must be heavily penalized using a
virtual cost that acts as a monetary penalty for each MW or A of violation on the minimum margin. This penalty, noted as
$P_{overload}$, ensures that the RAO will try using as many remedial actions as possible to achieve network security.

$$\text{Minimize } P_{overload} \times MMV$$

> TODO: link to parameter

> TODO: explain why it works (penalty >=0 so RAO tries to minimize MMV, i.e. maximize -MM)

## Objective function

$$\text{Minimize } \underbrace{\sum_{r \in \mathcal{RA}} \sum_{s \in \mathcal{S}} c(r, s)}_{\text{Functional cost = remedial actions expenses}} + \underbrace{P_{overload} \times MMV}_{\text{Virtual cost = penalty on minimal margin violation}}$$