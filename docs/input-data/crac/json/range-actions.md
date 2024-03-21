A FARAO "Range Action" is a remedial action with a continuous or discrete set-point. If the range action is inactive, its
set-point is equal to its value in the initial network. If it is activated, its set-point is optimized by the RAO to
improve the objective function.  
FARAO has four types of range actions : PST range actions, HVDC range actions, "injection" range actions and counter-
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

The final validity range of the PstRangeAction is the intersection of its TapRanges, with the intersection of the min/max feasible taps of the PST.  
The PstRangeAction also requires additional data, notably to be able to interpret the TapRanges. Those additional data are: the initial tap of the PST, and a conversion map which gives for each feasible tap of the PST its corresponding angle. Utility methods have been developed in FARAO to ease the management of these additional data during the creation of a PstRangeAction.

Two or more [aligned PST range actions](crac#range-action) must have the same (random) group ID defined. The RAO will
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
Note that the [PstHelper utility class](https://github.com/powsybl/powsybl-open-rao/blob/main/data/crac-creation/crac-creation-util/src/main/java/com/powsybl/openrao/data/craccreation/util/PstHelper.java) can ease the creation of the TapToAngleConversionMap.
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
⚪ **onInstant usage rules**: list of 0 to N OnInstant usage rules (see paragraph on [usage rules](#remedial-actions))  
⚪ **onState usage rules**: list of 0 to N OnContingencyState usage rules (see paragraph on [usage rules](#remedial-actions))  
⚪ **onFlowConstraintInCountry usage rules**: list of 0 to N OnFlowConstraintInCountry usage rules (see previous paragraph on [usage rules](#remedial-actions))  
⚪ **onFlowConstraint usage rules**: list of 0 to N OnFlowConstraint usage rules (see previous paragraph on [usage rules](#remedial-actions))  
⚪ **onAngleConstraint usage rules**: list of 0 to N OnAngleConstraint usage rules (see previous paragraph on [usage rules](#remedial-actions))
:::
::::

### HVDC Range Action
An HvdcRangeAction contains a network element that must point towards an [HvdcLine of the iidm PowSyBl network model](https://www.powsybl.org/pages/documentation/grid/model/#hvdc-line).  
The HvdcRangeAction will be able to modify its active power set-point.

The domain in which the HvdcRangeAction can modify the HvdcSetpoint is delimited by 'HvdcRanges'.
An HvdcRangeAction contains a list of HvdcRanges. A range must be defined with a min and a max.

Two or more [aligned HVDC range actions](crac#range-action) must have the same (random) group ID defined. The RAO will
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
    "freeToUseUsageRules" : [ {
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
⚪ **onInstant usage rules**: list of 0 to N OnInstant usage rules (see paragraph on [usage rules](#remedial-actions))  
⚪ **onContingencyState usage rules**: list of 0 to N OnContingencyState usage rules (see paragraph on [usage rules](#remedial-actions))  
⚪ **onFlowConstraint usage rules**: list of 0 to N OnFlowConstraint usage rules (see paragraph on [usage rules](#remedial-actions))  
:::
::::

### Injection Range Action

An InjectionRangeAction modifies given generators' & loads' injection set-points inside a given range.  
Each impacted generator or load has an associated "key", which is a coefficient of impact that is applied on its set-point.

This range action is mainly used to represent an HVDC line in an AC equivalent model (where the line is disconnected and
replaced by two injections, one on each side of the line, with opposite keys of 1 and -1).

![HVDC AC model](/_static/img/HVDC_AC_model.png)

Two or more [aligned injection range actions](crac#range-action) must have the same (random) group ID defined. The RAO will
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
⚪ **onFlowConstraint usage rules**: list of 0 to N OnFlowConstraint usage rules (see paragraph on usage rules)  
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
⚪ **onFlowConstraint usage rules**: list of 0 to N OnFlowConstraint usage rules (see paragraph on usage rules)  
⚪ **onAngleConstraint usage rules**: list of 0 to N OnAngleConstraint usage rules (see paragraph on usage rules)  
⚪ **onVoltageConstraint usage rules**: list of 0 to N OnVoltageConstraint usage rules (see paragraph on usage rules)  
:::
::::