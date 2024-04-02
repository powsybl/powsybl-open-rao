In the logs, the start and end of different steps are logged:  
- Start and end of the angle monitoring algorithm
- Start and end of the monitoring of each state (preventive or post-contingency)
- Start and end of each load-flow computation  

Also, the following information is logged:
- Applied remedial actions to relieve angle constraints
- At the end of each state monitoring, the list of remaining angle constraints
- At the end of the monitoring algorithm, the list of remaining angle constraints

**Example 1:**  
In this example, a curative constraint (after contingency "Co-1") induces the implementation of a CRA, but this CRA is 
not enough to solve the constraint.

~~~
INFO  c.f.f.commons.logs.RaoBusinessLogs - ----- Angle monitoring [start]
INFO  c.f.farao.commons.logs.TechnicalLogs - Using base network 'urn:uuid:4f4a3f29-6892-49ea-bfc1-92051973c799' on variant 'InitialState'
INFO  c.f.f.commons.logs.RaoBusinessLogs - -- Monitoring angles at state "Co-1 - curative" [start]
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [end]
INFO  c.f.f.commons.logs.RaoBusinessLogs - Applying the following remedial action(s) in order to reduce constraints on CNEC "AngleCnec1": RA-1
INFO  c.f.f.commons.logs.RaoBusinessLogs - Redispatching 108.0 MW in BE [start]
INFO  c.f.f.commons.logs.RaoBusinessLogs - Redispatching 108.0 MW in BE [end]
INFO  c.f.f.commons.logs.RaoBusinessLogs - Redispatching 150.0 MW in NL [start]
INFO  c.f.f.commons.logs.RaoBusinessLogs - Redispatching 150.0 MW in NL [end]
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [end]
INFO  c.f.f.commons.logs.RaoBusinessLogs - Some AngleCnecs are not secure:
INFO  c.f.f.commons.logs.RaoBusinessLogs - AngleCnec AngleCnec1 (with importing network element _d77b61ef-61aa-4b22-95f6-b56ca080788d and exporting network element _8d8a82ba-b5b0-4e94-861a-192af055f2b8) at state Co-1 - curative has an angle of 5°.
INFO  c.f.f.commons.logs.RaoBusinessLogs - -- Monitoring angles at state "Co-1 - curative" [end]
INFO  c.f.f.commons.logs.RaoBusinessLogs - -- Monitoring angles at state "Co-2 - curative" [start]
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [start]
INFO  c.f.farao.commons.logs.TechnicalLogs - Load-flow computation [end]
INFO  c.f.f.commons.logs.RaoBusinessLogs - All AngleCnecs are secure.
INFO  c.f.f.commons.logs.RaoBusinessLogs - -- Monitoring angles at state "Co-2 - curative" [end]
INFO  c.f.f.commons.logs.RaoBusinessLogs - Some AngleCnecs are not secure:
INFO  c.f.f.commons.logs.RaoBusinessLogs - AngleCnec AngleCnec1 (with importing network element _d77b61ef-61aa-4b22-95f6-b56ca080788d and exporting network element _8d8a82ba-b5b0-4e94-861a-192af055f2b8) at state Co-1 - curative has an angle of 5°.
INFO  c.f.f.commons.logs.RaoBusinessLogs - ----- Angle monitoring [end]
~~~
