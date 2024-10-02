# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.12.5: Handle HVDC range actions in RAO

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.12.5.1: HVDC range action with one preventive CNEC
    Given network file is "epic15/TestCase16NodesWithHvdc.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "PRA_HVDC" should be 1419 MW in preventive
    And the worst margin is 400 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 400 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.12.5.2: HVDC range action with two preventive CNECs
    Given network file is "epic15/TestCase16NodesWithHvdc.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "PRA_HVDC" should be 2008 MW in preventive
    And the worst margin is 191 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 191 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 191 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.12.5.3: HVDC range action with PST range action and two preventive CNECs
    Given network file is "epic15/TestCase16NodesWithHvdc.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "PRA_HVDC" should be 807 MW in preventive
    And the tap of PstRangeAction "PST_PRA_PST_be_BBE2AA11 BBE3AA11 1" should be 15 in preventive
    And the worst margin is 191 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 191 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 201 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.12.5.4: HVDC range action with outage CNEC
    Given network file is "epic15/TestCase16NodesWithHvdc.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case4.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "PRA_HVDC" should be 914 MW in preventive
    And the tap of PstRangeAction "PST_PRA_PST_be_BBE2AA11 BBE3AA11 1" should be 14 in preventive
    And the worst margin is 195 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 195 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - outage" after PRA should be 201 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - curative" after CRA should be 201 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 237 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 15.12.5.5: HVDC range action with one curative perimeter
    Given network file is "epic15/TestCase16NodesWithHvdc.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "PRA_HVDC" should be 1233 MW in preventive
    And the tap of PstRangeAction "PST_PRA_PST_be_BBE2AA11 BBE3AA11 1" should be 10 in preventive
    And the setpoint of RangeAction "CRA_HVDC" should be 1385 MW after "co1_be1_fr5" at "curative"
    And the worst margin is 196 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 196 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - outage" after PRA should be 200 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - curative" after CRA should be 300 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 346 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 15.12.5.6: HVDC range action with two curative perimeters and negative initial flow
    Given network file is "epic15/TestCase16NodesWithHvdc.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case6.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "PRA_HVDC" should be 1652 MW in preventive
    And the tap of PstRangeAction "PST_PRA_PST_be_BBE2AA11 BBE3AA11 1" should be 3 in preventive
    And the setpoint of RangeAction "CRA_HVDC" should be 1409 MW after "co1_be1_fr5" at "curative"
    And the setpoint of RangeAction "CRA_HVDC" should be 2000 MW after "co2_be1fr5_be4fr5" at "curative"
    And the worst margin is 129 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 129 MW
    And the margin on cnec "nl2_be3_co2 - NNL2AA11->BBE3AA11  - co2_be1fr5_be4fr5 - outage" after PRA should be 141 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - outage" after PRA should be 142 MW
    And the margin on cnec "nl2_be3_co2 - NNL2AA11->BBE3AA11  - co2_be1fr5_be4fr5 - curative" after CRA should be 187 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - curative" after CRA should be 300 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 314 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.12.5.7: HVDC with a negative optimal setpoint
    Given network file is "epic15/TestCase16NodesWithHvdc.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case7.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "PRA_HVDC" should be -354 MW in preventive
    And the worst margin is 50 MW
    And the margin on cnec "de2_de3_n - DDE2AA11->DDE3AA11 - preventive" after PRA should be 50 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 15.12.5.8: HVDC and PST filtering
    Given network file is "epic15/TestCase16NodesWithHvdc.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case8.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "CRA_HVDC" should be 1419 MW after "co1_be1_fr5" at "curative"
    And the worst margin is 300 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - curative" after CRA should be 300 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.12.5.9: HVDC inverted in CRAC file
    Given network file is "epic15/TestCase16NodesWithHvdc.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us12-5case9.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "PRA_HVDC" should be -354 MW in preventive
    And the worst margin is 50 MW
    And the margin on cnec "de2_de3_n - DDE2AA11->DDE3AA11 - preventive" after PRA should be 50 MW