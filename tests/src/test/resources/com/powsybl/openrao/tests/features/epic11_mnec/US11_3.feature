# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 11.3: Handle mnecs in search tree with only network actions

  @fast @rao @mock @dc @preventive-only
  Scenario: US 11.3.1: reference run, no mnec
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_networkAction_ref.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the remedial action "Open line FR1- FR2" is used in preventive
    And line "FFR1AA1  FFR2AA1  1" in network file with PRA has connection status to "false"
    And the remedial action "PST BE setpoint" is used in preventive
    And the worst margin is -143.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    And the flow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -1856.0 MW

  @fast @rao @mock @dc @preventive-only @mnec
  Scenario: US 11.3.2: margin on MNEC should stay positive (initial margin > 180MW)
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_networkAction_3_2.json"
    Given configuration file is "epic11/RaoParameters_maxMargin_megawatt_mnecDimin180.json"
    When I launch search_tree_rao
    Then the remedial action "Open line FR1- FR2" is used in preventive
    And line "FFR1AA1  FFR2AA1  1" in network file with PRA has connection status to "false"
    And 1 remedial actions are used in preventive
    And the worst margin is -307.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    And the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 108.0 MW
    And the flow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -1692.0 MW

  @fast @rao @mock @dc @preventive-only @mnec
  Scenario: US 11.3.3: margin on MNEC should stay above initial value - 180 MW
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_networkAction_3_3.json"
    Given configuration file is "epic11/RaoParameters_maxMargin_megawatt_mnecDimin180.json"
    When I launch search_tree_rao
    Then the remedial action "PST BE setpoint" is used in preventive
    And PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -16
    And 1 remedial actions are used in preventive
    And the worst margin is -326.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    And the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -673.0 MW
    And the flow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -1673.0 MW