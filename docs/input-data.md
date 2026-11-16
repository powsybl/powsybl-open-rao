# Input data

```{toctree}
:hidden:
input-data/network.md
input-data/glsk.md
input-data/crac.md
input-data/specific-input-data.md
```

## Network

The network data model used by the OpenRAO toolbox is the PowSyBl IIDM format.
To get detailed information about the network model, please refer to the [dedicated documentation](https://powsybl.readthedocs.io/projects/powsybl-core/en/stable/grid_model/)
on the PowSyBl documentation website.

Network exchange formats supported (as part of the PowSyBl project):
- CGMES
- UCTE
- XIIDM
- ...

## CRAC

CRAC (for "***C**ontingency list, **R**emedial **A**ctions and additional **C**onstraints*") objects are dedicated to
defining the security domain of the network model. They define contingencies to take into account in business processes,
constraints to monitor, and remedial actions available to relieve potential active constraints.

Based on our experience in different capacity calculation regions, formats for IGM/CGM (either UCTE/CGMES) or
CRAC/CBCORA (Critical Branch/Critical Outages/Remedial Actions) are not yet harmonized across Europe.  
To limit dependencies on input/output formats, OpenRAO uses its own [CRAC format](input-data/crac/json.md) in order to be easily
adaptable to any process.

Please refer to the [dedicated CRAC section](input-data/crac.md) for more information.

CRAC exchange formats currently supported by OpenRAO:
- [JSON CRAC](input-data/crac/json.md) (OpenRAO-specific)
- [FlowBasedConstraint CRAC](input-data/crac/fbconstraint.md) (used in CORE region)
- [CSE CRAC](input-data/crac/cse.md) (used in CSE region)
- [CIM CRAC](input-data/crac/cim.md) (used in SWE region)

## GLSK

GLSK (for "*Generation and Load Shift Keys*") objects are dedicated to defining scaling strategies to simulate injection
modifications on the network model.

Please refer to the [dedicated documentation page](input-data/glsk.md) to get more information about the GLSK data model, 
as well as to the dedicated [PowSyBl repository](https://github.com/powsybl/powsybl-entsoe).

GLSK exchange formats supported (as part of the PowSyBl project):
- [CIM GLSK](https://powsybl.readthedocs.io/projects/entsoe/en/latest/glsk/glsk-cim.html)
- [CSE GLSK](https://powsybl.readthedocs.io/projects/entsoe/en/latest/glsk/glsk-cse.html)
- [UCTE GLSK](https://powsybl.readthedocs.io/projects/entsoe/en/latest/glsk/glsk-ucte.html)



