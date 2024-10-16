In the [OpenRAO CRAC](/input-data/crac/json.md), the user can define angle or/and voltage constraints on network elements.  
These are constraints that ensure that the angle/voltage values on given network elements do not exceed a given threshold.  
OpenRAO allows modelling these constraints in [VoltageCnec](/input-data/crac/json.md#voltage-cnecs) and [AngleCnec](/input-data/crac/json.md#angle-cnecs) objects.  
However, modelling the impact of remedial actions on angle/voltage values is highly complex and non-linear. This is why CASTOR
does not inherently support  angle/voltage constraints.  
The [Monitoring](https://github.com/powsybl/powsybl-open-rao/tree/main/monitoring)
package allows monitoring angle/voltage values after a RAO has been run.

![Monitoring](/_static/img/monitoring.png){.forced-white-background}