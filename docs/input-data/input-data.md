# Network

The network data model used by FARAO toolbox is the PowSyBl IIDM format.
To get detailed information about the network model, please refer to [dedicated documentation](https://www.powsybl.org/pages/documentation/index.html#grid-model)
on PowSyBl website.

Network exchange formats supported (as part of PowSyBl project):
- CGMES
- UCTE
- XIIDM
- ...

# CRAC

CRAC (for "***C**ontingency list, **R**emedial **A**ctions and additional **C**onstraints*") are objects dedicated to
define security domain of the network object. They define contingencies to take into account in business process,
constraints to monitor and remedial actions available to get rid of potential active constraints.

Based on our experience on different capacity calculation regions, the format for IGM/CGM (either UCTE/CGMES) or
CRAC/CBCORA (Critical Branch/Critical Outages/Remedial Actions) are not yet harmonized over Europe.  
To limit dependencies with input/output formats, FARAO uses its own [CRAC format](/crac/json) in order to be easily
adaptable for any process.

Please refer to the [dedicated CRAC section](/crac/index.rst) for more information.

CRAC exchange formats actually supported by FARAO:
- [JSON CRAC](/crac/json) (FARAO-specific)
- [FlowBasedConstraint CRAC](/crac/fbconstraint) (used in CORE region)
- [CSE CRAC](/crac/cse) (used in CSE region)
- [CIM CRAC](/crac/cim) (used in SWE region)

# GLSK

GLSK (for "*Generation and Load Shift Keys*") are objects dedicated to define scaling strategies to simulate injections
modification on network model.

Please refer to the [dedicated documentation page](/input-data/glsk/index.rst) to get more information about GLSK data model, 
as well as to the dedicated [PowSyBl repository](https://github.com/powsybl/powsybl-entsoe).

GLSK exchange formats supported (as part of PowSyBl project):
- [CIM GLSK](/input-data/glsk/glsk-cim)
- [CSE GLSK](/input-data/glsk/glsk-cse)
- [UCTE GLSK](/input-data/glsk/glsk-ucte)



