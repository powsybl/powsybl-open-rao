# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.17: Optimize HVDC range actions initially in AC emulation mode


  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 15.17.10: HVDC useless in preventive but used in curative
    Given network file is "epic15/TestCase16NodesWithHvdcAcEmulation_HvdcCnec.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us17case10.json"
    Given configuration file is "epic15/RaoParameters_ep15us17case10.json"
    When I launch search_tree_rao
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
    When I launch search_tree_rao
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
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And the initial flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" should be 0 A
    And the flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" after PRA should be 0 A
