# Fast RAO

In general case, network congestion varies significantly across different transmission lines and system states.
This variation leads to an important observation: certain CNECs consistently maintain positive security margins,
regardless of which RAs are applied. 

This insight forms the foundation of Fast RAO: by excluding these consistently secure CNECs from the optimization process  
and focusing only on the critical ones, we can significantly reduce the problem's complexity. Resulting in a lighter optimization
problem that can be solved more quickly without compromising system security.

Read more about Fast RAO [here](../_static/pdf/FastRAO.pdf)
## Algorithm

Fast Rao iteratively builds a set of the **critical** CNECs. Starting with an empty set of CNECs, at each iteration, 
we selectively add only the CNECs that are identified as critical for the problem. See the diagram below.

Running multiple RAO on smaller problems is more efficient than performing a single RAO on the
entire, much larger problem at once.

![Current state of the algorithm](../_static/img/FastRAO.png)

> Currently, Fast RAO does not support multi-curative optimization


## Parameters 

See [fast rao parameters section](../parameters/implementation-specific-parameters.md#number-of-cnecs-to-add)
