# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.11.5: Additional tests to check various fixes concerning automaton state

  @fast @rao @mock @dc @second-preventive
  Scenario: US 15.11.5.1: test fix apply auto for curative cnecs post 2P
    Given network file is "epic15/TestCase12Nodes_15_11_5_1.uct"
    Given crac file is "epic15/crac_15_11_5_1bis.json"
    Given configuration file is "epic15/RaoParameters_ep15us11-5-1.json"
    When I launch search_tree_rao
    And the remedial action "open_de1_de2_open_nl2_be3 - prev" is used in preventive
    And the remedial action "open_de2_nl3 - co1 - auto" is used after "co1_fr2_de3" at "auto"
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_de3" at "curative"
    And the margin on cnec "be1_be3_co1 - BBE1AA11->BBE3AA11  - co1_fr2_de3 - curative" after CRA should be 112.7 MW

  @fast @rao @mock @dc @second-preventive
  Scenario: US 15.11.5.2: test fix condition 2P
    Given network file is "epic15/TestCase12Nodes_15_11_5_3_2.uct"
    Given crac file is "epic15/crac_15_11_5_2bis.json"
    Given configuration file is "epic15/RaoParameters_ep15us11-5-2.json"
    When I launch search_tree_rao
    And the margin on cnec "de2_nl3_co1 - DDE2AA11->NNL3AA11  - co1_de2_nl3 - curative" after PRA should be -2519.05 MW
    And the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_ONLY"

  @fast @rao @mock @dc @contingency-scenarios
  Scenario: US 15.11.5.3.1: test get highest functional cost worst cnec is a curative after 1PRAO
    Given network file is "epic15/TestCase12Nodes_15_11_5_3_2.uct"
    Given crac file is "epic15/crac_15_11_5_3_1.json"
    Given configuration file is "epic15/RaoParameters_ep15us11-5-3-3.json"
    When I launch search_tree_rao
    And the worst margin is -773.0 MW on cnec "be1_be3_co2 - BBE1AA11->BBE3AA11  - co2_de1_de3 - curative"

  @fast @rao @mock @dc @contingency-scenarios
  Scenario: US 15.11.5.3.2: test get highest functional cost worst cnec is auto after 1ARAO
    Given network file is "epic15/TestCase12Nodes_15_11_5_3_2.uct"
    Given crac file is "epic15/crac_15_11_5_3_2.json"
    Given configuration file is "epic15/RaoParameters_ep15us11-5-3-3.json"
    When I launch search_tree_rao
    And the worst margin is -1945.45 MW on cnec "de2_nl3_co1 - DDE2AA11->NNL3AA11  - co1_de1_de2 - auto"

  @fast @rao @mock @dc @contingency-scenarios
  Scenario: US 15.11.5.3.3: test get highest functional cost worst cnec is curative after 1CRAO
    Given network file is "epic15/TestCase12Nodes_15_11_5_3_2.uct"
    Given crac file is "epic15/crac_15_11_5_3_3.json"
    Given configuration file is "epic15/RaoParameters_ep15us11-5-3-3.json"
    When I launch search_tree_rao
    And the worst margin is -543.5 MW on cnec "be1_be3_co1 - BBE1AA11->BBE3AA11  - co1_de1_de2 - curative"
    
  @fast @rao @mock @dc @second-preventive
  Scenario: US 15.11.5.4: RaoResult AFTER PRA fixed for curative cnecs, with 2P
    Given network file is "epic15/TestCase12Nodes_15_11_5_1.uct"
    Given crac file is "epic15/crac_15_11_5_1.json"
    Given configuration file is "epic15/RaoParameters_ep15us11-5-1.json"
    When I launch search_tree_rao
    And the remedial action "open_de1_de2_open_nl2_be3 - prev" is used in preventive
    And the remedial action "open_de2_nl3 - co1 - auto" is used after "co1_fr2_de3" at "auto"
    And the remedial action "close_fr2_de3 - co1 - auto" is not used after "co1_fr2_de3" at "auto"
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_de3" at "curative"
    And the margin on cnec "be1_be3_co1 - BBE1AA11->BBE3AA11  - co1_fr2_de3 - curative" after PRA should be -293.5 MW
    And the margin on cnec "be1_be3_co1 - BBE1AA11->BBE3AA11  - co1_fr2_de3 - auto" after ARA should be -360.65 MW
    And the margin on cnec "be1_be3_co1 - BBE1AA11->BBE3AA11  - co1_fr2_de3 - curative" after CRA should be 112.7 MW
    And the optimization steps executed by the RAO should be "SECOND_PREVENTIVE_IMPROVED_FIRST"

  @fast @rao @mock @dc @contingency-scenarios
  Scenario: US 15.11.5.5: RaoResult AFTER PRA fixed for curative cnecs, without 2P
    Given network file is "epic15/TestCase12Nodes_15_11_5_1.uct"
    Given crac file is "epic15/crac_15_11_5_1.json"
    Given configuration file is "epic15/RaoParameters_ep15us11-5-3-3.json"
    When I launch search_tree_rao
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"

  @fast @rao @mock @dc @second-preventive
  Scenario: US 15.11.5.6: Considering ARA in 2P improves 2P optimization
    Given network file is "epic15/TestCase12Nodes_15_automaton.uct"
    Given crac file is "epic15/crac_15_11_5_2.json"
    Given configuration file is "epic15/RaoParameters_ep15us11-5-1.json"
    When I launch search_tree_rao
    And the remedial action "Open line BE1-BE2" is used in preventive
    And the remedial action "Open line FR2-DE3" is used after "Contingency_FR1_FR2" at "auto"
    And the margin on cnec "BE1-BE3 - preventive" after PRA should be 1500 MW
    And the margin on cnec "NL3-DE2 - curative" after PRA should be -1444 MW
    And the margin on cnec "NL3-DE2 - curative" after CRA should be 1385 MW
    And the optimization steps executed by the RAO should be "SECOND_PREVENTIVE_IMPROVED_FIRST"
