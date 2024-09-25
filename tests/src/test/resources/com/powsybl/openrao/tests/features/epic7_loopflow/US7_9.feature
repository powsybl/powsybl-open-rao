# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 7.9: Linear RAO with loopflow limitation

  @fast @rao @mock @ac @preventive-only
  Scenario: US 7.9.1: linear RAO without LF limitation
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf_rao_1.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    And the worst margin is 224.0 MW
    And the margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" after PRA should be 224.0 MW
    And the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive

  @fast @rao @mock @ac @preventive-only @loopflow
  Scenario: US 7.9.2: linear RAO with LF limited by predefined threshold (10% of Fmax)
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf_rao_1.json"
    Given Glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_ac_lf_false_10_100.json"
    When I launch loopflow search_tree_rao with default loopflow limit as 10.0 percent of pmax
    Then the worst margin is 198.0 MW
    And the margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" after PRA should be 198.0 MW
    And the tap of PstRangeAction "PRA_PST_BE" should be -9 in preventive
    And the loopflow threshold on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be 500.0 MW
    And the loopflow threshold on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be 500.0 MW
    And the loopflow threshold on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be 500.0 MW
    And the loopflow threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 500.0 MW
    And the initial loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be -391.0 MW
    And the initial loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -391.0 MW
    And the initial loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be -391.0 MW
    And the initial loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be -391.0 MW
    And the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be -489.0 MW
    And the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -489.0 MW
    And the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -489.0 MW
    And the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -489.0 MW

  @fast @rao @mock @ac @preventive-only @loopflow
  Scenario: US 7.9.3: linear RAO with LF limited by initial value
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf_rao_1.json"
    Given Glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_ac_lf_false_10_100.json"
    When I launch loopflow search_tree_rao with default loopflow limit as 5.0 percent of pmax
    Then the worst margin is 166.0 MW
    And the margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" after PRA should be 166.0 MW
    #And the tap of PstRangeAction "PRA_PST_BE" should be 0 in preventive
    And the loopflow threshold on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be 250.0 MW
    And the loopflow threshold on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be 250.0 MW
    And the loopflow threshold on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be 250.0 MW
    And the loopflow threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 250.0 MW
    And the initial loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be -391.0 MW
    And the initial loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -391.0 MW
    And the initial loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be -391.0 MW
    And the initial loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be -391.0 MW
    And the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be -391.0 MW
    And the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -391.0 MW
    And the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -391.0 MW
    And the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -391.0 MW