# RAO Technical Logs

## Introduction

These logs detail the steps of the search-tree algorithm. Activate them if you know the tool pretty well, and in case
you want to follow the algorithm in detail.  
Most logs contain normal information, but some may contain errors or warnings (possible cases are listed below).

Package name:

~~~java
com.farao_community.farao.commons.logs.TechnicalLogs
~~~

## Possible error cases

| Module               | Name                   | Label                                                                                                                                          | Description                                  | Consequence                                                                                                            |
|----------------------|------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| ra-optimisation      | OR-Tools load error    | "Native library jniortools could not be loaded. You can ignore this message if it is not needed."                                              | FARAO could not load native library OR-Tools | FARAO cannot call a solver to optimize the MIP (range actions). The RAO will only be able to optimize network actions. |
| commons              | Randomize string error | "There should at least be one try to generate randomized string." <br>or<br> "Failed to create a randomized string with prefix '{}' in {} {}." | FARAO failed to generate a random string     | Depends on the calling method (a FaraoException is thrown)                                                             |
| sensitivity-analysis | Sensitivity unit error | "The Sensitivity Provider should contain at least Megawatt or Ampere unit"                                                                     | The sensitivity is neither in MW nor in A    | Depends on the calling method (a SensitivityAnalysisException is thrown)                                               |

## Possible warnings

| Module               | Name                            | Label                                                                                     | Description                                                                                                                                    | Consequence                                                                   |
|----------------------|---------------------------------|-------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| ra-optimisaition     | Sensitivity computation failure | "Systematic sensitivity computation failed on {fallback / default} mode: {error message}" | Self-explanatory                                                                                                                               | Depends on the calling method (the SensitivityAnalysisException is re-thrown) |
| sensitivity-analysis | Sensitivity unit error          | "Unit {unit} cannot be handled by the sensitivity provider as it is not a flow unit"      | Happens if sensitivity is asked for in an unit other than MW or A                                                                              | Unpredictable                                                                 |
| ra-optimisation      | Leaf error                      | "A computation thread was interrupted"                                                    | In search tree, if a computation thread on a leaf has been interrupted for a reason.                                                           | The RAO skips the leaf in error.                                              | 
| ra-optimisation      | Solver interruption             | "The solver was interrupted. A feasible solution has been produced."                      | The solver has been interrupted while optimizing the MIP (for example if max time has been reached), but had time to find a feasible solution. | The best feasible solution found by the solver is used.                       |
| ra-optimisation      | Missing GLSK                    | "No GLSK found for CountryEICode {country EIC}"                                           | The GLSK is missing for a given country                                                                                                        | The PTDF is considered equal to zero                                          |
| sensitivity-analysis | PTDF ampere error               | "PtdfSensitivity provider currently only handle Megawatt unit"                            | This happens when PTDF sensitivity factors are asked for in a unit other than megawatts                                                        | The warning is printed and the factors are computed in MW                     |