# Inter-temporal constraints

```{toctree}
:hidden:
inter-temporal-constraints/power-gradient-constraint-filler.md
inter-temporal-constraints/generator-constraints-filler.md
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


### Creating redispatching actions

The definition of redispatching actions in the CRAC must follow certain guidelines:

- Make sure that the active load and the generator active power are defined correctly in the network.
  When using a UCTE network, for a given node in the network, two network elements are automatically created: a generator with ID `nodeCode + " _generator"` and a load with ID `nodeCode + " _load"`.
  For example, if you use the network element "FFR2AA1 _generator" make sure that the power associated with the node FFR2AA1 is defined in the 8th column in the ucte file (in the 6th column for loads). See the example below and [UCTE format definition](../../../_static/pdf/UCTE-format.pdf).

- Two different redispatching actions cannot be defined on the same network element.
  For example, if redispatchingAction1 uses "FFR2AA1 _generator", redispatchingAction2 can't use "FFR2AA1 _generator".
> A warning will be thrown but not an error, so be careful!


- When an injection range action has several generators/loads, each generator/load's value defined in the initial network divided by its key
  must be equal because an injection range action has a unique setpoint.

You can find below a complete illustration:

UCTE Network
```
##ZFR
FFR1AA1  FR1          0 2 400.00 0.00000 0.00000 -1000.0 0.00000 9000.00 -9000.0 9000.00 -9000.0
FFR2AA1  FR2          0 2 400.00 0.00000 0.00000 700.000 0.00000 9000.00 -9000.0 9000.00 -9000.0
```

CRAC Json

```json
"injectionRangeActions": [
    {
      "id": "redispatchingActionFR1FR2",
      "name": "redispatchingActionFR1FR2",
      "operator": "FR",
      "activationCost": 10.0,
      "variationCosts": {
        "up": 50.0,
        "down": 50.0
      },
      "onInstantUsageRules": [
        {
          "instant": "preventive",
          "usageMethod": "available"
        }
      ],
      "networkElementIdsAndKeys": {
        "FFR1AA1 _generator": 1.0,
        "FFR2AA1 _generator": -0.7
      },
      "ranges": [
        {
          "min": -1000.0,
          "max": 1000.0
        }
      ]
    }
]
```
In this case the initial active power of `FFR1AA1 _generator` is 1000 MW and  `FFR2AA1 _generator` -700 MW so
the initial setpoint of `redispatchingActionFR1FR2` is equal to $\frac{1000}{1}=\frac{-700}{-0.7}=1000$.

