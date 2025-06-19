# Do not optimize specific CNECs

## Definition

Some FlowCnecs $\mathcal{C} ^{specific}$ can be omitted from the optimization for the following reason:
- Curative CNECs from TSOs not sharing any CRAs are not taken into account when looking for the minimum margin in the 
objective function (only during CRA optimization), as long as the applied CRAs do not decrease these CNECs' margins 
(compared to their margins before applying any CRA).  
In other words, other TSOs' CRAs shall not be used to relieve CNECs from TSOs not sharing any CRA (unless if the former 
TSOs' CRAs actually have a negative impact on the latter TSOs' CNECs).  
This feature is needed in the CORE region capacity calculation process.
(see [do-not-optimize-curative-cnecs-for-tsos-without-cras](/parameters.md#do-not-optimize-curative-cnecs-for-tsos-without-cras))  

## Implementation

This situation can be modelled with constraints activating a binary variable when it becomes necessary to loosen the following constraint:
- Cnecs in $\mathcal{C} ^{specific}$ must not see their margins worsened

This binary variable forces cnecs in $\mathcal{C} ^{specific}$  to be taken into account in the minimum margin objective.
These constraints are modelled in the RAO under the linear problem as described [here](/castor/linear-problem/unoptimized-cnec-filler-cra.md).