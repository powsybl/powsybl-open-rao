## Introduction {#introduction}

The name CRAC is a standard denomination defined by the ENTSO-E which means: **C**ontingency list, **R**emedial 
**A**ctions, and additional **C**onstraints.

In other words, it gathers the following information:
- critical outages,
- critical network elements,
- and remedial actions.

It is typically used in European coordinated processes. It enables, for a given geographical region, to define the 
network elements that might be critical after specific outages, and the remedial actions that might help to manage them.  

**A CRAC object model has been designed in FARAO** in order to store all the aforementioned information. This page aims to present:
- the content and the organization of the data present in the FARAO CRAC object model,
- how a FARAO CRAC object can be built,
  - using the java API,
  - or using the FARAO internal Json CRAC format.

Note that other pages of this documentation describe how the FARAO CRAC object model can be built with other standard 
CRAC formats, such as the [FlowBasedConstraint](fbconstraint) format, the [CSE](cse) Format, and the [CIM](cim) format.

## Full CRAC examples {#full-crac-examples}
Example of complete CRACs are given below

::::{tabs}
:::{group-tab} JAVA creation API
The creation of a small CRAC is for instance made in this test class of farao-core repository:  
[example on GitHub](https://github.com/powsybl/powsybl-open-rao/blob/main/data/crac/crac-impl/src/test/java/com/powsybl/openrao/data/cracimpl/utils/CommonCracCreation.java)
:::
:::{group-tab} JSON file
An example of a small CRAC in the json internal format of FARAO is given below:  
[example on GitHub](https://github.com/powsybl/powsybl-open-rao/blob/main/ra-optimisation/search-tree-rao/src/test/resources/crac/small-crac-with-network-actions.json)
:::
::::
  
The following paragraphs of this page explain, step by step, the content of these examples.

> **KEY**  
> 🔴 marks a **mandatory** field  
> ⚪ marks an **optional** field  
> 🔵 marks a field that can be **mandatory in some cases**  
> ⭐ marks a field that must be **unique** in the CRAC  

## Network elements {#network-elements}
```{include} network-elements.md
```

## Contingencies {#contingencies}
```{include} contingencies.md
```

## Instants and States {#instants-states}
```{include} instants-states.md
```

## CNECs {#cnecs}
TODO  

## Remedial actions and usages rules {#remedial-actions}
TODO  

## Network Actions {#network-actions}
TODO  

## Range Actions {#range-actions}
TODO  
