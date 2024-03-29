This field contains macro information about which steps the [CASTOR RAO](https://farao-community.github.io/docs/engine/ra-optimisation/search-tree-rao) executed.  
(See also: [Forbidding cost increase](/parameters/parameters.md#forbid-cost-increase), [Second preventive RAO parameters](/parameters/parameters.md#second-preventive-rao))

| Value                                                    | Did CASTOR run a 1st preventive RAO? | Did CASTOR run a 2nd preventive RAO? | Did the RAO fall back to initial situation? | Did the RAO fall back to 1st preventive RAO result even though a 2nd was run? |  
|----------------------------------------------------------|--------------------------------------|--------------------------------------|---------------------------------------------|-------------------------------------------------------------------------------|
| FIRST_PREVENTIVE_ONLY                                    | ✔️                                   |                                      |                                             |                                                                               |
| FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION           | ✔️                                   |                                      | ✔️                                          |                                                                               |
| SECOND_PREVENTIVE_IMPROVED_FIRST                         | ✔️                                   | ✔️                                   |                                             |                                                                               |
| SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION          | ✔️                                   | ✔️                                   | ✔️                                          |                                                                               |
| SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION | ✔️                                   | ✔️                                   |                                             | ✔️                                                                            |

::::{tabs}
:::{group-tab} JAVA API

~~~java
OptimizationStepsExecuted getOptimizationStepsExecuted();
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
{
  "optimizationStepsExecuted": "Second preventive improved first preventive results",
  ...
}
~~~

:::
::::
