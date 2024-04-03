In the [OpenRAO CRAC](/input-data/crac/json.md), the user can define voltage constraints on network elements.  
These are constraints that monitor that the voltage values on given network elements do not exceed a given
threshold.  
OpenRAO allows modelling these constraints in [VoltageCnec](/input-data/crac/json.md#voltage-cnecs) objects.  
However, modelling the impact of remedial actions on voltage values is highly complex and non-linear. This is why CASTOR
does not inherently support voltage constraints.  
The [VoltageMonitoring](https://github.com/powsybl/powsybl-open-rao/tree/main/monitoring/voltage-monitoring)
package allows monitoring voltage values after a RAO has been run.

![Voltage monitoring](/_static/img/voltage_monitoring.png){.forced-white-background}