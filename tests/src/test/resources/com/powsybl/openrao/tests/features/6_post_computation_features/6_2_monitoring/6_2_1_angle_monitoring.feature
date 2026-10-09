# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 6.2.1: Angle Monitoring
  This feature covers angle monitoring, which is parametrised in the CRAC file.

  @fast @ac @rao @angle-monitoring @max-min-margin
  Scenario: 6.2.1.0: Basic Angle Monitoring with no action available
  Simple angle monitoring case with two curative AngleCNECs defined for two different contingencies.
  The CRAC file does not contain any FlowCNEC but the computation is still performed.
    Given network file is "epic94/MicroGrid.zip"
    Given crac file is "epic94/crac_angle_monitoring_6_2_1_0.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given monitoring glsk file is "epic94/GlskB45MicroGridTest.xml"
    When I launch rao at "2021-04-02 07:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    When I launch angle monitoring at "2021-04-02 07:00" on 1 threads
    Then the angle monitoring result is "HIGH_CONSTRAINT"
    Then the angle of CNEC "AngleCnec1" should be 3.79 at "curative"
    Then the angle margin of CNEC "AngleCnec1" should be -0.79 at "curative"
    Then the angle of CNEC "AngleCnec2" should be -19.33 at "curative"
    Then the angle margin of CNEC "AngleCnec2" should be 66.33 at "curative"

  @fast @ac @rao @angle-monitoring @max-min-margin
  Scenario: 6.2.1.1: Basic Angle Monitoring with one network action available
  Same case as 6.2.1.0, but this time we have a network action available: RA-1 -> action that shut down two generators (one belgian and one dutch).
  The actions is applied by default. The network action worsen the situation (-0.79° margin to -1.77°) but the action is still applied.
    Given network file is "epic94/MicroGrid.zip"
    Given crac file is "epic94/crac_angle_monitoring_6_2_1_1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given monitoring glsk file is "epic94/GlskB45MicroGridTest.xml"
    When I launch rao at "2021-04-02 07:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    When I launch angle monitoring at "2021-04-02 07:00" on 1 threads
    Then the network action "RA-1" is used after "Co-1" at "curative" during monitoring
    Then the angle monitoring result is "HIGH_CONSTRAINT"
    Then the angle of CNEC "AngleCnec1" should be 4.77 at "curative"
    Then the angle margin of CNEC "AngleCnec1" should be -1.77 at "curative"
    Then the angle of CNEC "AngleCnec2" should be -19.33 at "curative"
    Then the angle margin of CNEC "AngleCnec2" should be 66.33 at "curative"

  @fast @ac @rao @angle-monitoring
  Scenario: 6.2.1.2: Two network actions that act on the same generator but with the same setpoint
  Same case as 6.2.1.1, but this time two identical network actions are available for AngleCnec1.
  We should apply both action but we have the exact same result as 6.2.1.1. We should not redispatch
  the power twice.
    Given network file is "epic94/MicroGrid.zip"
    Given crac file is "epic94/crac_angle_monitoring_6_2_1_2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given monitoring glsk file is "epic94/GlskB45MicroGridTest.xml"
    When I launch rao at "2021-04-02 07:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    When I launch angle monitoring at "2021-04-02 07:00" on 1 threads
    Then the angle monitoring result is "HIGH_CONSTRAINT"
    Then 2 network actions are used after "Co-1" at "curative" during monitoring
    Then the network action "RA-1" is used after "Co-1" at "curative" during monitoring
    Then the network action "RA-2" is used after "Co-1" at "curative" during monitoring
    # Same angle and margin value as 6.2.1.2
    Then the angle of CNEC "AngleCnec1" should be 4.77 at "curative"
    Then the angle margin of CNEC "AngleCnec1" should be -1.77 at "curative"
    # No actions available for this AngleCnec
    Then the angle of CNEC "AngleCnec2" should be -19.33 at "curative"
    Then the angle margin of CNEC "AngleCnec2" should be 66.33 at "curative"

  @fast @ac @rao @angle-monitoring
  Scenario: 6.2.1.3: Two network actions that act on the same generator but with different setpoints
    Same case as 6.2.1.2, but this time the setpoints of the two injection actions are different. The monitoring fails
    and we get the same result as 6.2.1.0. -> no network actions applied + status set to FAILURE
    Given network file is "epic94/MicroGrid.zip"
    Given crac file is "epic94/crac_angle_monitoring_6_2_1_3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given monitoring glsk file is "epic94/GlskB45MicroGridTest.xml"
    When I launch rao at "2021-04-02 07:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    When I launch angle monitoring at "2021-04-02 07:00" on 1 threads
    Then the angle monitoring result is "FAILURE"
    Then 0 network actions are used after "Co-1" at "curative" during monitoring
    Then the angle of CNEC "AngleCnec1" should be 3.79 at "curative"
    Then the angle margin of CNEC "AngleCnec1" should be -0.79 at "curative"
    Then the angle of CNEC "AngleCnec2" should be -19.33 at "curative"
    Then the angle margin of CNEC "AngleCnec2" should be 66.33 at "curative"

  @fast @ac @rao @angle-monitoring @max-min-margin
  Scenario: 6.2.1.4: Case where the redispatching fails but the load flow still converge
  Two generator (one Belgian and one Dutch) are used. However the only generator in the GLSK for Belgium
  is the one that is used in the network action -> it cannot be reused for to balance the network.
  There is a slight unbalance in the network that is compensated by the slack. The network action is still applied.
    Given network file is "epic94/MicroGrid.zip"
    Given crac file is "epic94/crac_angle_monitoring_6_2_1_4.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given monitoring glsk file is "epic94/GlskB45MicroGridTest.xml"
    When I launch rao at "2021-04-02 07:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    When I launch angle monitoring at "2021-04-02 07:00" on 1 threads
    Then the network action "RA-1" is used after "Co-1" at "curative" during monitoring
    Then the angle monitoring result is "HIGH_CONSTRAINT"
    Then the angle of CNEC "AngleCnec1" should be 5.27 at "curative"
    Then the angle margin of CNEC "AngleCnec1" should be -2.27 at "curative"
    Then the angle of CNEC "AngleCnec2" should be -19.33 at "curative"
    Then the angle margin of CNEC "AngleCnec2" should be 66.33 at "curative"

  @fast @ac @rao @angle-monitoring @max-min-margin
  Scenario: 6.2.1.5: Case where the redispatching fails but the load flow diverge
  Same case as 6.2.1.6, however the setpoint of the elementary action on the Belgian generator is too important.
  The unbalance is too big and it makes the load flow diverge -> status FAILURE + no action applied
    Given network file is "epic94/MicroGrid.zip"
    Given crac file is "epic94/crac_angle_monitoring_6_2_1_5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given monitoring glsk file is "epic94/GlskB45MicroGridTest.xml"
    When I launch rao at "2021-04-02 07:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    When I launch angle monitoring at "2021-04-02 07:00" on 1 threads
    Then the angle monitoring result is "FAILURE"
    Then 0 network actions are used after "Co-1" at "curative" during monitoring
    Then the angle of CNEC "AngleCnec1" should be 3.79 at "curative"
    Then the angle margin of CNEC "AngleCnec1" should be -0.79 at "curative"
    Then the angle of CNEC "AngleCnec2" should be -19.33 at "curative"
    Then the angle margin of CNEC "AngleCnec2" should be 66.33 at "curative"