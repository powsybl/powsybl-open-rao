# CRAC

```{toctree}
:hidden:
import.md
creation-parameters.md
json.md
fbconstraint.md
cse.md
cim.md
csa.md
creation-context.md
```

The CRAC (***C**ontingency list, **R**emedial **A**ctions and additional **C**onstraints*) file model contains two main categories of objects:
- CNECs, containing the network elements to be monitored after given contingencies,
- and the remedial actions that can be used to secure these elements.

## CNEC

CNEC means *Critical Network Element & Contingency* as defined in the literature. It associates a **network element** with a **state**: it represents a network element at a specific instant in preventive state or after a contingency.

A CNEC is associated to **thresholds**. These thresholds can be of different types according to the considered network element, so that flows, voltage levels and angles can be monitored on these CNECs. Flow thresholds are usually associated to lines, while voltage and angle thresholds are usually associated to nodes.

## Remedial action

FARAO distinguishes two types of remedial actions, **range actions** and **network actions**. The key difference between the two types is that the latter has only two possible states (activated/deactivated), while the former can be activated in different "ways".

### Range action

Range actions are actions on the network with a degree of freedom: the choice of a **set-point** within a given range. 
These actions can be optimised linearly, with some approximations. For more information related to the linear optimisation 
of range actions in FARAO, please refer to the [dedicated documentation page](https://farao-community.github.io/docs/engine/ra-optimisation/linear-rao).

They can be defined on some categories of network elements:
- Phase Shift Transformer (PST),
- HVDC line,
- Production unit.

A range action can also be a counter-trade remedial action corresponding to an exchange from an exporting country to an
importing country.

The determination of the optimal set-point improving a network situation requires some data:
- the current value in a specified network,
- the minimal reachable value according to the specified network – or the maximal authorized variation for a decreasing variation,
- the maximal reachable value according to the specified network – or the maximal authorized variation for an increasing variation,
- the sensitivity of a set-point variation on every CNEC for the specified network.
  
Any 2 or more range actions (of same type) can be aligned into range action "groups" in FARAO, which constrains the RAO to set them to the same set-point at all times.

### Network action

Network actions are any other kind of action on the network, such as the opening/closing of a network element, setting the tap position of a PST to a given set-point, etc. They can only be activated, or remain inactive.
They are used in the [search-tree RAO](https://farao-community.github.io/docs/engine/ra-optimisation/search-tree-rao) only.  
One network action can combine one or multiple elementary actions. These are the types of elementary actions handled in FARAO:

#### Topological actions

It consists in the opening or the closing of one branch or one switch of the network.

#### PST set-point

It consists in the modification of the tap of a PST to a pre-defined target tap.

#### Injection set-point

It consists in the modification of an injection (load, generator, dangling line or shunt compensator) to a pre-defined set-point.

#### Switch pair

It consists in opening a switch and closing another.
