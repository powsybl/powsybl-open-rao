# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 11.1: Handle mnecs in linear RAO

  @fast @rao @mock @dc @preventive-only
  Scenario: US 11.1.1: reference run, no mnec
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_linearRao_ref.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    And the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    And PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -16
    And the value of the objective function after CRA should be -224.0
    And the worst margin is 224.0 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    And the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 949.0 MW

  @fast @rao @mock @dc @preventive-only @mnec
  Scenario: US 11.1.2: margin on MNEC should stay positive (initial margin > 50MW)
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_linearRao_1_2.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    And the tap of PstRangeAction "PRA_PST_BE" should be -9 in preventive
    And PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -9
    And the worst margin is 199.0 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    And the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 898.0 MW
    And the margin on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 2.0 MW

  @fast @rao @mock @dc @preventive-only @mnec
  Scenario: US 11.1.3: margin on MNEC should stay above initial value -50 MW [1] (initial margin < 0MW)
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_linearRao_1_3.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    And the tap of PstRangeAction "PRA_PST_BE" should be -7 in preventive
    And PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -7
    And the worst margin is 188.0 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    And the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 877.0 MW
    And the margin on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be -84.0 MW

  Scenario: US 11.1.4: margin on MNEC should stay above initial value -50 MW [2] (50MW > initial margin > 0MW)
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_linearRao_1_4.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    And the tap of PstRangeAction "PRA_PST_BE" should be -7 in preventive
    And PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -7
    And the worst margin is 188.0 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    And the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 877.0 MW
    And the margin on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be -34.0 MW