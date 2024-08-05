# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 19.5: max number of CRAs

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.5.1: Three allowed CRAs
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -12 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 997 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.5.2: Two allowed CRAs
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -12 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 5 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 1000 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.5.3: One allowed CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 945 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.5.4: One allowed CRA, BE PST not allowed
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case4.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 0 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 5 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 840 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.5.5: No allowed CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 679 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.5.6: Three topological CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case6.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_be1_be4" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr2" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 987 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.5.7: Two topological CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case7.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr2" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 973 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.5.8: One topological CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case8.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 839 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.5.9: One topological CRA, best FR topo not allowed
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case9.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 814 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.5.10: Test that the parameter is ignored in preventive
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case10.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"