# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 7.15: loop-flow acceptable augmentation parameter

  @fast @rao @ac @preventive-only @loopflow @max-min-margin
  Scenario: 7.15.1 : Test case with a loop-flow acceptable augmentation parameter of 40 MW
    #same case as 7.11.1 but with the new parameter, giving a better margin
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic7/crac_lf_rao_3_cbcora.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_ac_lf_false_5_100_lfAugmentation.json"
    When I launch loopflow rao at "2019-01-08 12:00" with default loopflow limit as 0 percent of pmax
    Then its security status should be "UNSECURED"
    Then the worst margin is -421.0 MW
    Then the worst margin is -421.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the tap of PstRangeAction "PRA_PST_BE" should be 11 in preventive
    Then the remedial action "Open FR1 FR2" is used in preventive
    Then the loopflow threshold on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be 1000.0 MW
    Then the loopflow threshold on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be 1000.0 MW
    Then the loopflow threshold on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be 250.0 MW
    Then the loopflow threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 1000.0 MW
    Then the initial loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be -391.0 MW
    Then the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be -425.0 MW
    Then the initial loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -391.0 MW
    Then the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -425.0 MW
    Then the initial loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be -391.0 MW
    Then the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -425.0 MW
    Then the initial loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be -391.0 MW
    Then the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -425.0 MW