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
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then 2 remedial actions are used in preventive
    Then the remedial action "acEmulationDeactivation_BBE2AA11 FFR3AA11 1" is used in preventive
    And the setpoint of RangeAction "PRA_HVDC" should be 1422 MW in preventive
    And the worst margin is 400 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 400 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.17.2: HVDC range action with two preventive CNECs
    # Copy of test case 15.12.5.2, except HVDC is initially in AC emulation mode
    Given network file is "epic15/TestCase16NodesWithHvdc_AC_emulation.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then 2 remedial actions are used in preventive
    And the setpoint of RangeAction "PRA_HVDC" should be 2008 MW in preventive
    And the worst margin is 191 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 191 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 191 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.17.3: HVDC range action with PST range action and two preventive CNECs
    # Copy of test case 15.12.5.3, except HVDC is initially in AC emulation mode
    Given network file is "epic15/TestCase16NodesWithHvdc_AC_emulation.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then 1 remedial actions are used in preventive
    And the setpoint of RangeAction "PRA_HVDC" should be 820 MW in preventive
    And the tap of PstRangeAction "PST_PRA_PST_be_BBE2AA11 BBE3AA11 1" should be 14 in preventive
    And the worst margin is 181 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 181 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 204 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 15.17.10: HVDC useless in preventive but used in curative
    Given network file is "epic15/TestCase16NodesWithHvdcAcEmulation_HvdcCnec.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us17case10.json"
    Given configuration file is "epic15/RaoParameters_ep15us17case10.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    And 1 remedial actions are used after "co1_be1_fr5" at "curative"
    And the setpoint of RangeAction "PRA_CRA_HVDC" should be 1364 MW after "co1_be1_fr5" at "curative"
    And the worst margin is 433 A
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - curative" after CRA should be 433 A
    And the initial flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" should be 0 A
    And the flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" after PRA should be 0 A
    And the flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" after CRA should be 0 A
    And the initial flow on cnec "be2_be5_co1 - BBE2AA11->BBE5AA11 - co1_be1_fr5 - curative" should be 0 A
    And the flow on cnec "be2_be5_co1 - BBE2AA11->BBE5AA11 - co1_be1_fr5 - curative" after PRA should be 0 A
    And the flow on cnec "be2_be5_co1 - BBE2AA11->BBE5AA11 - co1_be1_fr5 - curative" after CRA should be 1971 A

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 15.17.11: HVDC useless in preventive and in curative
    Given network file is "epic15/TestCase16NodesWithHvdcAcEmulation_HvdcCnec.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us17case11.json"
    Given configuration file is "epic15/RaoParameters_ep15us17case11.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    And 0 remedial actions are used after "co1_be1_fr5" at "curative"
    And the initial flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" should be 0 A
    And the flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" after PRA should be 0 A
    And the flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" after CRA should be 0 A
    And the initial flow on cnec "be2_be5_co1 - BBE2AA11->BBE5AA11 - co1_be1_fr5 - curative" should be 0 A
    And the flow on cnec "be2_be5_co1 - BBE2AA11->BBE5AA11 - co1_be1_fr5 - curative" after PRA should be 0 A
    And the flow on cnec "be2_be5_co1 - BBE2AA11->BBE5AA11 - co1_be1_fr5 - curative" after CRA should be 0 A

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.17.12: HVDC range action with one preventive CNEC, no impact on worst CNEC
    Given network file is "epic15/TestCase16NodesWithHvdcAcEmulation_HvdcCnec.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us17case12.json"
    Given configuration file is "epic15/RaoParameters_ep15us17case12.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    And the initial flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" should be 0 A
    And the flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" after PRA should be 0 A
