These results contain information about the selection of standard range actions by the RAO.  
Standard range actions are range actions that have a continuous set-point that can be optimised in a given range.  
Actually, standard range actions handled by FARAO are: **HVDC range actions** and **Injection range actions**.  
*Note that you will need to use [RangeAction](/input-data/crac/json.md#range-actions) objects from the CRAC for querying the Java API.*

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
*Note that you will need to use [PstRangeAction or RangeAction](/input-data/crac/json.md#pst-range-action) objects from the CRAC for querying the Java API.*

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
