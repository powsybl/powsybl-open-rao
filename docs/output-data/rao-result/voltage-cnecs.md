These results contain important RAO information about voltage CNECs.  
Note that you have to use [VoltageCnec](/input-data/crac/json.md#voltage-cnecs) objects from the CRAC in order to query the RaoResult Java API.

#### Voltage

Access the minimum and maximum voltage values of a VoltageCnec (one VoltageCnec contains multiple physical elements, 
so multiple voltage values can exist).

::::{tabs}
:::{group-tab} JAVA API

*Note that this feature is not implemented in the default RAO result implementation, as voltage CNECs are not optimised
by the RAO, but monitored by a [voltage monitoring module](/castor/monitoring/voltage-monitoring.md).*

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
        ...
        "minVoltage" : 4146.0,
        "maxVoltage" : 4156.0
      }
    },
    "preventive" : {
      "kilovolt" : {
        ...
        "minVoltage" : 4246.0,
        "maxVoltage" : 4256.0
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
by the RAO, but monitored by a [voltage monitoring module](/castor/monitoring/voltage-monitoring.md).*

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
        "margin" : 4141.0,
        "minVvoltage" : 4146.0,
        "maxVvoltage" : 4156.0
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
