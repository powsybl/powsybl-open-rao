# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 19.6: handle maximum number of TSOs using RAs in curative optimization

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.6.1: Two allowed TSOs - 2 TSOs in crac
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us6case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -12 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 999 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.6.2: Two allowed TSOs - 3 TSOs in crac
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us6case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -12 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 5 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 1000 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"


  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.6.3: One allowed TSO
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us6case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 945 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.6.4: One allowed TSO - BE PST not allowed
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us10case9.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr2" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 2 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 998 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.6.5: One allowed TSO - no PST allowed
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us6case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr2" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 973 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.6.6: Two allowed TSOs - no PST allowed
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us6case6.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr2" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_be1_be4" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 987 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.6.7: One allowed TSO - no PST allowed - best network action moved to be
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us6case7.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_be1_be4" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 876 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.6.8: No CRA allowed
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us6case8.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 680 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.6.9: Three PSTs - one allowed TSO (pst_fr in be)
    Given network file is "epic19/TestCase16Nodes_3PSTs.uct"
    Given crac file is "epic19/SL_ep19us6case9.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 16 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 15 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 972 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.6.10: No CRA allowed - check parameter is ignored in preventive
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us6case10.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"