# JSON RAO Result File

## Introduction

A **R**emedial **A**ction **O**ptimisation process provides an optimal list of remedial actions to be applied in basecase and after contingencies listed in the [CRAC](../input-data/crac.md). The decisions are based upon the impact of these remedial actions on the CRAC's [CNECs](../input-data/crac.md#cnec).

A **RaoResult object model** has been designed in OpenRAO in order to hold all the important results of optimisation.
In this page, we present:
- where to find the RaoResult instance,
- how to save a RaoResult java object to a JSON file,
- how to import a RaoResult java object from a JSON file,
- how to access information in the RaoResult, using either the RaoResult java object or the JSON file.

## Accessing the RAO result

The [RaoResult](https://github.com/powsybl/powsybl-open-rao/blob/main/data/rao-result/rao-result-api/src/main/java/com/powsybl/openrao/data/raoresult/api/RaoResult.java) java object is actually an interface that is implemented by many OpenRAO classes. However, one only needs to use the interface's functions.
A RaoResult object is returned by OpenRAO's main optimisation method:

~~~java
CompletableFuture<RaoResult> RaoProvider::run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant)
~~~

Where RaoProvider is the chosen implementation of the RAO, such as [CASTOR](https://github.com/powsybl/powsybl-open-rao/blob/main/ra-optimisation/search-tree-rao/src/main/java/com/powsybl/openrao/searchtreerao/castor/algorithm/Castor.java).

## Exporting and importing a JSON file

A RaoResult object can be saved into a JSON file (no matter what implementation it is).
A RaoResult JSON file can be imported into a [RaoResultImpl](https://github.com/powsybl/powsybl-open-rao/blob/main/data/rao-result/rao-result-impl/src/main/java/com/powsybl/openrao/data/raoresult/impl/RaoResultImpl.java), and used as a RaoResult java object.

### Export

Example:

~~~java
raoResult.write("JSON", crac, properties, outputStream);
~~~

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

~~~java
RaoResult importedRaoResult = RaoResult.read(inputStream, crac);
~~~

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

### Computation status

This field contains the status of sensitivity / load-flow computations during the RAO, for a given [state](../input-data/crac/json.md#instants-and-states),
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

### Security status

This field contains the security status of the network for a given optimized instant, and for a given list of physical
parameters among:
- FLOW
- ANGLE
- VOLTAGE

::::{tabs}
:::{group-tab} JAVA API

~~~java
    // Indicates whether all the CNECs of a given type at a given instant are secure.
    boolean isSecure(Instant optimizedInstant, PhysicalParameter... u);

    // Indicates whether all the CNECs of a given type are secure at last instant (i.e. after RAO).
    boolean isSecure(PhysicalParameter... u);

    // Indicates whether all the CNECs are secure at last instant (i.e. after RAO).
    boolean isSecure();
~~~

:::
:::{group-tab} JSON File

This information is not written in the json file, it is deduced from the values of the CNECs' margins (see below).

:::
::::

### Execution details

This field can either contain information about the failure when the RAO fails, or macro information about which steps the [CASTOR RAO](../algorithms/castor.md#algorithm-details) executed when it did not fail.  
(See also: [Second preventive RAO parameters](../parameters/implementation-specific-parameters.md#second-preventive-rao-parameters))

Note: This field replaced the optimizationStepsExecuted field.

::::{tabs}
:::{group-tab} JAVA API

~~~java
String getExecutionDetails();
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
{
  "executionDetails": "Second preventive improved first preventive results",
  ...
}
~~~

:::
::::

### Objective function cost results

Contains information about the reached objective function value, seperated into functional and virtual costs:
- the **functional** cost reflects the business value of the objective (e.g. the cost associated to the minimum margin and the business penalties on usage of remedial actions)
- the **virtual** cost reflects the violation of some constraints (e.g. MNEC & loop-flow constraints)

::::{tabs}
:::{group-tab} JAVA API

~~~java
// get the functional cost after optimisation of a given instant
double getFunctionalCost(Instant optimizedInstant);

// get the total virtual cost after optimisation of a given instant
double getVirtualCost(Instant optimizedInstant);

// get a specific virtual cost after optimisation of a given instant
double getVirtualCost(Instant optimizedInstant, String virtualCostName);

// get all the virtual cost names in the RAO
Set<String> getVirtualCostNames();

// get the overall cost (functional + total virtual) after optimisation of a given instant
double getCost(Instant optimizedInstant);
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
"costResults" : {
    "initial" : {
      "functionalCost" : 100.0,
      "virtualCost" : {
        "loopFlow" : 0.0,
        "MNEC" : 0.0
      }
    },
    "preventive" : {
      "functionalCost" : 80.0,
      "virtualCost" : {
        "loopFlow" : 0.0,
        "MNEC" : 0.0
      }
    },
    "auto" : {
      "functionalCost" : -20.0,
      "virtualCost" : {
        "loopFlow" : 15.0,
        "MNEC" : 20.0
      }
    },
    "curative" : {
      "functionalCost" : -50.0,
      "virtualCost" : {
        "loopFlow" : 10.0,
        "MNEC" : 2.0
      }
    }
  },
~~~

:::
::::

### Flow CNECs results

These results contain important RAO information about flow CNECs.  
Note that you have to use [FlowCnec](../input-data/crac/json.md#flow-cnecs) objects from the CRAC in order to query the RaoResult Java API.
Most results are power flow results (like flows & margins), and can be queried in two [units](https://github.com/powsybl/powsybl-open-rao/blob/main/commons/src/main/java/com/powsybl/openrao/commons/Unit.java)
(AMPERE & MEGAWATT) and on one or two sides (ONE & TWO).

#### Flow

The actual power flow on the branch.

::::{tabs}
:::{group-tab} JAVA API

~~~java
// get the flow on a given flow cnec, at a given side, in a given unit, after optimisation of a given instant
double getFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit);
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
"flowCnecResults" : [ {
    "flowCnecId" : "cnec1outageId",
    "initial" : {
      "megawatt" : {
        ...
        "side2" : {
            "flow": 1110.0,
            ...
        },
        "side1" : {
            "flow": 1105.0,
            ...
        }
      },
      "ampere" : {
        ...
        "side2" : {
            "flow": 2220.0,
            ...
        },
        "side1" : {
            "flow": 2210.0,
            ...
        }        
      },
      ...
    },
    "preventive" : {
      "megawatt" : {
        ...
        "side1" : {
            "flow": 1210.0,
            ...
        }
      },
      "ampere" : {
        ...
        "side1" : {
            "flow": 505.0,
            ...
        }
      },
      ...
    }
  }, {
    "flowCnecId" : "cnec1prevId",
    "initial" : {
        ...
~~~

:::
::::

#### Margin

The flow margin of the CNEC. If two flow values exist for the CNEC (i.e. if it is monitored on both sides) and / or if
it has multiple thresholds, the smallest margin is returned.

::::{tabs}
:::{group-tab} JAVA API

~~~java
// get the margin of a given flow cnec, in a given unit, after optimisation of a given instant
double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit);
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
"flowCnecResults" : [ {
    "flowCnecId" : "cnec1outageId",
    "initial" : {
      "megawatt" : {
        ...
        "margin" : 1111.0,
        ...
      },
      "ampere" : {
        ...
        "margin" : 1121.0,
        ...
      },
      ...
    },
    "preventive" : {
      "megawatt" : {
        ...
        "margin" : 1211.0,
        ...
      },
      "ampere" : {
        ...
        "margin" : 1221.0,
        ...
      },
      ...
    }
  }, {
    "flowCnecId" : "cnec1prevId",
    "initial" : {
        ...
~~~

:::
::::

#### Relative margin

The relative margin is the smallest flow margin of the CNEC.
- When the margin is negative, the relative margin is equal to the margin
- When the margin is positive, it is equal to the margin divided by the zonal PTDF absolute sum  
  It is used to artificially increase (in the RAO) the margin on flow cnecs that are less impacted by changes in
  power exchanges between commercial zones, in order to prioritize other CNECs in the minimum margin optimisation.  
  (See [Modelling the maximum minimum relative margin objective function](../algorithms/castor/linear-problem/objective-function-types/max-min-relative-margin-filler.md))

::::{tabs}
:::{group-tab} JAVA API

~~~java
// get the relative margin of a given flow cnec, in a given unit, after optimisation of a given instant
double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit);
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
"flowCnecResults" : [ {
    "flowCnecId" : "cnec1outageId",
    "initial" : {
      "megawatt" : {
        ...
        "relativeMargin" : 1112.0,
        ...
      },
      "ampere" : {
        ...
        "relativeMargin" : 1122.0,
        ...
      },
      ...
    },
    "preventive" : {
      "megawatt" : {
        ...
        "relativeMargin" : 1212.0,
        ...
      },
      "ampere" : {
        ...
        "relativeMargin" : 1222.0,
        ...
      },
      ...
    }
  }, {
    "flowCnecId" : "cnec1prevId",
    "initial" : {
        ...
~~~

:::
::::

#### Loop-Flow

The loop-flow value on a CNEC.  
*Can only be queried for flow CNECs limited by loop-flow thresholds (info in the CRAC).*

::::{tabs}
:::{group-tab} JAVA API

~~~java
// get the loop-flow on a given flow cnec, at a given side, in a given unit, after optimisation of a given instant
double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit);
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
"flowCnecResults" : [ {
    "flowCnecId" : "cnec1outageId",
    "initial" : {
      "megawatt" : {
        ...
        "side1" : {
            ...
            "loopFlow": 1113.0,
            ...
        }
        ...
      },
      "ampere" : {
        ...
        "side1" : {
            ...
            "loopFlow": 1123.0,
            ...
        }
        ...
      },
      ...
    },
    "preventive" : {
      "megawatt" : {
        ...
        "side2" : {
            ...
            "loopFlow": 1213.0,
            ...
        }
        ...
      },
      "ampere" : {
        ...
        "side2" : {
            ...
            "loopFlow": 1223.0,
            ...
        }
        ...
      },
      ...
    }
  }, {
    "flowCnecId" : "cnec1prevId",
    "initial" : {
        ...
~~~

:::
::::

#### Commercial flow

The commercial flow on a CNEC.  
*Can only be queried for flow CNECs limited by loop-flow thresholds (info in the CRAC).*

::::{tabs}
:::{group-tab} JAVA API

~~~java
// get the commercial flow on a given flow cnec, at a given side, in a given unit, after optimisation of a given instant
double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit);
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
"flowCnecResults" : [ {
    "flowCnecId" : "cnec1outageId",
    "initial" : {
      "megawatt" : {
        ...
        "side1" : {
            ...
            "commercialFlow": 1114.0,
            ...
        }
        ...
      },
      "ampere" : {
        ...
        "side1" : {
            ...
            "commercialFlow": 1124.0,
            ...
        }
        ...
      },
      ...
    },
    "preventive" : {
      "megawatt" : {
        ...
        "side2" : {
            ...
            "commercialFlow": 1214.0,
            ...
        }
        ...
      },
      "ampere" : {
        ...
        "side2" : {
            ...
            "commercialFlow": 1224.0,
            ...
        }
        ...
      },
      ...
    }
  }, {
    "flowCnecId" : "cnec1prevId",
    "initial" : {
        ...
~~~

:::
::::

#### Zonal PTDF absolute sum

The absolute sum of zonal PTDFs (influence factors) on a given CNEC. Reflects the influence of power exchanges between commercial zones on the CNEC.  
*Note that this value does not have a unit*

::::{tabs}
:::{group-tab} JAVA API

~~~java
// get the absolute sum of zonal PTDFs for a given flow cnec, at a given side, after optimisation of a given instant
double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side);
~~~

:::
:::{group-tab} JSON File

*For compactness, zonal PTDF sums are written in the MEGAWATTS section*
Example:

~~~json
"flowCnecResults" : [ {
    "flowCnecId" : "cnec1outageId",
    "initial" : {
        "megawatt" : {
            ...
            "side2" : {
                ...
                "zonalPtdfSum" : 0.1
                ...
            }
            ...
        },
        ...
    },
    "preventive" : {
      ...
~~~

:::
::::

#### Complete JSON example

This complete example was also enriched with two curative instants: `curative1` and  `curative2`.

~~~json
  "flowCnecResults" : [ {
    "flowCnecId" : "cnec1outageId",
    "initial" : {
      "megawatt" : {
        "margin" : 1111.0,
        "relativeMargin" : 1112.0,
        "side2" : {
          "flow" : 1110.5,
          "loopFlow" : 1113.5,
          "commercialFlow" : 1114.5,
          "zonalPtdfSum" : 0.6
        }
      },
      "ampere" : {
        "margin" : 1121.0,
        "relativeMargin" : 1122.0,
        "side2" : {
          "flow" : 1120.5,
          "loopFlow" : 1123.5,
          "commercialFlow" : 1124.5
        }
      }
    },
    "preventive" : {
      "megawatt" : {
        "margin" : 1211.0,
        "relativeMargin" : 1212.0,
        "side2" : {
          "flow" : 1210.5,
          "loopFlow" : 1213.5,
          "commercialFlow" : 1214.5,
          "zonalPtdfSum" : 0.6
        }
      },
      "ampere" : {
        "margin" : 1221.0,
        "relativeMargin" : 1222.0,
        "side2" : {
          "flow" : 1220.5,
          "loopFlow" : 1223.5,
          "commercialFlow" : 1224.5
        }
      }
    },
    "curative1" : {
      "megawatt" : {
        "margin" : 1311.0,
        "relativeMargin" : 1312.0,
        "side2" : {
          "flow" : 1310.5,
          "loopFlow" : 1313.5,
          "commercialFlow" : 1314.5,
          "zonalPtdfSum" : 0.13
        }
      },
      "ampere" : {
        "margin" : 1321.0,
        "relativeMargin" : 1322.0,
        "side2" : {
          "flow" : 1320.5,
          "loopFlow" : 1323.5,
          "commercialFlow" : 1324.5
        }
      }
    },
    "curative2" : {
      "megawatt" : {
        "margin" : 1411.0,
        "relativeMargin" : 1412.0,
        "side2" : {
          "flow" : 1410.5,
          "loopFlow" : 1413.5,
          "commercialFlow" : 1414.5,
          "zonalPtdfSum" : 0.14
        }
      },
      "ampere" : {
        "margin" : 1421.0,
        "relativeMargin" : 1422.0,
        "side2" : {
          "flow" : 1420.5,
          "loopFlow" : 1423.5,
          "commercialFlow" : 1424.5
        }
      }
    }
  }, {
    "flowCnecId" : "cnec1prevId",
    "initial" : {
      "megawatt" : {
        "margin" : 1111.0,
        "relativeMargin" : 1112.0,
        "side2" : {
          "flow" : 1110.5,
          "loopFlow" : 1113.5,
          "commercialFlow" : 1114.5,
          "zonalPtdfSum" : 0.6
        }
      },
      "ampere" : {
        "margin" : 1121.0,
        "relativeMargin" : 1122.0,
        "side2" : {
          "flow" : 1120.5,
          "loopFlow" : 1123.5,
          "commercialFlow" : 1124.5
        }
      }
    },
    "preventive" : {
      "megawatt" : {
        "margin" : 1211.0,
        "relativeMargin" : 1212.0,
        "side2" : {
          "flow" : 1210.5,
          "loopFlow" : 1213.5,
          "commercialFlow" : 1214.5,
          "zonalPtdfSum" : 0.6
        }
      },
      "ampere" : {
        "margin" : 1221.0,
        "relativeMargin" : 1222.0,
        "side2" : {
          "flow" : 1220.5,
          "loopFlow" : 1223.5,
          "commercialFlow" : 1224.5
        }
      }
    },
    "curative1" : {
      "megawatt" : {
        "margin" : 1311.0,
        "relativeMargin" : 1312.0,
        "side2" : {
          "flow" : 1310.5,
          "loopFlow" : 1313.5,
          "commercialFlow" : 1314.5,
          "zonalPtdfSum" : 0.13
        }
      },
      "ampere" : {
        "margin" : 1321.0,
        "relativeMargin" : 1322.0,
        "side2" : {
          "flow" : 1320.5,
          "loopFlow" : 1323.5,
          "commercialFlow" : 1324.5
        }
      }
    },
    "curative2" : {
      "megawatt" : {
        "margin" : 1411.0,
        "relativeMargin" : 1412.0,
        "side2" : {
          "flow" : 1410.5,
          "loopFlow" : 1413.5,
          "commercialFlow" : 1414.5,
          "zonalPtdfSum" : 0.14
          }
        },
        "ampere" : {
          "margin" : 1421.0,
          "relativeMargin" : 1422.0,
          "side2" : {
            "flow" : 1420.5,
            "loopFlow" : 1423.5,
            "commercialFlow" : 1424.5
          }
        }
      }
    }, ...
~~~

### Angle CNECs results

These results contain important RAO information about angle CNECs.  
Note that you have to use [AngleCnec](../input-data/crac/json.md#angle-cnecs) objects from the CRAC in order to query the RaoResult Java API.

#### Angle

Access the angle value of an AngleCnec.

::::{tabs}
:::{group-tab} JAVA API

*Note that this feature is not implemented in the default RAO result implementation, as angle CNECs are not optimised
by the RAO, but monitored by a [monitoring module](../algorithms/monitoring.md), with **ANGLE** as Physical parameter.*

~~~java
// get the angle value for a given angle cnec, at a given state, in a given angle unit
double getAngle(OptimizationState optimizationState, AngleCnec angleCnec, Unit unit);
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
  "angleCnecResults" : [ {
    "angleCnecId" : "angleCnecId",
    "initial" : {
      "degree" : {
        "angle" : 3135.0,
        ...
      }
    },
    "afterPRA" : {
      "degree" : {
        "angle" : 3235.0,
        ...
      }
    },
    ...
~~~

:::
::::

#### Margin

Access the angle margin value of an AngleCnec.

::::{tabs}
:::{group-tab} JAVA API

*Note that this feature is not implemented in the default RAO result implementation, as angle CNECs are not optimised
by the RAO, but monitored by a [monitoring module](../algorithms/monitoring.md), with **ANGLE** as Physical parameter.*

~~~java
// get the margin value for a given angle cnec, at a given state, in a given angle unit
double getMargin(OptimizationState optimizationState, AngleCnec angleCnec, Unit unit);
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
  "angleCnecResults" : [ {
    "angleCnecId" : "angleCnecId",
    "initial" : {
      "degree" : {
        "margin" : 3135.0,
        ...
      }
    },
    "afterPRA" : {
      "degree" : {
        "margin" : 3235.0,
        ...
      }
    },
    ...
~~~

:::
::::

#### Complete JSON example

~~~json
  "angleCnecResults" : [ {
    "angleCnecId" : "angleCnecId",
    "initial" : {
      "degree" : {
        "angle" : 3135.0,
        "margin" : 3131.0
      }
    },
    "afterPRA" : {
      "degree" : {
        "angle" : 3235.0,
        "margin" : 3231.0
      }
    }, ...
~~~

### Voltage CNECs results

These results contain important RAO information about voltage CNECs.  
Note that you have to use [VoltageCnec](../input-data/crac/json.md#voltage-cnecs) objects from the CRAC in order to query the RaoResult Java API.

#### Voltage

Access the minimum and maximum voltage values of a VoltageCnec (one VoltageCnec contains multiple physical elements,
so multiple voltage values can exist).

::::{tabs}
:::{group-tab} JAVA API

*Note that this feature is not implemented in the default RAO result implementation, as voltage CNECs are not optimised
by the RAO, but monitored by a [monitoring module](../algorithms/monitoring.md), with **VOLTAGE** as Physical parameter.*

~~~java
// get the min or max voltage value for a given voltage cnec, after optimisation of a given instant, in a given voltage unit
double getVoltage(Instant optimizedInstant, VoltageCnec voltageCnec, MinOrMax minOrMax, Unit unit);
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
  "voltageCnecResults" : [ {
    "voltageCnecId" : "voltageCnecId",
    "initial" : {
      "kilovolt" : {
        "minVoltage" : 4146.0,
        "maxVoltage" : 4156.0,
        ...
      }
    },
    "preventive" : {
      "kilovolt" : {
        "minVoltage" : 4246.0,
        "maxVoltage" : 4256.0, 
        ...
      }
    }, ...
~~~

:::
::::

#### Margin

Access the worst voltage margin value (between min & max) of a VoltageCnec.

::::{tabs}
:::{group-tab} JAVA API

*Note that this feature is not implemented in the default RAO result implementation, as voltage CNECs are not optimised
by the RAO, but monitored by a [voltage monitoring module](../algorithms/monitoring.md), with **VOLTAGE** as Physical parameter.*

~~~java
// get the margin value for a given voltage cnec, after optimisation of a given instant, in a given voltage unit
double getMargin(Instant optimizedInstant, VoltageCnec voltageCnec, Unit unit);
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
  "voltageCnecResults" : [ {
    "voltageCnecId" : "voltageCnecId",
    "initial" : {
      "kilovolt" : {
        "margin" : 4141.0,
        ...
      }
    },
    "preventive" : {
      "kilovolt" : {
        "margin" : 4241.0,
        ...
      }
    }, ...
~~~

:::
::::

#### Complete JSON example

~~~json
  "voltageCnecResults" : [ {
    "voltageCnecId" : "voltageCnecId",
    "initial" : {
      "kilovolt" : {
        "minVoltage" : 4146.0,
        "maxVoltage" : 4156.0,
        "margin" : 4141.0
      }
    },
    "preventive" : {
      "kilovolt" : {
        "margin" : 4241.0
        "minVoltage" : 4246.0,
        "maxVoltage" : 4246.0
      }
    }, ...
~~~

### Network actions results

These results contain information about the selection of network actions by the RAO.
*Note that you will need to use [NetworkAction](../input-data/crac/json.md#network-actions) objects from the CRAC for querying the Java API.*

::::{tabs}
:::{group-tab} JAVA API

~~~java
// query if a network action was already activated before a given state was studied
boolean wasActivatedBeforeState(State state, NetworkAction networkAction);

// query if a network action was chosen during the optimization of a given state
boolean isActivatedDuringState(State state, NetworkAction networkAction);

// get the list of all network actions chosen during the optimization of a given state
Set<NetworkAction> getActivatedNetworkActionsDuringState(State state);

// query if a network action was during or before a given state
boolean isActivated(State state, NetworkAction networkAction);
~~~

:::
:::{group-tab} JSON File

*Network actions that are not selected by the RAO do not appear in the JSON file*
Example:

~~~json
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
~~~

:::
::::

### Standard range actions results

These results contain information about the selection of standard range actions by the RAO.  
Standard range actions are range actions that have a continuous set-point that can be optimised in a given range.  
Actually, standard range actions handled by OpenRAO are: **HVDC range actions** and **Injection range actions**.  
*Note that you will need to use [RangeAction](../input-data/crac/json.md#range-actions) objects from the CRAC for querying the Java API.*

::::{tabs}
:::{group-tab} JAVA API

~~~java
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
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
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
~~~

:::
::::

### PST range actions results

These results contain information about the application of PST range actions by the RAO.  
PSTs are not standard range actions, because they have integer tap positions; a few extra methods in the API allow
querying the tap positions. All "set-point" methods of standard range actions also can also be used.    
*Note that you will need to use [PstRangeAction or RangeAction](../input-data/crac/json.md#pst-range-action) objects from the CRAC for querying the Java API.*

::::{tabs}
:::{group-tab} JAVA API

~~~java
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
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
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
~~~

:::
::::
