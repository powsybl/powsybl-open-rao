Here is a detailed description of how the monitoring algorithm operates:
- Apply optimal preventive remedial actions from RaoResult on the network
- From the CRAC, get the set of states on which AngleCnecs/VoltageCnecs exist
- For each of these states, monitor angles/voltages:
  - Use a new copy of the network
  - If the state is not preventive,
    - apply the contingency on the network
    - from the RaoResult, apply on the network the optimal remedial actions decided by the RAO (automatic and curative)
  - Compute load-flow
    - If it diverges, return the following content for this state, then move on to the next state:
      - If PhysicalParameter is ANGLE : the angles of the angle CNECs equal to NaN 
      - If PhysicalParameter is Voltage : the max and min values of the voltage CNECs equal to NaN
      - no applied remedial actions
      - status FAILURE
  - Compute the angles/voltages for all angle/voltage CNECs **(1)** :
      - If PhysicalParameter is ANGLE: angle values are the maximum phase difference between the 2 voltage levels 
        Angle in degrees = 180 / pi * (max(angle on buses of exporting voltage level) - min(angle on buses of importing voltage level))
      - If PhysicalParameter is VOLTAGE: voltage values are the min and max voltages on the voltage level buses
  - Compare the angles/voltages to their thresholds.
  - Compute and save each cnec security status (SECURE, HIGH_CONSTRAINT, LOW_CONSTRAINT, HIGH_AND_LOW_CONSTRAINTS, FAILURE) depending on the position of the angle/voltage value(s) regarding the Angle/Voltage Thresholds
  - For the Angle/Voltage Cnecs that have an angle/voltage overshoot (negative margin), get the associated remedial actions 
    - If the Angle/Voltage Cnec has no associated RA, then the constraint cannot be remedied: move on to the next Angle/Voltage Cnec.
    - If the state is preventive, do not apply any PRA (it would be incoherent with the RAO results). Move on to the next state.
    - For every RA:
      - If the remedial action is not a network action, do not apply it (if it's a RangeAction, we cannot know which set-point to use). Move on to the next RA.
      - If the RA is a network action, apply it on the network **(2)**.
  - If  PhysicalParameter is ANGLE and there is any injection-set-point RA applied, create and apply the re-dispatch that shall compensate the loss of generation / load **(3)**:
    - The amount of power to re-dispatch is the net sum (generation - load) of power generations & loads affected by the RAs, before changing the set-points
    - Exclude from the re-dispatching all the generators & loads that were modified by an injection-set-point RA, since they should not be affected
  - If no RA was applied, keep Cnec results from **(4)** and move on to the next state
  - If any RA was applied, recompute the load-flow
      - If it diverges, return the following content for this state, then move on to the next state:
          - the angles of the Angle/Voltage Cnecs equal to what was computed in step **(1)**
          - no applied remedial actions
          - security FAILURE
      - If it converges Re-compute all angle/voltage values and create a Cnec result, with updated angles/voltages, list of applied remedial actions in **(2)**, and new cnec status after comparing the new angle/voltage value(s) to their thresholds
- Assemble all the Angle/Voltage Cnec results in one overall result

![Monitoring algorithm](/_static/img/monitoring-algorithm.png){.forced-white-background}
