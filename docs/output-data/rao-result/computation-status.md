This field contains the status of sensitivity / load-flow computations during the RAO, for a given [state](/input-data/crac/json.md#instants-and-states),
or for the overall RAO (the status of the overall RAO is computed as the worst status among all states).  
It can have one of the following values:
- **DEFAULT**: the sensitivity / load-flow computations converged normally
- **FAILURE**: the sensitivity / load-flow computations failed

When the results are returned to the user of your application, they should be sufficiently warned if any state's 
sensitivity / load-flow computation has failed.

::::{tabs}
:::{group-tab} JAVA API

~~~java
ComputationStatus getComputationStatus();

ComputationStatus getComputationStatus(State state);
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
{
  "computationStatus" : "default",
  "computationStatusMap": [
    {
      "computationStatus": "default",
      "instant": "preventive"
    },
    {
      "computationStatus": "failure",
      "instant": "curative",
      "contingency": "contingency1Id"
    },
    {
      "computationStatus": "default",
      "instant": "auto",
      "contingency": "contingency2Id"
    }
  ],
  ...
~~~

:::
::::