# Combine Performance and Complexity

## Parallelization

There are different layers of parallelization possible in the search-tree methodology. In the current implementation
of the algorithm, the following parallelization features are all available and parameterized independently:

- Parallelization of contingency simulations:
In a security analysis, the load-flow computation for all contingencies can be done in parallel. A parameter is 
available to specify how many threads are available for each security analysis. This parallelization is directly
operated by the load-flow module itself. It is especially very useful for the preventive perimeter, where all
post-contingency flows must be calculated after each contingency.

- Parallelization of topological actions:
The consequences of each topological action must be evaluated independently. These evaluations can therefore be done
in parallel threads

- Parallelization of curative perimeters optimisation:
After the preventive optimisation finished, all the curative perimeters can be optimised independently. 
These optimisations can also be done in parallel, using dedicated threads.
 
- Parallelization of studied situations (timestamps):
When performing optimisation on multiple timestamps, it is also possible to separate the computation of 
the different studied situations on different threads.

---
See also: [Computations parallelism parameters](/parameters.md#multi-threading-parameters)

---

## Search-tree depth

The higher the number of critical branches, critical outages and remedial actions, the longer the calculation. 
For instance, an optimiser will try different combinations of topological actions to solve one constraint 
(RA1+RA2, RA1+RA3, RA2+RA3, RA1+RA2+RA3….). Simulating all combinations to find the best set would require 
more time than finding the first set respecting the criteria.

The complexity here can be defined as the maximal number of consecutive chosen network actions, also called search-tree depth.

For studies where complexity is high but performance is not the main priority, the search-tree depth can be configured 
accordingly in order to assess more combinations.

For operational process (such as capacity calculation/security analysis), where expectation for performance can be high, 
search-tree depth will be configured in order to match the allotted time for the calculation process.

By defining the search-tree depth as a parameter, CASTOR can be used easily in both applications.

Later on, that search tree depth will be configurable per TSO to reflect the maximum consecutive remedial actions allowed
by national operators due to the timing constraint for real-time operations. For now only the total depth is taken into
consideration.

---
See also: [Preventive search stop criterion parameters](/parameters.md#max-preventive-search-tree-depth), [Curative search stop criterion parameters](/parameters.md#max-curative-search-tree-depth)

---

## Network-action-minimum-impact-threshold

In addition to search-tree depth, the network-action-minimum-impact-threshold is also configurable to adjust the 
optimisation with performance expected by the operational process. The network-action-minimum-impact-threshold 
represents the minimal relative/absolute increase of objective function between two consecutive chosen network actions.
 
This minimal relative gain is expressed as a percentage of previous objective function (0.2 for 20%). 
By setting this parameter at a higher value, this could fasten the computation time while focusing only 
on the most efficient remedial actions. This avoids simulating a high number of remedial actions to only have
a small gain on a single CNEC.

This parameter can be defined also through absolute increase :
- minimum impact (in MW or A) for the application of topological remedial action 
- minimum impact (in MW/degree or A/degree) for the use of PST/HVDC

---
See also: [Network actions impact parameters](/parameters.md#network-actions-optimisation-parameters), [Range actions impact parameters](/parameters.md#range-actions-optimisation-parameters)

---

## Fast RAO

In general case, network congestion varies significantly across different transmission lines and system states.
This variation leads to an important observation: certain CNECs consistently maintain positive security margins,
regardless of which RAs are applied.

This insight forms the foundation of Fast RAO: by excluding these consistently secure CNECs from the optimization process  
and focusing only on the critical ones, we can significantly reduce the problem's complexity. Resulting in a lighter optimization
problem that can be solved more quickly without compromising system security.

Read more about Fast RAO [here](../_static/pdf/FastRAO.pdf)

### Algorithm

Fast Rao iteratively builds a set of the **critical** CNECs. Starting with an empty set of CNECs, at each iteration,
we selectively add only the CNECs that are identified as critical for the problem. See the diagram below.

Running multiple RAO on smaller problems is more efficient than performing a single RAO on the
entire, much larger problem at once.

![Current state of the algorithm](../_static/img/FastRAO.png)

> Currently, Fast RAO does not support multi-curative optimization


### How to run an optimization process using FastRao

```java

RaoInput raoInput = RaoInput.build(network, crac).build();
RaoResult raoResult = Rao.find("FastRao").run(raoInput, raoParameters);

// Run FastRAO with a pre defined set of cnecs to consider
FastRao.launchFilteredRao(raoInput, raoParameters, targetEndInstant, consideredCnecs);
```

### Fast Rao Specific Parameters

See [fast rao parameters section](../parameters/implementation-specific-parameters.md#number-of-cnecs-to-add)

