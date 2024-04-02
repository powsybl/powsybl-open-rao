Some CRAC files can define angle constraints on network elements.  
These are constraints that monitor that the current phase shift between two network elements does not exceed a given
threshold.  
OpenRAO allows modelling these constraints in [AngleCnec](/input-data/crac/json.md#angle-cnecs) objects.  
However, modelling the impact of remedial actions on angle values is highly complex and non-linear. This is why CASTOR
does not inherently support angle constraints.  
The [AngleMonitoring](https://github.com/powsybl/powsybl-open-rao/tree/main/monitoring/angle-monitoring)
package allows monitoring angle values after a RAO has been run, and activating re-dispatch remedial actions in case of
constrained elements.

![forceWhiteBackground](/_static/img/angle_monitoring.png)