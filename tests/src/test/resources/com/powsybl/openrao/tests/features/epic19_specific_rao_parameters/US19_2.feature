# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 19.2: Handle maximum topological CRA per TSO

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.2.1: Check that the maximum number of network actions per TSO is ignored in preventive 1
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us2case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"
    And the worst margin is -686.35 A on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.2.2: Check that the maximum number of network actions per TSO is respected in curative - reference run
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us2case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_be" should be -16 in preventive
    And 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be -5 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 254 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 254 A
    And the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 321 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.2.3: Check that the maximum number of network actions per TSO is respected in curative
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us2case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_be" should be -16 in preventive
    And 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be -5 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -144 A on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative"
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 254 A
    And the margin on cnec "FFR1AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 309 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.2.4: Simple case, with 2 curative states
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us2case4.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao after "co2_be1_be3" at "curative"
    Then 2 remedial actions are used after "co2_be1_be3" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co2_be1_be3" at "curative"
    And the remedial action "open_fr1_fr2" is used after "co2_be1_be3" at "curative"
    And the worst margin is 510 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - curative" after CRA should be 510 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - curative" after CRA should be 904 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.2.5: Check that the maximum number of network actions per TSO is ignored in preventive 2
    # Copy of 13.2.6 test but with a configuration limiting curative topo per TSO
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us2case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.2.6: Check country filtering is well done in curative
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us2case6.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    # Without limitation it should use 3 french topological actions as it is limited at 2, il will choose belgian topo instead
    Then 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr4" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_be1_be4" is used after "co1_fr2_fr3_1" at "curative"