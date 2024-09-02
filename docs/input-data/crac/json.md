---
title: Internal json CRAC format
---

# Internal json CRAC format

## Introduction

The name CRAC is a standard denomination defined by the ENTSO-E which means: **C**ontingency list, **R**emedial 
**A**ctions, and additional **C**onstraints.

In other words, it gathers the following information:
- critical outages,
- critical network elements,
- and remedial actions.

It is typically used in European coordinated processes. It enables, for a given geographical region, to define the 
network elements that might be critical after specific outages, and the remedial actions that might help to manage them.  

**A CRAC object model has been designed in OpenRAO** in order to store all the aforementioned information. This page aims to present:
- the content and the organization of the data present in the OpenRAO CRAC object model,
- how a OpenRAO CRAC object can be built,
  - using the java API,
  - or using the OpenRAO internal Json CRAC format.

Note that other pages of this documentation describe how the OpenRAO CRAC object model can be built with other standard 
CRAC formats, such as the [FlowBasedConstraint](fbconstraint) format, the [CSE](cse) Format, and the [CIM](cim) format.

## Full CRAC examples
Example of complete CRACs are given below

::::{tabs}
:::{group-tab} JAVA creation API
The creation of a small CRAC is for instance made in this test class of powsybl-open-rao repository:  
[example on GitHub](https://github.com/powsybl/powsybl-open-rao/blob/main/data/crac/crac-impl/src/test/java/com/powsybl/openrao/data/cracimpl/utils/CommonCracCreation.java)
:::
:::{group-tab} JSON file
An example of a small CRAC in the json internal format of OpenRAO is given below:  
[example on GitHub](https://github.com/powsybl/powsybl-open-rao/blob/main/ra-optimisation/search-tree-rao/src/test/resources/crac/small-crac-with-network-actions.json)
:::
::::
  
The following paragraphs of this page explain, step by step, the content of these examples.

> **KEY**  
> 🔴 marks a **mandatory** field  
> ⚪ marks an **optional** field  
> 🔵 marks a field that can be **mandatory in some cases**  
> ⭐ marks a field that must be **unique** in the CRAC  

## Network elements 
OpenRAO relies on the [PowSyBl framework](https://www.powsybl.org/), and OpenRAO's CRAC relies on some elements of
[PowSyBl's network model](https://www.powsybl.org/pages/documentation/grid/model/): the so-called network elements.

The network elements can be:
- the elements that are disconnected from the network by a contingency,
- the power lines that are identified as critical network elements in the CRAC,
- the PSTs that are identified by the CRAC as remedial actions,
- the switches that can be used as topological remedial actions...

Network elements are referenced in the CRAC with:

🔴⭐ an id
: The id **must** match the [unique id of a PowSyBl object](https://www.powsybl.org/pages/documentation/grid/model/#introduction).
When using a network element, the applications relying on the CRAC will look in the Network for an identifiable element
with the very same id: if this element cannot be found, it can lead to an error in the application. When building the CRAC,
one must therefore make sure that only the network elements whose IDs can be understood by the PowSyBl iidm Network are
added to the CRAC. This is particularly important to keep in mind if you want to [develop your own CRAC importer](import.md#implementing-new-crac-formats).

⚪ a name
: The name shouldn't be absolutely required by the application relying on the CRAC object, but it could be useful to
make the CRAC or some outputs of the application more readable for humans.

::::{tabs}
:::{group-tab} JAVA creation API
Network elements are never built on their own in the CRAC object: they are always a component of a larger business object,
and are added implicitly to the CRAC when the business object is added (e.g. a CNEC, or a remedial action).
When building a business object, one of the two following methods can be used to add a network element to it (using an
ID only, or an ID and a name).
~~~java
(...).withNetworkElement("network_element_id")
(...).withNetworkElement("network_element_id", "network_element_name")
~~~
These methods will be depicted in the following examples on this page.
:::
:::{group-tab} JSON file
Network elements' IDs are defined within the objects that contain them. Examples are given later in this page,
for contingencies, CNECs and remedial actions. Moreover, the internal Json CRAC format contains an index of network
element names, in which network elements that have a name are listed, with their name and their ID.
~~~json
"networkElementsNamePerId" : {
  "network_element_id_1" : "[BE-FR] Interconnection between Belgium and France",
  "network_element_id_4" : "[DE-DE] Internal PST in Germany"
},
~~~
:::
::::

## Contingencies
A CRAC object must define "critical contingencies" (or "critical outages", or "CO", or "N-k"...).  
The denomination chosen within the OpenRAO internal format is **"Contingency"**.

A contingency is the representation of an incident on the network (i.e. a cut line or a group/transformer failure, etc.).
In OpenRAO, it is modelled by the loss of one or several network elements. Usually we have either a one-network-element-loss
called "N-1", or a two-network-element-loss called "N-2".

Examples:
- N-1: The loss of one generator
- N-2: The loss of two parallel power lines

A contingency is a probable event that can put the grid at risk. Therefore, contingencies must
be considered when operating the electrical transmission / distribution system.

In OpenRAO, contingencies come from PowSyBl Contingency, where a contingency element has the id of the impacted network element, and its type is linked to the network elements type.
They are represented in the following way:

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
crac.newContingency()
    .withId("CO_0001")
    .withName("N-1 on generator")
    .withContingencyElement("powsybl_generator_id", ContingencyElementType.GENERATOR)
    .add();

crac.newContingency()
    .withId("CO_0002")
    .withName("N-2 on electrical lines")
    .withContingencyElement("powsybl_electrical_line_1_id", ContingencyElementType.LINE)
    .withContingencyElement("powsybl_electrical_line_2_id", ContingencyElementType.LINE)
    .add();
~~~
:::
:::{group-tab} JSON file
~~~json
"contingencies" : [{
    "id" : "CO_0001",
    "name" : "N-1 on generator",
    "networkElementsIds" : [ "powsybl_generator_id" ]
}, {
    "id" : "CO_0002",
    "name" : "N-1 electrical lines",
    "networkElementsIds" : [ "powsybl_electrical_line_1_id", "powsybl_electrical_line_2_id" ]
}],
~~~
:::
:::{group-tab} Object fields
🔴⭐ **identifier**  
⚪ **name**  
⚪ **contingency elements**: list of 0 to N contingency elements  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **contingency element id**: must be the id of a PowSyBl network identifiable  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **contingency element type**: type of element in the network. Currently, PowSyBl handles: 
GENERATOR, STATIC_VAR_COMPENSATOR, SHUNT_COMPENSATOR, BRANCH, HVDC_LINE, BUSBAR_SECTION, DANGLING_LINE, LINE, TWO_WINDINGS_TRANSFORMER, 
THREE_WINDINGS_TRANSFORMER, LOAD, SWITCH, BATTERY, BUS, TIE_LINE. The contingency elements type can be retrieved from the PowSyBl Network using the network element id, using: 
`ContingencyElement.of(network.getIdentifiable(id)).getType()`.  
:::
::::

## Instants and States
The instant is a moment in the chronology of a contingency event. Four instants kinds currently exist in OpenRAO:
- the **preventive** instant kind occurs before any contingency, and describes the "base-case" situation. A CRAC may
  contain only one instant of kind preventive.
- the **outage** instant kind occurs just after a contingency happens, in a time too short to allow the activation of any
  curative remedial action. A CRAC may contain only one instant of kind outage.
- the **auto** instant kind occurs after a contingency happens, and spans through the activation of automatic curative
  remedial actions ("automatons") that are triggered without any human intervention. These automatons are pre-configured
  to reduce some constraints, even though they can generate constraints elsewhere in the network. A CRAC may contain any
  number of instants of kind auto.
- the **curative** instant kind occurs after a contingency happens, after enough time that would allow the human activation
  of curative remedial actions. A CRAC may contain any number of instants of kind auto.

> 💡  **NOTE**  
> Flow / angle / voltage limits on critical network elements are usually different for each instant.  
> The outage and auto instant kinds are transitory, therefore less restrictive temporary limits (TATL) can be allowed in
> these instants.  
> On the contrary, the preventive and curative instant kinds are supposed to be a lasting moment during which the grid
> operation is nominal (sometimes thanks to preventive and/or curative remedial actions), so they usually come with
> more restrictive permanent limits (PATL).  
> OpenRAO allows a different limit setting for different instants on critical network elements (see [CNECs](#cnecs)).
>
> ![patl-vs-tatl](/_static/img/patl-tatl.png){.forced-white-background}
> (**PRA** = Preventive Remedial Action,
> **ARA** = Automatic Remedial Action,
> **CRA** = Curative Remedial Action)

The OpenRAO object model includes the notion of "state". A state is either:

- the preventive state: the state of the base-case network, without any contingency, at the preventive instant.
- the combination of a given contingency with instant outage, auto or curative: the state of the network after the said
  contingency, at the given instant (= with more or less delay after this contingency).

The scheme below illustrates these notions of instant and state. It highlights the combinations of the situations which can be described in a CRAC, with a base-case situation, but also variants of this situation occurring at different moments in time after different probable and hypothetical contingencies.

![Instants & states](/_static/img/States_AUTO.png)

States are not directly added to a OpenRAO CRAC object model; they are implicitly created by business objects
that are described in the following paragraphs ([CNECs](#cnecs) and [remedial actions](#remedial-actions-and-usages-rules)).

Instants are added one after the other in the CRAC object.
The first instant must be of kind preventive.
The second instant must be of kind outage.

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
crac.newInstant("preventive", InstantKind.PREVENTIVE)
    .newInstant("outage", InstantKind.OUTAGE)
    .newInstant("auto", InstantKind.AUTO)
    .newInstant("curative1", InstantKind.CURATIVE)
    .newInstant("curative2", InstantKind.CURATIVE);
~~~
:::
:::{group-tab} JSON file
~~~json
  "instants" : [ {
    "id": "preventive",
    "kind": "PREVENTIVE"
  }, {
    "id": "outage",
    "kind": "OUTAGE"
  }, {
    "id": "auto",
    "kind": "AUTO"
  }, {
  "id": "curative1",
  "kind": "CURATIVE"
  }, {
    "id": "curative2",
    "kind": "CURATIVE"
  } ],
~~~
:::
::::


## CNECs
A CRAC should define CNECs. A CNEC is a "**C**ritical **N**etwork **E**lement and **C**ontingency" (also known as "CBCO"
or "Critical Branch and Critical Outage").

A CNEC is a **network element**, which is considered at a given **instant**, after a given **contingency** (i.e. at a
given OpenRAO ["state"](#instants-and-states)).  
The contingency is omitted if the CNEC is defined at the preventive instant.

> 💡  **NOTE**  
> A OpenRAO CNEC is associated to one instant and one contingency only. This is not the case for all native CRAC formats:
> for instance, in the [CORE merged-CB CRAC format](fbconstraint), the post-outage CNECs are implicitly defined for the
> two instants outage and curative.
>
> However, we are talking here about the internal OpenRAO CRAC format, which has its own independent conventions, and which
> is imported from native CRAC formats using [CRAC importers](import).

A CNEC has an operator, i.e. the identifier of the TSO operating its network element.
It may also have a border, which can be useful on some processes to know where the line is located and so which remedial actions could be useful for it.
Moreover, a CNEC can have a reliability margin: a safety buffer to cope with unplanned events or uncertainties of input
data (i.e. an extra margin).

### Optimised and monitored CNECs
CNECs can be monitored and/or optimised. This notion of monitored/optimised has been introduced by the capacity
calculation on the CORE region, and is now also used for the CSE region:
- maximise the margins of CNECs that are "optimised"
- ensure that the margins of "monitored" CNECs are positive and/or are not decreased by the RAO.

OpenRAO contains 3 families of CNECs, depending on which type of physical constraints they have: **FlowCnecs**,
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
one/left. The convention of OpenRAO is that a positive flow is a flow in the "direct" direction, while a negative flow is
a flow in the "opposite" direction.

> 💡  **NOTE**  
> A OpenRAO FlowCnec is one implementation of the generic ["BranchCnec"](https://github.com/powsybl/powsybl-open-rao/blob/main/data/crac/crac-api/src/main/java/com/powsybl/openrao/data/cracapi/cnec/BranchCnec.java).
> If needed, this would allow you a fast implementation of other types of CNECs, on branches, but with a monitored
> physical parameter other than power flow.

#### Flow limits on a FlowCnec
A FlowCnec has flow limits, called "thresholds" in OpenRAO. These thresholds define the limits between which the power
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
      >   different (due to losses). The CracCreationParameters allows the user to [decide which side(s) should be monitored by default](creation-parameters.md#default-monitored-line-side).
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
In OpenRAO, FlowCnecs can be created by the java API, or written in the json CRAC internal format, as shown below:

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
crac.newFlowCnec()
    .withId("preventive-cnec-with-one-threshold-id")
    .withNetworkElement("network-element-id")
    .withInstant("preventive")
    .withOperator("operator1")
    .withBorder("border")
    .withReliabilityMargin(50.)
    .withOptimized(true)
    .newThreshold()
      .withUnit(Unit.MEGAWATT)
      .withSide(TwoSides.ONE)
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
    .withBorder("border")
    .newThreshold()
      .withUnit(Unit.PERCENT_IMAX)
      .withSide(TwoSides.TWO)
      .withMax(0.95)
      .add()
    .newThreshold()
      .withUnit(Unit.AMPERE)
      .withSide(TwoSides.ONE)
      .withMin(-450.)
      .add()
    .withReliabilityMargin(50.)
    .withOptimized(true)
    .withMonitored(false)
    .withNominalVoltage(380., TwoSides.ONE)
    .withNominalVoltage(220., TwoSides.TWO)
    .withIMax(500.) // this means that the value is the same on both sides, but the side could have been specified using "withImax(500., TwoSides.TWO)" instead 
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
  "border" : "border",
  "instant" : "preventive",
  "optimized" : true,
  "monitored" : false,
  "frm" : 50.0,
  "thresholds" : [ {
    "unit" : "megawatt",
    "min" : -1500.0,
    "max" : 1500.0,
    "side" : 1
  } ]
},  {
  "id" : "curative-cnec-with-two-thresholds-id",
  "name" : "curative-cnec-with-two-thresholds-name",
  "networkElementId" : "network-element-id",
  "operator" : "operator1",
  "border" : "border",
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
    "side" : 1
  }, {
    "unit" : "percent_imax",
    "max" : 0.95,
    "side" : 2
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
⚪ **border**: default value = ""  
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
When a FlowCnec carries a LoopFlowThreshold extension (and if [loop-flow constraints are enabled in the RAO](/parameters.md#loop-flow-extension)),
its loop-flow is monitored by the RAO, that will keep it [under its threshold](/castor/special-features/loop-flows.md)
when optimising remedial actions.  
The loop-flow extension defines the loop-flow threshold to be respected by the RAO (even though the initial loop-flow
value on this CNEC may override this user-defined thrshold, as explained [here](/castor/linear-problem/max-loop-flow-filler.md)).

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
  "extensions" : {
    "LoopFlowThreshold" : {
      "inputThreshold" : 150.0,
      "inputThresholdUnit" : "megawatt"
    }
  }
}, {
  "id" : "cnec-with-pimax-loop-flow-extension",
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

In terms of OpenRAO object model, an AngleCnec is a CNEC. Even though it is associated with a branch, it is not a
BranchCnec, because we cannot define on which side it is monitored: it is monitored on both sides (more specifically,
we monitor the phase shift between the two sides).

An AngleCnec has the following specificities:

- it contains two network elements, an importing node and an exporting node, that represent the importing and exporting ends of the branch.
- the physical parameter which is monitored by the CNEC is the **angle**.
- it must contain at least one threshold, defined in degrees. A threshold has a minimum and/or a maximum value.

> 💡  **NOTE**
> AngleCnecs currently cannot be optimised by the RAO, but they are monitored by an independent
> [AngleMonitoring](/castor/monitoring/angle-monitoring.md) module.

#### Creating an AngleCnec
In OpenRAO, AngleCnecs can be created by the java API, or written in the json CRAC internal format, as shown below:

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
 cnec1 = crac.newAngleCnec()
  .withId("angleCnecId1")
  .withName("angleCnecName1")
  .withInstant("outage")
  .withContingency(contingency1Id)
  .withOperator("cnec1Operator")
  .withBorder("border1")
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
  .withBorder("border2")
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
    "border" : "border1",
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
    "border" : "border2",
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
⚪ **border**: default value = ""  
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
> [VoltageMonitoring](/castor/monitoring/voltage-monitoring.md) module.

#### Creating a VoltageCnec
In OpenRAO, VoltageCnecs can be created by the java API, or written in the json CRAC internal format, as shown below:

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
crac.newVoltageCnec()
    .withId("voltageCnecId1")
    .withName("voltageCnecName1")
    .withInstant("outage")
    .withContingency(contingency1Id)
    .withOperator("cnec1Operator")
    .withBorder("border1")
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
    .withBorder("border2")
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
    "border" : "border1",
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
    "border" : "border2",
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
⚪ **border**: default value = ""   
⚪ **reliability margin**: default value = 0 kV  
⚪ **optimized**: default value = false  
⚪ **monitored**: default value = false  
🔴 **thresholds**: list of 1 to N thresholds, a VoltageCnec must contain at least one threshold  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **unit**  : must be in kilovolts  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔵 **minValue**  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔵 **maxValue**: at least one of these two values (min/max) is required   
:::
::::

## Remedial actions and usages rules
A remedial action is an action on the network that is considered capable of reducing constraints on the CNECs.

Two types of remedial action exist in OpenRAO:
- **Network Actions**: they have the specificity of being binary. A Network Action is either applied on the network, or
  not applied. Topological actions are a typical example of Network Actions.
- **Range Actions**: they have the specificity of having a degree of freedom, a set-point. When a Range Action is
  activated, it is activated at a given value of its set-point. PSTs are a typical example of Range Actions.

Both Network Actions and Range Actions have usage rules which define the conditions under which they can be activated.  
A usage rule contains a usage method which can be activated under certain conditions. The usage methods in OpenRAO are: 
- **UNDEFINED**: this means that the usage rule cannot decide if the remedial action can be used.
- **AVAILABLE**: when activated, this means that the remedial action is available for the RAO to assess. The RAO can 
  choose to activate it if it finds that it is optimal. Takes precedence over the above usage method.
- **FORCED**: when activated, this means that the remedial action must be applied by the RAO (usually used for automatons).
  Takes precedence over the above usage methods.
- **UNAVAILABLE**: when activated, this means that the remedial action cannot be used. Takes precedence over the above usage methods.  

OpenRAO has the following usage rules with their activation conditions:
- the **FreeToUse** usage rule (defined for a specific [instant](#instants-and-states)): the usage method is activated in all
  the states of a given instant.
- the **OnState** usage rule (defined for a specific [state](#instants-and-states)): the usage method is activated in a given state.
- the **OnFlowConstraintInCountry** usage rule (defined for a specific [Country](https://github.com/powsybl/powsybl-core/blob/main/iidm/iidm-api/src/main/java/com/powsybl/iidm/network/Country.java), a specific [instant](#instants-and-states)), 
  and an optional [contingency](#contingencies): the usage method is activated if any FlowCnec in the given country is
  constrained (ie has a flow greater than one of its thresholds) at the given instant. If a contingency is defined, then 
  only constraints on FlowCnecs with the same contingency count.  
- the **OnConstraint** usage rule (defined for a specific [instant](#instants-and-states) and a specific [Cnec](#cnecs)):
  the usage method is activated if the given Cnec is constrained at the given instant.

A remedial action has an operator, which is the name of the TSO which operates the remedial action.

::::{tabs}
:::{group-tab} JAVA creation API
The examples below are given for a Network Action, but the same methods exists for Range Actions.  
Complete examples of Network and Range Action creation are given in the following paragraphs.
~~~java
crac.newNetworkAction()
    .newFreeToUseUsageRule()
        .withUsageMethod(UsageMethod.AVAILABLE)
        .withInstant("preventive")
        .add();

crac.newNetworkAction()
    .newOnStateUsageRule()
        .withUsageMethod(UsageMethod.AVAILABLE)
        .withInstant("curative")
        .withContingency("contingency-id")
        .add();

crac.newNetworkAction()
    .newOnConstraintUsageRule()
        .withInstant("auto")
        .withCnec("flow-cnec-id")
        .add();

crac.newNetworkAction()
    .newOnFlowConstraintInCountryUsageRule()
        .withInstant("preventive")
        .withCountry(Country.FR)
        .withContingency("contingency-id")
        .add();

crac.newNetworkAction()
    .newOnConstraintUsageRule()
        .withInstant("curative")
        .withCnec("angle-cnec-id")
        .add();

crac.newNetworkAction()
    .newOnConstraintUsageRule()
        .withInstant("curative")
        .withCnec("voltage-cnec-id")
        .add();
~~~
:::
:::{group-tab} JSON file
Complete examples of Network and Range Action in Json format are given in the following paragraphs
~~~json
"onInstantUsageRules" : [ {
  "instant" : "preventive",
  "usageMethod" : "available"
} ],
"onContingencyStateUsageRules" : [ {
  "instant" : "curative",
  "contingencyId" : "contingency-id",
  "usageMethod" : "available"
} ],
"onConstraintUsageRules" : [ {
    "instant" : "auto",
    "flowCnecId" : "flow-cnec-id"
}, {
    "instant" : "curative",
    "angleCnecId" : "angle-cnec-id"
}, {
    "instant" : "curative",
    "voltageCnecId" : "voltage-cnec-id"
} ],
"onFlowConstraintInCountryUsageRules" : [ {
    "instant" : "preventive",
    "contingencyId" : "contingency-id",
    "country" : "FR"
} ]
~~~
:::
:::{group-tab} Object fields
<ins>**For FreeToUse usage rules**</ins>  
🔴 **instant**  
🔴 **usageMethod**  
<ins>**For OnState usage rules**</ins>  
🔴 **instant**  
🔴 **usageMethod**  
🔴 **contingency**: must be the id of a contingency that exists in the CRAC  
<ins>**For OnFlowConstraintInCountry usage rules**</ins>  
🔴 **instant**  
🔵 **contingency**: must be the id of a contingency that exists in the CRAC  
🔴 **country**: must be the [alpha-2 code of a country](https://github.com/powsybl/powsybl-core/blob/main/iidm/iidm-api/src/main/java/com/powsybl/iidm/network/Country.java)  
<ins>**For OnConstraint usage rules**</ins>  
🔴 **instant**  
🔴 **cnecId**: must be the id of a [Cnec](#cnecs) that exists in the CRAC  
<ins>**Usage methods**</ins>  
OpenRAO handles three different types of usage methods sorted by priority:
1- **UNAVAILABLE**: the remedial action can not be considered by the RAO.
2 - **FORCED**: For automaton instant, the RAO must activate the remedial action under the condition described by the usage rule. For other instants, it will be ignored.
3 - **AVAILABLE**: For automaton instant, the remedial action is ignored. Otherwise, it can be chosen by the RAO under the condition described by the usage rule.

*NB*: even though OnState usage rules on the preventive state is theoretically possible, it is forbidden by OpenRAO as the same purpose can be achieved with a FreeToUse usage rule on the preventive instant.  
:::
::::

## Network Actions
A OpenRAO "Network Action" is a remedial action with a binary state: it is either active or inactive.  
One network action is a combination of one or multiple "elementary actions", among the following:
- Terminals connection action: opening or closing all terminals of a connectable in the network.
- Switch action: opening or closing a switch in the network.
- Phase tap changer tap position action: setting the tap of a PST in the network to a specific position.
- Generator action: setting the active power of a generator in the network to a specific value.
- Load action: setting the active power of a load in the network to a specific value.
- Dangling line action: setting the active power of a [dangling line](https://www.powsybl.org/pages/documentation/grid/model/#dangling-line)) in the network to a specific value.
- Shunt compensator position action: setting the number of sections of a shunt compensator to a specific value.
- Switch pairs: opening a switch in the network and closing another (actually used to model [CSE bus-bar change remedial actions](cse.md#bus-bar-change)).

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
// combination of two switch actions
crac.newNetworkAction()
	.withId("topological-na-id")
    .withName("topological-na-name")
    .withOperator("operator")
    .newSwitchAction()
		.withNetworkElement("switch-id-1")
		.withActionType(ActionType.CLOSE)
		.add()
    .newSwitchAction()
		.withNetworkElement("switch-id-2")
		.withActionType(ActionType.OPEN)
		.add()
    .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT).add()
    .add();

// terminals connection action
crac.newNetworkAction()
	.withId("terminals-connection-na-id")
    .withName("terminals-connection-na-name")
    .withOperator("operator")
    .newTerminalsConnectionAction()
		.withNetworkElement("transformer-id")
		.withActionType(ActionType.CLOSE)
		.add()
    .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT).add()
    .add();

// phase tap chnager tap position action
crac.newNetworkAction()
	.withId("pst-setpoint-na-id")
    .withName("pst-setpoint-na-name")
    .withOperator("operator")
    .newPhaseTapChangerTapPositionAction()
		.withNormalizedSetpoint(15)
		.withNetworkElement("pst-id")
		.add()
    .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT).add()
    .add();

// generator action with two usage rules
crac.newNetworkAction()
	.withId("generator-action-na-id")
	.withOperator("operator")
	.newGeneratorAction()
		.withActivePowerValue(260.0)
		.withNetworkElement("generator-id")
		.add()
    .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT).add()
    .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency("contingency-id").withInstant(Instant.CURATIVE).add()
    .add();
	
// load action
crac.newNetworkAction()
	.withId("load-action-na-id")
	.withOperator("operator")
	.newLoadAction()
		.withActivePowerValue(260.0)
		.withNetworkElement("load-id")
		.add()
    .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT).add()
    .add();
	
// dangling line action
crac.newNetworkAction()
	.withId("dangling-line-na-id")
	.withOperator("operator")
	.newDanglingLineAction()
		.withActivePowerValue(260.0)
		.withNetworkElement("dangling-line-id")
		.add()
    .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT).add()
    .add();

// shunt compensator position action
crac.newNetworkAction()
    .withId("shunt-compensator-na-id")
    .withOperator("operator")
    .newShuntCompensatorPositionAction()
        .withSetpoint(3)
        .withNetworkElement("shunt-compensator-id")
        .add()
    .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT).add()
    .add();

// switch pair
crac.newNetworkAction()
	.withId("switch-pair-na-id")
	.withOperator("operator")
	.newSwitchPair()
		.withSwitchToOpen("switch-to-open-id", "switch-to-open-name")
		.withSwitchToClose("switch-to-close-id-and-name")
		.add()
    .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT).add()
    .add();
~~~
:::
:::{group-tab} JSON file
~~~json
 "networkActions" : [ {
    "id" : "switch-na-id",
    "name" : "switch-na-name",
    "operator" : "operator",
    "onInstantUsageRules" : [ {
      "instant" : "preventive",
      "usageMethod" : "available"
    } ],
    "switchActions" : [ {
      "networkElementId" : "switch-id-1",
      "actionType" : "close"
    }, {
      "networkElementId" : "switch-id-2",
      "actionType" : "open"
    } ]
  }, {
    "id" : "terminals-connection-na-id",
    "name" : "terminals-connection-na-name",
    "operator" : "operator",
    "freeToUseUsageRules" : [ {
      "instant" : "preventive",
      "usageMethod" : "available"
    } ],
    "terminalsConnectionActions" : [ {
      "networkElementId" : "transformer-id",
      "actionType" : "close"
    } ]
  }, {
    "id" : "phase-tap-postion-na-id",
    "name" : "phase-tap-postion-na-name",
    "operator" : "operator",
    "onInstantUsageRules" : [ {
      "instant" : "preventive",
      "usageMethod" : "available"
    } ],
    "phaseTapChangerTapPositionActions" : [ {
      "networkElementId" : "pst-id",
      "tapPosition" : 15
    } ]
  }, {
    "id" : "generator-action-na-id",
    "name" : "generator-action-na-id",
    "operator" : "operator",
    "onInstantUsageRules" : [ {
      "instant" : "preventive",
      "usageMethod" : "available"
    } ],
    "onContingencyStateUsageRules" : [ {
      "instant" : "curative",
      "contingencyId" : "contingency-id",
      "usageMethod" : "available"
    } ],
    "generatorActions" : [ {
      "networkElementId" : "generator-id",
      "activePowerValue" : 260.0
    } ]
  }, {
    "id" : "load-action-na-id",
    "name" : "load-action-na-id",
    "operator" : "operator",
    "freeToUseUsageRules" : [ {
      "instant" : "preventive",
      "usageMethod" : "available"
    } ],
    "loadActions" : [ {
      "networkElementId" : "load-id",
      "activePowerValue" : 260.0
    } ]
  }, {
    "id" : "dangling-line-action-na-id",
    "name" : "dangling-line-action-na-id",
    "operator" : "operator",
    "freeToUseUsageRules" : [ {
      "instant" : "preventive",
      "usageMethod" : "available"
    } ],
    "danglingLineActions" : [ {
      "networkElementId" : "dangling-line-id",
      "activePowerValue" : 260.0
    } ]
  }, {
    "id" : "shunt-compensator-na-id",
    "name" : "shunt-compensator-na-id",
    "operator" : "operator",
    "freeToUseUsageRules" : [ {
      "instant" : "preventive",
      "usageMethod" : "available"
    } ],
    "shuntCompensatorPositionActions" : [ {
      "networkElementId" : "shunt-compensator-id",
      "sectionCount" : 3
    } ]
  }, {
    "id" : "switch-pair-na-id",
    "name" : "switch-pair-na-id",
    "operator" : "operator",
    "onInstantUsageRules" : [ {
      "instant" : "preventive",
      "usageMethod" : "available"
    } ],
    "switchPairs" : [ {
      "open" : "switch-to-open-id",
      "close" : "switch-to-close-id"
    } ]
  } ]
~~~
:::
:::{group-tab} Object fields
🔴⭐ **identifier**  
⚪ **name**  
⚪ **operator**  
⚪ **freeToUse usage rules**: list of 0 to N FreeToUse usage rules (see previous paragraph on [usage rules](#remedial-actions-and-usages-rules))  
⚪ **onState usage rules**: list of 0 to N OnState usage rules (see previous paragraph on [usage rules](#remedial-actions-and-usages-rules))  
⚪ **onFlowConstraintInCountry usage rules**: list of 0 to N OnFlowConstraintInCountry usage rules (see previous paragraph on [usage rules](#remedial-actions-and-usages-rules))  
⚪ **onConstraint usage rules**: list of 0 to N OnConstraint usage rules (see previous paragraph on [usage rules](#remedial-actions-and-usages-rules))  
🔵 **terminals connection actions**: list of 0 to N TerminalsConnectionAction
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **network element**: id is mandatory, name is optional  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **action type**  
🔵 **switch actions**: list of 0 to N SwitchAction  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **network element**: id is mandatory, name is optional  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **action type**  
🔵 **phase tap changer tap position**: list of 0 to N PhaseTapChangerTapPositionAction  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **network element**: id is mandatory, name is optional  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **tap position**: integer, new tap of the PST  
🔵 **generator actions**: list of 0 to N GeneratorAction  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **network element**: id is mandatory, name is optional  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **active power value**: double, new value of the active power  
🔵 **load actions**: list of 0 to N LoadAction  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **network element**: id is mandatory, name is optional  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **active power value**: double, new value of the active power  
🔵 **dangling line action**: list of 0 to N DanglingLineAction  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **network element**: id is mandatory, name is optional  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **active power value**: double, new value of the active power  
🔵 **shunt compensator position action**: list of 0 to N ShuntCompensatorPositionAction  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **network element**: id is mandatory, name is optional  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **section count**: integer, new value of the section count  
🔵 **switch pairs**: list of 0 to N SwitchPair  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **switch to open (network element)**: id is mandatory, name is optional  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **switch to close (network element)**: id is mandatory, name is optional, must be different from switch to open  
<br>
*NB*: A Network Action must contain at least on elementary action.
:::
::::

## Range Actions
A OpenRAO "Range Action" is a remedial action with a continuous or discrete set-point. If the range action is inactive, its
set-point is equal to its value in the initial network. If it is activated, its set-point is optimized by the RAO to
improve the objective function.  
OpenRAO has four types of range actions : PST range actions, HVDC range actions, "injection" range actions and counter-
trading range actions.

### PST Range Action
A PstRangeAction contains a network element which must point to a [PST in the iidm PowSyBl network model](https://www.powsybl.org/pages/documentation/grid/model/#phase-tap-changer).
The PstRangeAction will be able to modify the set-point of this PST.

> 💡  **NOTE**
> - the set-point of the PstRangeAction is the angle of the PST, measured in degrees
> - all the methods of the PST which mention a set-point implicitly use angle values
> - however, they often have an equivalent that uses integer tap positions

The domain in which the PstRangeAction can modify the tap of the PST is delimited by 'TapRanges'. A PstRangeAction contains a list (which can be empty) of TapRanges.

TapRanges can be of different types:
- **absolute**: the mix/max admissible tap of the PST, given in the convention of the PowSyBl network model
- **relative to initial network**: the maximum variation of the tap of the PST relatively to its initial tap
- **relative to previous instant**: the maximum variation of the tap of the PST relatively to its tap in the previous instant. Note that this type of range does not make sense for PstRangeActions which are only available in the preventive instant, as there is no instant before the preventive one.
- **relative to previous time-step** : the maximum variation of the tap of the PST relatively to its tap during the previous time-step **(currently ignored by RAO)** 

The final validity range of the PstRangeAction is the intersection of its TapRanges, with the intersection of the min/max feasible taps of the PST.  
The PstRangeAction also requires additional data, notably to be able to interpret the TapRanges. Those additional data are: the initial tap of the PST, and a conversion map which gives for each feasible tap of the PST its corresponding angle. Utility methods have been developed in OpenRAO to ease the management of these additional data during the creation of a PstRangeAction.

Two or more [aligned PST range actions](#range-actions) must have the same (random) group ID defined. The RAO will
make sure their optimized set-points are always equal.

If the PstRangeAction is an automaton, it has to have a speed assigned. This is an integer that defines the relative
speed of this range action compared to other range-action automatons (smaller "speed" value = faster range action).
No two range-action automatons can have the same speed value, unless they are aligned.

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
crac.newPstRangeAction()
    .withId("pst-range-action-1-id")
    .withName("pst-range-action-1-name")
    .withOperator("operator")
    .withNetworkElement("pst-network-element-id")
    .withGroupId("pst-range-action-1 is aligned with pst-range-action-2")
    .withInitialTap(3)
    .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
    .newTapRange()
        .withRangeType(RangeType.ABSOLUTE)
        .withMinTap(0)
        .withMaxTap(3)
        .add()
    .newTapRange()
        .withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK)
        .withMinTap(-2)
        .withMaxTap(2)
        .add()
    .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
    .withSpeed(1)
    .add();
~~~
In that case, the validity domain of the PST (intersection of its ranges and feasible taps) is [1; 3]
Note that the [PstHelper utility class](https://github.com/powsybl/powsybl-open-rao/blob/main/data/crac-io/crac-io-util/src/main/java/com/powsybl/openrao/data/cracio/common/PstHelper.java) can ease the creation of the TapToAngleConversionMap.
:::
:::{group-tab} JSON file
~~~json
"pstRangeActions" : [ {
    "id" : "pst-range-action-1-id",
    "name" : "pst-range-action-1-name",
    "operator" : "operator",
    "onInstantUsageRules" : [ {
      "instant" : "preventive",
      "usageMethod" : "available"
    } ],
    "networkElementId" : "pst-network-element-id",
    "groupId" : "pst-range-action-1 is aligned with pst-range-action-2",
    "initialTap" : 2,
    "tapToAngleConversionMap" : {
      "-3" : 0.0,
      "-2" : 0.5,
      "-1" : 1.0,
      "0" : 1.5,
      "1" : 2.0,
      "2" : 2.5,
      "3" : 3.0
    },
    "speed" : 1,
    "ranges" : [ {
      "min" : 0,
      "max" : 3,
      "rangeType" : "absolute"
    }, {
      "min" : -2,
      "max" : 2,
      "rangeType" : "relativeToInitialNetwork"
    } ]
} ]
~~~
:::
:::{group-tab} Object fields
🔴⭐ **identifier**  
⚪ **name**  
⚪ **operator**  
🔴 **network element**: id is mandatory, name is optional  
⚪ **groupId**: if you want to align this range action with others, set the same groupId for all. You can use any
group ID you like, as long as you use the same for all the range actions you want to align.  
🔵 **speed**: mandatory if it is an automaton  
🔴 **initial tap**  
🔴 **tap to angle conversion map**  
🔴 **tap ranges**: list of 0 to N TapRange  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **range type**  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔵 **min tap**  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔵 **max tap**: at least one value must be defined  
⚪ **onInstant usage rules**: list of 0 to N OnInstant usage rules (see paragraph on [usage rules](#remedial-actions-and-usages-rules))  
⚪ **onState usage rules**: list of 0 to N OnContingencyState usage rules (see paragraph on [usage rules](#remedial-actions-and-usages-rules))  
⚪ **onFlowConstraintInCountry usage rules**: list of 0 to N OnFlowConstraintInCountry usage rules (see previous paragraph on [usage rules](#remedial-actions-and-usages-rules))  
⚪ **onConstraint usage rules**: list of 0 to N OnConstraint usage rules (see previous paragraph on [usage rules](#remedial-actions-and-usages-rules))  
:::
::::

### HVDC Range Action
An HvdcRangeAction contains a network element that must point towards an [HvdcLine of the iidm PowSyBl network model](https://www.powsybl.org/pages/documentation/grid/model/#hvdc-line).  
The HvdcRangeAction will be able to modify its active power set-point.

The domain in which the HvdcRangeAction can modify the HvdcSetpoint is delimited by 'HvdcRanges'.
An HvdcRangeAction contains a list of HvdcRanges. A range must be defined with a min and a max.

Two or more [aligned HVDC range actions](#range-actions) must have the same (random) group ID defined. The RAO will
make sure their optimized set-points are always equal.

If the HvdcRangeAction is an automaton, it has to have a speed assigned. This is an integer that defines the relative
speed of this range action compared to other range-action automatons (smaller "speed" value = faster range action).
No two range-action automatons can have the same speed value, unless they are aligned.

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
 crac.newHvdcRangeAction()
        .withId("hvdc-range-action-id")
		.withName("hvdc-range-action-name")
   		.withOperator("operator")
        .withNetworkElement("hvec-id")
        .newHvdcRange().withMin(-5).withMax(10).add()
        .newOnInstantUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
        .add();  
~~~
In that case, the validity domain of the HVDC is [-5; 10].
:::
:::{group-tab} JSON file
~~~json
"hvdcRangeActions" : [ {
    "id" : "hvdc-range-action-id",
    "name" : "hvdc-range-action-name",
    "operator" : "operator",
    "onInstantUsageRules" : [ {
      "instant" : "preventive",
      "usageMethod" : "available"
    } ],
    "networkElementId" : "hvdc-id",
    "ranges" : [ {
      "min" : -5.0,
      "max" : 10.0
    } ]
} ]
~~~
:::
:::{group-tab} Object fields
🔴⭐ **identifier**  
⚪ **name**  
⚪ **operator**  
🔴 **network element**: id is mandatory, name is optional  
⚪ **groupId**: if you want to align this range action with others, set the same groupId for all  
🔵 **speed**: mandatory if it is an automaton  
⚪ **hvdc ranges**: list of 0 to N HvdcRange  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **min**  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **max**  
⚪ **onInstant usage rules**: list of 0 to N OnInstant usage rules (see paragraph on [usage rules](#remedial-actions-and-usages-rules))  
⚪ **onContingencyState usage rules**: list of 0 to N OnContingencyState usage rules (see paragraph on [usage rules](#remedial-actions-and-usages-rules))  
⚪ **onConstraint usage rules**: list of 0 to N OnConstraint usage rules (see paragraph on [usage rules](#remedial-actions-and-usages-rules))  
:::
::::

### Injection Range Action

An InjectionRangeAction modifies given generators' & loads' injection set-points inside a given range.  
Each impacted generator or load has an associated "key", which is a coefficient of impact that is applied on its set-point.

This range action is mainly used to represent an HVDC line in an AC equivalent model (where the line is disconnected and
replaced by two injections, one on each side of the line, with opposite keys of 1 and -1).

![HVDC AC model](/_static/img/HVDC_AC_model.png){.forced-white-background}

Two or more [aligned injection range actions](#range-actions) must have the same (random) group ID defined. The RAO will
make sure their optimized set-points are always equal.

If the InjectionRangeAction is an automaton, it has to have a speed assigned. This is an integer that defines the relative
speed of this range action compared to other range-action automatons (smaller "speed" value = faster range action).
No two range-action automatons can have the same speed value, unless they are aligned.
::::{tabs}
:::{group-tab} JAVA creation API
~~~java
 crac.newInjectionRangeAction()
        .withId("injection-range-action-id")
		.withName("injection-range-action-name")
   		.withOperator("operator")
        .withNetworkElementAndKey(1, "network-element-1")
        .withNetworkElementAndKey(-0.5, "network-element-2")
        .newRange().withMin(-1200).withMax(500).add()
        .newOnInstantUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
        .add();     
~~~
In that case, the validity domain of the injection range action's reference set-point is [-1200; 500].  
This means the set-point of "network-element-1" (key = 1) can be changed between -1200 and +500, while that of "network-element-2" (key = -0.5) will be changed between -250 and +600
:::
:::{group-tab} JSON file
~~~json
"injectionRangeActions" : [ {
    "id" : "injection-range-action-id",
    "name" : "injection-range-action-name",
    "operator" : "operator",
    "onInstantUsageRules" : [ {
      "instant" : "preventive",
      "usageMethod" : "available"
    } ],
    "networkElementIdsAndKeys" : {
		"network-element-1" : 1.0,
		"network-element-2" : -0.5
	},
    "ranges" : [ {
      "min" : -1200.0,
      "max" : 500.0
    } ]
} ]
~~~
:::
:::{group-tab} Object fields
🔴⭐ **identifier**  
⚪ **name**  
⚪ **operator**  
🔴 **network element and key** (list of 1 to N): id and key are mandatory, name is optional  
⚪ **groupId**: if you want to align this range action with others, set the same groupId for all  
🔵 **speed**: mandatory if it is an automaton  
🔴 **ranges**: list of 1 to N Range  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **min**  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **max**  
⚪ **onInstant usage rules**: list of 0 to N OnInstant usage rules (see paragraph on usage rules)  
⚪ **onContingencyState usage rules**: list of 0 to N OnContingencyState usage rules (see paragraph on usage rules)  
⚪ **onConstraint usage rules**: list of 0 to N OnConstraint usage rules (see paragraph on usage rules)  
:::
::::

### Counter-Trade Range Action

A CounterTradeRangeAction is an exchange between two countries. The exporting country send power to the importing
country.

It is a costly remedial action which is currently not handled by the RAO.

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
 crac.newCounterTradeRangeAction()
        .withId("counter-trade-range-action-id")
        .withGroupId("group-id")
   		.withOperator("operator")
        .withExportingCountry(Country.FR)
        .withImportingCountry(Country.ES)
        .newRange().withMin(0).withMax(1000).add()
        .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
        .add();     
~~~
In that case, the validity domain of the counter-trade range action's reference set-point is [0; 1000]. The power is
exported from France to Spain.
:::
:::{group-tab} JSON file
~~~json
"counterTradeRangeActions" : [ {
    "id" : "counter-trade-range-action-id",
    "name" : "counterTradeRange1Name",
    "operator" : null,
    "speed" : 30,
    "onInstantUsageRules" : [ {
        "instant" : "preventive",
        "usageMethod" : "available"
    } ],
    "exportingCountry" : "FR",
    "importingCountry" : "ES",
    "initialSetpoint" : 50,
    "ranges" : [ {
        "min" : 0.0,
        "max" : 1000.0
    }, {
        "min" : -1000.0,
        "max" : 1000.0
    } ]
} ]
~~~
:::
:::{group-tab} Object fields
🔴⭐ **identifier**  
⚪ **name**  
⚪ **operator**  
🔴 **exporting country**  
🔴 **importing country**  
⚪ **groupId**: if you want to align this range action with others, set the same groupId for all  
🔵 **speed**: mandatory if it is an automaton  
⚪ **ranges**: list of 0 to N Range  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **min**  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **max**  
⚪ **onInstant usage rules**: list of 0 to N OnInstant usage rules (see paragraph on usage rules)  
⚪ **onContingencyState usage rules**: list of 0 to N OnContingencyState usage rules (see paragraph on usage rules)  
⚪ **onConstraint usage rules**: list of 0 to N OnConstraint usage rules (see paragraph on usage rules)  
:::
::::

## RAs usage limitations
A OpenRAO "ra-usage-limits-per-instant" consists in limits on the usage of remedial actions for given instants.  
The given instant IDs should match an ID of an instant in the CRAC. Otherwise, its limits will be ignored.  
See [here](creation-parameters.md#ra-usage-limits-per-instant) for further explanation on each field described in the following example.

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
crac.newRaUsageLimits("preventive")
    .withMaxRa(44)
    .withMaxTso(12)
    .withMaxRaPerTso(new HashMap<>(Map.of("FR", 41, "BE", 12)))
    .withMaxPstPerTso(new HashMap<>(Map.of("BE", 7)))
    .withMaxTopoPerTso(new HashMap<>(Map.of("DE", 5)))
    .withMaxElementaryActionPerTso(new HashMap<>(Map.of("BE", 20)))
    .add();
crac.newRaUsageLimits("curative")
    .withMaxRa(3)
    .add();
~~~
:::
:::{group-tab} JSON file
~~~json
"ra-usage-limits-per-instant" : [ {
  "instant": "preventive",
  "max-ra" : 44,
  "max-tso" : 12,
  "max-ra-per-tso" : {"FR": 41, "BE": 12},
  "max-topo-per-tso" : {"DE": 5},
  "max-pst-per-tso" : {"BE": 7}
  "max-elementary-actions-per-tso" : {"BE": 20}
}, {
  "instant": "curative",
  "max-ra" : 3
} ]
~~~
:::
::::

If several instants of the same kind are defined in the CRAC, the usage limits are **cumulative** among these instants.

For instance, let us consider a CRAC with 3 curative instants and the following usage limits:

```json
"ra-usage-limits-per-instant" : [ {
  "instant": "curative 1",
  "max-ra" : 1,
}, {
  "instant": "curative 2",
  "max-ra" : 3,
}, {
  "instant": "curative 3",
  "max-ra" : 7,
} ]
```

The maximum number of applicable remedial actions defined for the second curative instant (3) is a cumulated value that includes the maximum number of applicable remedial actions during the first curative instant (1). Thus, if 1 remedial action was applied during the first curative instant, only 2 remedial actions can actually be applied during the second curative instant. Likewise, the maximum number of remedial actions for the third curative instant includes the remedial actions applied at curative 1 and 2 instants. Depending on the number of previously applied remedial actions, the number of actually applicable remedial actions during the third curative instant can vary between 4 and 7.
