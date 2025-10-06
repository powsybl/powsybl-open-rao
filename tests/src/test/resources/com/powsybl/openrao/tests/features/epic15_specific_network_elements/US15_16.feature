# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.16: Handle parallel PSTs with different initial setpoint

  @fast @rao @mock @ac @preventive-only @search-tree-rao
  Scenario: US 15.16.1: Preventive search tree RAO with two aligned PSTs with different initial taps
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic15/CBCORA_15_5_1.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao at "2019-01-08 12:00" on preventive state
    Then 0 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_be" should be 0 in preventive
    And the tap of PstRangeAction "pst_fr" should be 5 in preventive
    And the worst margin is 1352 A