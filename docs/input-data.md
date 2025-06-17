# Input data

```{toctree}
:hidden:
input-data/network.md
input-data/glsk.md
input-data/crac.md
input-data/specific-input-data.md
```

## Network

The network data model used by OpenRAO toolbox is the PowSyBl IIDM format.
To get detailed information about the network model, please refer to [dedicated documentation](inv:powsyblcore:*:*#grid_model/index)
on PowSyBl documentation website.

Network exchange formats supported (as part of PowSyBl project):
- CGMES
- UCTE
- XIIDM
- ...

## CRAC

CRAC (for "***C**ontingency list, **R**emedial **A**ctions and additional **C**onstraints*") are objects dedicated to
define security domain of the network object. They define contingencies to take into account in business process,
constraints to monitor and remedial actions available to get rid of potential active constraints.

Based on our experience on different capacity calculation regions, the format for IGM/CGM (either UCTE/CGMES) or
CRAC/CBCORA (Critical Branch/Critical Outages/Remedial Actions) are not yet harmonized over Europe.  
To limit dependencies with input/output formats, OpenRAO uses its own [CRAC format](/input-data/crac/json) in order to be easily
adaptable for any process.

Please refer to the [dedicated CRAC section](/input-data/crac.md) for more information.

CRAC exchange formats actually supported by OpenRAO:
- [JSON CRAC](/input-data/crac/json.md) (OpenRAO-specific)
- [FlowBasedConstraint CRAC](/input-data//crac/fbconstraint) (used in CORE region)
- [CSE CRAC](/input-data//crac/cse) (used in CSE region)
- [CIM CRAC](/input-data//crac/cim) (used in SWE region)

## GLSK

GLSK (for "*Generation and Load Shift Keys*") are objects dedicated to define scaling strategies to simulate injections
modification on network model.

Please refer to the [dedicated documentation page](/input-data/glsk.md) to get more information about GLSK data model, 
as well as to the dedicated [PowSyBl repository](https://github.com/powsybl/powsybl-entsoe).

GLSK exchange formats supported (as part of PowSyBl project):
- [CIM GLSK](https://powsybl.readthedocs.io/projects/entsoe/en/latest/glsk/glsk-cim.html)
- [CSE GLSK](https://powsybl.readthedocs.io/projects/entsoe/en/latest/glsk/glsk-cse.html)
- [UCTE GLSK](https://powsybl.readthedocs.io/projects/entsoe/en/latest/glsk/glsk-ucte.html)



