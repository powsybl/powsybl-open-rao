# Getting started

```{toctree}
:hidden:
getting-started/tutorial.md
```
Open RAO (Remedial Action Optimizer) is an open-source toolbox that aims at providing a modular engine for remedial actions optimisation, part of the Linux Foundation Energy.

## Useful links

### Java

- [GitHub](https://github.com/powsybl/powsybl-open-rao)
- [![Slack](https://img.shields.io/badge/slack-powsybl-blueviolet.svg?logo=slack)](https://join.slack.com/t/powsybl/shared_invite/zt-rzvbuzjk-nxi0boim1RKPS5PjieI0rA)

A java tutorial is available [here](getting-started/tutorial.md).

### Python

Based on PyPowsybl, OpenRao can be used with Python.
- [GitHub](https://github.com/powsybl/pypowsybl-notebooks)
- [PyPowsybl documentation](https://powsybl.readthedocs.io/projects/pypowsybl/en/stable/)

A Jupyter notebook is defined [here](https://github.com/powsybl/pypowsybl-notebooks/blob/notebook_rao_v1.11/open_rao.ipynb).

## Start using OpenRao

- Mandatory [input data](input-data.md) are:
  - a [network](input-data/network.md)
  - a [crac](input-data/crac.md). Crac objects are detailed [here](input-data/crac/json.md)
- You will also need to define a set of [rao parameters](parameters.md) 
- Outputs:
  - Step-by-step calculation is detailed in the [logs](output-data/rao-logs.md). Learn more about how logs are configured [here](output-data/rao-logs/example.md)
  - Rao results are defined in a json file, described [here](output-data/rao-result.md)

## Understanding OpenRao

- The following pages describe how OpenRao works:
  - [CASTOR, the RAO algorithm](algorithms/castor.md)
  - [Optimisation steps](algorithms/castor/rao-steps.md)
  - [Monitoring module](algorithms/monitoring.md)

- For a fine understanding of the constraints defined in the Linear Problem, see this [section](algorithms/castor/linear-problem)


## Fine tuning your implementation of OpenRao

Check [Combining performance and complexity](algorithms/castor/performance.md) to adapt your rao implementation.