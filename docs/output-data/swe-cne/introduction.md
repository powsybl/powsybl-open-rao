The SWE CNE file is the standard RAO output file for the SWE CC process.  
The [FARAO toolbox](https://github.com/powsybl/powsybl-open-rao/tree/main/data/result-exporter/swe-cne-exporter)
allows exporting RAO results in a SWE CNE file using an [internal CRAC](/input-data/crac/json.md), a network, an internal [RAO result](/output-data/rao-result/rao-result-json.md) 
(containing [angle results](/castor/angle-monitoring/angle-monitoring.md) if the CRAC contains [Angle CNECs](/input-data/crac/json.md#angle-cnecs)), 
a [CimCracCreationContext](/input-data/crac/creation-context.md#cim), and a [RAO parameters](/parameters/parameters.md).

![SWE CNE](/_static/img/swe-cne.png)