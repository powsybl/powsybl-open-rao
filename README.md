# Open RAO

[![Actions Status](https://github.com/powsybl/powsybl-open-rao/actions/workflows/build_and_test.yml/badge.svg?branch=main)](https://github.com/powsybl/powsybl-open-rao/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=com.powsybl%3Aopen-rao&metric=coverage)](https://sonarcloud.io/component_measures?id=com.powsybl%3Aopen-rao&metric=coverage)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.powsybl%3Aopen-rao&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.powsybl%3Aopen-rao)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Slack](https://img.shields.io/badge/slack-powsybl-blueviolet.svg?logo=slack)](https://join.slack.com/t/powsybl/shared_invite/zt-rzvbuzjk-nxi0boim1RKPS5PjieI0rA)
[![Javadocs](https://www.javadoc.io/badge/com.powsybl/powsybl-open-rao.svg?color=blue)](https://www.javadoc.io/doc/com.powsybl/powsybl-open-rao)
[![ReadTheDocsStatus](https://readthedocs.org/projects/powsybl-openrao/badge/?version=stable)](https://powsybl.readthedocs.io/projects/openrao/en/stable/?badge=stable)

Open RAO (Remedial Action Optimizer) is an open-source toolbox that aims at providing a modular engine for remedial
actions optimisation, part of the Linux Foundation Energy.

**powsybl-open-rao** repository contains the main features of Open RAO.

For detailed information about Open RAO toolbox, please refer to
the [detailed documentation](https://powsybl.readthedocs.io/projects/openrao/en/stable/index.html).

This project and everyone participating in it is governed by
the [PowSyBl Code of Conduct](https://github.com/powsybl/.github/blob/main/CODE_OF_CONDUCT.md).
By participating, you are expected to uphold this code. Please report unacceptable behavior
to [powsybl-tsc@lists.lfenergy.org](mailto:powsybl-tsc@lists.lfenergy.org).

## Getting started

These instructions will get you a copy of the project up and running on your local machine
for development and testing purposes.

### Prerequisites

In order to build **powsybl-open-rao**, you need the following environment available:

- Install JDK *(17 or greater)*,
- Install Maven latest version.

### Installing

Open RAO needs a load flow implementation and a sensitivity analysis implementation at runtime, following the interfaces
of **powsybl-core** which documentation is available [here](https://powsybl.readthedocs.io/projects/powsybl-core). Note
that for obvious reasons, included performances, reliability and transparency, Open RAO
uses [Powsybl Open Load Flow](https://github.com/powsybl/powsybl-open-loadflow) by default, but you can prefer you own
implementation.

### Quick tutorial

If you prefer to get familiar with OpenRAO using a real example, take a look at
this [tutorial](https://powsybl.readthedocs.io/projects/openrao/en/stable/tutorial.html).

## License

This project is licensed under the Mozilla Public License 2.0 - see
the [LICENSE.txt](https://github.com/powsybl/powsybl-open-rao/blob/main/LICENSE.txt) file for details.

## Governance

PowSyBl-OpenRAO falls under the [PowSyBl governance](https://www.powsybl.org/pages/project/governance.html).

## Contributing

Please read the instructions on contributing to the code [here](CONTRIBUTING.md).  
If you want to discuss technical aspects of the code with other developers, are welcome to join the PowSyBl TSC
discussions during one of our [monthly meetings](https://lists.lfenergy.org/g/powsybl-tsc).

## Roadmap

You can find our [roadmap here](https://github.com/powsybl/.github/wiki/Roadmap#powsybl-open-rao)

## Adopters

- [RTE](https://www.rte-france.com/) uses OpenRAO (through GridCapa applications) for everyday operations
- [CORESO](https://www.coreso.eu/) uses OpenRAO (through GridCapa applications) for capacity calculation in the CORE,
  SWE, and IN regions

## Maintainers

The list of PowSyBl-OpenRAO's maintainers can be
found [here](https://github.com/powsybl/.github/blob/main/MAINTAINERS.md#powsybl-open-rao-).
