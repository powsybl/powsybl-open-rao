# FARAO - core
[![Actions Status](https://github.com/farao-community/farao-core/workflows/CI/badge.svg)](https://github.com/farao-community/farao-core/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=com.farao-community.farao%3Afarao-core&metric=coverage)](https://sonarcloud.io/component_measures?id=com.farao-community.farao%3Afarao-core&metric=coverage)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.farao-community.farao%3Afarao-core&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.farao-community.farao%3Afarao-core)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Join the community on Spectrum](https://withspectrum.github.io/badge/badge.svg)](https://spectrum.chat/farao-community)

![FARAO horizontal logo](https://raw.githubusercontent.com/farao-community/.github/master/logo-farao-horizontal.svg?sanitize=true)  

FARAO stands for *Fully Autonomous Remedial Actions Optimisation*. It is an open-source toolbox that aims at providing a modular engine for remedial actions optimisation.

**farao-core** repository contains the main features of FARAO. It provides a command line tool to interact with these features.

For detailed information about FARAO toolbox, please refer to the [detailed documentation](https://farao-community.github.io/docs/).

This project and everyone participating in it is governed by the [FARAO Code of Conduct](https://github.com/farao-community/.github/blob/master/CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.
Please report unacceptable behavior to [contact@farao-community.com](mailto:contact@farao-community.com).

## Getting started

These instructions will get you a copy of the project up and running on your local machine
for development and testing purposes.

### Prerequisites

In order to build **farao-core**, you need the following environment available:
  - Install JDK *(11 or greater)*,
  - Install Maven latest version.

### Installing

Before installing **farao-core**, you need to install OR-Tools, a software suite for optimisation that is used by FARAO remedial actions optimiser.

Please refer to [OR-tools website](https://developers.google.com/optimization/install/download) for installation instructions.

To build **farao-core**, enter on the command line:

```
$> git clone https://github.com/farao-community/farao-core.git
$> cd farao-core
$> ./install.sh
```

FARAO also needs a loadflow engine and a sensitivity calculation engine at runtime.
You may use any engine integrated in [PowSyBl framework](https://www.powsybl.org/).

Hades2 tool from RTE is available as a freeware for demonstration purpose.
For more information about how to get and install Hades2 loadflow and sensitivity computation, please refer to the
[dedicated documentation](https://rte-france.github.io/hades2/index.html).

### Running

In order for FARAO to run without error, you will need to configure your *itools* platform. *itools* is a command line interface
provided by PowSyBl framework. 

Two options are available:
1.  First, you can use the one provided by FARAO. It is saved in the *etc* directory of the installation, and is called *config.yml*.
You just have to copy-paste it in **$HOME/.itools** directory. You can also use one of the provided network post-processor scripts 
saved in the *etc* directory by copy-pasting in the same directory and adding these lines to your *config.yml* file :

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
 
