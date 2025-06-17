# Optimization of costly remedial actions

While some processes aim at generating as much margin as possible, others attempt to secure the network while spending as
little as possible on remedial actions. OpenRAO and its CASTOR implementation offer the possibility to optimize the
selection of remedial actions cost-wise.

> ⚙️ **Objective function setting**
>
> To run OpenRAO in costly mode, the [objective function type](../parameters/business-parameters.md#type) must be set to
> `MIN_COST`

In costly mode, the goal of the RAO is two-fold:

1. it must secure the network (i.e. secure the base-case and all contingency scenarios). When the RAO doesn't succeed in securing the network, the objective function is [penalized](#securing-the-network-cost)
2. this must come at the [lowest monetary cost](#remedial-action-cost) possible

## Securing the network cost

In costly mode, the aim is no longer to maximize the minimum margin, but simply to make it secure. In order to prevent the RAO from choosing set-points to make the minimum margin perfectly equal to 0, which could lead
to future rounding errors, one solution is to shift the security domain of the lines by a few MW or A to keep a margin
of error. This security shift is noted as $\Delta_{secure}$.

> ⚙️ The security domain shift is set with the
> [`shifted-violation-threshold`](../parameters/implementation-specific-parameters.md#shifted-violation-threshold) parameter.

<!-- TODO: definir ça en paramètre comme dans loop flow-->

Thus, securing the network means ensuring that  ($MM \geq \Delta_{secure}$). 
The minimum margin violation ($MMV$)  represents the gap (MW or A) between the minimum margin and the shifted threshold 

$$MM + MMV \geq \Delta_{secure} \text{, with }  MMV \geq 0$$


$MMV$ is penalized by a penalty $P_{overload}$ in the objective function, representing the cost of not having secured the network.

Participation to the objective function: 

$$\text{Minimize } P_{overload} \times MMV$$

> ⚙️ The overload penalty is set with the
> [`shifted-violation-penalty`](../parameters/implementation-specific-parameters.md#shifted-violation-penalty) parameter.


## Remedial action cost

Remedial actions have inherent costs that represent the operators' expenses. Costs can be divided in two categories:

1. **activation costs** which are only taken into account if the remedial action is chosen by the RAO;
2. **variation costs**, only defined for [range actions](../input-data/crac/json.md#range-actions). They
   represent what it costs to shift an HVDC or redispatching action's set-point by 1 MW, or to pass one tap for
   PSTs. These variation costs can be different based on the variation direction (upward or downward).

Using the same notations as in the
[section dedicated to the linear problem](linear-problem/core-problem-filler.md#remedial-actions-cost-optimization), the
participation of the costs of remedial actions to the objective function is the sum of the activation and
variation costs of remedial actions for all optimization states.

<!-- TODO: rajouter tableaux de parametres-->

$$\text{Minimize } \sum_{r \in \mathcal{RA}} \sum_{s \in \mathcal{S}} c(r, s)$$

$$c(r, s) = \begin{cases}
c^{act}(r) \delta(r,s) & \text{if $r$ is a network action}\\
c^{act}(r) \delta(r,s) + c_{\Delta}^{+}(r) \Delta^{+}(r,s) + c_{\Delta}^{-}(r) \Delta^{-}(r,s)
& \text{if $r$ is a range action}\\
\end{cases}$$

## Objective function

$$\text{Minimize } \underbrace{\sum_{r \in \mathcal{RA}} \sum_{s \in \mathcal{S}}
c(r, s)}_{\text{Functional cost = remedial actions expenses}} + \underbrace{P_{overload}
\times MMV}_{\text{Virtual cost = penalty on minimal margin violation}}$$