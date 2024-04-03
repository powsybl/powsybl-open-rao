These results contain information about the selection of network actions by the RAO.
*Note that you will need to use [NetworkAction](/input-data/crac/json.md#network-actions) objects from the CRAC for querying the Java API.*

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
