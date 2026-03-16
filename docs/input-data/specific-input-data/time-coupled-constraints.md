# Time-coupled constraints

## Generator constraints

Generators have physical and mechanical constraints that can be taken in account by the RAO to ensure a more realistic
behavior.

> To see the mathematical formulation of these constraints, please refer to the dedicated linear problem
> [page](../../algorithms/castor/linear-problem/time-coupled-constraints/generator-constraints-filler.md).

### Lead time

The **lead time** corresponds to the time elapsed between the moment when the generator gets a start-up order and the
moment its power reaches it minimal operational power ($P_{\min}$).

### Lag time

The **lag time** corresponds to the time required by the generator to reach complete shutdown from its $P_{\min}$

### Power gradients

When operated above its $P_{\min}$, a generator can be restricted by **power gradients** that limit the amount of MW its
power can vary between two consecutive timestamps. These gradients can be defined upward and/or downward.

### Shut Down allowed

This is a boolean to indicate if a generator can be shutdown. If no value is defined, value will be set to true, meaning
that the generator can be shutdown.

### Start Up Allowed

This is a boolean to indicate if a generator can be started up from standstill. If no value is defined, value will be set to true, meaning
that generator can be started up.

## JSON API

### Generator constraints

| Field name              | Type    | Status    | Unit  | Description                                                                             |
|-------------------------|---------|-----------|-------|-----------------------------------------------------------------------------------------|
| `generatorId`           | string  | Mandatory | -     | Id of the generator on which the constraints apply.                                     |
| `leadTime`              | number  | Optional  | hours | Lead time of the generator, in hours. Must be positive.                                 |
| `lagTime`               | number  | Optional  | hours | Lag time of the generator, in hours. Must be positive.                                  |
| `upwardPowerGradient`   | number  | Optional  | MW/h  | Upward power gradient for the generator's power variation, in MW/h. Must be positive.   |
| `downwardPowerGradient` | number  | Optional  | MW/h  | Downward power gradient for the generator's power variation, in MW/h. Must be negative. |
| `isShutDownAllowed`     | boolean | Optional  | -     | Indicate if RA RD can be shutdown. Default value : true                                 |
| `isStartUpAllowed`      | boolean | Optional  | -     | Indicates if RA RD can be started up . Default value : true                             |

### Comprehensive example

```json
{
  "type": "OpenRAO Time-Coupled Constraints",
  "version": "1.0",
  "generatorConstraints": [
    {
      "generatorId": "generator-1",
      "leadTime": 1.15,
      "lagTime": 2.0,
      "upwardPowerGradient": 100.0,
      "downwardPowerGradient": -50.0,
      "shutDownAllowed": false
    },
    {
      "generatorId": "generator-2"
    },
    {
      "generatorId": "generator-3",
      "leadTime": 0.5,
      "lagTime": 4.0,
      "downwardPowerGradient": -1000.0,
      "shutDownAllowed": true,
      "startUpAllowed": false
    }
  ]
}
```

### Import

```java
TimeCoupledConstraints timeCoupledConstraintsPool = JsonTimeCoupledConstraints.read(getClass().getResourceAsStream("/time-coupled-constraints.json"));
```