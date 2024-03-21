A FARAO "Network Action" is a remedial action with a binary state: it is either active or inactive.  
One network action is a combination of one or multiple "elementary actions", among the following:
- Topological action: opening or closing a branch or a switch in the network.
- PST set-point: setting the tap of a PST in the network to a specific position.
- Injection set-point: setting the active power set-point of an element in the network (load, generator, or [dangling line](https://www.powsybl.org/pages/documentation/grid/model/#dangling-line))
  or the number of sections of a shunt compensator to a specific value.
- Switch pairs: opening a switch in the network and closing another (actually used to model [CSE bus-bar change remedial actions](cse#bus-bar)).

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
// combination of two topological actions
crac.newNetworkAction()
	.withId("topological-na-id")
    .withName("topological-na-name")
    .withOperator("operator")
    .newTopologicalAction()
		.withNetworkElement("network-element-id-1")
		.withActionType(ActionType.CLOSE)
		.add()
    .newTopologicalAction()
		.withNetworkElement("network-element-id-2")
		.withActionType(ActionType.OPEN)
		.add()
    .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT).add()
    .add();

// pst set-point
crac.newNetworkAction()
	.withId("pst-setpoint-na-id")
    .withName("pst-setpoint-na-name")
    .withOperator("operator")
    .newPstSetPoint()
		.withSetpoint(15)
		.withNetworkElement("pst-network-element-id")
		.add()
    .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT).add()
    .add();

// injection set-point with two usage rules
crac.newNetworkAction()
	.withId("injection-setpoint-na-id")
	.withOperator("operator")
	.newInjectionSetPoint()
		.withSetpoint(260)
		.withNetworkElement("generator-network-element-id")
		.withUnit(Unit.MEGAWATT)
		.add()
    .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT).add()
    .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency("contingency-id").withInstant(Instant.CURATIVE).add()
    .add();

// injection set-point on a shunt compensator
    crac.newNetworkAction()
        .withId("injection-setpoint-shunt-compensator-id")
        .withOperator("operator")
        .newInjectionSetPoint()
          .withSetpoint(3)
          .withNetworkElement("shunt-compensator-id")
          .withUnit(Unit.SECTION_COUNT)
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
    "id" : "topological-na-id",
    "name" : "topological-na-name",
    "operator" : "operator",
    "freeToUseUsageRules" : [ {
      "instant" : "preventive",
      "usageMethod" : "available"
    } ],
    "topologicalActions" : [ {
      "networkElementId" : "network-element-id-1",
      "actionType" : "close"
    }, {
      "networkElementId" : "network-element-id-2",
      "actionType" : "open"
    } ]
  }, {
    "id" : "pst-setpoint-na-id",
    "name" : "pst-setpoint-na-name",
    "operator" : "operator",
    "freeToUseUsageRules" : [ {
      "instant" : "preventive",
      "usageMethod" : "available"
    } ],
    "pstSetpoints" : [ {
      "networkElementId" : "pst-network-element-id",
      "setpoint" : 15
    } ]
  }, {
    "id" : "injection-setpoint-na-id",
    "name" : "injection-setpoint-na-id",
    "operator" : "operator",
    "freeToUseUsageRules" : [ {
      "instant" : "preventive",
      "usageMethod" : "available"
    } ],
    "onStateUsageRules" : [ {
      "instant" : "curative",
      "contingencyId" : "contingency-id",
      "usageMethod" : "available"
    } ],
    "injectionSetpoints" : [ {
      "networkElementId" : "generator-network-element-id",
      "setpoint" : 260.0,
      "unit" : "megawatt"
    } ]
  }, {
    "id" : "switch-pair-na-id",
    "name" : "switch-pair-na-id",
    "operator" : "operator",
    "freeToUseUsageRules" : [ {
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
⚪ **freeToUse usage rules**: list of 0 to N FreeToUse usage rules (see previous paragraph on [usage rules](#remedial-actions))  
⚪ **onState usage rules**: list of 0 to N OnState usage rules (see previous paragraph on [usage rules](#remedial-actions))  
⚪ **onFlowConstraintInCountry usage rules**: list of 0 to N OnFlowConstraintInCountry usage rules (see previous paragraph on [usage rules](#remedial-actions))  
⚪ **onFlowConstraint usage rules**: list of 0 to N OnFlowConstraint usage rules (see previous paragraph on [usage rules](#remedial-actions))  
⚪ **onAngleConstraint usage rules**: list of 0 to N OnAngleConstraint usage rules (see previous paragraph on [usage rules](#remedial-actions))  
🔵 **topological actions**: list of 0 to N TopologicalAction  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **network element**: id is mandatory, name is optional  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **action type**  
🔵 **pst set points**: list of 0 to N PstSetPoint  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **network element**: id is mandatory, name is optional  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **setpoint**: integer, new tap of the PST  
🔵 **injection set points**: list of 0 to N InjectionSetPoint  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **network element**: id is mandatory, name is optional  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **setpoint**: double, new value of the injection  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **unit**: Unit, unit of the InjectionSetPoint (MEGAWATT for generators, loads and dangling lines, or SECTION_COUNT for linear shunt compensators)  
🔵 **switch pairs**: list of 0 to N SwitchPair  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **switch to open (network element)**: id is mandatory, name is optional  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **switch to close (network element)**: id is mandatory, name is optional, must be different from switch to open  
<br>
*NB*: A Network Action must contain at least on elementary action.
:::
::::