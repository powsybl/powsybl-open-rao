# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.5: Handle parallel PSTs

  @fast @rao @mock @ac @preventive-only @search-tree-rao
  Scenario: US 15.5.1: Preventive search tree RAO with two aligned PSTs
    Given network file is "epic15/TestCase16Nodes_alignedPsts.uct"
    Given crac file is "epic15/CBCORA_15_5_1.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao at "2019-01-08 12:00" on preventive state
    Then 2 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_be" should be 16 in preventive
    And the tap of PstRangeAction "pst_fr" should be 16 in preventive
    And the margin on cnec "fr4_de1_N - preventive" after PRA should be 1481 A