# CRAC creation parameters

## Introduction
Native CRAC formats do not always hold all the information needed in order to conduct a precise remedial action optimisation.  
For instance, when monitoring current flows (vs their limits) on lines, one can wonder if they shall monitor 
the current on both sides of the line, on the left side only, or on the right side only.  
In DC convention, it doesn't matter: it is enough for the RAO to monitor the left side, allowing it to have a smaller optimisation problem.  
In AC convention, it is generally preferred to monitor both sides, as flows on both sides can be different because of losses.  
  
In OpenRAO's [internal CRAC format](json), it is possible to define which side(s) to monitor, and this is needed in the RAO.  
However, no CRAC format actually defines this configuration, thus it is necessary to add an extra configuration object 
when creating a CRAC object to be used in the RAO.  
This is the purpose of OpenRAO's "CRAC creation parameters".

## Creating a CracCreationParameters object

(and reading/writing it to a file).  
Some examples:  
```java
// Creating the object
CracCreationParameters parameters = new CracCreationParameters();

// Writing it to an output stream
OutputStream outputStreeam = ...
JsonCracCreationParameters.write(parameters, outputStream);

// Reading an object from a file
Path jsonFilePath = ...
parameters = JsonCracCreationParameters.read(jsonFilePath);
```
  
  
## Non-specific parameters
OpenRAO's [CracCreationParameters](https://github.com/powsybl/powsybl-open-rao/blob/main/data/crac-io/crac-io-api/src/main/java/com/powsybl/openrao/data/cracio/api/parameters/CracCreationParameters.java) 
defines a few parameters needed for all native CRAC formats.

### crac-factory
OpenRAO's [Crac](https://github.com/powsybl/powsybl-open-rao/blob/main/data/crac/crac-api/src/main/java/com/powsybl/openrao/data/crac/api/Crac.java) 
object is actually just an interface, with a default implementation in [CracImpl](https://github.com/powsybl/powsybl-open-rao/tree/main/data/crac/crac-impl/src/main/java/com/powsybl/openrao/data/cracimpl).  
As a OpenRAO toolbox user, you are allowed to define your own custom Crac implementation. This implementation shall be instanced using a [CracFactory](https://github.com/powsybl/powsybl-open-rao/blob/main/data/crac/crac-api/src/main/java/com/powsybl/openrao/data/crac/api/CracFactory.java).  
OpenRAO's default implementation is [CracImplFactory](https://github.com/powsybl/powsybl-open-rao/blob/main/data/crac/crac-impl/src/main/java/com/powsybl/openrao/data/cracimpl/CracImplFactory.java).  
Parameter "crac-factory" allows the user to define which CracFactory implementation (thus which Crac implementation) to 
use. If you do not have a custom implementation (which should be the case of most users), set it to "CracImplFactory".  

### default-monitored-line-side
This parameter defines which side(s) of a line the RAO should monitor by default (side is defined as per [PowSyBl](inv:powsyblcore:*:*#index) 
convention), when optimizing line's flow margin.    
Note that this parameter is ignored when the line side to monitor is defined by the native CRAC itself (e.g. when a 
cross-border tie-line is monitored by one TSO only, then the RAO will automatically detect on which side this TSO is).  
Possible values for this parameter are:  
- **monitor-lines-on-side-one** to monitor lines on side one only (typically to be used in DC-loadflow mode)
- **monitor-lines-on-side-two** to monitor lines on side two only (alternatively in DC-loadflow mode)
- **monitor-lines-on-both-sides** to monitor lines on both sides; the flow limits defined in the native CRAC file will then 
apply to both sides (typically to be used in AC-loadflow mode)
  
> 💡  **NOTE**  
> If you don't know which option to choose, it is safest to go with **monitor-lines-on-both-sides**

### ra-usage-limits-per-instant
This parameter limits the usage of remedial actions for given instants.  
The instant ID must match an ID of an instant in the CRAC.   
If the given instant contains multiple states (possible for auto and curative instant), the given limits are applied independently on each state.  
The RAs usage limits contain the following fields :

 - **max-ra :**
    - _Expected value:_ integer
    - _Default value:_ 2^32 -1 (max integer value)
    - _Usage:_ It defines the maximum number of remedial actions allowed for the given instant. The RAO will prioritize remedial actions that have the best impact on the minimum margin.

  - **max-tso :**
    - _Expected value:_ integer
    - _Default value:_ 2^32 -1 (max integer value)
    - _Usage:_ It defines the maximum number of TSOs that can apply remedial actions for the given instant. The RAO will choose the best TSOs combination to maximize the minimum margin.

  - **max-ra-per-tso :**
    - _Expected value:_ a map with string keys and integer values. The keys should be the same as the RAs’ operators as written in the CRAC file
    - _Default value:_ empty map
    - _Usage:_ It defines the maximum number of remedial actions allowed for each TSO, for the given instant.
    The TSOs should be identified using the same IDs as in the CRAC. If a TSO is not listed in this map, then the number of its allowed RAs is supposed infinite.

  - **max-topo-per-tso :**  
    Exactly the same as **max-ra-per-tso** but it only concerns topological RAs

  - **max-pst-per-tso :**  
    Exactly the same as **max-ra-per-tso** but it only concerns PST RAs

  - **max-elementary-actions-per-tso :**  
    - _Expected value:_ a map with string keys and integer values. The keys should be RAs’ operators as written in the CRAC file
    - _Default value:_ empty map
    - _Usage:_ It defines the maximum number of elementary actions allowed for each TSO, for the given instant. For PST range actions, moving one tap is considered to be an elementary action.
    The TSOs should be identified using the same IDs as in the CRAC. If a TSO is not listed in this map, then the number of its allowed RAs is supposed infinite.
    _⚠️ This usage limit is only applicable if PSTs are approximated as integer taps (see [APPROXIMATED_INTEGERS](/parameters.md#pst-model))._

### complete example
::::{tabs}
:::{group-tab} JAVA API
```java
CracCreationParameters cracCreationParameters = new CracCreationParameters();
cracCreationParameters.setCracFactoryName("CracImplFactory");
cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
RaUsageLimits raUsageLimits = new RaUsageLimits();
raUsageLimits.setMaxRa(10);
raUsageLimits.setMaxRaPerTso(Map.of("FR", 4, "BE", 6))
cracCreationParameters.addRaUsageLimitsForAGivenInstant("curative", raUsageLimits);
```
:::
:::{group-tab} JSON file
```json
{
  "crac-factory": "CracImplFactory",
  "default-monitored-line-side" : "monitor-lines-on-both-sides",
  "ra-usage-limits-per-instant" : [ {
    "instant": "curative",
    "max-ra" : 10,
    "max-ra-per-tso" : {"FR": 4, "BE": 6}
  } ]
}
```
:::
::::

## CSE-specific parameters

The [CSE native crac format](cse) lacks important information that other formats don't.  
The user can define a [CseCracCreationParameters](https://github.com/powsybl/powsybl-open-rao/blob/main/data/crac-io/crac-io-cse/src/main/java/com/powsybl/openrao/data/cracio/cse/parameters/CseCracCreationParameters.java) 
extension to the CracCreationParameters object in order to define them.  

### range-action-groups (CSE)

The CSE native CRAC format does not allow defining [aligned range actions](json.md#range-actions). This extra parameter 
allows the user to do just that.  
To use it, you have to define a list of strings containing the IDs of range actions that have to be aligned seperated by a 
" + " sign; for example "range-action-1-id + range-action-17-id" and "range-action-8-id + range-action-9-id".  
See [example below](#full-cse-example) for a better illustration.

### bus-bar-change-switches

As explained in the CSE native CRAC format section [here](cse.md#bus-bar-change), bus-bar-change remedial actions are defined in OpenRAO 
as [switch pair network actions](/input-data/crac.md#switch-pair).  
These switches are not defined in the native CRAC nor in the original network, they should be created artificially in the 
network and their IDs should be sent to the RAO.  
This parameter allows the definition of the switch(es) to open and the switch(es) to close for every bus-bar change remedial action.  
To use it, for every bus-bar-change remedial action ID, define the IDs of the pairs of switches to open/close.  
See [example below](#full-cse-example) for a better illustration.

### Full CSE example

::::{tabs}
:::{group-tab} JAVA API
```java
// Create CracCreationParameters and set global parameters
CracCreationParameters cracCreationParameters = new CracCreationParameters();
cracCreationParameters.setCracFactoryName("CracImplFactory");
cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES);
// Create CSE-specific parameters
CseCracCreationParameters cseParameters = new CseCracCreationParameters();
cseParameters.setRangeActionGroupsAsString(List.of("rangeAction3 + rangeAction4", "hvdc1 + hvdc2"));
cseParameters.setBusBarChangeSwitchesSet(Set.of(
    new BusBarChangeSwitches("remedialAction1", Set.of(new SwitchPairId("switch1", "switch2"), new SwitchPairId("switch3", "switch4"))),
    new BusBarChangeSwitches("remedialAction2", Set.of(new SwitchPairId("switch5", "switch6")))
));
// Add CSE extension to CracCreationParameters
cracCreationParameters.addExtension(CseCracCreationParameters.class, cseParameters);
```
:::
:::{group-tab} JSON file
```json
{
  "crac-factory": "CracImplFactory",
  "default-monitored-line-side": "monitor-lines-on-both-sides",
  "extensions": {
    "CseCracCreatorParameters": {
      "range-action-groups": [
        "rangeAction3 + rangeAction4",
        "hvdc1 + hvdc2"
      ],
      "bus-bar-change-switches": [
        {
          "remedial-action-id": "remedialAction1",
          "switch-pairs": [
            {
              "open": "switch1",
              "close": "switch2"
            },
            {
              "open": "switch3",
              "close": "switch4"
            }
          ]
        },
        {
          "remedial-action-id": "remedialAction2",
          "switch-pairs": [
            {
              "open": "switch5",
              "close": "switch6"
            }
          ]
        }
      ]
    }
  }
}
```
:::
::::

## CIM-specific parameters

The [CIM native CRAC format](cim) lacks important information that other formats don't.  
The user can define a [CimCracCreationParameters](https://github.com/powsybl/powsybl-open-rao/blob/main/data/crac-io/crac-io-cim/src/main/java/com/powsybl/openrao/data/cracio/cim/parameters/CimCracCreationParameters.java)
extension to the CracCreationParameters object in order to define them.

### timeseries-mrids

Some processes require the RAO to split the CIM CRAC into multiple smaller CRACs, in particular in order to optimize different 
borders separately. For example, the SWE CC process requires the RAO to be split into one France-Spain RAO and one 
Spain-Portugal RAO. This is possible thanks to the CIM CRAC's TimeSeries tags, that can allocate crac objects to one of 
the two borders.  
The "timeseries-mrids" parameters allows the user to set which timeseries should be read from the CIM CRAC file, in order 
to define the CNECs and remedial actions of the border-specific RAO. TimeSeries are identified by their "mRID" value.  
See [example below](#full-cim-example) for a better illustration.

### range-action-groups (CIM)

Like the CSE native CRAC format, the CIM format does not allow defining [aligned range actions](json.md#range-actions). 
This extra parameter allows the user to do just that.  
To use it, you have to define a list of strings containing the IDs of range actions that have to be aligned seperated by a
" + " sign; for example "range-action-1-id + range-action-17-id" and "range-action-8-id + range-action-9-id".  
See [example below](#full-cim-example) for a better illustration.

### range-action-speeds

OpenRAO can simulate range-action automatons, that is automatons that shift their set-points until one or many CNECs are secured.  
In order to do that, OpenRAO must know which automaton is quicker than the other, because activating one automaton can render 
the others useless.
As the CIM native CRAC format does not allow the definition of relative automaton speeds, this parameter allows the user to do it.  
To use it, set the speed of every range action automaton, defined by its ID. A smaller value means a speedier automaton.  
Beware that OpenRAO cannot optimize range-action automatons that do not have a defined speed ; also that aligned range actions 
must have the same speed.  
See [example below](#full-cim-example) for a better illustration.

### voltage-cnecs-creation-parameters

The CIM CRAC does not allow the definition of [VoltageCnecs](json.md#voltage-cnecs). This parameter allows the user 
to add VoltageCnecs during CRAC creation.  
To define voltage CNECs, the user has to define:
- A list of monitored network elements, identified by their unique ID in the network file. These network elements must be VoltageLevels.
- Instants for which these elements should be monitored (among instant IDs defined in the CRAC)
- For instants other than preventive that are selected, a list of contingencies after which these elements are monitored 
at defined instants (the contingences shall be identified by their CIM CRAC mRIDs as they figure in the B55 Series/Contingency_Series)
- For every instant, the minimum and maximum voltage thresholds to be respected for every nominal voltage level.  
See [example below](#full-cim-example) for a better illustration.

### timestamp
This parameter allows the user to define the timestamp for which to create the CRAC.

In the json file, the timestamp has to be defined using the ISO 8601 standard ex. " 2019-01-08T12:00+02:00"

### Full CIM example

::::{tabs}
:::{group-tab} JAVA API
```java
// Create CracCreationParameters and set global parameters
CracCreationParameters cracCreationParameters = new CracCreationParameters();
// Create CIM-specific parameters
CimCracCreationParameters cimParameters = new CimCracCreationParameters();
// Only read TimeSeries with mRIDs "border1-ts1" and "border1-ts2" from the CIM CRAC
cimParameters.setTimeseriesMrids(Set.of("border1-ts1", "border1-ts2"));
// Align rangeAction1 with rangeAction2 and rangeAction10 with rangeAction11
cimParameters.setRangeActionGroupsAsString(List.of("rangeAction1 + rangeAction2", "rangeAction10 + rangeAction11"));
// rangeAction1 and rangeAction2 are automatons that act faster than rangeAction3
cimParameters.setRemedialActionSpeed(Set.of(
    new RangeActionSpeed("rangeAction1", 1),
    new RangeActionSpeed("rangeAction2", 1),
    new RangeActionSpeed("rangeAction3", 2)
));
// Define voltage CNECs to be created
// Monitor these voltage levels (using their IDs in the network):
Set<String> voltageMonitoredElements = Set.of("ne1", "ne2");
// At preventive instant, constrain voltage CNECs:
// - with a nominal V of 400kV, to stay between 395 and 430kV
// - with a nominal V of 200kV, to stay above 180kV
Map<Double, VoltageThreshold> preventiveVoltageThresholds = Map.of(
    400., new VoltageThreshold(Unit.KILOVOLT, 395., 430.),
    200., new VoltageThreshold(Unit.KILOVOLT, 180., null)
);
// At curative instant, constrain voltage CNECs:
// - with a nominal V of 400kV, to stay between 380 and 430kV
// - with a nominal V of 210kV, to stay below 230kV
Map<Double, VoltageThreshold> curativeVoltageThresholds = Map.of(
    400., new VoltageThreshold(Unit.KILOVOLT, 380., 430.),
    210., new VoltageThreshold(Unit.KILOVOLT, null, 230.)
);
// Define these voltage CNECs for the following contingencies, as identified in the CIM CRAC:
Set<String> voltageContingencies = Set.of("N-1 ONE", "N-1 TWO");
// Put all this together in the CIM CRAC creation parameters
cimParameters.setVoltageCnecsCreationParameters(new VoltageCnecsCreationParameters(
    Map.of(
        "preventive", new VoltageMonitoredContingenciesAndThresholds(null, preventiveVoltageThresholds),
        "curative", new VoltageMonitoredContingenciesAndThresholds(voltageContingencies, curativeVoltageThresholds)
    ),
    voltageMonitoredElements
));
cimParameters.setTimestamp(OffsetDateTime.parse("2019-01-08T12:00+02:00"));
// Add CIM extension to CracCreationParameters
cracCreationParameters.addExtension(CimCracCreationParameters.class, cimParameters);
```
:::
:::{group-tab} JSON file
```json
{
  "crac-factory": "CracImplFactory",
  "default-monitored-line-side": "monitor-lines-on-both-sides",
  "extensions": {
    "CimCracCreatorParameters": {
      "timeseries-mrids" : [ "border1-ts1", "border1-ts2" ],
      "range-action-groups": [
        "rangeAction1 + rangeAction2",
        "rangeAction10 + rangeAction11"
      ],
      "range-action-speeds": [
        {
          "range-action-id": "rangeAction1",
          "speed": 1
        },
        {
          "range-action-id": "rangeAction2",
          "speed": 1
        },
        {
          "range-action-id": "rangeAction3",
          "speed": 2
        }
      ],
      "voltage-cnecs-creation-parameters": {
        "monitored-states-and-thresholds": [
          {
            "instant": "preventive",
            "thresholds-per-nominal-v": [
              {
                "nominalV": 400,
                "unit": "kilovolt",
                "min": 395,
                "max": 430
              },
              {
                "nominalV": 200,
                "unit": "kilovolt",
                "min": 180
              }
            ]
          },
          {
            "instant": "curative",
            "thresholds-per-nominal-v": [
              {
                "nominalV": 400,
                "unit": "KiloVolt",
                "min": 380,
                "max": 430
              },
              {
                "nominalV": 210,
                "unit": "KILOVOLT",
                "max": 230
              }
            ],
            "contingency-names": [
              "N-1 ONE",
              "N-1 TWO"
            ]
          }
        ],
        "monitored-network-elements": [
          "ne1",
          "ne2"
        ]
      },
      "timestamp": "2019-01-08T12:00+02:00"
    }
  }
}
```
:::
::::

## CSA-specific parameters

The CSA profiles from the [CSA native CRAC format](csa) need additional information to be converted to the internal OpenRAO CRAC format. The user can define a [CsaCracCreationParameters](https://github.com/powsybl/powsybl-open-rao/blob/main/data/crac-io/crac-io-csa-profiles/src/main/java/com/powsybl/openrao/data/cracio/csaprofile/parameters/CsaCracCreationParameters.java) extension to the CracCreationParameters object in order to define them.

### capacity-calculation-region-eic-code

In the CSA profiles, the [AssessedElements](csa.md#cnecs) objects can be declared with a `SecuredForRegion` or a `ScannedForRegion` attribute to respectively indicate if the resulting CNEC should be optimized or simply monitored. Both these fields point to the EI code of a Capacity Calculation Region (CCR). For the importer to know which code to expect, it has to be declared in the CRAC creation parameters.

### sps-max-time-to-implement-threshold-in-seconds

Instead of using a `SchemeRemedialAction` to define an [auto remedial action](csa.md#using-gridstatealterationremedialaction-and-timetoimplement), it is possible to declare a classical curative `GridStateAlterationRemedialAction` with a `timeToImplement` attribute set to a certain value, which, if below a general configurable time threshold (in seconds), means that the remedial action must be considered as an ARA instead of a CRA.

### cra-application-window

Because of the three curative instants used in CSA, the definition of the instant of a FlowCNEC is [quite complex](csa.md#tatl-to-flowcnec-instant-association) and each curative instant must be linked to an application time for the importer to know which instant must be associated to a given TATL (or PATL).

### use-patl-in-final-state

Usually, the PATL is used as the operational limit for the final state (i.e. after all three batches of CRAs have been applied) but some TSOs may want to use a TATL instead so this information has to be configurable.

### timestamp

This parameter allows the user to define the timestamp for which to create the CRAC.

In the json file, the timestamp has to be defined using the ISO 8601 standard ex. " 2019-01-08T12:00+02:00".

### Full CSA example

::::{tabs}
:::{group-tab} JAVA API
```java
// Create CracCreationParameters and set global parameters
CracCreationParameters cracCreationParameters = new CracCreationParameters();
// Create CSA-specific parameters
CimCracCreationParameters csaParameters = new CsaCracCreationParameters();
// Indicate the EI Code of the CCR region (SWE)
csaParameters.setCapacityCalculationRegionEicCode("10Y1001C--00095L");
// Force all curative GridStateAlterationRemedialActions with a timeToImplement of 0 to be processed as ARAs
csaParameters.setSpsMaxTimeToImplementThresholdInSeconds(0);
// Indicate that REN and RTE use the PATL as the final state limit, but that REE does not
csaParameters.setUsePatlInFinalState(Map.of(
    "REE", false,
    "REN", true,
    "RTE", true
));
// Associate each curative instant to an application time
csaParameters.setCraApplicationWindow(Map.of(
    "curative 1", 300,
    "curative 2", 600,
    "curative 3", 1200
));
csaParameters.setTimestamp(OffsetDateTime.parse("2019-01-08T12:00+02:00"));
// Add CSA extension to CracCreationParameters
cracCreationParameters.addExtension(CsaCracCreationParameters.class, csaParameters);
```
:::

:::{group-tab} JSON file
```json
{
  "crac-factory" : "CracImplFactory",
  "extensions" : {
    "CsaCracCreatorParameters" : {
      "capacity-calculation-region-eic-code": "10Y1001C--00095L",
      "sps-max-time-to-implement-threshold-in-seconds": 0,
      "use-patl-in-final-state": {
        "REE": false,
        "REN": true,
        "RTE": true
      },
      "cra-application-window": {
        "curative 1": 300,
        "curative 2": 600,
        "curative 3": 1200
      }, 
      "timestamp": "2019-01-08T12:00+02:00"
    }
  }
}
```
:::
::::

## Flow Based Constraint-specific parameters

The Flow Based Constraint from the [Flow Based Constraint CRAC format](fbconstraint) need an additional information to be converted to the internal OpenRAO CRAC format. 
The user can define a [FbConstraintCracCreationParameters](https://github.com/powsybl/powsybl-open-rao/tree/main/data/crac/crac-io/crac-io-fb-constraint/src/main/java/com/powsybl/openrao/data/crac/io/fbconstraint/parameters/FbConstraintCracCreationParameters.java) extension to the CracCreationParameters object in order to define them.

### timestamp

This parameter allows the user to define the timestamp for which to create the CRAC.

In the json file, the timestamp has to be defined using the ISO 8601 standard ex. " 2019-01-08T12:00+02:00".


### Full FbConstraint example

::::{tabs}
:::{group-tab} JAVA API
```java
// Create CracCreationParameters and set global parameters
CracCreationParameters cracCreationParameters = new CracCreationParameters();
// Create CSA-specific parameters
FbConstraintCracCreationParameters fbConstraintParameters = new FbConstraintCracCreationParameters();
// Add timestamp
fbConstraintParameters.setTimestamp(OffsetDateTime.parse("2019-01-08T12:00+02:00"));
// Add FbConstraint extension to CracCreationParameters
cracCreationParameters.addExtension(FbConstraintCracCreationParameters.class, fbConstraintParameters);
```
:::

:::{group-tab} JSON file
```json
{
  "crac-factory" : "CracImplFactory",
  "extensions" : {
    "FbConstraintCracCreatorParameters" : {
      "timestamp": "2019-01-08T12:00+02:00"
    }
  }
}
```
:::
::::