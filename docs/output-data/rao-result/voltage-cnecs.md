These results contain important RAO information about voltage CNECs.  
Note that you have to use [VoltageCnec](/input-data/crac/json.md#voltage-cnecs) objects from the CRAC in order to query the RaoResult Java API.

#### Voltage

Access the voltage value of an VoltageCnec.

::::{tabs}
:::{group-tab} JAVA API

*Note that this feature is not implemented in the default RAO result implementation, as voltage CNECs are not optimised
by the RAO, but monitored by a [voltage monitoring module](/castor/monitoring/voltage-monitoring.md).*

~~~java
// get the voltage value for a given voltage cnec, after optimisation of a given instant, in a given voltage unit
double getVoltage(Instant optimizedInstant, VoltageCnec voltageCnec, Unit unit);
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
  "voltageCnecResults" : [ {
    "voltageCnecId" : "voltageCnecId",
    "initial" : {
      "kilovolt" : {
        "voltage" : 4146.0,
        ...
      }
    },
    "preventive" : {
      "kilovolt" : {
        "voltage" : 4246.0,
        ...
      }
    }, ...
~~~

:::
::::

#### Margin

Access the voltage margin value of a VoltageCnec.

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
        ...
        "margin" : 4141.0
      }
    },
    "preventive" : {
      "kilovolt" : {
        ...
        "margin" : 4241.0
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
        "voltage" : 4146.0,
        "margin" : 4141.0
      }
    },
    "preventive" : {
      "kilovolt" : {
        "voltage" : 4246.0,
        "margin" : 4241.0
      }
    }, ...
~~~
