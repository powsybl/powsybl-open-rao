# JSON RAO Result File

## Introduction

A **R**emedial **A**ction **O**ptimisation process provides an optimal list of remedial actions to be applied in basecase and after contingencies listed in the [CRAC](/input-data/crac.md). The decisions are based upon the impact of these remedial actions on the CRAC's [CNECs](/input-data/crac.md#cnec).

A **RaoResult object model** has been designed in OpenRAO in order to hold all the important results of optimisation.
In this page, we present:
- where to find the RaoResult instance,
- how to save a RaoResult java object to a JSON file,
- how to import a RaoResult java object from a JSON file,
- how to access information in the RaoResult, using either the RaoResult java object or the JSON file.

## Accessing the RAO result

The [RaoResult](https://github.com/powsybl/powsybl-open-rao/blob/main/data/rao-result/rao-result-api/src/main/java/com/powsybl/openrao/data/raoresultapi/RaoResult.java) java object is actually an interface that is implemented by many OpenRAO classes. However, one only needs to use the interface's functions.
A RaoResult object is returned by OpenRAO's main optimisation method:

~~~java
CompletableFuture<RaoResult> RaoProvider::run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant)
~~~

Where RaoProvider is the chosen implementation of the RAO, such as [CASTOR](https://github.com/powsybl/powsybl-open-rao/blob/main/ra-optimisation/search-tree-rao/src/main/java/com/powsybl/openrao/searchtreerao/castor/algorithm/Castor.java).

## Exporting and importing a JSON file

A RaoResult object can be saved into a JSON file (no matter what implementation it is).
A RaoResult JSON file can be imported into a [RaoResultImpl](https://github.com/powsybl/powsybl-open-rao/blob/main/data/rao-result/rao-result-impl/src/main/java/com/powsybl/openrao/data/raoresultimpl/RaoResultImpl.java), and used as a RaoResult java object.

### Export

Example:

~~~java
raoResult.write("JSON", crac, properties, outputStream);
~~~

Where:
- **`raoResult`** is the RaoResult object you obtained from the RaoProvider;
- **`crac`** is the CRAC object you used in the RAO;
- **`properties`** is a set of specific parameters for the JSON export, currently two are defined:
  - `"rao-result.export.json.flows-in-amperes"` (optional, default is `"false"`): whether to export the flow measurements in `AMPERE`
  - `"rao-result.export.json.flows-in-megawatts"` (optional, default is `"false"`): whether to export the flow measurements in `MEGAWATT`
- **`outputStream`** is the `java.io.OutputStream` you want to write the JSON file into.

> At least one of `"rao-result.export.json.flows-in-amperes"` or `"rao-result.export.json.flows-in-megawatts"` must be true for the export to work properly.

### Import

Example:

~~~java
RaoResult importedRaoResult = RaoResult.read(inputStream, crac);
~~~

Where:
- **`crac`** is the CRAC object you used in the RAO
- **`inputStream`** is the `java.io.InputStream` you read the JSON file into

## Contents of the RAO result

The RAO result object generally contains information about post-optimisation results.  
However, in some cases, it may be interesting to get some information about the initial state (e.g. power flows before 
optimisation), or about the situation after preventive optimisation (e.g. optimal PST tap positions in preventive). 
This is why **most of the information in the RAO results are stored by optimized instant**:  
- **INITIAL** (json) or **null** (Java API): values before remedial action optimisation (initial state)
- Instant of kind **PREVENTIVE** or **OUTAGE**: values after optimizing preventive instant, i.e. after applying optimal preventive remedial actions
- Instant of kind **AUTO**: values after simulating auto instant, i.e. after applying automatic remedial actions
- Instant of kind **CURATIVE**: values after optimizing curative instant, i.e. after applying optimal curative remedial actions
  
_See also: [RAO steps](/castor/rao-steps.md)_

### Computation status

```{include} rao-result/computation-status.md
:parser: markdown
```

### Security status

```{include} rao-result/security-status.md
:parser: markdown
```

### Execution details

```{include} rao-result/execution-details.md
:parser: markdown
```

### Objective function cost results

```{include} rao-result/obj-function.md
:parser: markdown
```

### Flow CNECs results

```{include} rao-result/flow-cnecs.md
:parser: markdown
```

### Angle CNECs results

```{include} rao-result/angle-cnecs.md
:parser: markdown
```

### Voltage CNECs results

```{include} rao-result/voltage-cnecs.md
:parser: markdown
```

### Network actions results

```{include} rao-result/network-actions.md
:parser: markdown
```

### Standard range actions results

```{include} rao-result/range-actions.md
:parser: markdown
```
