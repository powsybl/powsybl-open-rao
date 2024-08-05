# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 13.4: Dynamic of topological remedial actions available in several instants

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.4.1: Topological RA already applied in initial network : not available for optimization
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us4case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -522 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -522 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -184 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 145 A
    And the value of the objective function after CRA should be 522

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.4.2: Topological RA available in preventive and curative : used in preventive
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us4case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"
    And the worst margin is -522 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.4.3: Topological RA available in preventive and curative : used in curative
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us4case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_fr" should be 5 in preventive
    And the tap of PstRangeAction "pst_be" should be 0 in preventive
    And 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 15 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -315 A
    And the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - curative" after CRA should be -315 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -184 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 145 A
    And the value of the objective function after CRA should be 315

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.4.4: Topological RA duplicated into PRA and CRA : PRA is activated
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us4case4.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"
    And the worst margin is -522 A


  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.4.5: Topological RA duplicated into PRA and CRA : CRA is activated
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us4case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_fr" should be 5 in preventive
    And the tap of PstRangeAction "pst_be" should be 0 in preventive
    And 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5_cra" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 15 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -315 A
    And the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - curative" after CRA should be -315 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -184 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 145 A
    And the value of the objective function after CRA should be 315

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.4.6: Topological RA with inverted CRA : PRA is not used, so the CRA is not available
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us4case6.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -184 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -184 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -55 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 145 A
    And the value of the objective function after CRA should be 184

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.4.7: Topological RA with inverted CRA : line opened in preventive and closed in curative
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us4case7.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 3 remedial actions are used in preventive
    And the remedial action "open_fr1_fr2_pra" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be -5 in preventive
    And the tap of PstRangeAction "pst_be" should be -16 in preventive
    And 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr2_cra" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be -5 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 200 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 200 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 267 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 613 A
    And the value of the objective function after CRA should be -200