# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 7.11: Additional tests with CBCORA input files and FRM

  @fast @rao @mock @ac @preventive-only @loopflow
  Scenario: 7.11.1 : Replication of test case 7.10.3, using a CBCORA file as input instead of a SL
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic7/crac_lf_rao_3_cbcora.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_ac_lf_false_5_100.json"

    When I launch loopflow search_tree_rao at "2019-01-08 00:00" with default loopflow limit as 0.0 percent of pmax
    # if the loopflow limit is defined equal to zero, josiris-server will not create LoopFlowExtensions
    # and the limits given in the CBCORA file will be used instead

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
    And the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be -384.0 MW
    And the initial loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -391.0 MW
    And the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -384.0 MW
    And the initial loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be -391.0 MW
    And the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -384.0 MW
    And the initial loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be -391.0 MW
    And the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -384.0 MW

  @fast @rao @mock @dc @preventive-only @loopflow
  Scenario: 7.11.2 : Loop-flow limitation with FRM
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic7/crac_lf_rao_3_with_frm_cbcora.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_dc_lf_false_5_100.json"

    When I launch loopflow search_tree_rao at "2019-01-08 12:00" with default loopflow limit as 0.0 percent of pmax
      # if the loopflow limit is defined equal to zero, josiris-server will not create LoopFlowExtensions
      # and the limits given in the CBCORA file will be used instead

    Then its security status should be "UNSECURED"
    Then the worst margin is -505.0 MW
    Then the worst margin is -505.0 MW on cnec "FFR2AA1  DDE3AA1  1 - preventive"
    Then the tap of PstRangeAction "PRA_PST_BE" should be 14 in preventive
    Then the remedial action "Open FR1 FR2" is used in preventive
    And the loopflow threshold on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be 900.0 MW
    And the loopflow threshold on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be 800.0 MW
    And the loopflow threshold on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be 450.0 MW
    And the loopflow threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 650.0 MW
    And the initial loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" should be -391.0 MW
    And the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be -437.0 MW
    And the initial loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -391.0 MW
    And the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -437.0 MW
    And the initial loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be -391.0 MW
    And the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -437.0 MW
    And the initial loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be -391.0 MW
    And the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -437.0 MW