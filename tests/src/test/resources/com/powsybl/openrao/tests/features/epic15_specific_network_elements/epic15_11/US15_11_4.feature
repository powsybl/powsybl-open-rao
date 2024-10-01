# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.11.4: ARAO with 2P

  @fast @rao @mock @dc @second-preventive @mnec
  Scenario: US 15.11.4.1: Check that PRAO2 ignores applied ARAs from ARAO1
    Given network file is "common/TestCase12Nodes2PSTs.uct"
    Given crac file is "epic15/jsonCrac_ep15us11-4case1.json"
    Given configuration file is "epic15/RaoParameters_ep15us11-4.json"
    When I launch search_tree_rao
    # With the recent development, the RAO falls back to 1st PRAO result
    # because 2PRA increases the cost
    # Then the RAO falls back to 1st preventive RAO result
    Then 2 remedial actions are used in preventive
    And the remedial action "Open line NL1-NL2" is used in preventive
    And the tap of PstRangeAction "PRA_PST_BE" should be -7 in preventive
    And 1 remedial actions are used after "Contingency_FR1_FR3" at "auto"
    And 0 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    And the worst margin is -140 MW
    And the value of the objective function after CRA should be 140
    And the margin on cnec "FR2-FR3-O - preventive" after PRA should be -140 MW
    And the margin on cnec "NL2-BE3-O - curative" after CRA should be 8.9 MW
    Then the optimization steps executed by the RAO should be "SECOND_PREVENTIVE_IMPROVED_FIRST"

  @fast @rao @mock @dc @second-preventive @mnec
  Scenario: US 15.11.4.2: ARAO2
    Given network file is "common/TestCase12Nodes2PSTs.uct"
    Given crac file is "epic15/jsonCrac_ep15us11-4case2.json"
    Given configuration file is "epic15/RaoParameters_ep15us11-4.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    And the remedial action "Open line NL1-NL2" is used in preventive
    And the remedial action "PRA_CRA_PST_BE" is not used in preventive
    And 1 remedial actions are used after "Contingency_FR1_FR3" at "auto"
    And the tap of PstRangeAction "ARA_PST_DE" should be 4 after "Contingency_FR1_FR3" at "auto"
    And 0 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    And the remedial action "PRA_CRA_PST_BE" is not used after "Contingency_FR1_FR3" at "curative"
    And the worst margin is -141 MW
    And the value of the objective function after CRA should be 141
    And the margin on cnec "NL2-BE3-O - curative" after CRA should be 205 MW
    Then the optimization steps executed by the RAO should be "SECOND_PREVENTIVE_IMPROVED_FIRST"