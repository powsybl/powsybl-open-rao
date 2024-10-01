# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 7.14: Use a refProg file to calculate the loop-flows

  @fast @loopflow-computation @mock @ac @loopflow
  Scenario: 7.14.1 : calculate loop-flows with a refProg file
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic7/crac_lf.json"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "epic7/refProg_12nodes.xml"
    When I launch loopflow_computation with OpenLoadFlow at "2019-01-08 21:30"
    And the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after loopflow computation should be -610.0 MW
    And the loopflow on cnec "NNL1AA1  NNL3AA1  1 - preventive" after loopflow computation should be 303.0 MW
    And the loopflow on cnec "NNL1AA1  NNL2AA1  1 - preventive" after loopflow computation should be 196.0 MW
    And the loopflow on cnec "FFR2AA1  FFR3AA1  1 - preventive" after loopflow computation should be -1193.0 MW
    And the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after loopflow computation should be -410.0 MW
    And the loopflow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after loopflow computation should be -730.0 MW
    And the loopflow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after loopflow computation should be -869.0 MW
    And the loopflow on cnec "FFR1AA1  FFR3AA1  1 - preventive" after loopflow computation should be -96.0 MW
    And the loopflow on cnec "DDE2AA1  DDE3AA1  1 - preventive" after loopflow computation should be -593.0 MW
    And the loopflow on cnec "FFR1AA1  FFR2AA1  1 - preventive" after loopflow computation should be 1096.0 MW
    And the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after loopflow computation should be -310.0 MW
    And the loopflow on cnec "DDE1AA1  DDE3AA1  1 - preventive" after loopflow computation should be -796.0 MW
    And the loopflow on cnec "DDE1AA1  DDE2AA1  1 - preventive" after loopflow computation should be -203.0 MW
    And the loopflow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after loopflow computation should be 106.0 MW
    And the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after loopflow computation should be -210.0 MW

  @fast @rao @mock @ac @preventive-only @loopflow
  Scenario: 7.14.2 : run a search tree RAO with a refProg file
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic7/crac_lf_rao_3_cbcora.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "epic7/refProg_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_ac_lf_false_5_100.json"
    When I launch loopflow search_tree_rao at "2019-01-08 21:30" with default loopflow limit as 0.0 percent of pmax

    Then the worst margin is -473.0 MW
    Then the worst margin is -473.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the tap of PstRangeAction "PRA_PST_BE" should be 16 in preventive
    Then the remedial action "Open FR1 FR2" is used in preventive

    And the loopflow threshold on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be 1000.0 MW
    And the initial loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be -610.0 MW
    And the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be -601.0 MW

    And the loopflow threshold on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be 1000.0 MW
    And the initial loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -410.0 MW
    And the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -401.0 MW

    And the loopflow threshold on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be 250.0 MW
    And the initial loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be -310.0 MW
    And the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -301.0 MW

    And the loopflow threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 1000.0 MW
    And the initial loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be -210.0 MW
    And the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -201.0 MW

  @fast @rao @mock @ac @preventive-only @loopflow
  Scenario: 7.14.3 : run another search tree RAO with a refProg file
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic7/crac_lf_rao_3_with_frm_cbcora.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "epic7/refProg_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_ac_lf_false_5_100.json"
    When I launch loopflow search_tree_rao at "2019-01-08 21:30" with default loopflow limit as 0.0 percent of pmax

    Then the worst margin is -378.0 MW
    Then the worst margin is -378.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the tap of PstRangeAction "PRA_PST_BE" should be 2 in preventive
    Then the remedial action "Open FR1 FR2" is used in preventive

    And the loopflow threshold on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be 900.0 MW
    And the initial loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be -610.0 MW
    And the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be -746.0 MW

    And the loopflow threshold on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be 800.0 MW
    And the initial loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -410.0 MW
    And the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -546.0 MW

    And the loopflow threshold on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be 450.0 MW
    And the initial loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be -310.0 MW
    And the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -446.0 MW

    And the loopflow threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 650.0 MW
    And the initial loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be -210.0 MW
    And the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -346.0 MW