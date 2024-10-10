In the logs, the start and end of different steps are logged:  
- Start and end of the 'angle/voltage' monitoring algorithm
- Start and end of the monitoring of each state (preventive or post-contingency)
- Start and end of each load-flow computation  

Also, the following information is logged:
- Applied remedial actions to relieve 'angle/voltage' constraints
- At the end of each state monitoring, the list of remaining 'angle/voltage' constraints
- At the end of the monitoring algorithm, the list of remaining 'angle/voltage constraints

**Example 1 - ANGLE Monitoring:**  
In this example, a curative constraint (after contingency "Co-1") induces the implementation of a CRA, but this CRA is
not enough to solve the constraint.
~~~
INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- ANGLE monitoring [start]
INFO  c.p.o.commons.logs.TechnicalLogs - Using base network 'urn:uuid:4f4a3f29-6892-49ea-bfc1-92051973c799+urn:uuid:9e7050a8-960b-4e1a-8e34-7f56bc2b2a7b' on variant 'InitialState'
INFO  c.p.o.commons.logs.RaoBusinessLogs - -- 'ANGLE' Monitoring at state 'Co-1 - curative' [start]
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Applied the following remedial action(s) in order to reduce constraints on CNEC "AngleCnec1": RA-1
INFO  c.p.o.commons.logs.RaoBusinessLogs - Redispatching 108.0 MW in BE [start]
WARN  c.p.o.commons.logs.RaoBusinessWarns - Redispatching failed: asked=108.0 MW, applied=0.0 MW
INFO  c.p.o.commons.logs.RaoBusinessLogs - Redispatching 108.0 MW in BE [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Redispatching 150.0 MW in NL [start]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Redispatching 150.0 MW in NL [end]
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - -- 'ANGLE' Monitoring at state 'Co-1 - curative' [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Some ANGLE Cnecs are not secure:
INFO  c.p.o.commons.logs.RaoBusinessLogs - AngleCnec AngleCnec1 (with importing network element _d77b61ef-61aa-4b22-95f6-b56ca080788d and exporting network element _8d8a82ba-b5b0-4e94-861a-192af055f2b8) at state Co-1 - curative has an angle of 5°.
INFO  c.p.o.commons.logs.RaoBusinessLogs - -- 'ANGLE' Monitoring at state 'Co-2 - curative' [start]
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - -- 'ANGLE' Monitoring at state 'Co-2 - curative' [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - All ANGLE Cnecs are secure.
INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- ANGLE monitoring [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Some ANGLE Cnecs are not secure:
INFO  c.p.o.commons.logs.RaoBusinessLogs - AngleCnec AngleCnec1 (with importing network element _d77b61ef-61aa-4b22-95f6-b56ca080788d and exporting network element _8d8a82ba-b5b0-4e94-861a-192af055f2b8) at state Co-1 - curative has an angle of 5°.

~~~

**Example 2 - VOLTAGE Monitoring:**  
In this example, a constraint exists in preventive (it cannot be solved because OpenRAO cannot implement PRAs to solve
voltage constraints), and another one in curative (but implementing the CRA does not solve it).
~~~
INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- VOLTAGE monitoring [start]
INFO  c.p.o.commons.logs.RaoBusinessLogs - -- 'VOLTAGE' Monitoring at state 'preventive' [start]
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [end]
WARN  c.p.o.commons.logs.RaoBusinessWarns - VOLTAGE Cnec vcPrev is constrained in preventive state, it cannot be secured.
INFO  c.p.o.commons.logs.RaoBusinessLogs - -- 'VOLTAGE' Monitoring at state 'preventive' [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Some VOLTAGE Cnecs are not secure:
INFO  c.p.o.commons.logs.RaoBusinessLogs - Network element VL1 at state preventive has a min voltage of 400 kV and a max voltage of 400 kV.
INFO  c.p.o.commons.logs.TechnicalLogs - Using base network 'phaseShifter' on variant 'InitialState'
INFO  c.p.o.commons.logs.RaoBusinessLogs - -- 'VOLTAGE' Monitoring at state 'co - curative' [start]
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Applied the following remedial action(s) in order to reduce constraints on CNEC "vc": Open L1 - 2
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - -- 'VOLTAGE' Monitoring at state 'co - curative' [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Some VOLTAGE Cnecs are not secure:
INFO  c.p.o.commons.logs.RaoBusinessLogs - Network element VL1 at state co - curative has a min voltage of 400 kV and a max voltage of 400 kV.
INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- VOLTAGE monitoring [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Some VOLTAGE Cnecs are not secure:
INFO  c.p.o.commons.logs.RaoBusinessLogs - Network element VL1 at state co - curative has a min voltage of 400 kV and a max voltage of 400 kV.
INFO  c.p.o.commons.logs.RaoBusinessLogs - Network element VL1 at state preventive has a min voltage of 400 kV and a max voltage of 400 kV.
~~~

**Example 3 - VOLTAGE Monitoring:**  
In this example, a curative constraint (after contingency "co") is solved by a CRA.
~~~
INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- VOLTAGE monitoring [start]
INFO  c.p.o.commons.logs.TechnicalLogs - Using base network 'phaseShifter' on variant 'InitialState'
INFO  c.p.o.commons.logs.RaoBusinessLogs - -- 'VOLTAGE' Monitoring at state 'co - curative' [start]
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - Applied the following remedial action(s) in order to reduce constraints on CNEC "vc": Close L1 - 1
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.p.o.commons.logs.TechnicalLogs - Load-flow computation [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - -- 'VOLTAGE' Monitoring at state 'co - curative' [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - All VOLTAGE Cnecs are secure.
INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- VOLTAGE monitoring [end]
INFO  c.p.o.commons.logs.RaoBusinessLogs - All VOLTAGE Cnecs are secure.
~~~
