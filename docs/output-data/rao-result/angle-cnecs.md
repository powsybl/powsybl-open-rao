These results contain important RAO information about angle CNECs.  
Note that you have to use [AngleCnec](/input-data/crac/json.md#angle-cnecs) objects from the CRAC in order to query the RaoResult Java API.

#### Angle

Access the angle value of an AngleCnec.

::::{tabs}
:::{group-tab} JAVA API

*Note that this feature is not implemented in the default RAO result implementation, as angle CNECs are not optimised
by the RAO, but monitored by an [angle monitoring module](/castor/angle-monitoring/angle-monitoring.md).*

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
by the RAO, but monitored by an [angle monitoring module](/castor/angle-monitoring/angle-monitoring.md).*

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