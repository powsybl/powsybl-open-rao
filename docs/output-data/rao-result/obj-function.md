Contains information about the reached objective function value, seperated into functional and virtual costs:
- the **functional** cost reflects the business value of the objective (e.g. the cost associated to the minimum margin and the business penalties on usage of remedial actions)
- the **virtual** cost reflects the violation of some constraints (e.g. MNEC & loop-flow constraints)

::::{tabs}
:::{group-tab} JAVA API

~~~java
// get the functional cost after optimisation of a given instant
double getFunctionalCost(Instant optimizedInstant);

// get the total virtual cost after optimisation of a given instant
double getVirtualCost(Instant optimizedInstant);

// get a specific virtual cost after optimisation of a given instant
double getVirtualCost(Instant optimizedInstant, String virtualCostName);

// get all the virtual cost names in the RAO
Set<String> getVirtualCostNames();

// get the overall cost (functional + total virtual) after optimisation of a given instant
double getCost(Instant optimizedInstant);
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
"costResults" : {
    "initial" : {
      "functionalCost" : 100.0,
      "virtualCost" : {
        "loopFlow" : 0.0,
        "MNEC" : 0.0
      }
    },
    "preventive" : {
      "functionalCost" : 80.0,
      "virtualCost" : {
        "loopFlow" : 0.0,
        "MNEC" : 0.0
      }
    },
    "auto" : {
      "functionalCost" : -20.0,
      "virtualCost" : {
        "loopFlow" : 15.0,
        "MNEC" : 20.0
      }
    },
    "curative" : {
      "functionalCost" : -50.0,
      "virtualCost" : {
        "loopFlow" : 10.0,
        "MNEC" : 2.0
      }
    }
  },
~~~

:::
::::
