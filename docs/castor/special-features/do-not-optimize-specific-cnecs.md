# Do not optimize specific CNECs

## Definition

Some FlowCnecs $\mathcal{C} ^{specific}$ can be omitted from the optimization for two reasons:
- Curative CNECs from TSOs not sharing any CRAs are not taken into account when looking for the minimum margin in the 
objective function (only during CRA optimization), as long as the applied CRAs do not decrease these CNECs' margins 
(compared to their margins before applying any CRA).  
In other words, other TSOs' CRAs shall not be used to relieve CNECs from TSOs not sharing any CRA (unless if the former 
TSOs' CRAs actually have a negative impact on the latter TSOs' CNECs).  
This feature is needed in the CORE region capacity calculation process.
(see [do-not-optimize-curative-cnecs-for-tsos-without-cras](/parameters/parameters.md#do-not-optimize-curative-cnecs-for-tsos-without-cras))
- CNECs configured as in series with a PST (see [do-not-optimize-cnec-secured-by-its-pst](/parameters/parameters.md#do-not-optimize-cnec-secured-by-its-pst))
are not taken into account when looking for the minimum margin in the objective function, as long as these CNECs have a 
positive margin or an overload that can be fully relieved by the action of this PST. In other words, they will only be
taken into account if the PST has too few tap positions left (before hitting its limit) to relieve their overload.  
This feature is needed for specific network elements in the SWE region (for both capacity calculation and coordinated security analysis).  

## Implementation

Both these situations can be modelled with constraints activating a binary variable when it becomes necessary to loosen the following constraint:
- Cnecs in $\mathcal{C} ^{specific}$ must not see their margins worsened
- Cnecs in $\mathcal{C} ^{specific}$ are secure or can be secured if their associated PST is shifted

This binary variable forces cnecs in $\mathcal{C} ^{specific}$  to be taken into account in the minimum margin objective.
These constraints are modelled in the RAO under the linear problem as:
- [Modelling un-optimized CNECs (CRAs)](https://farao-community.github.io/docs/castor/linear-optimisation-problem/unoptimized-cnec-filler-cra)
- [Modelling un-optimized CNECs (PSTs)](https://farao-community.github.io/docs/castor/linear-optimisation-problem/unoptimized-cnec-filler-pst)