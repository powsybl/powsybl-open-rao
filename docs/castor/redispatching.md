# Redispatching

## Definition

Redispatching are defined as [injection range actions](../input-data/crac/json.md#injection-range-action). The total
redispatching activation must preserve the balance of the network so the sum of power variations of all generators and
loads implicated must be null.

## Power gradient constraints

OpenRAO supports redispatching actions in an intertemporal context, i.e. when several timestamps are being optimized at
once. To match reality, physical constraints can be applied to the generators. Currently, only power gradients are
supported by the RAO.

A redispatching action can be defined with upward or downward power gradients which means that the power variation per
hour (in MW/h) cannot exceed or fall under bounds. Thus, for two consecutive timestamps $t_{i}$ and $t_{i + 1}$ with a
time gap of $\Delta t_{i \rightarrow i + 1}$ (in h), the power variation $P_{i + 1} - P_{i}$ (in MW) is constrained as

$$\nabla_{P}^{-} \Delta t_{i \rightarrow i + 1} \leq P_{i + 1} - P_{i} \leq \nabla_{P}^{+} \Delta t_{i \rightarrow i + 1}$$

where $\nabla_{P}^{-}$ and $\nabla_{P}^{-}$ are respectively the downward and upward power gradients of the action (in
MW/h).

## ICS Data

### Static

- RA RD ID
- TSO
- Preventive
- Curative
- Time From
- Time To
- Generator Name
- RD description mode
- UCT Node or GSK ID
- Minimum Redispatch [MW]
- Fuel type
- Minimum up-time [h]
- Minimum down-time [h]
- Maximum positive power gradient [MW/h]
- Maximum negative power gradient [MW/h]
- Lead time [h]
- Lag time [h]
- Startup allowed
- Shutdown allowed

### Series

Types of time series:

- P0
- RDP+
- RDP-

### GSK

- GSK ID
- Node
- Weight

![ICS Importer](../_static/img/ics-importer.png)