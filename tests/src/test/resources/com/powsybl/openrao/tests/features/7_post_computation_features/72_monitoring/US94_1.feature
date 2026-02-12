# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 94.1: Angle Monitoring

  @fast @ac @rao @angle-monitoring @max-min-margin @ampere
  Scenario: US 94.1.1: Basic Angle Monitoring
  Simple angle monitoring case with two curative AngleCNECs defined for two different contingencies.
  The CRAC file does not contain any FlowCNEC but the computation is still performed.
  "AngleCnec1" has a high angle constraint, "AngleCnec2" is secure.
    Given network file is "epic94/MicroGrid.zip"
    Given crac creation parameters file is "epic94/CimCracCreationParameters.json"
    Given crac file is "epic94/CIM_21_7_1_AngMon.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given monitoring glsk file is "epic94/GlskB45MicroGridTest.xml"
    When I launch rao at "2021-04-02 05:00"
    When I launch angle monitoring at "2021-04-02 05:00" on 1 threads
    Then the angle monitoring result is "HIGH_CONSTRAINT"
    Then the angle of CNEC "AngleCnec1" should be 5.22 at "curative"
    Then the angle margin of CNEC "AngleCnec1" should be -2.22 at "curative"
    Then the angle of CNEC "AngleCnec2" should be -19.33 at "curative"
    Then the angle margin of CNEC "AngleCnec2" should be 66.33 at "curative"
    Then the network action "RA-1" is used after "Co-1" at "curative" during monitoring
