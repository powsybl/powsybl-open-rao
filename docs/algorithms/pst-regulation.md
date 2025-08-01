# PST Regulation

## Presentation

At the end of the curative optimization of the RAO, it is possible to perform a PST regulation in case there are still
overloads and if the most limiting element is a FlowCNEC secured by one of the PSTs to be regulated. This set of PSTs to
regulate can be provided in the
[RAO parameters](../parameters/implementation-specific-parameters.md#pst-regulation-parameters). The, for each curative
scenario, if the most limiting element is a FlowCNEC monitoring one of these monitored lines, they are all regulated.

Such monitored lines can be the PSTs themselves or lines connected in series.

> ⚠️ **Remark: Sub-optimality**
>
> Note that there is a risk that PST regulation worsens the minimal margin.

PST regulation is performed directly by the load flow engine provided
by [PowSyBl Open Load Flow](https://powsybl.readthedocs.io/projects/powsybl-open-loadflow/en/stable/index.html). This
basically consists in iterative load flow computations with the tap of one of the regulated PST moved in the resistive
direction each time. As soon as the monitored line is secure, the tap does no longer need to be moved. When all taps are converged,
the iterations stop and the regulation is over.

Regulation results are then merged with the RAO result to provide the final results. If a PST was moved during
regulation, this will appear as a result from the curative optimization.

## Example

Let us consider a simple network of only two nodes and three parallel branches, one of these being a PST. We will denote
these lines as `branch-1`, `branch-2` and `branch-3`; `branch-3` being the PST to regulate. All three branches are
considered to have the same permanent limit (PATL) and the same impedance. The PST is initially on tap -5 and its
neutral tap is 0.

The first step is to fill the RAO parameters with
[PST regulation parameters](../parameters/implementation-specific-parameters.md#pst-regulation-parameters) to indicate
that the PST can be regulated.

```json
{
  "version": "3.1",
  "extensions": {
    "open-rao-search-tree-parameters": {
      "pst-regulation-parameters" : {
        "psts-to-regulate": { "branch-3" : "branch-3" }
      }
    }
  }
}
```

We will consider that there is a contingency on `branch-1` and that when it occurs, there is no way so secure both
`branch-2` and `branch-3` at the same time.

As these two branches have the same impedance and the same permanent limit, the status quo found by the RAO during
curative optimization is to set the PST on its neutral tap (position 0). That way, both branches are overloaded but the
minimal margin is maximized, because no branch is relieved to the detriment of the other.

If the RAO were to stop here, the activation of the PST in the RAO result would look like:

```json
{
  "rangeActionId": "pstRangeAction",
  "initialTap": -5,
  "activatedStates": [
    {
      "instant": "curative",
      "contingencyId": "contingency-branch-1",
      "tap": 0
    }
  ]
}
```

However, the PST on `branch-3` can be regulated. As it is overloaded and is also the most limiting element, regulation
can be performed to try to secure the PST. We will consider that in the network configuration, the PST can be secured
from tap position 7, though it increases the overload on `branch-2`.

Thus, the regulation algorithm will move the tap of the PST from position 0 to position 7 and the final RAO result will
contain this activation result instead of the previous one:

```json
{
  "rangeActionId": "pstRangeAction",
  "initialTap": -5,
  "activatedStates": [
    {
      "instant": "curative",
      "contingencyId": "contingency-branch-1",
      "tap": 7
    }
  ]
}
```

The flows will also be updated and the limiting element is now `branch-2` alone with a greater overload than previously.
