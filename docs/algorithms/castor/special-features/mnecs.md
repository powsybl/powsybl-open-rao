# MNECs

## Definition

MNECs (Monitored Network Elements) are monitored rather than optimized,
see [Optimized and monitored CNECs](../../../input-data/crac/json.md#optimized-and-monitored-cnecs)
Their margin must be kept positive or above its initial value.

## Implementation

MNEC constraints are modeled in the RAO under the linear problem
as [MNEC constraints](../linear-problem/special-features/mnec-filler.md).
