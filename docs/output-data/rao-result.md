# JSON RAO Result File

## Introduction

A **R**emedial **A**ction **O**ptimization process provides an optimal list of remedial actions to be applied in
basecase and after contingencies listed in the [CRAC](../input-data/crac.md). The decisions are based upon the impact of
these remedial actions on the CRAC's [CNECs](../input-data/crac.md#cnec).

A **RAO Result object model** has been designed in OpenRAO in order to hold all the important results of optimization.
In this page, we present:
- where to find the RaoResult instance,
- how to save a RaoResult java object to a JSON file,
- how to import a RaoResult java object from a JSON file,
- how to access information in the RaoResult, using either the RaoResult java object or the JSON file.

## Accessing the RAO result

The [RaoResult](https://github.com/powsybl/powsybl-open-rao/blob/main/data/rao-result/rao-result-api/src/main/java/com/powsybl/openrao/data/raoresult/api/RaoResult.java)
is a Java interface, an instance of which is returned by OpenRAO's main optimisation method:

```java
CompletableFuture<RaoResult> RaoProvider::run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant)
```

where `RaoProvider` is the chosen implementation of the RAO, such as [CASTOR](https://github.com/powsybl/powsybl-open-rao/blob/main/ra-optimisation/search-tree-rao/src/main/java/com/powsybl/openrao/searchtreerao/castor/algorithm/Castor.java).

## Exporting and importing a JSON file

A RaoResult object can be saved into a JSON file (no matter what implementation it is).
A RaoResult JSON file can be imported into a [RaoResultImpl](https://github.com/powsybl/powsybl-open-rao/blob/main/data/rao-result/rao-result-impl/src/main/java/com/powsybl/openrao/data/raoresult/impl/RaoResultImpl.java), and used as a RaoResult java object.

### Export

Example:

```java
raoResult.write("JSON", crac, properties, outputStream);
```

Where:
- **`raoResult`** is the RaoResult object you obtained from the RaoProvider;
- **`crac`** is the CRAC object you used in the RAO;
- **`properties`** is a set of specific parameters for the JSON export, currently two are defined:
  - `"rao-result.export.json.flows-in-amperes"` (optional, default is `"false"`): whether to export the flow measurements in `AMPERE`
  - `"rao-result.export.json.flows-in-megawatts"` (optional, default is `"false"`): whether to export the flow measurements in `MEGAWATT`
- **`outputStream`** is the `java.io.OutputStream` you want to write the JSON file into.

> At least one of `"rao-result.export.json.flows-in-amperes"` or `"rao-result.export.json.flows-in-megawatts"` must be true for the export to work properly.

### Import

Example:

```java
RaoResult importedRaoResult = RaoResult.read(inputStream, crac);
```

Where:
- **`crac`** is the CRAC object you used in the RAO
- **`inputStream`** is the `java.io.InputStream` you read the JSON file into

## Contents of the RAO result

The RAO result object generally contains information about post-optimisation results.  
However, in some cases, it may be interesting to get some information about the initial state (e.g. power flows before 
optimisation), or about the situation after preventive optimisation (e.g. optimal PST tap positions in preventive). 
This is why **most of the information in the RAO results are stored by optimized instant**:  
- **INITIAL** (json) or **null** (Java API): values before remedial action optimisation (initial state)
- Instant of kind **PREVENTIVE** or **OUTAGE**: values after optimizing preventive instant, i.e. after applying optimal preventive remedial actions
- Instant of kind **AUTO**: values after simulating auto instant, i.e. after applying automatic remedial actions
- Instant of kind **CURATIVE**: values after optimizing curative instant, i.e. after applying optimal curative remedial actions
  
_See also: [RAO steps](../algorithms/castor/rao-steps.md)_

### Remedial actions activations

Since version 2.0, network actions, standard range actions and PST range actions activations are gathered, state by
state, in the `"remedialActionActivations"` array of the JSON file. Each entry identifies a state (`instant`, optional
`contingency`, optional `timestamp`) and lists its activated remedial actions with their `setPoint` (standard range
actions) or `tap` (PST range actions). Network actions only have an `id`. Remedial actions that are not activated do not appear.

```json
"remedialActionActivations" : [ {
    "instant" : "preventive",
    "timestamp" : "2025-02-03T10:12:00Z",
    "activatedRemedialActions" : [
      { "id" : "complexNetworkActionId" },
      { "id" : "hvdcRange1Id", "setPoint" : -1000.0 },
      { "id" : "pstRange1Id", "tap" : 3 }
    ]
  }, {
    "instant" : "curative",
    "contingency" : "contingency1Id",
    "timestamp" : "2025-02-03T10:12:00Z",
    "activatedRemedialActions" : [ { "id" : "injectionRange1Id", "setPoint" : -300.0 } ]
  } ]
```

#### Network actions results

These results contain information about the selection of network actions by the RAO.

> Note that you will need to use [NetworkAction](../input-data/crac/json.md#network-actions) objects from the CRAC for querying the Java API.

::::{tabs}
:::{group-tab} JAVA API

```java
// query if a network action was already activated before a given state was studied
boolean wasActivatedBeforeState(State state, NetworkAction networkAction);

// query if a network action was chosen during the optimization of a given state
boolean isActivatedDuringState(State state, NetworkAction networkAction);

// get the list of all network actions chosen during the optimization of a given state
Set<NetworkAction> getActivatedNetworkActionsDuringState(State state);

// query if a network action was during or before a given state
boolean isActivated(State state, NetworkAction networkAction);
```

:::
:::{group-tab} JSON File

> Network actions that are not selected by the RAO do not appear in the JSON file.

Example:

```json
"networkActionResults" : [ {
    "networkActionId" : "complexNetworkActionId",
    "activatedStates" : [ {
      "instant" : "preventive"
    } ]
  }, {
    "networkActionId" : "injectionSetpointRaId",
    "activatedStates" : [ {
      "instant" : "auto",
      "contingency" : "contingency2Id"
    } ]
  }
  ...
```

:::
::::

#### Standard range actions results

These results contain information about the selection of standard range actions by the RAO.  
Standard range actions are range actions that have a continuous set-point that can be optimised in a given range.  
Actually, standard range actions handled by OpenRAO are: **HVDC range actions** and **Injection range actions**.  

> Note that you will need to use [RangeAction](../input-data/crac/json.md#range-actions) objects from the CRAC for querying the Java API.

::::{tabs}
:::{group-tab} JAVA API

```java
// get a range action's set-point before the optimization of a given state
double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction);

// query if a range action was activated (ie setpoint changed) during the optimization of a given state
boolean isActivatedDuringState(State state, RangeAction<?> rangeAction);

// get all range actions that were activated by the RAO in a given state
Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state);

// get a range action's optimal setpoint, that was chosen by the RAO in a given state
double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction);

// get all range actions' optimal setpoints, that were chosen by the RAO in a given state
Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state);
```

:::
:::{group-tab} JSON File

Example:

```json
"rangeActionResults" : [ {
    "rangeActionId" : "hvdcRange1Id",
    "initialSetpoint" : 0.0,
    "activatedStates" : [ {
      "instant" : "preventive",
      "setpoint" : -1000.0
    } ]
  }, {
    "rangeActionId" : "hvdcRange2Id",
    "initialSetpoint" : -100.0,
    "activatedStates" : [ {
      "instant" : "curative",
      "contingency" : "contingency1Id",
      "setpoint" : 100.0
    }, {
      "instant" : "curative",
      "contingency" : "contingency2Id",
      "setpoint" : 400.0
    } ]
  }, {
    "rangeActionId" : "injectionRange1Id",
    "initialSetpoint" : 100.0,
    "activatedStates" : [ {
      "instant" : "curative",
      "contingency" : "contingency1Id",
      "setpoint" : -300.0
    } ]
  } ]
```

:::
::::

#### PST range actions results

These results contain information about the application of PST range actions by the RAO.  
PSTs are not standard range actions, because they have integer tap positions; a few extra methods in the API allow
querying the tap positions. All "set-point" methods of standard range actions also can also be used.    

> Note that you will need to use [PstRangeAction or RangeAction](../input-data/crac/json.md#pst-range-action) objects from the CRAC for querying the Java API.

::::{tabs}
:::{group-tab} JAVA API

```java
/* --- Standard RangeAction methods: --- */

// get a range action's setpoint before the optimization of a given state
double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction);

// query if a range action was activated (ie setpoint changed) during the optimization of a given state
boolean isActivatedDuringState(State state, RangeAction<?> rangeAction);

// get all range actions that were activated by the RAO in a given state
Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state);

// get a range action's optimal setpoint, that was chosen by the RAO in a given state
double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction);

// get all range actions' optimal setpoints, that were chosen by the RAO in a given state
Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state);

/* --- PST-specific methods: --- */

// get a PST range action's tap position before the optimization of a given state
int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction);

// get a PST range action's optimal tap position, that was chosen by the RAO in a given state
int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction);

// get all PST range actions' optimal tap positions, that were chosen by the RAO in a given state
Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state);
```

:::
:::{group-tab} JSON File

Example:

```json
  "rangeActionResults" : [ {
    "rangeActionId" : "pstRange1Id",
    "initialSetpoint" : 0.0,
    "initialTap" : -3,
    "activatedStates" : [ {
      "instant" : "preventive",
      "tap" : 3,
      "setpoint" : 3.0
    } ]
  }, {
    "rangeActionId" : "pstRange2Id",
    "initialSetpoint" : 1.5,
    "initialTap" : 0,
    "activatedStates" : [ ]
  }, {
    "rangeActionId" : "pstRange3Id",
    "initialSetpoint" : 1.0,
    "initialTap" : -1,
    "activatedStates" : [ ]
  } ]
```

:::
::::

### Extensions

Since version **2.0** of the RAO result JSON format, all other results are stored
as [extensions](https://powsybl.readthedocs.io/projects/powsybl-core/en/stable/) of the RaoResult, as they can easily be
recomputed from a list of optimal remedial actions. Yet, having them available can save computation time and data
duplication.  In the JSON file, they are gathered in the `"extensions"` object, each extension being identified by its
unique name:

| Extension (Java class) | JSON name         | Content                                  |
|------------------------|-------------------|------------------------------------------|
| `Metadata`             | `metadata`        | computation status and execution details |
| `CostResult`           | `cost-results`    | functional and virtual costs             |
| `FlowResult`           | `flow-results`    | FlowCNECs results                        |
| `AngleResult`          | `angle-results`   | AngleCNECs results                       |
| `VoltageResult`        | `voltage-results` | VoltageCNECs results                     |

They can be accessed with:

```java
Metadata metadata = raoResult.getExtension(Metadata.class);
CostResult costResult = raoResult.getExtension(CostResult.class);
FlowResult flowResult = raoResult.getExtension(FlowResult.class);
AngleResult angleResult = raoResult.getExtension(AngleResult.class);
VoltageResult voltageResult = raoResult.getExtension(VoltageResult.class);
```

> **Deprecation:** the former getters of the `RaoResult` interface (`getComputationStatus`, `getExecutionDetails`,
> `getFlow`, `getMargin`, `getCost`, `getAngle`, `getMinVoltage`, ...) are deprecated since 7.6.0. They still work by
> delegating to the corresponding extension, but throw an `OpenRaoException` if the extension is missing.
> The Java snippets below show the extension methods to use instead.
> In the JSON file, instants are identified by their id (`initial` for the initial state).
> These deprecated methods will soon be removed so users are encouraged to rely on extensions from now on.

```json
{
  "type" : "RAO_RESULT",
  "version" : "2.0",
  "info" : "Generated by OpenRAO https://powsybl.readthedocs.io/projects/openrao",
  "remedialActionActivations" : [ ... ],
  "extensions" : {
    "metadata" : { ... },
    "cost-results" : { ... },
    "flow-results" : [ ... ],
    "angle-results" : [ ... ],
    "voltage-results" : [ ... ]
  }
}
```

#### Metadata

::::{tabs}
:::{group-tab} JAVA API

```java
Metadata metadata = raoResult.getExtension(Metadata.class);
```

:::
:::{group-tab} JSON File

Example:

```json
{
  "extensions": {
    "metadata": {
      "computationStatus": "default",
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
      "executionDetails": "The RAO only went through first preventive"
    }
  }
}
```

:::
::::

##### Computation status

####### Per state

::::{tabs}
:::{group-tab} JAVA API

```java
// src/main/java/com/powsybl/openrao/data/raoresult/api/extension/Metadata.java
ComputationStatus getComputationStatus(State state);
```

:::
:::{group-tab} JSON File

Example:

```json
{
  "metadata": {
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
  }
}
```

:::
::::

This field contains the status of sensitivity / load-flow computations during the RAO, for a given [state](../input-data/crac/json.md#instants-and-states),
or for the overall RAO (the status of the overall RAO is computed as the worst status among all states).  
It can have one of the following values:
- **DEFAULT**: the sensitivity / load-flow computations converged normally
- **FAILURE**: the sensitivity / load-flow computations failed

When the results are returned to the user of your application, they should be sufficiently warned if any state's
sensitivity / load-flow computation has failed.

> For compactness's sake, only the failed states are exported in the `"computationStatusMap"`.

####### Global

::::{tabs}
:::{group-tab} JAVA API

```java
// src/main/java/com/powsybl/openrao/data/raoresult/api/extension/Metadata.java

ComputationStatus getComputationStatus();
```

:::
:::{group-tab} JSON File

Example:

```json
{
  "metadata": {
    "computationStatus": "default",
    ...
  }
}
```

:::
::::

The global computation status is automatically computed from the individual states' status:
- **DEFAULT**: the sensitivity / load-flow computations converged normally for all states
- **PARTIAL_FAILURE**: the sensitivity / load-flow computations failed for at least one state, but not the preventive one
- **FAILURE**: the sensitivity / load-flow computations failed for all states or at least the preventive one

##### Execution details

This field can either contain information about the failure when the RAO fails, or macro information about which steps the [CASTOR RAO](../algorithms/castor.md#algorithm-details) executed when it did not fail.  
(See also: [Second preventive RAO parameters](../parameters/implementation-specific-parameters.md#second-preventive-rao-parameters))

> **Note:** This field replaced the optimizationStepsExecuted field.

::::{tabs}
:::{group-tab} JAVA API

```java
// src/main/java/com/powsybl/openrao/data/raoresult/api/extension/Metadata.java

Optional<String> getExecutionDetails();
```

:::
:::{group-tab} JSON File

Example:

```json
{
  "metadata": {
    "computationStatus": "default",
    "executionDetails": "Second preventive improved first preventive results"
  }
}
```

:::
::::

The different possible execution details are:
- **The RAO only went through first preventive**: only the first preventive optimization was run (no second preventive); this is the default situation;
- **First preventive fell back to initial situation**: the first preventive RAO did not improve the situation, so the initial situation (no remedial action applied) was kept;
- **Second preventive improved first preventive results**: the second preventive RAO gave better or similar results than the first one (cost-wise), so its results were kept;
- **Second preventive fell back to first preventive results**: the second preventive RAO did not improve on the first one, so the first preventive results were kept;
- **Second preventive fell back to initial situation**: the second preventive RAO did not improve the situation, so the initial situation was kept;
- **`<specific failure reason>`**: when the RAO fails, the field contains a message describing the cause of the failure.

If the RAO went through angle or voltage [monitoring](../algorithms/monitoring.md), said monitoring(s) are appended to the execution details from the first flow RAO.

For [MARMOT](../algorithms/marmot.md), one detail per timestamp is given as `<timestamp>: <detail>`, separated by ` - `; the default value is "RAO went through independent topological optimization and global time-coupled linear optimization".

#### Objective function cost results

Contains information about the reached objective function value.

::::{tabs}
:::{group-tab} JAVA API

```java
CostResult costResult = raoResult.getExtension(CostResult.class);
```

:::
:::{group-tab} JSON File

Example:

```json
{
  "extensions": {
    "cost-results": {
      "initial": {
        "functionalCost": 100.0,
        "virtualCost": {
          "loopFlow": 0.0,
          "MNEC": 0.0
        }
      },
      "preventive": {
        "functionalCost": 80.0,
        "virtualCost": {
          "loopFlow": 0.0,
          "MNEC": 0.0
        }
      },
      "auto": {
        "functionalCost": -20.0,
        "virtualCost": {
          "loopFlow": 15.0,
          "MNEC": 20.0
        }
      },
      "curative": {
        "functionalCost": -50.0,
        "virtualCost": {
          "loopFlow": 10.0,
          "MNEC": 2.0
        }
      }
    }
  }
}
```

:::
::::

Said objective function value is separated into functional and virtual costs:
- the **functional** cost reflects the business value of the objective (e.g. the cost associated to the minimum margin and the business penalties on usage of remedial actions)
- the **virtual** cost reflects the violation of some constraints (e.g. MNEC & loop-flow constraints)

::::{tabs}
:::{group-tab} JAVA API

```java
// src/main/java/com/powsybl/openrao/data/raoresult/api/extension/CostResult.java

// get the functional cost after optimization of a given instant
double getFunctionalCost(Instant optimizedInstant);

// get the total virtual cost after optimization of a given instant
double getVirtualCost(Instant optimizedInstant);

// get a specific virtual cost after optimization of a given instant
double getVirtualCost(Instant optimizedInstant, String virtualCostName);

// get all the virtual cost names in the RAO
Set<String> getVirtualCostNames();

// get the overall cost (functional + total virtual) after optimization of a given instant
double getCost(Instant optimizedInstant);
```

:::
:::{group-tab} JSON File

Example:

```json
{
  "cost-results": {
    "initial": {
      "functionalCost": 100.0,
      "virtualCost": {
        "loopFlow": 0.0,
        "MNEC": 0.0
      }
    },
    "preventive": {
      "functionalCost": 80.0,
      "virtualCost": {
        "loopFlow": 0.0,
        "MNEC": 0.0
      }
    },
    "auto": {
      "functionalCost": -20.0,
      "virtualCost": {
        "loopFlow": 15.0,
        "MNEC": 20.0
      }
    },
    "curative": {
      "functionalCost": -50.0,
      "virtualCost": {
        "loopFlow": 10.0,
        "MNEC": 2.0
      }
    }
  }
}
```

:::
::::

#### Flow CNECs results

These results contain important RAO information about FlowCNECs.

::::{tabs}
:::{group-tab} JAVA API

```java
FlowResult flowResult = raoResult.getExtension(FlowResult.class);
```

:::
:::{group-tab} JSON File

Example:

```json
{
  "extensions": {
    "flow-results": [
      {
        "flowCnecId": "curativeCnecId",
        "initial": {
          "megawatt": {
            "side2": {
              "flow": 1110.5,
              "loopFlow": 1113.5,
              "commercialFlow": 1114.5,
              "zonalPtdfSum": 0.6
            }
          },
          "ampere": {
            "side2": {
              "flow": 1120.5,
              "loopFlow": 1123.5,
              "commercialFlow": 1124.5
            }
          }
        },
        "preventive": {
          "megawatt": {
            "side2": {
              "flow": 1210.5,
              "loopFlow": 1213.5,
              "commercialFlow": 1214.5,
              "zonalPtdfSum": 0.6
            }
          },
          "ampere": {
            "side2": {
              "flow": 1220.5,
              "loopFlow": 1223.5,
              "commercialFlow": 1224.5
            }
          }
        },
        "curative1": {
          "megawatt": {
            "side2": {
              "flow": 1310.5,
              "loopFlow": 1313.5,
              "commercialFlow": 1314.5,
              "zonalPtdfSum": 0.13
            }
          },
          "ampere": {
            "side2": {
              "flow": 1320.5,
              "loopFlow": 1323.5,
              "commercialFlow": 1324.5
            }
          }
        },
        "curative2": {
          "megawatt": {
            "side2": {
              "flow": 1410.5,
              "loopFlow": 1413.5,
              "commercialFlow": 1414.5,
              "zonalPtdfSum": 0.14
            }
          },
          "ampere": {
            "side2": {
              "flow": 1420.5,
              "loopFlow": 1423.5,
              "commercialFlow": 1424.5
            }
          }
        }
      }
    ]
  }
}
```

:::
::::

Note that you have to use [FlowCnec](../input-data/crac/json.md#flow-cnecs) objects from the CRAC in order to query the RaoResult Java API.
Most results are power flow results (like flows and margins), and can be queried in two [units](https://github.com/powsybl/powsybl-open-rao/blob/main/commons/src/main/java/com/powsybl/openrao/commons/Unit.java)
(AMPERE and MEGAWATT) and on one or two sides (ONE and TWO).

> **Note:** since version 2.0 of the JSON format, only `flow`, `commercialFlow` and `zonalPtdfSum` are written in the
> `flow-results` extension. Margins, relative margins and loop-flows are computed on the fly by the `FlowResult`
> extension from these values and the CNECs' thresholds; the `margin`, `relativeMargin` and `loopFlow` fields shown in
> the examples below are only read from files written with older versions.

##### Flow

The actual power flow on the branch.

::::{tabs}
:::{group-tab} JAVA API

```java
// src/main/java/com/powsybl/openrao/data/raoresult/api/extension/FlowResult.java

// get the flow on a given FlowCNEC, at a given side, in a given unit, after optimization of a given instant
double getFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit);
```

:::
:::{group-tab} JSON File

Example:

```json
{
  "flow-results": [
    {
      "flowCnecId": "curativeCnecId",
      "initial": {
        "megawatt": {
          "side2": {
            "flow": 1110.5,
            ...
          }
        },
        "ampere": {
          "side2": {
            "flow": 1120.5,
            ...
          }
        }
      },
      "preventive": {
        "megawatt": {
          "side2": {
            "flow": 1210.5,
            ...
          }
        },
        "ampere": {
          "side2": {
            "flow": 1220.5,
            ...
          }
        }
      },
      "curative1": {
        "megawatt": {
          "side2": {
            "flow": 1310.5,
            ...
          }
        },
        "ampere": {
          "side2": {
            "flow": 1320.5,
            ...
          }
        }
      },
      "curative2": {
        "megawatt": {
          "side2": {
            "flow": 1410.5,
            ...
          }
        },
        "ampere": {
          "side2": {
            "flow": 1420.5,
            ...
          }
        }
      }
    }
  ]
}
```

:::
::::

##### Margin

The flow margin of the CNEC. If two flow values exist for the CNEC (i.e. if it is monitored on both sides) and / or if
it has multiple thresholds, the smallest margin is returned.

::::{tabs}
:::{group-tab} JAVA API

```java
// src/main/java/com/powsybl/openrao/data/raoresult/api/extension/FlowResult.java

// get the margin of a given FlowCNEC, in a given unit, after optimization of a given instant
double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit);
```

:::
:::{group-tab} JSON File

Not serialized.

:::
::::

##### Relative margin

The relative margin is the smallest flow margin of the CNEC.
- When the margin is negative, the relative margin is equal to the margin
- When the margin is positive, it is equal to the margin divided by the zonal PTDF absolute sum  
  It is used to artificially increase (in the RAO) the margin on flow cnecs that are less impacted by changes in
  power exchanges between commercial zones, in order to prioritize other CNECs in the minimum margin optimisation.  
  (See [Modelling the maximum minimum relative margin objective function](../algorithms/castor/linear-problem/objective-function-types/max-min-relative-margin-filler.md))

::::{tabs}
:::{group-tab} JAVA API

```java
// src/main/java/com/powsybl/openrao/data/raoresult/api/extension/FlowResult.java

// get the relative margin of a given FlowCNEC, in a given unit, after optimization of a given instant
double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit);
```

:::
:::{group-tab} JSON File

Not serialized.

:::
::::

##### Loop-Flow

The loop-flow value on a CNEC, defined as the difference between the flow and the commercial flow.

> Can only be queried for FlowCNECs limited by loop-flow thresholds (info in the CRAC).

::::{tabs}
:::{group-tab} JAVA API

```java
// src/main/java/com/powsybl/openrao/data/raoresult/api/extension/FlowResult.java

// get the loop-flow on a given FlowCNEC, at a given side, in a given unit, after optimization of a given instant
double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit);
```

:::
:::{group-tab} JSON File

Not serialized.

:::
::::

##### Commercial flow

The commercial flow on a CNEC.

> Can only be queried for flow CNECs limited by loop-flow thresholds (info in the CRAC).

::::{tabs}
:::{group-tab} JAVA API

```java
// src/main/java/com/powsybl/openrao/data/raoresult/api/extension/FlowResult.java

// get the commercial flow on a given FlowCNEC, at a given side, in a given unit, after optimization of a given instant
double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit);
```

:::
:::{group-tab} JSON File

Example:

```json
{
  "flow-results": [
    {
      "flowCnecId": "curativeCnecId",
      "initial": {
        "megawatt": {
          "side2": {
            "commercialFlow": 1114.5,
            ...
          }
        },
        "ampere": {
          "side2": {
            "commercialFlow": 1124.5,
            ...
          }
        }
      },
      "preventive": {
        "megawatt": {
          "side2": {
            "commercialFlow": 1214.5,
            ...
          }
        },
        "ampere": {
          "side2": {
            "commercialFlow": 1224.5,
            ...
          }
        }
      },
      "curative1": {
        "megawatt": {
          "side2": {
            "commercialFlow": 1314.5,
            ...
          }
        },
        "ampere": {
          "side2": {
            "commercialFlow": 1324.5,
            ...
          }
        }
      },
      "curative2": {
        "megawatt": {
          "side2": {
            "commercialFlow": 1414.5,
            ...
          }
        },
        "ampere": {
          "side2": {
            "commercialFlow": 1424.5,
            ...
          }
        }
      }
    }
  ]
}
```

:::
::::

##### Zonal PTDF absolute sum

The absolute sum of zonal PTDFs (influence factors) on a given CNEC. Reflects the influence of power exchanges between
commercial zones on the CNEC.

> Note that this value does not have a unit.

::::{tabs}
:::{group-tab} JAVA API

```java
// src/main/java/com/powsybl/openrao/data/raoresult/api/extension/FlowResult.java

// get the absolute sum of zonal PTDFs for a given FlowCNEC, at a given side, after optimization of a given instant
double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side);
```

:::
:::{group-tab} JSON File

> For compactness's sake, zonal PTDF sums are written in the MEGAWATTS section only:

Example:

```json
{
  "flow-results": [
    {
      "flowCnecId": "curativeCnecId",
      "initial": {
        "megawatt": {
          "side2": {
            "zonalPtdfSum": 0.6,
            ...
          }
        }
      },
      "preventive": {
        "megawatt": {
          "side2": {
            "zonalPtdfSum": 0.6,
            ...
          }
        }
      },
      "curative1": {
        "megawatt": {
          "side2": {
            "zonalPtdfSum": 0.13,
            ...
          }
        }
      },
      "curative2": {
        "megawatt": {
          "side2": {
            "zonalPtdfSum": 0.14,
            ...
          }
        }
      }
    }
  ]
}
```

:::
::::

#### Angle CNECs results

These results contain important RAO information about angle CNECs. 

::::{tabs}
:::{group-tab} JAVA API

```java
AngleResult angleResult = raoResult.getExtension(AngleResult.class);
```

:::
:::{group-tab} JSON File

Example:

```json
{
  "extensions": {
    "angle-results": [
      {
        "angleCnecId": "angleCnecId",
        "initial": {
          "degree": {
            "angle": 40.0
          }
        },
        "preventive": {
          "degree": {
            "angle": 2.0
          }
        }
      }
    ]
  }
}
```

:::
::::

Note that you have to use [AngleCnec](../input-data/crac/json.md#angle-cnecs) objects from the CRAC in order to query
the RaoResult Java API.

##### Angle

Access the angle value of an AngleCnec.

::::{tabs}
:::{group-tab} JAVA API

> - Note that this feature is not implemented in the default RAO result implementation, as AngleCNECs are not optimized
>   by the RAO, but monitored by a [monitoring module](../algorithms/monitoring.md),
>   with **ANGLE** as Physical parameter.
> - Currently, the only supported unit is **DEGREE**.

```java
// src/main/java/com/powsybl/openrao/data/raoresult/api/extension/AngleResult.java

// get the angle value for a given AngleCNEC, at a given state, in a given angle unit
double getAngle(Instant optimizedInstant, AngleCnec angleCnec, Unit unit);
```

:::
:::{group-tab} JSON File

Example:

```json
{
  "angle-results": [
    {
      "angleCnecId": "angleCnecId",
      "initial": {
        "degree": {
          "angle": 40.0
        }
      },
      "preventive": {
        "degree": {
          "angle": 2.0
        }
      }
    }
  ]
}
```

:::
::::

##### Margin

Access the angle margin value of an AngleCNEC.

::::{tabs}
:::{group-tab} JAVA API

> - Note that this feature is not implemented in the default RAO result implementation, as AngleCNECs are not optimized
>   by the RAO, but monitored by a [monitoring module](../algorithms/monitoring.md),
>   with **ANGLE** as Physical parameter.
> - Currently, the only supported unit is **DEGREE**.

```java
// src/main/java/com/powsybl/openrao/data/raoresult/api/extension/AngleResult.java

// get the margin value for a given AngleCNEC, at a given state, in a given angle unit
double getMargin(Instant optimizedInstant, AngleCnec angleCnec, Unit unit);
```

:::
:::{group-tab} JSON File

Not serialized.

:::
::::

#### Voltage CNECs results

These results contain important RAO information about voltage CNECs.

::::{tabs}
:::{group-tab} JAVA API

```java
VoltageResult voltageResult = raoResult.getExtension(AngleResult.class);
```

:::
:::{group-tab} JSON File

Example:

```json
{
  "extensions": {
    "voltage-results": [
      {
        "voltageCnecId": "voltageCnecId",
        "initial": {
          "kilovolt": {
            "minVoltage": 440.0,
            "maxVoltage": 417.0
          }
        },
        "preventive": {
          "kilovolt": {
            "minVoltage": 435.0,
            "maxVoltage": 432.0
          }
        }
      }
    ]
  }
}
```

:::
::::

Note that you have to use [VoltageCnec](../input-data/crac/json.md#voltage-cnecs) objects from the CRAC in order to
query the RaoResult Java API.

##### Voltage

Access the minimum and maximum voltage values of a VoltageCNEC (one VoltageCNEC contains multiple physical elements,
so multiple voltage values can exist).

::::{tabs}
:::{group-tab} JAVA API

> - Note that this feature is not implemented in the default RAO result implementation, as VoltageCNECs are not optimized
>   by the RAO, but monitored by a [monitoring module](../algorithms/monitoring.md),
>   with **VOLTAGE** as Physical parameter.
> - Currently, the only supported unit is **KILOVOLT**.

```java
// src/main/java/com/powsybl/openrao/data/raoresult/api/extension/VoltageResult.java

// get the min or max voltage value for a given VoltageCNEC, after optimization of a given instant, in a given voltage unit
double getMinVoltage(Instant optimizedInstant, VoltageCnec voltageCnec, Unit unit);
double getMaxVoltage(Instant optimizedInstant, VoltageCnec voltageCnec, Unit unit);
```

:::
:::{group-tab} JSON File

Example:

```json
{
  "voltage-results": [
    {
      "voltageCnecId": "voltageCnecId",
      "initial": {
        "kilovolt": {
          "minVoltage": 440.0,
          "maxVoltage": 417.0
        }
      },
      "preventive": {
        "kilovolt": {
          "minVoltage": 435.0,
          "maxVoltage": 432.0
        }
      }
    }
  ]
}
```

:::
::::

##### Margin

Access the worst voltage margin value (between min and max) of a VoltageCNEC.

::::{tabs}
:::{group-tab} JAVA API

> - Note that this feature is not implemented in the default RAO result implementation, as VoltageCNECs are not optimized
>   by the RAO, but monitored by a [monitoring module](../algorithms/monitoring.md),
>   with **VOLTAGE** as Physical parameter.
> - Currently, the only supported unit is **KILOVOLT**.

```java
// src/main/java/com/powsybl/openrao/data/raoresult/api/extension/VoltageResult.java

// get the margin value for a given VoltageCNEC, after optimization of a given instant, in a given voltage unit
double getMargin(Instant optimizedInstant, VoltageCnec voltageCnec, Unit unit);
```

:::
:::{group-tab} JSON File

Not serialized.

:::
::::

#### Implementation-specific extensions

Different RAO implementations can add extensions to the RAO result. These extensions allow the RAO implementation to
provide extra information about the computations, for example results of intermediary steps.  

##### FastRAO extensions

####### Critical CNECs result

This extension lists the CNECs that are considered in the last step of [FastRAO](../algorithms/castor/performance.md#fastrao).  

::::{tabs}
:::{group-tab} JAVA API

```java
// Access the extension
CriticalCnecsResult extension = raoResult.getExtension(CriticalCnecsResult.class);
// Fetch the critical CNEC IDs
Set<String> criticalCnecIds = extension.getCriticalCnecIds();
```

:::
:::{group-tab} JSON File

Example:

```json

{
  "type" : "RAO_RESULT",
  "version" : "2.0",
  ...,
  "extensions" : {
    "critical-cnecs-result" : [ {
      "flowCnecId" : "cnec1outageId"
    }, {
      "flowCnecId" : "cnec1prevId"
    }, {
      "flowCnecId" : "cnec2prevId"
    }, {
      "flowCnecId" : "cnec3autoId"
    }, {
      "flowCnecId" : "cnec3curId"
    }, {
      "flowCnecId" : "cnec3prevId"
    }, {
      "flowCnecId" : "cnec4prevId"
    } ]
  }
}

```

:::
::::

##### MARMOT extensions

###### Pre-time coupling overloaded CNECs

This extension lists the CNECs that are overloaded (i.e. have a negative margin) after time-independent RAOs.  
This means that these CNECs need to be secured by the time-coupled RAO.  
For curative CNECs in particular, this means that extra preventive actions should be implemented to secure them. 

::::{tabs}
:::{group-tab} JAVA API

```java
// Access the extension
PreTimeCouplingOverloadedCnecs extension = raoResult.getExtension(PreTimeCouplingOverloadedCnecs.class);
// Fetch the overloaded CNEC IDs
Set<String> overloadedCnecs = extension.getCriticalCnecIds();
```

:::
:::{group-tab} JSON File

Example:

```json

{
  "type" : "RAO_RESULT",
  "version" : "2.0",
  ...,
  "extensions" : {
    "pre-time-coupling-overloaded-cnecs" : [ {
      "flowCnecId" : "cnec1outageId"
    }, {
      "flowCnecId" : "cnec1prevId"
    }, {
      "flowCnecId" : "cnec2prevId"
    }, {
      "flowCnecId" : "cnec3autoId"
    } ]
  }
}

```

:::
::::
