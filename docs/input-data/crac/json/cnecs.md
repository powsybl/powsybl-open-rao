A CRAC should define CNECs. A CNEC is a "**C**ritical **N**etwork **E**lement and **C**ontingency" (also known as "CBCO"
or "Critical Branch and Critical Outage").

A CNEC is a **network element**, which is considered at a given **instant**, after a given **contingency** (i.e. at a
given FARAO ["state"](#instants-states)).  
The contingency is omitted if the CNEC is defined at the preventive instant.

> 💡  **NOTE**  
> A FARAO CNEC is associated to one instant and one contingency only. This is not the case for all native CRAC formats:
> for instance, in the [CORE merged-CB CRAC format](fbconstraint), the post-outage CNECs are implicitly defined for the
> two instants outage and curative.
>
> However, we are talking here about the internal FARAO CRAC format, which has its own independent conventions, and which
> is imported from native CRAC formats using [CRAC importers](import).

A CNEC has an operator, i.e. the identifier of the TSO operating its network element.  
Moreover, a CNEC can have a reliability margin: a safety buffer to cope with unplanned events or uncertainties of input
data (i.e. an extra margin).

### Optimised and monitored CNECs
CNECs can be monitored and/or optimised. This notion of monitored/optimised has been introduced by the capacity
calculation on the CORE region, and is now also used for the CSE region:
- maximise the margins of CNECs that are "optimised"
- ensure that the margins of "monitored" CNECs are positive and/or are not decreased by the RAO.

FARAO contains 3 families of CNECs, depending on which type of physical constraints they have: **FlowCnecs**,
**AngleCnecs** and **VoltageCnecs**.

### Flow CNECs
A "FlowCnec" has the two following specificities:

- it contains one network element that is a **Branch**. In the PowSyBl vocabulary, a "Branch"
  is an element connected to two terminals. For instance, 
  [lines](https://www.powsybl.org/pages/documentation/grid/model/#line), 
  [tie-lines](https://www.powsybl.org/pages/documentation/grid/model/#tie-line), 
  [transformers](https://www.powsybl.org/pages/documentation/grid/model/#transformers) 
  and [PSTs](https://www.powsybl.org/pages/documentation/grid/model/#phase-tap-changer)
  are all "Branches".
- the physical parameter which is monitored on the CNEC is the **power flow**.

A FlowCnec has **two sides**, which correspond to the two terminals of the PowSyBl network element of the FlowCnec
(usually called terminals "one" and "two", or terminals "left" and "right").
The notion of **direction** is also inherent to the FlowCnec: a flow in direction "direct" is a flow from terminal
one/left to terminal two/right, while a flow in direction "opposite" is a flow from terminal two/right to terminal
one/left. The convention of FARAO is that a positive flow is a flow in the "direct" direction, while a negative flow is
a flow in the "opposite" direction.

> 💡  **NOTE**  
> A FARAO FlowCnec is one implementation of the generic ["BranchCnec"](https://github.com/powsybl/powsybl-open-rao/blob/main/data/crac/crac-api/src/main/java/com/powsybl/openrao/data/cracapi/cnec/BranchCnec.java).
> If needed, this would allow you a fast implementation of other types of CNECs, on branches, but with a monitored
> physical parameter other than power flow.

#### Flow limits on a FlowCnec
A FlowCnec has flow limits, called "thresholds" in FARAO. These thresholds define the limits between which the power
flow of the FlowCnec should ideally remain.
- They can be defined in megawatt, ampere, or in percentage of the Imax of the branch (in which case the Imax is read in
  the network file).
- A threshold is defined either on the left or on the right side of the FlowCnec.
  > 💡  **NOTE**
  > The side of the branch on which the threshold is set is particularly crucial in the following cases:
  > - when the threshold is defined in ampere or %Imax on a [**transformer**](https://www.powsybl.org/pages/documentation/grid/model/#transformers),
  >   as the current values on the two sides of a transformer are different,
  > - when the threshold is defined in %Imax on a [**tie-line**](https://www.powsybl.org/pages/documentation/grid/model/#tie-line),
  >   as the current limits are usually different on both sides of a tie-line,
  > - when the application uses **AC load-flow** computation, as the flow values on the two sides of a branch are
  >   different (due to losses). The CracCreationParameters allows the user to [decide which side(s) should be monitored by default](creation-parameters#default-monitored-line-side).
- A threshold has a minimum and/or a maximum value. The maximum value represents the maximum value of the flow in the "direct" direction
  and the minimum value represents the maximum value of the flow in the "opposite" direction.
  Therefore, for FlowCnecs that only have one minimum or one maximum value, the flow is implicitly monitored in
  only one direction (see example 1 of picture below).

![FlowCnec-Threshold](/_static/img/flowcnec.png)


In the examples above, all the thresholds are defined in megawatt. If the thresholds are defined in ampere, or in %Imax,
additional data is required in order to handle the following conversions:
- if one threshold of the FlowCnec is in ampere or in percentage of Imax, the nominal voltage on both sides of the threshold must be defined
- if one threshold of the FlowCnec is in percentage of Imax, the Imax of the FlowCnec on the side of the threshold must be defined

> 💡  **NOTE**
> A FlowCnec's reliability margin is also known as FRM for Flow Reliability Margin.
> It can only be defined in megawatt, it is subtracted from the thresholds of the FlowCnec, adding an extra constraint.

#### Creating a FlowCnec
In FARAO, FlowCnecs can be created by the java API, or written in the json CRAC internal format, as shown below:

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
crac.newFlowCnec()
    .withId("preventive-cnec-with-one-threshold-id")
    .withNetworkElement("network-element-id")
    .withInstant("preventive")
    .withOperator("operator1")
    .withReliabilityMargin(50.)
    .withOptimized(true)
    .newThreshold()
      .withUnit(Unit.MEGAWATT)
      .withSide(Side.LEFT)
      .withMin(-1500.)
      .withMax(1500.)
      .add()
    .add();

crac.newFlowCnec()
    .withId("curative-cnec-with-two-thresholds-id")
    .withName("curative-cnec-with-two-thresholds-name")
    .withNetworkElement("network-element-id")
    .withInstant("curative")
    .withContingency("contingency-id")
    .withOperator("operator1")
    .newThreshold()
      .withUnit(Unit.PERCENT_IMAX)
      .withSide(Side.RIGHT)
      .withMax(0.95)
      .add()
    .newThreshold()
      .withUnit(Unit.AMPERE)
      .withSide(Side.LEFT)
      .withMin(-450.)
      .add()
    .withReliabilityMargin(50.)
    .withOptimized(true)
    .withMonitored(false)
    .withNominalVoltage(380., Side.LEFT)
    .withNominalVoltage(220., Side.RIGHT)
    .withIMax(500.) // this means that the value is the same on both sides, but the side could have been specified using "withImax(500., Side.RIGHT)" instead 
    .add();
~~~
:::
:::{group-tab} JSON file
~~~json
"flowCnecs" : [ {
  "id" : "preventive-cnec-with-one-threshold-id",
  "name" : "preventive-cnec-with-one-threshold-id",
  "networkElementId" : "network-element-id",
  "operator" : "operator1",
  "instant" : "preventive",
  "optimized" : true,
  "monitored" : false,
  "frm" : 50.0,
  "thresholds" : [ {
    "unit" : "megawatt",
    "min" : -1500.0,
    "max" : 1500.0,
    "side" : "left"
  } ]
},  {
  "id" : "curative-cnec-with-two-thresholds-id",
  "name" : "curative-cnec-with-two-thresholds-name",
  "networkElementId" : "network-element-id",
  "operator" : "operator1",
  "instant" : "curative",
  "contingencyId" : "contingency-id",
  "optimized" : true,
  "monitored" : false,
  "frm" : 50.0,
  "iMax" : [ 500.0 ],
  "nominalV" : [ 380.0, 220.0 ],
  "thresholds" : [ {
    "unit" : "ampere",
    "min" : -450.0,
    "side" : "left"
  }, {
    "unit" : "percent_imax",
    "max" : 0.95,
    "side" : "right"
  } ]
} ]
~~~
:::
:::{group-tab} Object fields
🔴⭐ **identifier**  
⚪ **name**  
🔴 **network element**: one network element  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **network element id**: must be the id of a PowSyBl network identifiable  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ⚪ **network element name**  
🔴 **instant**  
🔵 **contingency**: mandatory, except if the instant is preventive. Must be the id of a contingency which exists in the CRAC  
⚪ **operator**  
⚪ **reliability margin**: default value = 0 MW  
⚪ **optimized**: default value = false  
⚪ **monitored**: default value = false  
🔴 **thresholds**: list of 1 to N thresholds, a FlowCnec must contain at least one threshold  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **unit**  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **side**  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔵 **minValue**  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔵 **maxValue**: at least one of minValue/maxValue should be defined
🔵 **nominal voltages**: mandatory if the FlowCnec has at least one threshold in %Imax or A  
🔵 **iMax**:  mandatory if the FlowCnec has at least one threshold in %Imax  
:::
::::

#### Loop-flow extension
When a FlowCnec carries a LoopFlowThreshold extension (and if [loop-flow constraints are enabled in the RAO](/docs/parameters#loop-flow-parameters)),
its loop-flow is monitored by the RAO, that will keep it [under its threshold](/docs/engine/ra-optimisation/loop-flows)
when optimising remedial actions.  
The loop-flow extension defines the loop-flow threshold to be respected by the RAO (even though the initial loop-flow
value on this CNEC may override this user-defined thrshold, as explained [here](/docs/castor/linear-optimisation-problem/max-loop-flow-filler)).

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
flowCnec = crac.getFlowCnec("cnec-with-mw-loop-flow-extension");
flowCnec.newExtension(LoopFlowThresholdAdder.class)
        .withUnit(Unit.MEGAWATT)
        .withValue(150.0)
        .add();

flowCnec = crac.getFlowCnec("cnec-with-pimax-loop-flow-extension");
flowCnec.newExtension(LoopFlowThresholdAdder.class)
        .withUnit(Unit.PERCENT_IMAX)
        .withValue(0.9)
        .add();
~~~
:::
:::{group-tab} JSON file
~~~json
"flowCnecs" : [ {
  "id" : "cnec-with-mw-loop-flow-extension",
  ...
  "extensions" : {
    "LoopFlowThreshold" : {
      "inputThreshold" : 150.0,
      "inputThresholdUnit" : "megawatt"
    }
  }
}, {
  "id" : "cnec-with-pimax-loop-flow-extension",
  ...
  "extensions" : {
    "LoopFlowThreshold" : {
      "inputThreshold" : 0.9,
      "inputThresholdUnit" : "percent_imax"
    }
  }
} ]
~~~
:::
:::{group-tab} Object fields
🔴 **unit**: unit of the threshold  
🔴 **value**: value of the threshold in the given unit  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; *(if the unit is %Imax, the value should be between 0 and 1, where 1 = 100%)*
:::
::::

### Angle CNECs
An "AngleCnec" is a branch which may see a phase angle shift between its two ends when it's disconnected. This may induce
insecurities in the network when it's back up. That's why we monitor angle CNECs and associate with them remedial actions
(generally re-dispatching) that can reduce the phase angle shift between the two ends.

In terms of FARAO object model, an AngleCnec is a CNEC. Even though it is associated with a branch, it is not a
BranchCnec, because we cannot define on which side it is monitored: it is monitored on both sides (more specifically,
we monitor the phase shift between the two sides).

An AngleCnec has the following specificities:

- it contains two network elements, an importing node and an exporting node, that represent the importing and exporting ends of the branch.
- the physical parameter which is monitored by the CNEC is the **angle**.
- it must contain at least one threshold, defined in degrees. A threshold has a minimum and/or a maximum value.

> 💡  **NOTE**
> AngleCnecs currently cannot be optimised by the RAO, but they are monitored by an independent
> [AngleMonitoring](/docs/engine/monitoring/angle-monitoring) module.

#### Creating an AngleCnec
In FARAO, AngleCnecs can be created by the java API, or written in the json CRAC internal format, as shown below:

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
 cnec1 = crac.newAngleCnec()
  .withId("angleCnecId1")
  .withName("angleCnecName1")
  .withInstant("outage")
  .withContingency(contingency1Id)
  .withOperator("cnec1Operator")
  .withExportingNetworkElement("eneId1", "eneName1")
  .withImportingNetworkElement("ineId1", "ineName1")
  .newThreshold()
    .withUnit(Unit.DEGREE)
    .withMax(1000.0)
    .withMin(-1000.0)
    .add()
  .withMonitored()
  .add();

cnec2 = crac.newAngleCnec()
  .withId("angleCnecId2")
  .withInstant("preventive")
  .withOperator("cnec2Operator")
  .withExportingNetworkElement("eneId2")
  .withImportingNetworkElement("ineId2")
  .withReliabilityMargin(5.0)
  .newThreshold()
    .withUnit(Unit.DEGREE)
    .withMax(500.0)
    .add()
  .withMonitored()
  .add();
~~~
:::
:::{group-tab} JSON file
~~~json
    "angleCnecs" : [ {
    "id" : "angleCnecId1",
    "name" : "angleCnecName1",
    "exportingNetworkElementId" : "eneId1",
    "importingNetworkElementId" : "ineId1",
    "operator" : "cnec1Operator",
    "instant" : "outage",
    "contingencyId" : "contingency1Id",
    "optimized" : false,
    "monitored" : true,
    "reliabilityMargin" : 0.0,
    "thresholds" : [ {
      "unit" : "degree",
      "min" : -1000.0,
      "max" : 1000.0
    } ]
  } ],
    "angleCnecs" : [ {
    "id" : "angleCnecId2",
    "exportingNetworkElementId" : "eneId2",
    "importingNetworkElementId" : "ineId2",
    "operator" : "cnec2Operator",
    "instant" : "preventive",
    "optimized" : false,
    "monitored" : true,
    "reliabilityMargin" : 5.0,
    "thresholds" : [ {
      "unit" : "degree",
      "max" : 500.0
    } ]
  } ]
~~~
:::
:::{group-tab} Object fields
🔴⭐ **identifier**  
⚪ **name**  
🔴 **importing network element**: one network element  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **network element id**: must be the id of a PowSyBl network identifiable  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ⚪ **network element name**  
🔴 **exporting network element**: one network element  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **network element id**: must be the id of a PowSyBl network identifiable  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ⚪ **network element name**  
🔴 **instant**  
🔵 **contingency**: mandatory, except if the instant is preventive. Must be the id of a contingency which exists in the CRAC  
⚪ **operator**  
⚪ **reliability margin**: default value = 0 °  
⚪ **optimized**: default value = false  
⚪ **monitored**: default value = false  
🔴 **thresholds**: list of 1 to N thresholds, an AngleCnec must contain at least one threshold  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **unit**  : must be in degrees  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔵 **minValue**  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔵 **maxValue**: at least one of these two values (min/max) is required   
:::
::::

### Voltage CNECs
A "VoltageCnec" is a CNEC on which we monitor the voltage on substations. It has the following specificities:
- it contains one network element (a [VoltageLevel](https://www.powsybl.org/pages/documentation/grid/model/#voltage-level))
- the physical parameter which is monitored by the CNEC is the **voltage**.
- it must contain at least one threshold, defined in kilovolts. A threshold has a minimum and/or a maximum value.

> 💡  **NOTE**
> VoltageCnecs currently cannot be optimised by the RAO, but they are monitored by an independent
> [VoltageMonitoring](/docs/engine/monitoring/voltage-monitoring) module.

#### Creating a VoltageCnec
In FARAO, VoltageCnecs can be created by the java API, or written in the json CRAC internal format, as shown below:

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
crac.newVoltageCnec()
    .withId("voltageCnecId1")
    .withName("voltageCnecName1")
    .withInstant("outage")
    .withContingency(contingency1Id)
    .withOperator("cnec1Operator")
    .withNetworkElement("neId1", "neName1")
    .newThreshold()
      .withUnit(Unit.KILOVOLT)
      .withMax(420.0)
      .withMin(360.0)
      .add()
    .withMonitored()
    .add();
crac.newVoltageCnec()
    .withId("voltageCnecId2")
    .withInstant("preventive")
    .withOperator("cnec2Operator")
    .withNetworkElement("neId2")
    .newThreshold()
      .withUnit(Unit.KILOVOLT)
      .withMax(440.0)
      .add()
    .withMonitored()
    .add();
~~~
:::
:::{group-tab} JSON file
~~~json
    "voltageCnecs" : [ {
    "id" : "voltageCnecId1",
    "name" : "voltageCnecName1",
    "networkElementId" : "neId1",
    "operator" : "cnec1Operator",
    "instant" : "outage",
    "contingencyId" : "contingency1Id",
    "optimized" : false,
    "monitored" : true,
    "reliabilityMargin" : 0.0,
    "thresholds" : [ {
      "unit" : "kilovolt",
      "min" : 360.0,
      "max" : 420.0
    } ]
  } ],
    "voltageCnecs" : [ {
    "id" : "voltageCnecId2",
    "networkElementId" : "neId2",
    "operator" : "cnec2Operator",
    "instant" : "preventive",
    "optimized" : false,
    "monitored" : true,
    "reliabilityMargin" : 0.0,
    "thresholds" : [ {
      "unit" : "kilovolt",
      "max" : 440.0
    } ]
  } ]
~~~
:::
:::{group-tab} Object fields
🔴⭐ **identifier**  
⚪ **name**  
🔴 **network element**: one network element  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **network element id**: must be the id of a PowSyBl network identifiable  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ⚪ **network element name**  
🔴 **instant**  
🔵 **contingency**: mandatory, except if the instant is preventive. Must be the id of a contingency which exists in the CRAC  
⚪ **operator**  
⚪ **reliability margin**: default value = 0 kV  
⚪ **optimized**: default value = false  
⚪ **monitored**: default value = false  
🔴 **thresholds**: list of 1 to N thresholds, a VoltageCnec must contain at least one threshold  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **unit**  : must be in kilovolts  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔵 **minValue**  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔵 **maxValue**: at least one of these two values (min/max) is required   
:::
::::