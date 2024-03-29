# What is FARAO

FARAO stands for "*Fully Autonomous Remedial Actions Optimisation*". It is an open-source
toolbox that aims at providing a modular engine for remedial actions optimisation.

FARAO is a [PowSyBl](https://www.powsybl.org) based toolbox that provides software
solutions for power systems coordinated capacity calculation and security analysis projects.
It is published on [GitHub](https://github.com/farao-community) under the [Mozilla Public License 2.0](https://www.mozilla.org/en-US/MPL/2.0/).

# Functional overview

[FARAO toolbox](https://github.com/farao-community/farao-core) functional perimeter is declined following three main development axis:

- Business data modelisation - providing a java modelisation of all the business objects
for power systems coordinated capacity calculation and security analysis projects.
- Computation engines - providing open interface and efficient implementation of standard
tools for supporting capacity calculation projects.
- Exchange standards interface - providing importers and exporters to support ENTSO-E exchange
data interface in projects implementation.    

By extending the [PowSyBl](https://www.powsybl.org) framework, FARAO aims at providing an open, transparent,
and extandable implementation of the tools used for an efficient usage of the electricity transport
and distribution system.


Thanks to our modular architecture, implementing a new process in existing RAO modules or creating a new RAO based on an existing input format is fastened (see concrete example below)

![FARAO modules](/_static/img/modular.png) 


Take benefit from our platform for optimisation with different methodologies already implemented for CACM/SO methodologies. Moreover, it is possible to contribute by adding your features/improvements in existing RAO modules and creating new ones if needed.

![FARAO modules usage](/static/img/modular2.png) 
