You can easily call the angle monitoring module using the [JAVA API](https://github.com/powsybl/powsybl-open-rao/blob/main/monitoring/angle-monitoring/src/main/java/com/powsybl/openrao/monitoring/anglemonitoring/AngleMonitoring.java):
1. Build an AngleMonitoring object using one of two constructors. If you have a CIM GLSK document at hand:

~~~java
public AngleMonitoring(Crac crac, Network network, RaoResult raoResult, CimGlskDocument cimGlskDocument, OffsetDateTime glskOffsetDateTime)
~~~

With:
- crac: the CRAC object used for the RAO, and containing [AngleCnecs](/input-data/crac/json.md#angle-cnecs) to be monitored.
- network: the network to be monitored.
- raoResult: the [RaoResult](/output-data/rao-result/rao-result-json.md) object containing selected remedial actions (that shall
  be applied on the network before monitoring angle values)
- cimGlskDocument: the [CIM GLSK document](/input-data/glsk/glsk-cim.md) that will allow proper application of re-dispatch
  remedial actions.
- glskOffsetDateTime: the timestamp for which the computation is made, as it is necessary to correctly read relevant
  values from the CIM GLSK file.

Otherwise, you can still run the angle monitoring algorithm, which will automatically generate a 
**[proportional-to-target-power GLSK](/input-data/glsk.md#proportional-to-target-power-glsk)** 
for its internal functioning (ie in order to re-dispatch lost generation when generator-tripping remedial actions are activated):  

~~~java
public AngleMonitoring(Crac crac, Network network, RaoResult raoResult, Set<Country> glskCountries)
~~~
With:
- crac: the CRAC object used for the RAO, and containing [AngleCnecs](/input-data/crac/json.md#angle-cnecs) to be monitored.
- network: the network to be monitored.
- raoResult: the [RaoResult](/output-data/rao-result/rao-result-json.md) object containing selected remedial actions (that shall
  be applied on the network before monitoring angle values)
- glskCountries: the countries which might need re-dispatching, for which the algorithm should generate a proportional GLSK  

2. Run the monitoring algorithm using the constructed object's following method:

~~~java
public RaoResult runAndUpdateRaoResult(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel)
~~~

With:
- loadFlowProvider: the name of the load-flow computer to use. This should refer to a [PowSyBl load flow provider implementation](https://www.powsybl.org/pages/documentation/simulation/powerflow/)
- loadFlowParameters: the PowSyBl LoadFlowParameters object to configure load-flow computation.
- numberOfLoadFlowsInParallel: the number of contingencies to monitor in parallel, allowing a maximum utilization of
  your computing resources (set it to your number of available CPUs).

> 💡  **NOTE**  
> For now, only CIM GLSK format is supported by the angle monitoring module

Here is a (almost) complete example:

~~~java
Crac crac = ...
Network network = ...
RaoResult raoResult = Rao.find(...).run(...)
CimGlskDocument glsk = ...
LoadFlowParameters loadFlowParameters = ...
RaoResult raoResultWithAngleMonitoring = new AngleMonitoring(crac, network, raoResult, ...).runAndUpdateRaoResult("OpenLoadFlow", loadFlowParameters, 2);
~~~
