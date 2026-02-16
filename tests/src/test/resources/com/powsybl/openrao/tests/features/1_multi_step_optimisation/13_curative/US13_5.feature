# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 13.5: dynamic of range actions available in several instants

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 13.5.1: Preventive and curative PST RA, with same taps in both states
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us5case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the worst margin is -582 A
    Then the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - curative" after CRA should be -582 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -87 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 236 A
    Then the tap of PstRangeAction "pst_fr" should be 5 in preventive
    Then the tap of PstRangeAction "pst_be" should be -16 in preventive
    Then the tap of PstRangeAction "pst_fr" should be 15 after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    Then the value of the objective function after CRA should be 582

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 13.5.2: Preventive and curative PST RA, with different taps in each state
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us5case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the worst margin is 71 A
    Then the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - curative" after CRA should be 71 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 370 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 777 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 786 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 1196 A
    Then the remedial action "open_fr1_fr2" is used in preventive
    Then the remedial action "open_fr1_fr3" is used in preventive
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -16 in preventive
    Then the tap of PstRangeAction "pst_fr" should be 16 after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 0 in preventive
    Then the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    Then the value of the objective function after CRA should be -71

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 13.5.3: Preventive and curative PST RA, with an activation in the curative state limited by a RELATIVE_DYNAMIC range
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us5case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the worst margin is -99 A
    Then the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - curative" after CRA should be -99 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 104 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 525 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 813 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 1183 A
    Then the remedial action "open_fr1_fr2" is used in preventive
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -5 in preventive
    Then the tap of PstRangeAction "pst_fr" should be 5 after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 0 in preventive
    Then the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    Then the value of the objective function after CRA should be 99

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 13.5.4: Duplicated RA on the same PST, one being a PRA and the other one being a CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us5case4.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the worst margin is -47 A
    Then the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - curative" after CRA should be -47 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 149 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 575 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 774 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 1179 A
    Then the remedial action "open_fr1_fr2" is used in preventive
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr_pra" should be -7 in preventive
    Then the tap of PstRangeAction "pst_fr_cra" should be 1 after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 0 in preventive
    Then the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    Then the value of the objective function after CRA should be 47

  @fast @rao @ac @contingency-scenarios @max-min-margin @megawatt
  Scenario: US 13.5.5: Preventive and curative optimization with absolute limit on curative PST
    Given network file is "epic13/TestCase12NodesForCurative.uct"
    Given crac file is "epic13/12nodes_onePreventiveAndCurativePst_relativeLimit.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_ac.json"
    When I launch rao at "2019-01-08 00:30"
    Then the worst margin is 1432 MW
    Then the margin on cnec "CnecCurativeDir - curative" after CRA should be 1432 MW
    Then the tap of PstRangeAction "SelectTapPSTPrev" should be 12 in preventive
    Then the tap of PstRangeAction "SelectTapPSTCur" should be 14 after "Contingency" at "curative"
    Then the value of the objective function after CRA should be -1432

  @fast @rao @ac @contingency-scenarios @max-min-margin @megawatt
  Scenario: US 13.5.6: Preventive and curative optimization with relative limit on curative PST
    Given network file is "epic13/TestCase12NodesForCurative.uct"
    Given crac file is "epic13/12nodes_onePreventiveAndCurativePst_absoluteLimit.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_ac.json"
    When I launch rao at "2019-01-08 00:30"
    Then the worst margin is 1432 MW
    Then the margin on cnec "CnecCurativeDir - curative" after CRA should be 1432 MW
    Then the tap of PstRangeAction "SelectTapPSTPrev" should be 12 in preventive
    Then the tap of PstRangeAction "SelectTapPSTCur" should be 14 after "Contingency" at "curative"

  @fast @rao @ac @contingency-scenarios @max-min-margin @megawatt
  Scenario: US 13.5.7: Preventive and curative optimization with wrong absolute limit on curative PST
    Given network file is "epic13/TestCase12NodesForCurative.uct"
    Given crac file is "epic13/12nodes_onePreventiveAndCurativePst_absoluteLimitError.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_ac.json"
    When I launch rao at "2019-01-08 00:30"
    Then the worst margin is 1422 MW
    Then the margin on cnec "CnecCurativeDir - curative" after CRA should be 1422 MW
    Then the tap of PstRangeAction "SelectTapPSTPrev" should be 12 in preventive
    Then the tap of PstRangeAction "SelectTapPSTCur" should be 12 after "Contingency" at "curative"
    Then the value of the objective function after CRA should be -1422

  @fast @crac
  Scenario: US 13.5.8: PST filtering
    Given network file is "epic13/TestCase12NodesForCurative.uct"
    Given crac file is "epic13/12nodes_oneCorrectPreventivePst.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_ac.json"
    When I import crac at "2019-01-08 12:00"
    Then it should have 2 range actions
    Then range action "SelectTapPSTPrev" should have 1 ranges
    Then range action "SelectTapPSTPrevWrongRange" should have 0 ranges

  @fast @rao @ac @preventive-only @max-min-margin @megawatt
  Scenario: US 13.5.9: Preventive optimization after PST filtering
    Given network file is "epic13/TestCase12NodesForCurative.uct"
    Given crac file is "epic13/12nodes_oneCorrectPreventivePst.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_ac.json"
    When I launch rao at "2019-01-08 00:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then the worst margin is 1637 MW
    Then the margin on cnec "CnecPreventiveDir - preventive" after PRA should be 1654 MW
    Then the tap of PstRangeAction "SelectTapPSTPrev" should be 12 in preventive
    Then the value of the objective function after CRA should be -1637

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 13.5.10: CBCORA, CRA and PRA on same PSTs (as done in CORE CC data)
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct"
    Given crac file is "epic13/CBCORA_ep13us5case10.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao at "2019-01-08 12:00"
    Then 2 remedial actions are used in preventive
    Then the tap of PstRangeAction "pra_pst_fr" should be -15 in preventive
    Then the tap of PstRangeAction "pra_pst_be" should be 13 in preventive
    Then the tap of PstRangeAction "cra_pst_fr" should be -15 in preventive
    Then the tap of PstRangeAction "cra_pst_be" should be 13 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    # only one PST count as activated, as the other one didn't change its tap
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "cra_pst_fr" should be -15 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "cra_pst_be" should be 15 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pra_pst_fr" should be -15 after "CO1_fr2_fr3_1" at "curative"
    # Then the tap of PstRangeAction "pra_pst_be" should be 15 after "CO1_fr2_fr3_1" at "curative"
    # does not work currently: expected behaviour not clear yet in that case
    Then the worst margin is 992 A

  @fast @rao @ac @second-preventive @max-min-margin @ampere
  Scenario: US 13.5.11: CBCORA, CRA and PRA on same PSTs, with 2P optimisation
    # same results as previous test case, but important to test as RaoResult implementation
    # is not the same with 2d preventive optimisation
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct"
    Given crac file is "epic13/CBCORA_ep13us5case10.xml"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch rao at "2019-01-08 12:00"
    Then 2 remedial actions are used in preventive
    Then the tap of PstRangeAction "pra_pst_fr" should be -14 in preventive
    Then the tap of PstRangeAction "cra_pst_fr" should be -14 in preventive
    Then the tap of PstRangeAction "pra_pst_be" should be 10 in preventive
    Then the tap of PstRangeAction "cra_pst_be" should be 10 in preventive

    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    # only one PST count as activated, as the other one didn't change its tap
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "cra_pst_fr" is not used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "cra_pst_be" is used after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 996 A
    Then the execution details should be "Second preventive improved first preventive results"

  @fast @rao @ac @second-preventive @max-min-margin @ampere
  Scenario: US 13.5.11.bis: CBCORA, CRA and PRA on same PSTs, with global 2P optimisation
    # slightly better results thanks to the global 2P optimization
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct"
    Given crac file is "epic13/CBCORA_ep13us5case10.xml"
    Given configuration file is "epic13/RaoParameters_maxMargin_ampere_2p_global.json"
    When I launch rao at "2019-01-08 12:00"
    Then 2 remedial actions are used in preventive
    Then the tap of PstRangeAction "pra_pst_fr" should be -14 in preventive
    Then the tap of PstRangeAction "pra_pst_be" should be 10 in preventive
    Then the tap of PstRangeAction "cra_pst_fr" should be -14 in preventive
    Then the tap of PstRangeAction "cra_pst_be" should be 10 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    # only one PST count as activated, as the other one didn't change its tap
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "cra_pst_fr" should be -14 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "cra_pst_be" should be 15 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pra_pst_fr" should be -14 after "CO1_fr2_fr3_1" at "curative"
    # Then the tap of PstRangeAction "pra_pst_be" should be 15 after "CO1_fr2_fr3_1" at "curative"
    # does not work currently: expected behaviour not clear yet in that case
    Then the worst margin is 995 A
    Then the execution details should be "Second preventive improved first preventive results"