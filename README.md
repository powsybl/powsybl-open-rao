# FARAO - core

[![Build Status](https://travis-ci.com/farao-community/farao-core.svg?branch=master)](https://travis-ci.com/farao-community/farao-core)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=com.farao-community.farao%3Afarao-core&metric=coverage)](https://sonarcloud.io/component_measures?id=com.farao-community.farao%3Afarao-core&metric=coverage)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.farao-community.farao%3Afarao-core&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.farao-community.farao%3Afarao-core)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Join the community on Spectrum](https://withspectrum.github.io/badge/badge.svg)](https://spectrum.chat/farao-community)

For detailed information about FARAO toolbox, please refer to the [documentation website](https://farao-community.github.io/docs/)

## Requirements
In order to build **farao-core**, you need the following environment available:
  - Install JDK *(1.8 or greater)*
  - Install Maven latest version

## Install
To build farao-core, just do the following:

```
$> git clone https://github.com/farao-community/farao-core.git
$> cd farao-core
$> ./install.sh
```

FARAO also needs a loadflow engine and a sensitivity calculation engine.

Hades2 tool from RTE is available as a freeware for demonstration purpose.
For more information about how to get and install Hades2 loadflow, please refer to the
[dedicated documentation](https://rte-france.github.io/hades2/index.html)

## Configure your itools platform
In order for farao to run without error, you will need to configure your itools platform.

Two options are available:
1.  First, you can use the one provided by FARAO. It is saved in the *etc* directory of the installation, and is called *config.yml*.
You just have to copy-paste it in a **$HOME/.itools** directory. 

2.  Expert users can also adapt it to their own needs.

## Using itools
```bash
cd <install-prefix>/bin
./itools help
```
