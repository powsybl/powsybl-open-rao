# Parameters

```{toctree}
:hidden:
parameters/business-parameters.md
parameters/implementation-specific-parameters.md
```

## Introduction

The RAO parameters allow tuning the RAO.

It contains **business parameters** (see [business-parameters](parameters/business-parameters.md)) which allow
to choose the business objective function of the RAO (maximize min margin, get a positive margin, ...), 
to activate/deactivate optional business features, etc.

It also contains **extensions** which are **implementation specific parameters** 
(see [implementation-specific-parameters](parameters/implementation-specific-parameters.md)), in particular the open 
rao search tree extension. These extensions allow to fine-tune the search algorithm, improve performance and/or 
quality of results.

RAO parameters can be constructed using:
- The Java API (see [source code](https://github.com/powsybl/powsybl-open-rao/blob/main/ra-optimisation/rao-api/src/main/java/com/powsybl/openrao/raoapi/parameters/RaoParameters.java))
- A JSON file (see [example](#examples))
- A PowSyBl configuration file (see [example](#examples))


## Examples

Examples of rao parameters with business and implementation specific parameters

> ⚠️  **NOTE**  
> The following examples in json and yaml are not equivalent

::::{tabs}
:::{group-tab} JSON
~~~json
{
  "version" : "3.3",
  "objective-function" : {
    "type" : "SECURE_FLOW",
    "unit" : "A",
    "enforce-curative-security" : true
  },
  "range-actions-optimization" : {
    "pst-ra-min-impact-threshold" : 0.01,
    "hvdc-ra-min-impact-threshold" : 0.001,
    "injection-ra-min-impact-threshold" : 0.001
  },
  "topological-actions-optimization" : {
    "relative-minimum-impact-threshold" : 0.0,
    "absolute-minimum-impact-threshold" : 1.0
  },
  "not-optimized-cnecs" : {
    "do-not-optimize-curative-cnecs-for-tsos-without-cras" : false
  },
  "extensions" : {
    "open-rao-search-tree-parameters": {
      "objective-function" : {
        "curative-min-obj-improvement" : 0.0
      },
      "range-actions-optimization" : {
        "max-mip-iterations" : 5,
        "pst-sensitivity-threshold" : 0.0,
        "pst-model" : "APPROXIMATED_INTEGERS",
        "hvdc-sensitivity-threshold" : 0.0,
        "injection-ra-sensitivity-threshold" : 0.0,
        "linear-optimization-solver" : {
          "solver" : "CBC",
          "relative-mip-gap" : 0.001,
          "solver-specific-parameters" : "THREADS 10 MAXTIME 3000"
        }
      },
      "topological-actions-optimization" : {
        "max-preventive-search-tree-depth" : 2,
        "max-curative-search-tree-depth" : 2,
        "predefined-combinations" : [ "na1 + na2", "na4 + na5 + na6"],
        "skip-actions-far-from-most-limiting-element" : false,
        "max-number-of-boundaries-for-skipping-actions" : 2
      },
      "multi-threading" : {
        "available-cpus" : 4
      },
      "second-preventive-rao" : {
        "execution-condition" : "POSSIBLE_CURATIVE_IMPROVEMENT",
        "hint-from-first-preventive-rao" : true
      },
      "load-flow-and-sensitivity-computation" : {
        "load-flow-provider" : "OpenLoadFlow",
        "sensitivity-provider" : "OpenLoadFlow",
        "sensitivity-failure-over-cost" : 0.0,
        "sensitivity-parameters" : {
          "version" : "1.0",
          "load-flow-parameters" : {
            "version" : "1.9",
            "voltageInitMode" : "DC_VALUES",
            "transformerVoltageControlOn" : false,
            "phaseShifterRegulationOn" : true,
            "noGeneratorReactiveLimits" : false,
            "twtSplitShuntAdmittance" : false,
            "shuntCompensatorVoltageControlOn" : false,
            "readSlackBus" : true,
            "writeSlackBus" : false,
            "dc" : false,
            "distributedSlack" : true,
            "balanceType" : "PROPORTIONAL_TO_GENERATION_P",
            "dcUseTransformerRatio" : true,
            "countriesToBalance" : [ "TR", "BE", "SI", "CH", "AL", "ES", "SK", "BA", "RO", "PT", "DE", "AT", "FR", "CZ", "ME", "NL", "PL", "GR", "IT", "UA", "HU", "BG", "MK", "HR", "RS" ],
            "connectedComponentMode" : "MAIN",
            "hvdcAcEmulation" : true,
            "dcPowerFactor" : 1.0,
            "extensions" : {
              "open-load-flow-parameters" : {
                "plausibleActivePowerLimit" : 10000.0,
                "minPlausibleTargetVoltage" : 0.5,
                "maxPlausibleTargetVoltage" : 1.5,
                "maxNewtonRaphsonIterations" : 100,
                "newtonRaphsonConvEpsPerEq" : 1.0E-3,
                "slackBusSelectionMode" : "MOST_MESHED",
                "slackBusesIds" : [ ],
                "throwsExceptionInCaseOfSlackDistributionFailure" : false,
                "lowImpedanceBranchMode" : "REPLACE_BY_ZERO_IMPEDANCE_LINE",
                "loadPowerFactorConstant" : false,
                "addRatioToLinesWithDifferentNominalVoltageAtBothEnds" : true,
                "slackBusPMaxMismatch" : 1.0,
                "voltagePerReactivePowerControl" : false,
                "voltageInitModeOverride" : "NONE",
                "transformerVoltageControlMode" : "WITH_GENERATOR_VOLTAGE_CONTROL",
                "minRealisticVoltage" : 0.5,
                "maxRealisticVoltage" : 1.5,
                "reactiveRangeCheckMode" : "MAX"
              }
            }
          }
        }
      },
      "loop-flow-parameters" : {
        "acceptable-increase" : 10.0,
        "ptdf-approximation" : "FIXED_PTDF",
        "constraint-adjustment-coefficient" : 10.0,
        "violation-cost" : 10.0,
        "countries" : [ "FR", "ES", "PT" ]
      },
      "mnec-parameters" : {
        "acceptable-margin-decrease" : 50.0,
        "violation-cost" : 10.0,
        "constraint-adjustment-coefficient" : 1.0
      },
      "relative-margins-parameters" : {
        "ptdf-boundaries" : [ "{FR}-{BE}", "{FR}-{DE}", "{BE}-{NL}", "{NL}-{DE}", "{DE}-{PL}", "{DE}-{CZ}", "{DE}-{AT}", "{PL}-{CZ}", "{PL}-{SK}", "{CZ}-{SK}", "{CZ}-{AT}", "{AT}-{HU}", "{AT}-{SI}", "{SI}-{HR}", "{SK}-{HU}", "{HU}-{RO}", "{HU}-{HR}", "{BE}-{22Y201903144---9}-{DE}+{22Y201903145---4}" ],
        "ptdf-approximation" : "FIXED_PTDF",
        "ptdf-sum-lower-bound" : 0.01
      }
    }
  }
}
~~~
:::
:::{group-tab} iTools
Based on PowSyBl's [configuration mechanism](inv:powsyblcore:std:doc#user/configuration/index).
~~~yaml
rao-objective-function:
  type: SECURE_FLOW
  unit: AMPERE

rao-range-actions-optimization:
  pst-ra-min-impact-threshold: 0.01
  
search-tree-range-actions-optimization:
  max-mip-iterations: 5
  pst-sensitivity-threshold: 0.01
  pst-model: APPROXIMATED_INTEGERS

search-tree-linear-optimization-solver:
  solver: CBC

rao-topological-actions-optimization:
  relative-minimum-impact-threshold: 0.0
  absolute-minimum-impact-threshold: 2.0

search-tree-topological-actions-optimization:
  max-preventive-search-tree-depth: 3
  max-curative-search-tree-depth: 3
  predefined-combinations: [ "{na1}+{na2}", "{na3}+{na4}+{na5}" ]
  relative-minimum-impact-threshold: 0.0
  absolute-minimum-impact-threshold: 2.0

search-tree-multi-threading:
  available-cpus: 4

search-tree-second-preventive-rao:
  execution-condition: POSSIBLE_CURATIVE_IMPROVEMENT
  hint-from-first-preventive-rao: true

rao-not-optimized-cnecs:
  do-not-optimize-curative-cnecs-for-tsos-without-cras: false

search-tree-load-flow-and-sensitivity-computation:
  load-flow-provider: OpenLoadFlow
  sensitivity-provider: OpenLoadFlow

load-flow-default-parameters:
  voltageInitMode: DC_VALUES
  balanceType: PROPORTIONAL_TO_GENERATION_P
  countriesToBalance: AL,AT,BA,BE,BG,CH,CZ,DE,ES,FR,GR,HR,HU,IT,ME,MK,NL,PL,PT,RO,RS,SI,SK,UA
  update parameters to version 2.0 phaseShifterRegulationOn: true

open-loadflow-default-parameters:
  minPlausibleTargetVoltage: 0.5
  maxPlausibleTargetVoltage: 1.5
  plausibleActivePowerLimit: 10000
  newtonRaphsonConvEpsPerEq : 1.0E-2
~~~
:::
::::
