Here is a detailed description of how the voltage monitoring algorithm operates:
- Apply optimal preventive remedial actions from RaoResult on the network
- For each of these states, monitor voltages:
- From the CRAC, get the set of states on which VoltageCnecs exist
- (A) For each of these states, generate a state-specific VoltageMonitoringResult:
  - If one of these states is the preventive state, deal with it before handling any of the contingency states. Directly work on the input
    network. That way, applied voltage preventive remedial actions will be taken into account in the contingency states. Go to (B)
  - Else, for all contingency states :
    - Use a new variant of the network
    - apply the contingency on the network
    - From the RaoResult, apply on the network the optimal remedial actions decided by the RAO (ARA+CRA, given PRAs have
      already been applied). Go to (B).
  - (B) Compute the LoadFlow
    - If it diverges, return a VoltageMonitoringResult with the following content for this state, then move on to the next state:
      - the voltages of the VoltageCnecs equal to NaN
      - no applied remedial actions
      - security status UNKNOWN
  - Compute the voltages for all VoltageCnecs (computeVoltages method)
  - Compare the voltages to their thresholds. For the VoltageCnecs that have a voltage overshoot, get the associated remedial actions (with
    an OnVoltageConstraint usage rule containing the VoltageCnec and the state's instant)
    - If the VoltageCnec has no associated RA, log a warning that the voltage constraint cannot be remedied, and move on to the
      next VoltageCnec.
    - For every RA:
      - If the remedial action is not a network action, do not apply it and log a warning. Then move on to the next RA.
      - If the RA is a network action, apply it on the network
  - If you applied any RA, recompute the LoadFlow
    - If it diverges, return a VoltageMonitoringResult with the following content for this state, then move on to the next state:
      - the voltages of the VoltageCnecs equal to what was computed before RA application
      - no applied remedial actions
      - security UNSECURE (variant of HIGH/LOW_VOLTAGE_CONSTRAINT)
  - Re-compute all voltage values
  - Create a state-specific voltage monitoring result with voltages, list of applied remedial actions , and secure flag set to SECURE if there is
    no more overshoot (after re-verifying thresholds)
  - Move on to next state (Go to (A)).
- Assemble all the state-specific results in one overall result, and [update the RAO result object](#the-voltage-monitoring-result)
  
![Voltage monitoring algorithm](/_static/img/voltage_monitoring_algorithm.png){.forced-white-background}
