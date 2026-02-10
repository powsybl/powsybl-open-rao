# Intertemporal constraints

## Generator constraints

Generators have physical and mechanical constraints that can be taken in account by the RAO to ensure a more realistic
behavior.

### Lead time

The **lead time** corresponds to the time elapsed between the moment when the generator gets a start-up order and the
moment its power reaches it minimal operational power ($P_{\min}$).

### Lag time

The **lag time** corresponds to the time elapsed between the moment when the generator gets a shut-down order and the
moment it is completely off.

### Power gradients

When operated above its $P_{\min}$, a generator can be restricted by **power gradients** that limit the amount of MW its
power can vary per hour. These gradients can be defined upward and/or downward.

### Minimum up-time

The **minimum up-time** is the minimum time the generator must be operated above its $P_{\min}$. As long as this
duration is not reached, it cannot be shut down.

### Maximum up-time

The **maximum up-time** is the maximum time the generator can be operated above its $P_{\min}$. After such a duration,
the generator must be shut down.

### Minimum off-time

The **minimum off-time** is the minimum time the generator must remain off when it is shut down before to be started up
again.

## JSON API

### Generator constraints

| Field name              | Type   | Status    | Unit  | Description                                                                             |
|-------------------------|--------|-----------|-------|-----------------------------------------------------------------------------------------|
| `generatorId`           | string | Mandatory | -     | Id of the generator on which the constraints apply.                                     |
| `leadTime`              | number | Optional  | hours | Lead time of the generator, in hours. Must be positive.                                 |
| `lagTime`               | number | Optional  | hours | Lag time of the generator, in hours. Must be positive.                                  |
| `upwardPowerGradient`   | number | Optional  | MW/h  | Upward power gradient for the generator's power variation, in MW/h. Must be positive.   |
| `downwardPowerGradient` | number | Optional  | MW/h  | Downward power gradient for the generator's power variation, in MW/h. Must be negative. |
| `minUpTime`             | number | Optional  | hours | Minimum up-time of the generator, in hours. Must be positive.                           |
| `maxUpTime`             | number | Optional  | hours | Maximum up-time of the generator, in hours. Must be positive.                           |
| `minOffTime`            | number | Optional  | hours | Minimum off-time of the generator, in hours. Must be positive.                          |

### Comprehensive example

```json
{
  "type": "OpenRAO Intertemporal Constraints",
  "version": "1.0",
  "generatorConstraints": [
    {
      "generatorId": "generator-1",
      "leadTime": 1.15,
      "lagTime": 2.0,
      "upwardPowerGradient": 100.0,
      "downwardPowerGradient": -50.0,
      "minUpTime": 5.0,
      "maxUpTime": 15.0,
      "minOffTime": 10.0
    },
    {
      "generatorId": "generator-2"
    },
    {
      "generatorId": "generator-3",
      "leadTime": 0.5,
      "lagTime": 4.0,
      "downwardPowerGradient": -1000.0
    }
  ]
}
```