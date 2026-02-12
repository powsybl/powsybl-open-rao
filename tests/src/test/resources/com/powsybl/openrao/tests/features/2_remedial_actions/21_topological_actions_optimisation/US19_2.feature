# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 19.2: Handle maximum topological CRA per TSO

  @fast @rao @ac @contingency-scenarios
  Scenario: US 19.2.1: Check that the maximum number of network actions per TSO is ignored in preventive 1
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us2case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 3 remedial actions are used in preventive
    Then the remedial action "open_be1_be4" is used in preventive
    Then the remedial action "open_fr1_fr2" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be -15 in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -15 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is -500 A on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative"
    Then the margin on cnec "BBE4AA1  FFR5AA1  1 - preventive" after PRA should be 300 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 308 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -500 A
    Then the margin on cnec "BBE4AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 326 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 334 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 371 A

  @fast @rao @ac @contingency-scenarios
  Scenario: US 19.2.2: Check that the maximum number of network actions per TSO is respected in curative - reference run
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us2case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_be" should be -16 in preventive
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -8 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 254 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 254 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 450 A

  @fast @rao @ac @contingency-scenarios
  Scenario: US 19.2.3: Check that the maximum number of network actions per TSO is respected in curative
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us2case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_be" should be -16 in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -14 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 254 A on cnec "BBE2AA1  FFR3AA1  1 - preventive"
    Then the margin on cnec "FFR1AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 437 A

  @fast @rao @ac @contingency-scenarios @search-tree-rao
  Scenario: US 19.2.4: Simple case, with 2 curative states
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us2case4.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao after "co2_be1_be3" at "curative"
    Then 2 remedial actions are used after "co2_be1_be3" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "co2_be1_be3" at "curative"
    Then the remedial action "open_fr1_fr2" is used after "co2_be1_be3" at "curative"
    Then the worst margin is 510 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - curative" after CRA should be 510 A
    Then the margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - curative" after CRA should be 904 A

  @fast @rao @ac @contingency-scenarios
  Scenario: US 19.2.5: Check that the maximum number of network actions per TSO is ignored in preventive 2
    # Copy of 13.2.6 test but with a configuration limiting curative topo per TSO
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us2case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 2 remedial actions are used in preventive
    Then the remedial action "open_be1_be4" is used in preventive
    Then the remedial action "open_fr1_fr2" is used in preventive
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"

  @fast @rao @ac @contingency-scenarios
  Scenario: US 19.2.6: Check country filtering is well done in curative
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us2case6.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    # Without limitation it should use 3 french topological actions as it is limited at 2, il will choose belgian topo instead
    Then 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr4" is used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_be1_be4" is used after "co1_fr2_fr3_1" at "curative"