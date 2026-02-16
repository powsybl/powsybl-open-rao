# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 7.5: Loopflow PTDF update parameter

  @fast @rao @dc @preventive-only @loopflow @max-min-margin @megawatt
  Scenario: US 7.5.0: RAO with loop-flow in DC with FIXED_PTDF
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic7/crac_lf_rao_3_cbcora.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "epic7/refProg_12nodes_same_as_uct.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_dc_lf_false_5_100.json"
    When I launch loopflow rao at "2019-01-08 21:30" with default loopflow limit as 0.0 percent of pmax
    Then its security status should be "UNSECURED"
    Then 0 remedial actions are used in preventive
    Then the worst margin is -500 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the loopflow threshold on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be 1000.0 MW
    Then the loopflow threshold on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be 1000.0 MW
    Then the loopflow threshold on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be 250.0 MW
    Then the loopflow threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 1000.0 MW
    Then the initial loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be -391.0 MW
    Then the initial loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -391.0 MW
    Then the initial loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be -391.0 MW
    Then the initial loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be -391.0 MW

  @fast @rao @dc @preventive-only @loopflow @max-min-margin @megawatt
  Scenario: US 7.5.1: RAO with loop-flow in DC with UPDATE_PTDF_WITH_TOPO_AND_PST
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic7/crac_lf_rao_3_cbcora.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "epic7/refProg_12nodes_same_as_uct.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_dc_lf_accurate_5_100.json"
    When I launch loopflow rao at "2019-01-08 21:30" with default loopflow limit as 0.0 percent of pmax
    Then its security status should be "UNSECURED"
    Then the remedial action "Open FR1 FR2" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 15 in preventive
    Then the worst margin is -465 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the loopflow threshold on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be 1000.0 MW
    Then the loopflow threshold on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be 1000.0 MW
    Then the loopflow threshold on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be 250.0 MW
    Then the loopflow threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 1000.0 MW
    Then the initial loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be -391.0 MW
    Then the initial loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -391.0 MW
    Then the initial loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be -391.0 MW
    Then the initial loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be -391.0 MW
    Then the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be -384.0 MW
    Then the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -384.0 MW
    Then the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -384.0 MW
    Then the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -384.0 MW

  @fast @rao @dc @preventive-only @loopflow @max-min-margin @megawatt
  Scenario: US 7.5.2: RAO with loop-flow in DC with UPDATE_PTDF_WITH_TOPO
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic7/crac_lf_rao_3_cbcora.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "epic7/refProg_12nodes_same_as_uct.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_dc_lf_inBetween_5_100.json"
    When I launch loopflow rao at "2019-01-08 21:30" with default loopflow limit as 0.0 percent of pmax
    Then its security status should be "UNSECURED"
    Then the remedial action "Open FR1 FR2" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 15 in preventive
    Then the worst margin is -465 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the loopflow threshold on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be 1000.0 MW
    Then the loopflow threshold on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be 1000.0 MW
    Then the loopflow threshold on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be 250.0 MW
    Then the loopflow threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 1000.0 MW
    Then the initial loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be -391.0 MW
    Then the initial loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -391.0 MW
    Then the initial loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be -391.0 MW
    Then the initial loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be -391.0 MW
    Then the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be -384.0 MW
    Then the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -384.0 MW
    Then the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -384.0 MW
    Then the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -384.0 MW