# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 11.3: Handle MNECs in search tree with only network actions
  # TODO: This feature covers

  @fast @rao @ac @preventive-only @max-min-margin @megawatt
  Scenario: US 11.3.1: reference run, no MNEC
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_networkAction_ref.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_ac.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the remedial action "Open line FR1- FR2" is used in preventive
    Then line "FFR1AA1  FFR2AA1  1" in network file with PRA has connection status to "false"
    Then the remedial action "PST BE setpoint" is used in preventive
    Then the worst margin is -143.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -1856.0 MW on side 1

  @fast @rao @ac @preventive-only @mnec @max-min-margin @megawatt
  Scenario: US 11.3.2: margin on MNEC should stay positive (initial margin > 180MW)
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_networkAction_3_2.json"
    Given configuration file is "epic11/RaoParameters_maxMargin_megawatt_ac_mnecDimin180.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the remedial action "Open line FR1- FR2" is used in preventive
    Then line "FFR1AA1  FFR2AA1  1" in network file with PRA has connection status to "false"
    Then 1 remedial actions are used in preventive
    Then the worst margin is -307.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 108.0 MW
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -1692.0 MW on side 1

  @fast @rao @ac @preventive-only @mnec @max-min-margin @megawatt
  Scenario: US 11.3.3: margin on MNEC should stay above initial value - 180 MW
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_networkAction_3_3.json"
    Given configuration file is "epic11/RaoParameters_maxMargin_megawatt_ac_mnecDimin180.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the remedial action "PST BE setpoint" is used in preventive
    Then PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -16
    Then 1 remedial actions are used in preventive
    Then the worst margin is -326.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -673.0 MW
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -1673.0 MW on side 1