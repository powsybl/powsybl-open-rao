# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.12.7: Handle CSE's HVDCs with range actions on injections

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.12.7.1: UCTE HVDC as InjectionRangeAction with one preventive CNEC
    Given network file is "epic15/TestCase16NodesWithUcteHvdc.uct"
    Given crac file is "epic15/cseCrac_ep15us12-5case1.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "PRA_HVDC" should be 1406 MW in preventive
    And the worst margin is 380 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 380 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.12.7.2: UCTE HVDC as InjectionRangeAction with two preventive CNECs
    Given network file is "epic15/TestCase16NodesWithUcteHvdc.uct"
    Given crac file is "epic15/cseCrac_ep15us12-5case2.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "PRA_HVDC" should be 1985 MW in preventive
    And the worst margin is 172 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 172 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 172 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.12.7.3: UCTE HVDC as InjectionRangeAction with PST range action and two preventive CNECs
    Given network file is "epic15/TestCase16NodesWithUcteHvdc.uct"
    Given crac file is "epic15/cseCrac_ep15us12-5case3.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "PRA_HVDC" should be 828 MW in preventive
    And the tap of PstRangeAction "PST_PRA_PST_be_BBE2AA11 BBE3AA11 1" should be 15 in preventive
    And the worst margin is 187 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 187 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 190 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.12.7.4: UCTE HVDC as InjectionRangeAction with outage CNEC
    Given network file is "epic15/TestCase16NodesWithUcteHvdc.uct"
    Given crac file is "epic15/cseCrac_ep15us12-5case4.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "PRA_HVDC" should be 929 MW in preventive
    And the tap of PstRangeAction "PST_PRA_PST_be_BBE2AA11 BBE3AA11 1" should be 14 in preventive
    And the worst margin is 176 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 176 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - outage" after PRA should be 191 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - curative" after CRA should be 191 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 225 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 15.12.7.5: UCTE HVDC as InjectionRangeAction with one curative perimeter
    Given network file is "epic15/TestCase16NodesWithUcteHvdc.uct"
    Given crac file is "epic15/cseCrac_ep15us12-5case5.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "PRA_HVDC" should be 1230 MW in preventive
    And the tap of PstRangeAction "PST_PRA_PST_be_BBE2AA11 BBE3AA11 1" should be 10 in preventive
    And the setpoint of RangeAction "CRA_HVDC" should be 1372 MW after "co1_be1_fr5" at "curative"
    And the worst margin is 186 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - outage" after PRA should be 191 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 186 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - curative" after CRA should be 285 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 329 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 15.12.7.6: UCTE HVDC as InjectionRangeAction with two curative perimeters and negative initial flow
    Given network file is "epic15/TestCase16NodesWithUcteHvdc.uct"
    Given crac file is "epic15/cseCrac_ep15us12-5case6.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "PRA_HVDC" should be 1661 MW in preventive
    And the tap of PstRangeAction "PST_PRA_PST_be_BBE2AA11 BBE3AA11 1" should be 3 in preventive
    And the setpoint of RangeAction "CRA_HVDC" should be 1397 MW after "co1_be1_fr5" at "curative"
    And the setpoint of RangeAction "CRA_HVDC" should be 2000 MW after "co2_be1fr5_be4fr5" at "curative"
    And the worst margin is 110 MW
    And the margin on cnec "be1_be2_n - BBE1AA11->BBE2AA11 - preventive" after PRA should be 127 MW
    And the margin on cnec "nl2_be3_co2 - NNL2AA11->BBE3AA11  - co2_be1fr5_be4fr5 - outage" after PRA should be 111 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - outage" after PRA should be 110 MW
    And the margin on cnec "nl2_be3_co2 - NNL2AA11->BBE3AA11  - co2_be1fr5_be4fr5 - curative" after CRA should be 165 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - curative" after CRA should be 285 MW
    And the margin on cnec "be1_fr5_n - BBE1AA11->FFR5AA11 - preventive" after PRA should be 285 MW

  @fast @rao @mock @dc @preventive-only @hvdc
  Scenario: US 15.12.7.7: UCTE HVDC as InjectionRangeAction with a negative optimal setpoint
    Given network file is "epic15/TestCase16NodesWithUcteHvdc.uct"
    Given crac file is "epic15/cseCrac_ep15us12-5case7.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "PRA_HVDC" should be -353 MW in preventive
    And the worst margin is 47 MW
    And the margin on cnec "de2_de3_n - DDE2AA11->DDE3AA11 - preventive" after PRA should be 47 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 15.12.7.8: UCTE HVDC as InjectionRangeAction and PST filtering
    Given network file is "epic15/TestCase16NodesWithUcteHvdc.uct"
    Given crac file is "epic15/cseCrac_ep15us12-5case8.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    Given crac creation parameters file is "epic15/us_15_2_7.json"
    When I launch search_tree_rao
    Then the setpoint of RangeAction "CRA_HVDC" should be 1406 MW after "co1_be1_fr5" at "curative"
    And the worst margin is 285 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - curative" after CRA should be 285 MW
