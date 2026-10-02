
# Monitoring algorithm

> **Difference between voltage and angle monitoring**
>
> **Angle constraint** can only be solved using "injection" network actions (i.e. network action with an elementary action that is a LoadAction or a GeneratorAction with a predefined setpoint) 
> whereas for a voltage constraint, all network actions are allowed.

The monitoring algorithm essentially works as described below:

- For each state that contains an angle/voltage CNEC:
1. Evaluate the state by computing angle/voltage values and margins (see [this section](#evaluation-of-a-monitoring-state))
2. If some **CNECs are constrained** and the state is **curative**, we try to solve the constraint by using the available network actions (see [this section](#solving-cnec-overshooting-constraint))
3. If any injection network actions are applied, create and apply the redispatching that shall compensate for the change of generation/load:
    - The amount of power to redispatch is the net imbalance created by the injection network actions = the difference between the original and the new setpoints for the affected generators and loads.
    - The power will be redispatched between the countries' generators and loads not modified by an injection network action.
4. Re-evaluate the state after applying those additional network actions

- Assemble all the angle/voltage CNECs results in one overall result

![Monitoring algorithm](../../_static/img/monitoring-algo.png){.forced-white-background}

## Evaluation of a monitoring state

> ⚠️ WARNING️
>
> Only angle/voltage CNECs defined on **the preventive state and the last curative instant** can be monitored!
>
> If a voltage/angle CNEC is defined on an intermediate instant (ex. auto or not final curative instant), the CNEC will not be monitored (a warning will be issued) and
> will be ignored in the final augmented RaoResult.

To evaluate a monitoring state:
1. Apply the contingency if the state is curative
2. Apply all the optimal remedial actions from the initial RaoResult
3. Compute a loadflow
4. If the loadflow converges: compute the angles/voltages and margins for all angle/voltage CNECs:
- **Angle value** in degrees = 180 / pi * (max(angle on buses of exporting voltage level) - min(angle on buses of importing voltage level))
- **Voltage values** are the min and max voltages on the voltage level buses

![Monitoring algorithm details 1](../../_static/img/monitoring-algo-2.png){.forced-white-background}

## Solving CNEC overshooting constraint

> ⚠️ **If the state is preventive, we cannot apply any actions.** This could create inconsistencies with the other states.
>
> ⚠️ **Only network actions can be used.** The monitoring module is not an optimization module, so it cannot determine which setpoint to apply for a range action.

Identify and apply all the network actions that can be applied to solve the constrained CNEC:
- A network action is considered available if it has an usage rule [OnConstraint](../../input-data/crac/json.md#remedial-actions-and-usages-rules) defined on the constrained CNEC.
- If it is an **angle CNEC**, only injection network actions are allowed
- If it is a **voltage CNEC**, all network actions are allowed

![Monitoring algorithm details 2](../../_static/img/monitoring-algo-3.png){.forced-white-background}