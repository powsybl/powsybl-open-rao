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

::::{tabs}
:::{group-tab} JAVA creation API
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
:::{group-tab} JSON file
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