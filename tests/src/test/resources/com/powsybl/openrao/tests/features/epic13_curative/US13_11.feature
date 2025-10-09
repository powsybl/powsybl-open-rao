# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 13.11: curative RAO stop criterion

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.11.1: Skip curative RAO
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us11case1.json"
    Given configuration file is "epic13/RaoParameters_stop_curative_at_preventive.json"
    When I launch rao
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 301 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 680 A
    And the value of the objective function after CRA should be -301

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.11.2: Stop curative RAO after root leaf optimization
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us11case1.json"
    Given configuration file is "epic13/RaoParameters_best_preventive_by_500.json"
    When I launch rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 15 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 16 after "co1_fr2_fr3_1" at "curative"
    And the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 301 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 971 A
    And the value of the objective function after CRA should be -301

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.11.3: Stop curative RAO after reaching set difference with preventive
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us11case3.json"
    Given configuration file is "epic13/RaoParameters_best_preventive_by_628.json"
    When I launch rao
    Then 2 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And 2 remedial actions are used after "co2_be1_be3" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co2_be1_be3" at "curative"
    And the remedial action "open_be1_be4" is used after "co2_be1_be3" at "curative"
    And the worst margin is 124 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - preventive" after PRA should be 124 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - curative" after CRA should be 752.5 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 854 A
    And the value of the objective function after CRA should be -124

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.11.4: Stop curative RAO after making perimeters secure
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us11case4.json"
    Given configuration file is "epic13/RaoParameters_best_preventive_by_300_secure.json"
    When I launch rao
    Then 2 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 8 after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr2" is used after "co1_fr2_fr3_1" at "curative"
    And 1 remedial actions are used after "co2_be1_be3" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co2_be1_be3" at "curative"
    And the worst margin is -376.0 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - preventive" after PRA should be -376 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 2 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - curative" after CRA should be 115.0 A
    And the value of the objective function after CRA should be 376
