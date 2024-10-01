# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 7.10: Search-tree RAO with loopflow limitation

  @fast @rao @mock @dc @preventive-only
  Scenario: US 7.10.1: Simple search tree RAO without LF limitation
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf_rao_3.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then its security status should be "UNSECURED"
    Then the worst margin is -160.0 MW
    Then the worst margin is -160.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    Then 2 remedial actions are used in preventive
    Then the remedial action "Open FR1 FR2" is used in preventive
    Then the remedial action "PRA_PST_BE" is used in preventive

  @fast @rao @mock @dc @preventive-only @loopflow
  Scenario: US 7.10.2: Simple search tree RAO with LF limited by a predefined threshold
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf_rao_3.json"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_dc_lf_false_5_100.json"
    When I launch loopflow search_tree_rao with default loopflow limit as 50.0 percent of pmax
    Then its security status should be "UNSECURED"
    Then the worst margin is -401.0 MW
    Then the worst margin is -401.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the tap of PstRangeAction "PRA_PST_BE" should be -10 in preventive
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_PST_BE" is used in preventive
    And the loopflow threshold on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be 2000.0 MW
    And the loopflow threshold on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be 2000.0 MW
    And the loopflow threshold on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be 500.0 MW
    And the loopflow threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 2000.0 MW
    And the initial loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be -391.0 MW
    And the initial loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -391.0 MW
    And the initial loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be -391.0 MW
    And the initial loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be -391.0 MW
    And the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be -490.0 MW
    And the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -490.0 MW
    And the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -490.0 MW
    And the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -490.0 MW

  @fast @rao @mock @ac @preventive-only @loopflow
  Scenario: US 7.10.3: Simple search tree RAO with LF limited by their initial value
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf_rao_3.json"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_ac_lf_false_5_100.json"
    When I launch loopflow search_tree_rao with default loopflow limit as 25.0 percent of pmax
    Then its security status should be "UNSECURED"
    Then the worst margin is -463.0 MW
    Then the worst margin is -463.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the tap of PstRangeAction "PRA_PST_BE" should be 15 in preventive
    Then the remedial action "Open FR1 FR2" is used in preventive
    And the loopflow threshold on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be 1000.0 MW
    And the loopflow threshold on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be 1000.0 MW
    And the loopflow threshold on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be 250.0 MW
    And the loopflow threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 1000.0 MW
    And the initial loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be -391.0 MW
    And the initial loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -391.0 MW
    And the initial loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be -391.0 MW
    And the initial loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be -391.0 MW
    And the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be -384.0 MW
    And the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -384.0 MW
    And the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -384.0 MW
    And the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -384.0 MW

  @fast @rao @mock @ac @preventive-only @loopflow
  Scenario: US 7.10.4: Simple search tree RAO with loop-approximation ON
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf_rao_3.json"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_dc_lf_true_5_100.json"
    When I launch loopflow search_tree_rao with default loopflow limit as 50.0 percent of pmax
    Then its security status should be "UNSECURED"
    Then the worst margin is -391.0 MW
    Then the worst margin is -391.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the tap of PstRangeAction "PRA_PST_BE" should be -11 in preventive
    And the loopflow threshold on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be 2000.0 MW
    And the loopflow threshold on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be 2000.0 MW
    And the loopflow threshold on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be 500.0 MW
    And the loopflow threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 2000.0 MW
    And the initial loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be -391.0 MW
    And the initial loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -391.0 MW
    And the initial loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be -391.0 MW
    And the initial loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be -391.0 MW
    And the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be -499.0 MW
    And the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -499.0 MW
    And the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -499.0 MW

  @fast @rao @mock @ac @preventive-only
  Scenario: US 7.10.5: Complex search tree RAO without LF limitation
    Given network file is "common/TestCase12Nodes2PSTs.uct"
    Given crac file is "epic7/crac_lf_rao_4.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then its security status should be "UNSECURED"
    Then the worst margin is -251.0 MW
    Then the worst margin is -251.0 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    Then the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    Then the tap of PstRangeAction "PRA_PST_DE" should be -1 in preventive
    Then 4 remedial actions are used in preventive
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the remedial action "PRA_PST_DE" is used in preventive
    Then the remedial action "Open_BE1_BE3" is used in preventive
    Then the remedial action "Open_NL1_NL2" is used in preventive

  @fast @rao @mock @dc @preventive-only @loopflow
  Scenario: US 7.10.6: Complex search tree RAO with LF limitation
    Given network file is "common/TestCase12Nodes2PSTs.uct"
    Given crac file is "epic7/crac_lf_rao_4.json"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_dc_lf_false_5_100.json"
    When I launch loopflow search_tree_rao with default loopflow limit as 35.0 percent of pmax
    Then its security status should be "UNSECURED"
    Then the worst margin is -290.0 MW
    Then the worst margin is -290.0 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    Then the tap of PstRangeAction "PRA_PST_DE" should be -12 in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be -1 in preventive
    Then 2 remedial actions are used in preventive
    And the loopflow threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 525.5 MW
    And the initial loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be -391.0 MW
    And the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -521.0 MW