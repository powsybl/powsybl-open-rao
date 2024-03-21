# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 24.1 : Run RAO for SWE CSA process

  @fast @rao @ac @contingency-scenarios
  Scenario: US 24.1.1: Duplicate of test case 13.5.4 in CSA-profiles format
    Given network file is "epic24/TestCase_13_5_4.zip"
    Given crac file is "epic24/TestCase_13_5_4.zip"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao at "2023-04-30 22:30"
    Then the worst margin is -184.1 A
    And the remedial action "open_fr1_fr2" is used in preventive
    # TODO: define "close" RA and change this test when closing topo actions is supported
    # And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr_pra" should be -7 in preventive
    And the tap of PstRangeAction "pst_fr_cra" should be 1 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 0 in preventive
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    And the value of the objective function after CRA should be 342