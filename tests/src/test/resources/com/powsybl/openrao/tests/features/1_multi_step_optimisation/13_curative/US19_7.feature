# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 19.7: handle CNECs belonging to TSOs that don't share CRAs

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.7.1.A: All CNECs belong to one operator not sharing CRAs - AMP
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case1.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 0 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is -81 A
    Then the value of the objective function after CRA should be -890.0
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be -81 A
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 690 A
    Then the margin on cnec "fr3_fr5_CO1 - OPP - outage" after PRA should be 890 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.7.2.A: All CNECs belong to one operator sharing one CRA - AMP
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case2.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 519 A
    Then the value of the objective function after CRA should be -519
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 519 A
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 557 A
    Then the margin on cnec "fr3_fr5_CO1 - OPP - outage" after PRA should be 890 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.7.3.A: Most limiting CNEC belongs to operator not sharing CRAs - AMP
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case3.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 10 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 478 A
    Then the value of the objective function after CRA should be -890.0
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 478 A
    Then the margin on cnec "fr3_fr5_CO1 - OPP - outage" after PRA should be 890 A
    Then the margin on cnec "fr1_fr4_CO1 - outage" after CRA should be 919 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.7.4.A: Second most limiting CNEC belongs to operator not sharing CRAs - AMP
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case4.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 519 A
    Then the value of the objective function after CRA should be -519
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 519 A
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 557 A
    Then the margin on cnec "fr3_fr5_CO1 - OPP - outage" after PRA should be 890 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.7.5.A: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and is improved in CRAO - AMP
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case5.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 2 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr_cur" should be -4 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 511 A
    Then the value of the objective function after CRA should be -689.0
    Then the margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 511 A
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 689 A
    Then the margin on cnec "fr4_de1_CO1 - curative" after CRA should be 716 A
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 744 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.7.6.A: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and can become most limiting in CRAO - AMP
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case6.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 463 A
    Then the value of the objective function after CRA should be -557
    Then the margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 463 A
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 557 A
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 719 A
    Then the margin on cnec "fr3_fr5_CO1 - OPP - outage" after CRA should be 890 A

  @fast @rao @ac @contingency-scenarios @search-tree-rao @max-min-margin @ampere
  Scenario: 19.7.7.A: Only PSTs - All CNECs belong to one operator sharing a CRA - AMP
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case7.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -3 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 603 A
    Then the value of the objective function after CRA should be -603.0
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 603 A
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 610 A

  @fast @rao @ac @contingency-scenarios @search-tree-rao @max-min-margin @ampere
  Scenario: 19.7.8.A: Only PSTs - Most limiting CNEC belongs to an operator not sharing CRAs - AMP
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case8.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 9 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be 4 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 130 A
    Then the value of the objective function after CRA should be -827.0
    Then the margin on cnec "fr4_de1_CO1 - curative" after CRA should be 827 A
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 827 A
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 130 A

  @fast @rao @ac @contingency-scenarios @search-tree-rao @max-min-margin @ampere
  Scenario: 19.7.9.A: Only PSTs - Second most limiting CNEC belongs to an operator not sharing CRAs - AMP
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case9.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -3 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 603 A
    Then the value of the objective function after CRA should be -603.0
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 603 A
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 610 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @megawatt
  Scenario: 19.7.1.MW: All CNECs belong to one operator not sharing CRAs - MW
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case1.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 0 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is -81 A
    Then the value of the objective function after CRA should be -616.0
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be -54 MW
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 478 MW
    Then the margin on cnec "fr3_fr5_CO1 - OPP - outage" after PRA should be 616 MW

  @fast @rao @ac @contingency-scenarios @max-min-margin @megawatt
  Scenario: 19.7.2.MW: All CNECs belong to one operator sharing one CRA - MW
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case2.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 360 MW
    Then the value of the objective function after CRA should be -360
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 360 MW
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 385 MW
    Then the margin on cnec "fr3_fr5_CO1 - OPP - outage" after PRA should be 617 MW

  @fast @rao @ac @contingency-scenarios @max-min-margin @megawatt
  Scenario: 19.7.3.MW: Most limiting CNEC belongs to operator not sharing CRAs - MW
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case3.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 10 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 332 MW
    Then the value of the objective function after CRA should be -617
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 332 MW
    Then the margin on cnec "fr3_fr5_CO1 - OPP - outage" after PRA should be 617 MW
    Then the margin on cnec "fr1_fr4_CO1 - outage" after CRA should be 638 MW

  @fast @rao @ac @contingency-scenarios @max-min-margin @megawatt
  Scenario: 19.7.4.MW: Second most limiting CNEC belongs to operator not sharing CRAs - MW
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case4.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 360 MW
    Then the value of the objective function after CRA should be -360
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 360 MW
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 386 MW
    Then the margin on cnec "fr3_fr5_CO1 - OPP - outage" after PRA should be 617 MW

  @fast @rao @ac @contingency-scenarios @max-min-margin @megawatt
  Scenario: 19.7.5.MW: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and is improved in CRAO - MW
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case5.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 2 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr_cur" should be -4 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 354 MW
    Then the value of the objective function after CRA should be -478.0
    Then the margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 354 MW
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 478 MW
    Then the margin on cnec "fr4_de1_CO1 - curative" after CRA should be 496 MW
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 516 MW

  @fast @rao @ac @contingency-scenarios @max-min-margin @megawatt
  Scenario: 19.7.6.MW: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and can become most limiting in CRAO - MW
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case6.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 321 MW
    Then the value of the objective function after CRA should be -386
    Then the margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 321 MW
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 386 MW
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 499 MW
    Then the margin on cnec "fr3_fr5_CO1 - OPP - outage" after CRA should be 617 MW

  @fast @rao @ac @contingency-scenarios @search-tree-rao @max-min-margin @megawatt
  Scenario: 19.7.7.MW: Only PSTs - All CNECs belong to one operator sharing a CRA - MW
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case7.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -2 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 420 MW
    Then the value of the objective function after CRA should be -420.0
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 420 MW
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 430 MW

  @fast @rao @ac @contingency-scenarios @search-tree-rao @max-min-margin @megawatt
  Scenario: 19.7.8.MW: Only PSTs - Most limiting CNEC belongs to an operator not sharing CRAs - MW
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case8.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 9 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be 4 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 91 MW
    Then the value of the objective function after CRA should be -572.0
    Then the margin on cnec "fr4_de1_CO1 - curative" after CRA should be 573 MW
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 572 MW
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 91 MW

  @fast @rao @ac @contingency-scenarios @search-tree-rao @max-min-margin @megawatt
  Scenario: 19.7.9.MW: Only PSTs - Second most limiting CNEC belongs to an operator not sharing CRAs - MW
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case9.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -2 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 420 MW
    Then the value of the objective function after CRA should be -420.0
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 420 MW
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 430 MW

  @fast @rao @ac @contingency-scenarios @max-min-relative-margin @ampere
  Scenario: 19.7.1.A.R: All CNECs belong to one operator not sharing CRAs - AMP - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case1.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the remedial action "close_de3_de4" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 0 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is -136 A
    Then the value of the objective function after CRA should be -1198.0
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be -136 A
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 1059 A
    Then the relative margin on cnec "fr4_de1_N - preventive" after PRA should be 1198 A

  @fast @rao @ac @contingency-scenarios @max-min-relative-margin @ampere
  Scenario: 19.7.2.A.R: All CNECs belong to one operator sharing one CRA - AMP - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case2.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the remedial action "close_de3_de4" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 326 A
    Then the value of the objective function after CRA should be -458.0
    Then the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 458 A
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 1050 A
    Then the relative margin on cnec "fr4_de1_N - preventive" after PRA should be 1198 A

  @fast @rao @ac @contingency-scenarios @max-min-relative-margin @ampere
  Scenario: 19.7.3.A.R: Most limiting CNEC belongs to operator not sharing CRAs - AMP - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case3.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the remedial action "close_de3_de4" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -7 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 314 A
    Then the value of the objective function after CRA should be -1198.0
    Then the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 441 A
    Then the relative margin on cnec "fr4_de1_N - preventive" after PRA should be 1198 A
    Then the relative margin on cnec "fr1_fr4_CO1 - outage" after CRA should be 1215 A

  @fast @rao @ac @contingency-scenarios @max-min-relative-margin @ampere
  Scenario: 19.7.4.A.R: Second most limiting CNEC belongs to operator not sharing CRAs - AMP - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case4.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the remedial action "close_de3_de4" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 326 A
    Then the value of the objective function after CRA should be -458.0
    Then the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 458 A
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 1050 A
    Then the relative margin on cnec "fr4_de1_N - preventive" after PRA should be 1198 A


  @fast @rao @ac @contingency-scenarios @max-min-relative-margin @ampere
  Scenario: 19.7.5.A.R: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and is improved in CRAO - AMP - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case5R.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the remedial action "close_de3_de4" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 15 in preventive
    Then 3 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -14 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr_cur" should be 7 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 324 A
    Then the value of the objective function after CRA should be -1076.0
    Then the relative margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 399 A
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 1076 A
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - outage" after PRA should be 1334 A

  @fast @rao @ac @contingency-scenarios @max-min-relative-margin @ampere
  Scenario: 19.7.6.A.R: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and can become most limiting in CRAO - AMP - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case6R.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the remedial action "close_de3_de4" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 15 in preventive
    Then 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 382 A
    Then the value of the objective function after CRA should be -669.0
    Then the relative margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 470 A
    Then the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 669 A
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 784 A


  @fast @rao @ac @contingency-scenarios @search-tree-rao @max-min-relative-margin @ampere
  Scenario: 19.7.7.A.R: Only PSTs - All CNECs belong to one operator sharing a CRA - AMP - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case7.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra_mip.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 2 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    Then the worst relative margin is 833 A
    Then the value of the objective function after CRA should be -833.0
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 843 A
    Then the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 833 A

  @fast @rao @ac @contingency-scenarios @search-tree-rao @max-min-relative-margin @ampere
  Scenario: 19.7.8.A.R: Only PSTs - Most limiting CNEC belongs to an operator not sharing CRAs - AMP - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case8.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra_mip.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 13 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be 4 after "CO1_fr2_fr3_1" at "curative"
    Then the worst relative margin is 164 A
    Then the value of the objective function after CRA should be -1084.0
    Then the relative margin on cnec "fr4_de1_CO1 - curative" after CRA should be 1084 A
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 1097 A
    Then the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 164 A

  @fast @rao @ac @contingency-scenarios @search-tree-rao @max-min-relative-margin @ampere
  Scenario: 19.7.9.A.R: Only PSTs - Second most limiting CNEC belongs to an operator not sharing CRAs - AMP - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case9.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra_mip.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 2 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    Then the worst relative margin is 833 A
    Then the value of the objective function after CRA should be -833.0
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 843 A
    Then the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 833 A

  @fast @rao @ac @contingency-scenarios @max-min-relative-margin @megawatt
  Scenario: 19.7.1.MW.R: All CNECs belong to one operator not sharing CRAs - MW - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case1.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the remedial action "close_de3_de4" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 0 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is -93 MW
    Then the value of the objective function after CRA should be -830
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be -93 MW
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 734 MW
    Then the relative margin on cnec "fr4_de1_N - preventive" after PRA should be 830 MW

  @fast @rao @ac @contingency-scenarios @max-min-relative-margin @megawatt
  Scenario: 19.7.2.MW.R: All CNECs belong to one operator sharing one CRA - MW - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case2.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the remedial action "close_de3_de4" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 227 MW
    Then the value of the objective function after CRA should be -319.0
    Then the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 319 MW
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 727 MW
    Then the relative margin on cnec "fr4_de1_N - preventive" after PRA should be 830 MW

  @fast @rao @ac @contingency-scenarios @max-min-relative-margin @megawatt
  Scenario: 19.7.3.MW.R: Most limiting CNEC belongs to operator not sharing CRAs - MW - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case3.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the remedial action "close_de3_de4" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -7 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 218 MW
    Then the value of the objective function after CRA should be -830.0
    Then the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 306 MW
    Then the relative margin on cnec "fr4_de1_N - preventive" after PRA should be 830 MW
    Then the relative margin on cnec "fr1_fr4_CO1 - outage" after CRA should be 844 MW

  @fast @rao @ac @contingency-scenarios @max-min-relative-margin @megawatt
  Scenario: 19.7.4.MW.R: Second most limiting CNEC belongs to operator not sharing CRAs - MW - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case4.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the remedial action "close_de3_de4" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 9 in preventive
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 227 MW
    Then the value of the objective function after CRA should be -319.0
    Then the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 319 MW
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 727 MW
    Then the relative margin on cnec "fr4_de1_N - preventive" after PRA should be 830 MW

  @fast @rao @ac @contingency-scenarios @max-min-relative-margin @megawatt
  Scenario: 19.7.5.MW.R: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and is improved in CRAO - MW - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case5R.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the remedial action "close_de3_de4" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 15 in preventive
    Then 3 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -14 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr_cur" should be 7 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 225 MW
    Then the value of the objective function after CRA should be -745.0
    Then the relative margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 276 MW
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 745 MW
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - outage" after PRA should be 924 MW

  @fast @rao @ac @contingency-scenarios @max-min-relative-margin @megawatt
  Scenario: 19.7.6.MW.R: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and can become most limiting in CRAO - MW - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case6R.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the remedial action "close_de3_de4" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 15 in preventive
    Then 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 265 MW
    Then the value of the objective function after CRA should be -466
    Then the relative margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 325 MW
    Then the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 466 MW
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 543 MW

  @fast @rao @ac @contingency-scenarios @search-tree-rao @max-min-relative-margin @megawatt
  Scenario: 19.7.7.MW.R: Only PSTs - All CNECs belong to one operator sharing a CRA - MW - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case7.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra_mip.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 2 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    Then the worst relative margin is 578 MW
    Then the value of the objective function after CRA should be -578.0
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 585 MW
    Then the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 578 MW

  @fast @rao @ac @contingency-scenarios @search-tree-rao @max-min-relative-margin @megawatt
  Scenario: 19.7.8.MW.R: Only PSTs - Most limiting CNEC belongs to an operator not sharing CRAs - MW - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case8.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra_mip.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 13 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be 4 after "CO1_fr2_fr3_1" at "curative"
    Then the worst relative margin is 115 MW
    Then the value of the objective function after CRA should be -751.0
    Then the relative margin on cnec "fr4_de1_CO1 - curative" after CRA should be 751 MW
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 760 MW
    Then the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 115 MW

  @fast @rao @ac @contingency-scenarios @search-tree-rao @max-min-relative-margin @megawatt
  Scenario: 19.7.9.MW.R: Only PSTs - Second most limiting CNEC belongs to an operator not sharing CRAs - MW - relative
    Given network file is "epic19/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case9.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra_mip.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 2 after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    Then the worst relative margin is 578 MW
    Then the value of the objective function after CRA should be -578.0
    Then the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 585 MW
    Then the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 578 MW
