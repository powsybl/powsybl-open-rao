You can easily call the voltage monitoring module using the [JAVA API](https://github.com/powsybl/powsybl-open-rao/blob/main/monitoring/voltage-monitoring/src/main/java/com/powsybl/openrao/monitoring/voltagemonitoring/VoltageMonitoring.java):
1. Build a VoltageMonitoring object using:

~~~java
public VoltageMonitoring(Crac crac, Network network, RaoResult raoResult)
~~~

With:
- crac: the CRAC object used for the RAO, and containing [VoltageCnecs](/input-data/crac/json.md#voltage-cnecs) to be monitored.
- network: the network to be monitored.
- raoResult: the [RaoResult](/output-data/rao-result.md) object containing selected remedial actions (that shall
  be applied on the network before monitoring voltage values)

2. Run the monitoring algorithm using the constructed object's following method:

~~~java
public RaoResult runAndUpdateRaoResult(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel)

~~~
With:
- loadFlowProvider: the name of the load-flow computer to use. This should refer to a [PowSyBl load flow provider implementation](inv:powsyblcore:*:*#simulation/loadflow/index)
- loadFlowParameters: the PowSyBl LoadFlowParameters object to configure load-flow computation.
- numberOfLoadFlowsInParallel: the number of contingencies to monitor in parallel, allowing a maximum utilization of
  your computing resources (set it to your number of available CPUs).

Here is a complete example:

~~~java
Crac crac = ...
Network network = ...
RaoResult raoResult = Rao.find(...).run(...)
LoadFlowParameters loadFlowParameters = ...
RaoResult raoResultWithVoltageMonitoring = new VoltageMonitoring(crac, network, raoResult).runAndUpdateRaoResult("OpenLoadFlow", loadFlowParameters, 2);
~~~
