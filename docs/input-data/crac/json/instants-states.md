The instant is a moment in the chronology of a contingency event. Four instants kinds currently exist in FARAO:
- the **preventive** instant kind occurs before any contingency, and describes the "base-case" situation. A CRAC may 
  contain only one instant of kind preventive.
- the **outage** instant kind occurs just after a contingency happens, in a time too short to allow the activation of any
  curative remedial action. A CRAC may contain only one instant of kind outage.
- the **auto** instant kind occurs after a contingency happens, and spans through the activation of automatic curative 
  remedial actions ("automatons") that are triggered without any human intervention. These automatons are pre-configured 
  to reduce some constraints, even though they can generate constraints elsewhere in the network. A CRAC may contain any 
  number of instants of kind auto.
- the **curative** instant kind occurs after a contingency happens, after enough time that would allow the human activation
  of curative remedial actions. A CRAC may contain any number of instants of kind auto.

> 💡  **NOTE**  
> Flow / angle / voltage limits on critical network elements are usually different for each instant.  
> The outage and auto instant kinds are transitory, therefore less restrictive temporary limits (TATL) can be allowed in
> these instants.  
> On the contrary, the preventive and curative instant kinds are supposed to be a lasting moment during which the grid
> operation is nominal (sometimes thanks to preventive and/or curative remedial actions), so they usually come with
> more restrictive permanent limits (PATL).  
> FARAO allows a different limit setting for different instants on critical network elements (see [CNECs](#cnecs)).
>
> ![patl-vs-tatl](/_static/img/patl-tatl.png)
> (**PRA** = Preventive Remedial Action,
> **ARA** = Automatic Remedial Action,
> **CRA** = Curative Remedial Action)

The FARAO object model includes the notion of "state". A state is either:

- the preventive state: the state of the base-case network, without any contingency, at the preventive instant.
- the combination of a given contingency with instant outage, auto or curative: the state of the network after the said
  contingency, at the given instant (= with more or less delay after this contingency).

The scheme below illustrates these notions of instant and state. It highlights the combinations of the situations which can be described in a CRAC, with a base-case situation, but also variants of this situation occurring at different moments in time after different probable and hypothetical contingencies.

![Instants & states](/_static/img/States_AUTO.png)

States are not directly added to a FARAO CRAC object model; they are implicitly created by business objects
that are described in the following paragraphs ([CNECs](#cnecs) and [remedial actions](#remedial-actions)).

Instants are added one after the other in the CRAC object. 
The first instant must be of kind preventive. 
The second instant must be of kind outage.

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
crac.newInstant("preventive", InstantKind.PREVENTIVE)
    .newInstant("outage", InstantKind.OUTAGE)
    .newInstant("auto", InstantKind.AUTO)
    .newInstant("curative1", InstantKind.CURATIVE)
    .newInstant("curative2", InstantKind.CURATIVE);
~~~
:::
:::{group-tab} JSON file
~~~json
  "instants" : [ {
    "id": "preventive",
    "kind": "PREVENTIVE"
  }, {
    "id": "outage",
    "kind": "OUTAGE"
  }, {
    "id": "auto",
    "kind": "AUTO"
  }, {
  "id": "curative1",
  "kind": "CURATIVE"
  }, {
    "id": "curative2",
    "kind": "CURATIVE"
  } ],
~~~
:::
::::
