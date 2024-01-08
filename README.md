# Open RAO
[![Actions Status](https://github.com/farao-community/farao-core/workflows/CI/badge.svg)](https://github.com/farao-community/farao-core/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=com.farao-community.farao%3Afarao-core&metric=coverage)](https://sonarcloud.io/component_measures?id=com.farao-community.farao%3Afarao-core&metric=coverage)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.farao-community.farao%3Afarao-core&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.farao-community.farao%3Afarao-core)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Join the community on Spectrum](https://withspectrum.github.io/badge/badge.svg)](https://spectrum.chat/farao-community)

![Open RAO horizontal logo](https://raw.githubusercontent.com/farao-community/.github/master/logo-farao-horizontal.svg?sanitize=true)  

Open RAO (Remedial Action Optimizer) is an open-source toolbox that aims at providing a modular engine for remedial actions optimisation.

**open-rao** repository contains the main features of Open RAO.

For detailed information about Open RAO toolbox, please refer to the [detailed documentation](  https://farao-community.github.io/docs/).

This project and everyone participating in it is governed by the [Open RAO Code of Conduct](https://github.com/farao-community/.github/blob/master/CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.
Please report unacceptable behavior to [contact@farao-community.com](mailto:contact@farao-community.com).

## Getting started

These instructions will get you a copy of the project up and running on your local machine
for development and testing purposes.

### Prerequisites

In order to build **open-rao**, you need the following environment available:
  - Install JDK *(17 or greater)*,
  - Install Maven latest version.

### Installing

Before installing **open-rao**, you need to install OR-Tools, a software suite for optimisation that is used by Open RAO.

Please refer to [OR-tools website](https://developers.google.com/optimization/install/download) for installation instructions.

Open RAO also needs a loadflow engine and a sensitivity calculation engine at runtime.
You may use any engine integrated in [PowSyBl framework](https://www.powsybl.org/).

Hades2 tool from RTE is available as a freeware for demonstration purpose.
For more information about how to get and install Hades2 loadflow and sensitivity computation, please refer to the
[dedicated documentation](https://rte-france.github.io/hades2/index.html).

### Running

In order for Open RAO to run without error, you will need to configure your *itools* platform. *itools* is a command line interface
provided by PowSyBl framework. 

Two options are available:
1.  First, you can use the one provided by Open RAO. It is saved in the *etc* directory of the installation, and is called *config.yml*.
You just have to copy-paste it in **$HOME/.itools** directory. You can also use one of the provided network post-processor scripts 
saved in the *etc* directory by copy-pasting them in the same directory and adding these lines to your *config.yml* file :

```$yml
import:
  postProcessors: groovyScript

groovy-post-processor:
    script: $HOME/.itools/core-cc-ucte-postProcessor.groovy

``` 

2.  Expert users can also adapt it to their own needs.

For using *itools*, enter on the command line:
 
```bash
cd <install-prefix>/bin
./itools help
```

For more information about *itools*, do not hesitate to visit [PowSyBl documentation](https://www.powsybl.org/docs/).

## License

This project is licensed under the Mozilla Public License 2.0 - see the [LICENSE.txt](https://github.com/farao-community/farao-core/blob/master/LICENSE.txt) file for details.
 
