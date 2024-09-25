# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.11.3: Simulate range action automatons right after topological automatons

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 15.11.3.1: 1 auto HVDC
    Given network file is "epic15/TestCase16NodesWithHvdc_AC_emulation.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us11-3case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And the setpoint of RangeAction "ARA_HVDC" should be 0.0 MW in preventive
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - auto" after PRA should be -631.83 MW
    And 1 remedial actions are used after "co1_be1_fr5" at "auto"
    And the remedial action "ARA_HVDC" is used after "co1_be1_fr5" at "auto"
    And the setpoint of RangeAction "ARA_HVDC" should be 1879.68 MW after "co1_be1_fr5" at "auto"
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - auto" after ARA should be 0.0 MW
    And the margin on cnec "be3_be4_co1 - BBE3AA11->BBE4AA11  - co1_be1_fr5 - auto" after ARA should be 12.01 MW
    And its security status should be "secured"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 15.11.3.2: 1 auto PST
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic15/SL_ep15us11-3case2.json"
    Given configuration file is "epic13/RaoParameters_maxMargin_ampere_absolute_threshold.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_be" should be 0 in preventive
    And the initial margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - auto" should be -107.6 A
    And the initial margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - auto" should be -41.2 A
    And 1 remedial actions are used after "co2_be1_be3" at "auto"
    And the remedial action "pst_be" is used after "co2_be1_be3" at "auto"
    And the tap of PstRangeAction "pst_be" should be -8 after "co2_be1_be3" at "auto"
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - auto" after ARA should be 98.9 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - auto" after ARA should be 0.2 A
    And the worst margin is -22 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 15.11.3.3: 2 auto range actions
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic15/SL_ep15us11-3case3.json"
    Given configuration file is "epic13/RaoParameters_maxMargin_ampere_absolute_threshold.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_fr" should be 5 in preventive
    And the tap of PstRangeAction "pst_be" should be 0 in preventive
    And the initial margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - auto" should be -107.6 A
    And the initial margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - auto" should be -41.2 A
    And 2 remedial actions are used after "co2_be1_be3" at "auto"
    And the remedial action "pst_fr" is used after "co2_be1_be3" at "auto"
    And the remedial action "pst_be" is used after "co2_be1_be3" at "auto"
    And the tap of PstRangeAction "pst_fr" should be 15 after "co2_be1_be3" at "auto"
    And the tap of PstRangeAction "pst_be" should be -3 after "co2_be1_be3" at "auto"
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - auto" after ARA should be 9.0 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - auto" after ARA should be 146.5 A
    And the worst margin is -22 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 15.11.3.4: auto range actions and topological range actions
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic15/SL_ep15us11-3case4.json"
    Given configuration file is "epic13/RaoParameters_maxMargin_ampere_absolute_threshold.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_fr" should be 5 in preventive
    And the tap of PstRangeAction "pst_be" should be 0 in preventive
    And the initial margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - auto" should be -107.6 A
    And the initial margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - auto" should be -41.2 A
    And 3 remedial actions are used after "co2_be1_be3" at "auto"
    And the remedial action "open_be1_be4" is used after "co2_be1_be3" at "auto"
    And the remedial action "pst_fr" is used after "co2_be1_be3" at "auto"
    And the remedial action "pst_be" is used after "co2_be1_be3" at "auto"
    And the tap of PstRangeAction "pst_fr" should be 10 after "co2_be1_be3" at "auto"
    And the tap of PstRangeAction "pst_be" should be -1 after "co2_be1_be3" at "auto"
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - auto" after ARA should be 15.3 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - auto" after ARA should be 65.7 A
    And the worst margin is -22 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: Verify post-ARAO setpoint for automatic+curative range action
    # copy of test case Scenario: US 15.11.3.2: 1 auto PST
    # except that pst_be is also preventive and curative
    # it should be used in preventive at tap position -2
    # in auto at -8
    # in curative at -16
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic15/SL_ep15us11-3case2_withPstCra.json"
    Given configuration file is "epic13/RaoParameters_maxMargin_ampere_absolute_threshold.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_be_pra" should be -2 in preventive
    And the tap of PstRangeAction "pst_be_ara" should be -2 in preventive
    And the tap of PstRangeAction "pst_be_cra" should be -2 in preventive
    And the initial margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - auto" should be -107.6 A
    And the initial margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - auto" should be -41.2 A
    And 1 remedial actions are used after "co2_be1_be3" at "auto"
    And the remedial action "pst_be_ara" is used after "co2_be1_be3" at "auto"
    And the tap of PstRangeAction "pst_be_pra" should be -8 after "co2_be1_be3" at "auto"
    And the tap of PstRangeAction "pst_be_ara" should be -8 after "co2_be1_be3" at "auto"
    And the tap of PstRangeAction "pst_be_cra" should be -8 after "co2_be1_be3" at "auto"
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - auto" after ARA should be 98.9 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - auto" after ARA should be 0.2 A
    And 1 remedial actions are used after "co2_be1_be3" at "auto"
    And the remedial action "pst_be_cra" is used after "co2_be1_be3" at "curative"
    And the tap of PstRangeAction "pst_be_pra" should be -16 after "co2_be1_be3" at "curative"
    And the tap of PstRangeAction "pst_be_cra" should be -16 after "co2_be1_be3" at "curative"
    And the tap of PstRangeAction "pst_be_ara" should be -16 after "co2_be1_be3" at "curative"
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - curative" after CRA should be -58 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - curative" after CRA should be 305 A
    And the worst margin is -58 A
