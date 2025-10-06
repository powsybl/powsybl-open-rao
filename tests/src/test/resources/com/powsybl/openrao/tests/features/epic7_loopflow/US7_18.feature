# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 7.18: Virtual hubs in loopflow computation

  @fast @loopflow-computation @mock @ac @loopflow
  Scenario: 7.18.1 : Loop flow computation with one virtual hub on a classic UCTE node
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic7/crac_lf.json"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "epic7/refProg_12nodes_virtual_1.xml"
    Given Virtual hubs configuration file is "conf_virtual_hub_1.xml"
    When I launch loopflow_computation with OpenLoadFlow at "2019-01-08 21:30"
    And the loopflow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after loopflow computation should be -909.0 MW
    And the loopflow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after loopflow computation should be -790.0 MW
    And the loopflow on cnec "FFR1AA1  FFR2AA1  1 - preventive" after loopflow computation should be 1023.0 MW
    And the loopflow on cnec "FFR1AA1  FFR3AA1  1 - preventive" after loopflow computation should be -23.0 MW
    And the loopflow on cnec "FFR2AA1  FFR3AA1  1 - preventive" after loopflow computation should be -1046.0 MW
    And the loopflow on cnec "DDE1AA1  DDE2AA1  1 - preventive" after loopflow computation should be -176.0 MW
    And the loopflow on cnec "DDE1AA1  DDE3AA1  1 - preventive" after loopflow computation should be -823.0 MW
    And the loopflow on cnec "DDE2AA1  DDE3AA1  1 - preventive" after loopflow computation should be -646.0 MW
    And the loopflow on cnec "NNL1AA1  NNL2AA1  1 - preventive" after loopflow computation should be -76.0 MW
    And the loopflow on cnec "NNL1AA1  NNL3AA1  1 - preventive" after loopflow computation should be -323.0 MW
    And the loopflow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after loopflow computation should be -246.0 MW
    And the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after loopflow computation should be -530.0 MW
    And the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after loopflow computation should be -530.0 MW
    And the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after loopflow computation should be -330.0 MW
    And the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after loopflow computation should be -430.0 MW

  @fast @loopflow-computation @mock @ac @loopflow
  Scenario: 7.18.2 Loop flow computation with one virtual hub on a external Xnode border
    Given network file is "epic7/TestCase12Nodes_with_Xnodes_dangling.uct" for CORE CC
    Given crac file is "epic7/crac_lf_xnodes.json"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "epic7/refProg_12nodes_virtual_2.xml"
    Given Virtual hubs configuration file is "conf_virtual_hub_2.xml"
    When I launch loopflow_computation with OpenLoadFlow at "2019-01-08 21:30"
    And the loopflow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after loopflow computation should be -931.0 MW
    And the loopflow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after loopflow computation should be -901.0 MW
    And the loopflow on cnec "FFR1AA1  FFR2AA1  1 - preventive" after loopflow computation should be 1090.0 MW
    And the loopflow on cnec "FFR1AA1  FFR3AA1  1 - preventive" after loopflow computation should be 109.0 MW
    And the loopflow on cnec "FFR2AA1  FFR3AA1  1 - preventive" after loopflow computation should be -981.0 MW
    And the loopflow on cnec "DDE1AA1  DDE2AA1  1 - preventive" after loopflow computation should be -109.0 MW
    And the loopflow on cnec "DDE1AA1  DDE3AA1  1 - preventive" after loopflow computation should be -890.0 MW
    And the loopflow on cnec "DDE2AA1  DDE3AA1  1 - preventive" after loopflow computation should be -781.0 MW
    And the loopflow on cnec "NNL1AA1  NNL2AA1  1 - preventive" after loopflow computation should be 190.0 MW
    And the loopflow on cnec "NNL1AA1  NNL3AA1  1 - preventive" after loopflow computation should be 309.0 MW
    And the loopflow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after loopflow computation should be 118.0 MW
    And the loopflow on cnec "DDE2AA1  X_NLDE1  1 + NNL3AA1  X_NLDE1  1 - preventive" after loopflow computation should be -328.0 MW
    And the loopflow on cnec "DDE3AA1  X_DEFR1  1 + FFR2AA1  X_DEFR1  1 - preventive" after loopflow computation should be 228.0 MW
    And the loopflow on cnec "BBE3AA1  X_NLBE1  1 + NNL2AA1  X_NLBE1  1 - preventive" after loopflow computation should be 428.0 MW
    And the loopflow on cnec "BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - preventive" after loopflow computation should be -628.0 MW

  @fast @rao @mock @dc @preventive-only @loopflow
  Scenario: 7.18.3 RAO with one virtual hub on a external Xnode border
    Given network file is "epic7/TestCase12Nodes_with_Xnodes_dangling.uct" for CORE CC
    Given crac file is "epic7/crac_lf_rao_3_cbcora_xnodes.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "epic7/refProg_12nodes_virtual_2.xml"
    Given Virtual hubs configuration file is "conf_virtual_hub_2.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_dc_lf_false_10_100.json"
    When I launch loopflow rao at "2019-01-08 21:30" with default loopflow limit as 0.0 percent of pmax

    Then the worst margin is 498.0 MW on cnec "DDE3AA1  X_DEFR1  1 - preventive"
    Then the tap of PstRangeAction "PRA_PST_BE" should be -10 in preventive
    Then the remedial action "Open FR1 FR2" is not used in preventive

    And the loopflow threshold on cnec "DDE2AA1  X_NLDE1  1 - preventive" should be 1000.0 MW
    And the loopflow threshold on cnec "BBE3AA1  X_NLBE1  1 - preventive" should be 1000.0 MW
    And the loopflow threshold on cnec "DDE3AA1  X_DEFR1  1 - preventive" should be 350.0 MW
    And the loopflow threshold on cnec "BBE2AA1  X_BEFR1  1 - preventive" should be 1000.0 MW

    And the initial loopflow on cnec "DDE2AA1  X_NLDE1  1 - preventive" should be -328.0 MW
    And the loopflow on cnec "DDE2AA1  X_NLDE1  1 - preventive" after PRA should be -437.0 MW
    And the initial loopflow on cnec "BBE3AA1  X_NLBE1  1 - preventive" should be 428.0 MW
    And the loopflow on cnec "BBE3AA1  X_NLBE1  1 - preventive" after PRA should be 537.0 MW
    And the initial loopflow on cnec "DDE3AA1  X_DEFR1  1 - preventive" should be 228.0 MW
    And the loopflow on cnec "DDE3AA1  X_DEFR1  1 - preventive" after PRA should be 337.0 MW
    And the initial loopflow on cnec "BBE2AA1  X_BEFR1  1 - preventive" should be -628.0 MW
    And the loopflow on cnec "BBE2AA1  X_BEFR1  1 - preventive" after PRA should be -737.0 MW
