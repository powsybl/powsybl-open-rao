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

::::{tabs}
:::{group-tab} JAVA creation API
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
:::{group-tab} JSON file
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
:::{group-tab} Object fields
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