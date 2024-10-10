You can easily call the monitoring module using the [JAVA API](https://github.com/powsybl/powsybl-open-rao/blob/main/monitoring/src/main/java/com/powsybl/openrao/monitoring/Monitoring.java):
1. Build a MonitoringInput object using the MonitoringInputBuilder:

For angle monitoring :
~~~java
MonitoringInput monitoringInput = new MonitoringInput.MonitoringInputBuilder().withCrac(crac).withNetwork(network).withRaoResult(raoResult).withPhysicalParameter(PhysicalParameter.ANGLE).withScalableZonalData(scalableZonalData).build();
~~~

For voltage monitoring :
~~~java
MonitoringInput monitoringInput = new MonitoringInput.MonitoringInputBuilder().withCrac(crac).withNetwork(network).withRaoResult(raoResult).withPhysicalParameter(PhysicalParameter.VOLTAGE).build();
~~~

With:
- PhysicalParameter.VOLTAGE to start voltage monitoring and PhysicalParameter.ANGLE to start angle monitoring
- crac: the CRAC object used for the RAO, and containing [VoltageCnecs](/input-data/crac/json.md#voltage-cnecs)/ [AngleCnecs](/input-data/crac/json.md#angle-cnecs) to be monitored.
- network: the network to be monitored.
- raoResult: the [RaoResult](/output-data/rao-result.md) object containing selected remedial actions (that shall
  be applied on the network before monitoring angle/voltage values)
- scalableZonalData for redispatching in case of angle monitoring

2. Run the monitoring algorithm using the constructed object's following method:

~~~java
public RaoResult runAngleAndUpdateRaoResult(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel, monitoringInput)

~~~
With:
- loadFlowProvider: the name of the load-flow computer to use. This should refer to a [PowSyBl load flow provider implementation](inv:powsyblcore:*:*#simulation/loadflow/index)
- loadFlowParameters: the PowSyBl LoadFlowParameters object to configure load-flow computation.
- numberOfLoadFlowsInParallel: the number of contingencies to monitor in parallel, allowing a maximum utilization of
  your computing resources (set it to your number of available CPUs).
- monitoringInput as built above in step 1.

Here is a complete example while augmenting rao result by voltage monitoring result then augmenting the outcome by angle monitoring result:

~~~java
Crac crac = ...
Network network = ...
RaoResult raoResult = Rao.find(...).run(...)
LoadFlowParameters loadFlowParameters = ..
        
MonitoringInput voltageMonitoringInput = new MonitoringInput.MonitoringInputBuilder().withCrac(crac).withNetwork(network).withRaoResult(raoResult).withPhysicalParameter(PhysicalParameter.VOLTAGE).build();
RaoResult raoResultWithVoltageMonitoring = Monitoring.runVoltageAndUpdateRaoResult("OpenLoadFlow", loadFlowParameters, 2, voltageMonitoringInput);

MonitoringInput angleMonitoringInput = new MonitoringInput.MonitoringInputBuilder().withCrac(crac).withNetwork(network).withRaoResult(raoResultWithVoltageMonitoring).withPhysicalParameter(PhysicalParameter.ANGLE).withScalableZonalData(scalableZonalData).build();
RaoResult raoResultWithVoltageAndAngleMonitoring = Monitoring.runAngleAndUpdateRaoResult("OpenLoadFlow", loadFlowParameters, 2, angleMonitoringInput);
~~~
