# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 7.16: monitor loopflows on specific countries

  @fast @rao @ac @preventive-only @loopflow @max-min-margin @megawatt
  Scenario: 7.16.1 : loopflows monitored on the borders of BE and of NL
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic7/crac_lf_rao_3_cbcora.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "epic7/refProg_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_ac_lf_false_5_100_BE_NL.json"
    When I launch loopflow rao at "2019-01-08 21:30" with default loopflow limit as 0.0 percent of pmax

    Then the worst margin is -143.0 MW
    Then the worst margin is -143.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    Then the remedial action "Open FR1 FR2" is used in preventive

    Then the loopflow threshold on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be 1000.0 MW
    Then the initial loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be -610.0 MW
    Then the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be -933.0 MW

    Then the loopflow threshold on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be 1000.0 MW
    Then the initial loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -410.0 MW
    Then the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -733.0 MW

    Then the loopflow threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 1000.0 MW
    Then the initial loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be -210.0 MW
    Then the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -533.0 MW

  @fast @rao @ac @preventive-only @loopflow @max-min-margin @megawatt
  Scenario: 7.16.2 : loopflows monitored on the borders of NL
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic7/crac_lf_rao_3_with_frm_cbcora.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "epic7/refProg_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_ac_lf_false_5_100_NL.json"
    When I launch loopflow rao at "2019-01-08 21:30" with default loopflow limit as 0.0 percent of pmax

    Then the worst margin is -235.0 MW
    Then the worst margin is -235.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the tap of PstRangeAction "PRA_PST_BE" should be -12 in preventive
    Then the remedial action "Open FR1 FR2" is used in preventive

    Then the loopflow threshold on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be 900.0 MW
    Then the initial loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be -610.0 MW
    Then the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be -891.0 MW

    Then the loopflow threshold on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be 800.0 MW
    Then the initial loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -410.0 MW
    Then the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -691.0 MW