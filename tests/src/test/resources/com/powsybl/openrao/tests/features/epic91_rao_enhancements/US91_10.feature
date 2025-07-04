# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 91.10: MIP test cases

  @fast @rao @mock @ac @preventive-only @loopflow
  Scenario: US 91.10.1: Non MIP range action optimization cannot respect loopflows
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf_rao_1.json"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given configuration file is "epic91/RaoParameters_maxMargin_ampere_ac_lf_false_3_100.json"
    When I launch loopflow search_tree_rao with default loopflow limit as 10.0 percent of pmax
    Then the worst margin is 238.0 A
    And the margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" after PRA should be 238.0 A
    And the tap of PstRangeAction "PRA_PST_BE" should be 0 in preventive

  @fast @rao @mock @ac @preventive-only @loopflow
  Scenario: US 91.10.2: MIP range action optimization respects loopflows
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf_rao_1.json"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given configuration file is "epic91/RaoParameters_maxMargin_ampere_ac_lf_false_3_100_mip.json"
    When I launch loopflow search_tree_rao with default loopflow limit as 10.0 percent of pmax
    Then the worst margin is 198.0 MW
    And the margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" after PRA should be 198.0 MW
    And the tap of PstRangeAction "PRA_PST_BE" should be -9 in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: US 91.10.3: MIP with slightly different aligned PSTs
    Given network file is "epic91/TestCase16Nodes_alignedPsts.uct"
    Given crac file is "epic91/CBCORA_alignedPsts.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_mip.json"
    When I launch search_tree_rao at "2019-01-08 12:00" on preventive state
    Then 2 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_be" should be 7 in preventive
    And the tap of PstRangeAction "pst_fr" should be 7 in preventive
    And the margin on cnec "fr4_de1_N - preventive" after PRA should be 2032 A
