In the logs, the start and end of different steps are logged:  
- Start and end of the voltage monitoring algorithm
- Start and end of the monitoring of each state (preventive or post-contingency)
- Start and end of each load-flow computation  

Also, the following information is logged:
- Applied remedial actions to relieve voltage constraints
- At the end of each state monitoring, the list of remaining voltage constraints
- At the end of the monitoring algorithm, the list of remaining voltage constraints

**Example 1:**  
In this example, a constraint exists in preventive (it cannot be solved because FARAO cannot implement PRAs to solve 
voltage constraints), and another one in curative (but implementing the CRA does not solve it).
~~~
INFO  c.f.f.commons.logs.RaoBusinessLogs - ----- Voltage monitoring [start]
INFO  c.f.f.commons.logs.RaoBusinessLogs - -- Monitoring voltages at state "preventive" [start]
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [end]
WARN  c.f.f.commons.logs.RaoBusinessWarns - VoltageCnec vcPrev is constrained in preventive state, it cannot be secured.
INFO  c.f.f.commons.logs.RaoBusinessLogs - Some voltage CNECs are not secure:
INFO  c.f.f.commons.logs.RaoBusinessLogs - Network element VL1 at state preventive has a voltage of 400 - 400 kV.
INFO  c.f.f.commons.logs.RaoBusinessLogs - -- Monitoring voltages at state "preventive" [end]
INFO  c.f.farao.commons.logs.TechnicalLogs - Using base network 'phaseShifter' on variant 'InitialState'
INFO  c.f.f.commons.logs.RaoBusinessLogs - -- Monitoring voltages at state "co - curative" [start]
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [end]
INFO  c.f.f.commons.logs.RaoBusinessLogs - Applying the following remedial action(s) in order to reduce constraints on CNEC "vc": Open L1 - 2
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [end]
INFO  c.f.f.commons.logs.RaoBusinessLogs - Some voltage CNECs are not secure:
INFO  c.f.f.commons.logs.RaoBusinessLogs - Network element VL1 at state co - curative has a voltage of 400 - 400 kV.
INFO  c.f.f.commons.logs.RaoBusinessLogs - -- Monitoring voltages at state "co - curative" [end]
INFO  c.f.f.commons.logs.RaoBusinessLogs - Some voltage CNECs are not secure:
INFO  c.f.f.commons.logs.RaoBusinessLogs - Network element VL1 at state co - curative has a voltage of 400 - 400 kV.
INFO  c.f.f.commons.logs.RaoBusinessLogs - Network element VL1 at state preventive has a voltage of 400 - 400 kV.
INFO  c.f.f.commons.logs.RaoBusinessLogs - ----- Voltage monitoring [end]
~~~

**Example 2:**  
In this example, a curative constraint (after contingency "co") is solved by a CRA.  
~~~
INFO  c.f.f.commons.logs.RaoBusinessLogs - ----- Voltage monitoring [start]
INFO  c.f.farao.commons.logs.TechnicalLogs - Using base network 'phaseShifter' on variant 'InitialState'
INFO  c.f.f.commons.logs.RaoBusinessLogs - -- Monitoring voltages at state "co - curative" [start]
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [end]
INFO  c.f.f.commons.logs.RaoBusinessLogs - Applying the following remedial action(s) in order to reduce constraints on CNEC "vc": Close L1 - 1
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [end]
INFO  c.f.f.commons.logs.RaoBusinessLogs - All voltage CNECs are secure.
INFO  c.f.f.commons.logs.RaoBusinessLogs - -- Monitoring voltages at state "co - curative" [end]
INFO  c.f.f.commons.logs.RaoBusinessLogs - All voltage CNECs are secure.
INFO  c.f.f.commons.logs.RaoBusinessLogs - ----- Voltage monitoring [end]
~~~
