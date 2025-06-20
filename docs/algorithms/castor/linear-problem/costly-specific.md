# Costly specific

```{toctree}
:hidden:
costly-specific/power-gradient-constraint-filler.md
```

Remedial actions can be integrated in costly computations by configuring their activation cost
and variation cost.

## Redispatching remedial actions

Redispatching remedial actions can only be integrated in costly computations.
They are defined as [injection range actions](../../../input-data/crac/json.md#injection-range-action). A redispatching remedial action is defined
on network elements representing generators. The total
redispatching activation must preserve the balance of the network so the sum of power variations of all generators and
loads implicated must be null, see [here](core-problem-filler.md#span-stylecolor-marooncostly-onlyspan---injection-balance-constraint)

Redispatching remedial actions require specific data. They can be defined with [ICS data](../../../input-data/specific-input-data/ics.md), or directly by creating
IntertemporalRaoInput objects (in java) via the GeneratorConstraints objects.
Currently, only power gradients are supported by the RAO - therefore the only specific data supported are :
- Maximum positive power gradient [MW/h]
- Maximum negative power gradient [MW/h]

