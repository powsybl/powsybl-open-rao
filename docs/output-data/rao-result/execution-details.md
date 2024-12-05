This field can either contain information about the failure when the RAO fails, or macro information about which steps the [CASTOR RAO](/castor.md#algorithm) executed when it did not fail.  
(See also: [Forbidding cost increase](/parameters.md#forbid-cost-increase), [Second preventive RAO parameters](/parameters.md#second-preventive-rao-parameters))

Note: This field replaced the optimizationStepsExecuted field.

::::{tabs}
:::{group-tab} JAVA API

~~~java
String getExecutionDetails();
~~~

:::
:::{group-tab} JSON File

Example:

~~~json
{
  "executionDetails": "Second preventive improved first preventive results",
  ...
}
~~~

:::
::::
