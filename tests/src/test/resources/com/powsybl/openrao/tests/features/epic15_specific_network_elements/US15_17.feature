# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.17: Optimize HVDC range actions initially in AC emulation mode

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.17.1: HVDC range action with one preventive CNEC
    # Copy of test case 15.12.5.1, except HVDC is initially in AC emulation mode
    Given network file is "epic15/TestCase16NodesWithHvdc_AC_emulation.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the setpoint of RangeAction "PRA_HVDC" should be 1422 MW in preventive
    And the worst margin is 400 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 400 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.17.2: HVDC range action with two preventive CNECs
    # Copy of test case 15.12.5.2, except HVDC is initially in AC emulation mode
    Given network file is "epic15/TestCase16NodesWithHvdc_AC_emulation.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the setpoint of RangeAction "PRA_HVDC" should be 2008 MW in preventive
    And the worst margin is 191 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 191 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 191 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.17.3: HVDC range action with PST range action and two preventive CNECs
    # Copy of test case 15.12.5.3, except HVDC is initially in AC emulation mode
    Given network file is "epic15/TestCase16NodesWithHvdc_AC_emulation.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used in preventive
    And the setpoint of RangeAction "PRA_HVDC" should be 810 MW in preventive
    And the tap of PstRangeAction "PST_PRA_PST_be_BBE2AA11 BBE3AA11 1" should be 14 in preventive
    And the worst margin is 187 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 187 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 201 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.17.4: HVDC range action with outage CNEC
    # Copy of test case 15.12.5.4, except HVDC is initially in AC emulation mode
    Given network file is "epic15/TestCase16NodesWithHvdc_AC_emulation.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case4.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used in preventive
    And the setpoint of RangeAction "PRA_HVDC" should be 917 MW in preventive
    And the tap of PstRangeAction "PST_PRA_PST_be_BBE2AA11 BBE3AA11 1" should be 13 in preventive
    And the worst margin is 177 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 177 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - outage" after PRA should be 201 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - curative" after CRA should be 201 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 237 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 15.17.5: HVDC range action with one curative perimeter
    # Copy of test case 15.12.5.5, except HVDC is initially in AC emulation mode
    Given network file is "epic15/TestCase16NodesWithHvdc_AC_emulation.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_maximizing_curative.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used in preventive
    And the setpoint of RangeAction "PRA_HVDC" should be 1236 MW in preventive
    And the tap of PstRangeAction "PST_PRA_PST_be_BBE2AA11 BBE3AA11 1" should be 9 in preventive
    And 1 remedial actions are used after "co1_be1_fr5" at "curative"
    And the setpoint of RangeAction "CRA_HVDC" should be 1388 MW after "co1_be1_fr5" at "curative"
    And the worst margin is 196 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 196 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - outage" after PRA should be 200 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - curative" after CRA should be 300 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 346 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 15.17.6: HVDC range action with two curative perimeters and negative initial flow
    # Copy of test case 15.12.5.6, except HVDC is initially in AC emulation mode
    Given network file is "epic15/TestCase16NodesWithHvdc_AC_emulation.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case6.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_maximizing_curative.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used in preventive
    And the setpoint of RangeAction "PRA_HVDC" should be 1652 MW in preventive
    And the tap of PstRangeAction "PST_PRA_PST_be_BBE2AA11 BBE3AA11 1" should be 3 in preventive
    And 1 remedial actions are used after "co1_be1_fr5" at "curative"
    And the setpoint of RangeAction "CRA_HVDC" should be 1411 MW after "co1_be1_fr5" at "curative"
    And 1 remedial actions are used after "co1_be1_fr5" at "curative"
    And the setpoint of RangeAction "CRA_HVDC" should be 2000 MW after "co2_be1fr5_be4fr5" at "curative"
    And the worst margin is 141 MW
    And the margin on cnec "nl2_be3_co2 - NNL2AA11->BBE3AA11  - co2_be1fr5_be4fr5 - outage" after PRA should be 141 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - outage" after PRA should be 142 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 143 MW
    And the margin on cnec "nl2_be3_co2 - NNL2AA11->BBE3AA11  - co2_be1fr5_be4fr5 - curative" after CRA should be 187 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - curative" after CRA should be 300 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 314 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.17.7: HVDC with a negative optimal setpoint
    # Copy of test case 15.12.5.7, except HVDC is initially in AC emulation mode
    Given network file is "epic15/TestCase16NodesWithHvdc_AC_emulation.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case7.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the setpoint of RangeAction "PRA_HVDC" should be -307 MW in preventive
    And the worst margin is 50 MW
    And the margin on cnec "de2_de3_n - DDE2AA11->DDE3AA11 - preventive" after PRA should be 50 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 15.17.8: HVDC and PST filtering
    # Copy of test case 15.12.5.8, except HVDC is initially in AC emulation mode
    Given network file is "epic15/TestCase16NodesWithHvdc_AC_emulation.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case8.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And 1 remedial actions are used after "co1_be1_fr5" at "curative"
    And the setpoint of RangeAction "CRA_HVDC" should be 1422 MW after "co1_be1_fr5" at "curative"
    And the worst margin is 300 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - curative" after CRA should be 300 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.17.9: HVDC inverted in CRAC file
    # Copy of test case 15.12.5.9, except HVDC is initially in AC emulation mode
    Given network file is "epic15/TestCase16NodesWithHvdc_AC_emulation.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case9.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the setpoint of RangeAction "PRA_HVDC" should be -307 MW in preventive
    And the worst margin is 50 MW
    And the margin on cnec "de2_de3_n - DDE2AA11->DDE3AA11 - preventive" after PRA should be 50 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 15.17.10: HVDC useless in preventive but used in curative
    Given network file is "epic15/TestCase16NodesWithHvdcAcEmulation_HvdcCnec.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us17case10.json"
    Given configuration file is "epic15/RaoParameters_ep15us17case10.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And 1 remedial actions are used after "co1_be1_fr5" at "curative"
    And the setpoint of RangeAction "PRA_CRA_HVDC" should be 1364 MW after "co1_be1_fr5" at "curative"
    And the worst margin is 300 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - curative" after CRA should be 300 MW
    And the initial flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" should be 608 MW
    And the flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" after PRA should be 608 MW
    And the flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" after CRA should be 608 MW
    And the initial flow on cnec "be2_be5_co1 - BBE2AA11->BBE5AA11 - co1_be1_fr5 - curative" should be 649 MW
    And the flow on cnec "be2_be5_co1 - BBE2AA11->BBE5AA11 - co1_be1_fr5 - curative" after PRA should be 649 MW
    And the flow on cnec "be2_be5_co1 - BBE2AA11->BBE5AA11 - co1_be1_fr5 - curative" after CRA should be 1364 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 15.17.11: HVDC useless in preventive and in curative
    Given network file is "epic15/TestCase16NodesWithHvdcAcEmulation_HvdcCnec.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us17case11.json"
    Given configuration file is "epic15/RaoParameters_ep15us17case11.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And 0 remedial actions are used after "co1_be1_fr5" at "curative"
    And the initial flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" should be 608 MW
    And the flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" after PRA should be 608 MW
    And the flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" after CRA should be 608 MW
    And the initial flow on cnec "be2_be5_co1 - BBE2AA11->BBE5AA11 - co1_be1_fr5 - curative" should be 649 MW
    And the flow on cnec "be2_be5_co1 - BBE2AA11->BBE5AA11 - co1_be1_fr5 - curative" after PRA should be 649 MW
    And the flow on cnec "be2_be5_co1 - BBE2AA11->BBE5AA11 - co1_be1_fr5 - curative" after CRA should be 649 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.17.12: HVDC range action with one preventive CNEC, no impact on worst CNEC
    Given network file is "epic15/TestCase16NodesWithHvdcAcEmulation_HvdcCnec.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us17case12.json"
    Given configuration file is "epic15/RaoParameters_ep15us17case12.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And the initial flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" should be 608 MW
    And the flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" after PRA should be 608 MW
