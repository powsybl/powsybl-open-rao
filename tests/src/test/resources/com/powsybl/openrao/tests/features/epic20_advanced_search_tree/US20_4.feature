# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 20.4: Handle MNECs in second preventive optimization

  @fast @rao @mock @dc @second-preventive @mnec
  Scenario: US 20.4.1: MNEC constraint in curative is solved by 2P
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic20/CBCORA_ep20us4case1.xml"
    Given configuration file is "epic20/RaoParameters_20_4.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 2 remedial actions are used in preventive
    And the remedial action "Open line NL1-NL2" is used in preventive
    And the tap of PstRangeAction "PRA_PST_BE" should be -2 in preventive
    And 0 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    And the worst margin is -168 MW
    And the value of the objective function after CRA should be 168
    And the margin on cnec "FR2-FR3-O - preventive" after PRA should be -168 MW
    And the margin on cnec "NL2-BE3-O - curative" after CRA should be 7 MW
    Then the optimization steps executed by the RAO should be "SECOND_PREVENTIVE_IMPROVED_FIRST"

  @fast @rao @mock @dc @second-preventive @mnec
  Scenario: US 20.4.2: MNEC constraint in curative is solved by CRA + 2P
    Given network file is "common/TestCase12Nodes2PSTs.uct" for CORE CC
    Given crac file is "epic20/CBCORA_ep20us4case2.xml"
    Given configuration file is "epic20/RaoParameters_20_4.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 2 remedial actions are used in preventive
    And the remedial action "Open line NL1-NL2" is used in preventive
    And the tap of PstRangeAction "PRA_PST_BE" should be -3 in preventive
    And 1 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    And the tap of PstRangeAction "CRA_PST_DE" should be 1 after "Contingency_FR1_FR3" at "curative"
    And the worst margin is -161 MW
    And the value of the objective function after CRA should be 161
    And the margin on cnec "FR2-FR3-O - preventive" after PRA should be -161 MW
    And the margin on cnec "NL2-BE3-O - curative" after CRA should be 7 MW
    Then the optimization steps executed by the RAO should be "SECOND_PREVENTIVE_IMPROVED_FIRST"

  @fast @rao @mock @dc @second-preventive @mnec
  Scenario: US 20.4.3: MNEC constraint avoided on preventive MNEC in 2P
    Given network file is "common/TestCase12Nodes2PSTs.uct" for CORE CC
    Given crac file is "epic20/CBCORA_ep20us4case3.xml"
    Given configuration file is "epic20/RaoParameters_20_4.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 0 remedial actions are used in preventive
    And 0 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    And the worst margin is -182 MW
    And the value of the objective function after CRA should be 182
    And the margin on cnec "NL1-NL3-D - curative" after CRA should be -182 MW
    And the margin on cnec "NL2-BE3-O - curative" after CRA should be -145 MW
    And the margin on cnec "FR2-FR3-O - preventive" after PRA should be -96 MW
    Then the optimization steps executed by the RAO should be "SECOND_PREVENTIVE_IMPROVED_FIRST"