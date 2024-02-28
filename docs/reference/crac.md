## Introduction {#introduction}

:::{dropdown} Dropdown Title
:open:
Dropdown content
:::

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

::::{tab-set}
:::{tab-item} JAVA creation API
:sync: tab1
The creation of a small CRAC is for instance made in this test class of farao-core repository:  
[example on GitHub](https://github.com/powsybl/powsybl-open-rao/blob/main/data/crac/crac-impl/src/test/java/com/powsybl/openrao/data/cracimpl/utils/CommonCracCreation.java)
:::
:::{tab-item} JSON file
:sync: tab2
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
FARAO relies on the [PowSyBl framework](https://www.powsybl.org/), and FARAO's CRAC relies on some elements of
[PowSyBl's network model](https://www.powsybl.org/pages/documentation/grid/model/): the so-called network elements.

The network elements can be:
- the elements that are disconnected from the network by a contingency,
- the power lines that are identified as critical network elements in the CRAC,
- the PSTs that are identified by the CRAC as remedial actions,
- the switches that can be used as topological remedial actions...

Network elements are referenced in the CRAC with:

🔴⭐ an id
: The id **must** match the [unique id of a PowSyBl object](https://www.powsybl.org/pages/documentation/grid/model/#introduction).
When using a network element, the applications relying on the CRAC will look in the Network for an identifiable element
with the very same id: if this element cannot be found, it can lead to an error in the application. When building the CRAC,
one must therefore make sure that only the network elements whose IDs can be understood by the PowSyBl iidm Network are
added to the CRAC. This is particularly important to keep in mind if you want to [develop your own CRAC importer](import#new-formats).

⚪ a name
: The name shouldn't be absolutely required by the application relying on the CRAC object, but it could be useful to
make the CRAC or some outputs of the application more readable for humans.

::::{tab-set}
:::{tab-item} JAVA creation API
:sync: tab1
Network elements are never built on their own in the CRAC object: they are always a component of a larger business object,
and are added implicitly to the CRAC when the business object is added (e.g. a contingency, a CNEC, or a remedial action).
When building a business object, one of the two following methods can be used to add a network element to it (using an
ID only, or an ID and a name).
~~~java
(...).withNetworkElement("network_element_id")
(...).withNetworkElement("network_element_id", "network_element_name")
~~~
These methods will be depicted in the following examples on this page.
:::
:::{tab-item} JSON file
:sync: tab2
Network elements' IDs are defined within the objects that contain them. Examples are given later in this page,
for contingencies, CNECs and remedial actions. Moreover, the internal Json CRAC format contains an index of network
element names, in which network elements that have a name are listed, with their name and their ID.
~~~json
"networkElementsNamePerId" : {
  "network_element_id_1" : "[BE-FR] Interconnection between Belgium and France",
  "network_element_id_4" : "[DE-DE] Internal PST in Germany"
},
~~~
:::
::::

## Contingencies {#contingencies}
A CRAC object must define "critical contingencies" (or "critical outages", or "CO", or "N-k"...).  
The denomination chosen within the FARAO internal format is **"Contingency"**.

A contingency is the representation of an incident on the network (i.e. a cut line or a group/transformer failure, etc.).
In FARAO, it is modelled by the loss of one or several network elements. Usually we have either a one-network-element-loss
called "N-1", or a two-network-element-loss called "N-2".

Examples:
- N-1: The loss of one generator
- N-2: The loss of two parallel power lines

A contingency is a probable event that can put the grid at risk. Therefore, contingencies must
be considered when operating the electrical transmission / distribution system.

In FARAO, contingencies are defined in the following way:

::::{tab-set}
:::{tab-item} JAVA creation API
:sync: tab1
~~~java
crac.newContingency()
    .withId("CO_0001")
    .withName("N-1 on generator")
    .withNetworkElement("powsybl_generator_id", "my_generators_name")
    .add();

crac.newContingency()
    .withId("CO_0002")
    .withName("N-2 on electrical lines")
    .withNetworkElement("powsybl_electrical_line_1_id")
    .withNetworkElement("powsybl_electrical_line_2_id")
    .add();
~~~
:::
:::{tab-item} JSON file
:sync: tab2
~~~json
"contingencies" : [{
    "id" : "CO_0001",
    "name" : "N-1 on generator",
    "networkElementsIds" : [ "powsybl_generator_id" ]
}, {
    "id" : "CO_0002",
    "name" : "N-1 electrical lines",
    "networkElementsIds" : [ "powsybl_electrical_line_1_id", "powsybl_electrical_line_2_id" ]
}],
~~~
:::
:::{tab-item} Object fields
:sync: tab3
🔴⭐ **identifier**  
⚪ **name**  
⚪ **network elements**: list of 0 to N network elements  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔴 **network element id**: must be the id of a PowSyBl network identifiable  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ⚪ **network element names**: names are optional, they can be used to make the CRAC
more understandable from a business viewpoint, but applications relying on the CRAC do not necessarily need them.  
:::
::::

> 💡  **NOTE**  
> The network elements currently handled by FARAO's contingencies are: internal lines, interconnections, transformers,
> PSTs, generators, HVDCs, bus-bar sections, and dangling lines.

## Instants and States {#instants-states}
TODO  

## CNECs {#cnecs}
TODO  

## Remedial actions and usages rules {#remedial-actions}
TODO  

## Network Actions {#network-actions}
TODO  

## Range Actions {#range-actions}
TODO  
