# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 4.2: Optimization in A/MW, thresholds in A/MW, computation in AC/DC

  @fast @rao @mock @dc @preventive-only
  Scenario: US 4.2.1: MW thresholds in DC mode and min margin in MW
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic4/SL_ep4us2_4MR_MW.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the worst margin is 19.0 MW
    And the value of the objective function after CRA should be -19.0
    And the tap of PstRangeAction "PRA_PST_BE" should be 5 in preventive
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 27.0 MW
    And the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 20.0 MW
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 44.0 MW

  @fast @rao @mock @ac @preventive-only
  Scenario: US 4.2.2: MW thresholds in AC mode and min margin in MW
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic4/SL_ep4us2_4MR_MW.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the worst margin is 19.0 MW
    And the value of the objective function after CRA should be -19.0
    And the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 25.0 MW
    And the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 19.0 MW
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 49.0 MW

  @fast @rao @mock @dc @preventive-only
  Scenario: US 4.2.3: A thresholds in DC mode and min margin in MW
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic4/SL_ep4us2_4MR_A.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the worst margin is 15.0 MW
    Then the value of the objective function after CRA should be -15.0
    Then the tap of PstRangeAction "PRA_PST_BE" should be 5 in preventive
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 23.0 MW
    And the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 20.0 MW
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 40.0 MW

  @fast @rao @mock @ac @preventive-only
  Scenario: US 4.2.4: A thresholds in AC mode and min margin in A
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic4/SL_ep4us2_4MR_A.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then the worst margin is 19.0 A
    Then the value of the objective function after CRA should be -19.0
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive
    And the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 19.0 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 31.0 A
    And the margin on cnec "NNL2AA1  BBE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 51.0 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 65.0 A

  @fast @rao @mock @dc @preventive-only
  Scenario: US 4.2.5: mixed thresholds in DC mode and min margin in MW
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic4/SL_ep4us2_4MR_mixed.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the worst margin is 19.0 MW
    Then the value of the objective function after CRA should be -19.0
    Then the tap of PstRangeAction "PRA_PST_BE" should be 5 in preventive
    And the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 24.0 MW
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 23.0 MW

  @fast @rao @mock @ac @preventive-only
  Scenario: US 4.2.6: mixed thresholds in AC mode and min margin in MW
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic4/SL_ep4us2_4MR_mixed.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the worst margin is 19.0 MW
    Then the value of the objective function after CRA should be -19.0
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive
    And the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 19.0 MW
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 23.0 MW