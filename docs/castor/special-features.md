# Special features

```{toctree}
:hidden:
special-features/loop-flows.md
special-features/mnecs.md
special-features/do-not-optimize-specific-cnecs.md
```

Several special features can be included in the RAO optimisation:
- limiting loop-flows on cross-zonal cnecs 
- monitoring (rather than optimising) the flow on certain FlowCnecs (MNECs)
- not taking into account certain FlowCnecs - under specific conditions - for the computation of the minimum margin in the objective function

The objective function value is separated into two categories:
- the **functional** cost reflects the business value of the objective (i.e. the cost associated to the minimum margin and the business penalties on usage of remedial actions)
- the **virtual** cost reflects the violation of some special constraints. The first two special features (loop-flows & MNECs) are handled through the virtual cost.