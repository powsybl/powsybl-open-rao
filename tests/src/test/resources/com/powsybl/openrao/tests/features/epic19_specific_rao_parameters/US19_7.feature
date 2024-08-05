# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 19.7: handle CNECs belonging to TSOs that don't share CRAs

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.1.A: All CNECs belong to one operator not sharing CRAs - AMP
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic13/CBCORA_ep13us3case10.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    And the remedial action "close_de3_de4" is used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 0 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is -493 A
    And the value of the objective function after CRA should be -984.0
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be -493 A
    And the margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 784 A
    And the margin on cnec "fr3_fr5_CO1 - DIR - outage" after PRA should be 984 A
    And the margin on cnec "fr4_de1_N - preventive" after PRA should be 984 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.2.A: All CNECs belong to one operator sharing one CRA - AMP
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case2.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.3.A: Most limiting CNEC belongs to operator not sharing CRAs - AMP
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case3.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    And the remedial action "close_de3_de4" is used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is -450 A
    And the value of the objective function after CRA should be -984.0
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be -450 A
    And the margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 998 A
    And the margin on cnec "fr3_fr5_CO1 - DIR - outage" after PRA should be 984 A
    And the margin on cnec "fr4_de1_N - preventive" after PRA should be 984 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.4.A: Second most limiting CNEC belongs to operator not sharing CRAs - AMP
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case4.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.5.A: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and is improved in CRAO - AMP
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case5.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    And the remedial action "close_de3_de4" is used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 3 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr_cur" should be -5 after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is 45 A
    And the value of the objective function after CRA should be -718.0
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 738 A
    And the margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 45 A
    And the margin on cnec "fr3_fr5_CO1 - DIR - outage" after PRA should be 984 A
    And the margin on cnec "fr4_de1_CO1 - curative" after CRA should be 718 A
    And the margin on cnec "fr4_de1_N - preventive" after PRA should be 984 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.6.A: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and can become most limiting in CRAO - AMP
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case6.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.7.A: Only PSTs - All CNECs belong to one operator sharing a CRA - AMP
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case7.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -3 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is 603 A
    And the value of the objective function after CRA should be -603.0
    And the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 603 A
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 610 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.8.A: Only PSTs - Most limiting CNEC belongs to an operator not sharing CRAs - AMP
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case8.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 9 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 4 after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is 130 A
    And the value of the objective function after CRA should be -827.0
    And the margin on cnec "fr4_de1_CO1 - curative" after CRA should be 827 A
    And the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 827 A
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 130 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.9.A: Only PSTs - Second most limiting CNEC belongs to an operator not sharing CRAs - AMP
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case9.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_ampere_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -3 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is 603 A
    And the value of the objective function after CRA should be -603.0
    And the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 603 A
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 610 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.1.MW: All CNECs belong to one operator not sharing CRAs - MW
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic13/CBCORA_ep13us3case10.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    And the remedial action "close_de3_de4" is used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 0 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is -339 MW
    And the value of the objective function after CRA should be -682.0
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be -339 MW
    And the margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 543 MW
    And the margin on cnec "fr3_fr5_CO1 - DIR - outage" after PRA should be 682 MW
    And the margin on cnec "fr4_de1_N - preventive" after PRA should be 682 MW

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.2.MW: All CNECs belong to one operator sharing one CRA - MW
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case2.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.3.MW: Most limiting CNEC belongs to operator not sharing CRAs - MW
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case3.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    And the remedial action "close_de3_de4" is used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is -310 MW
    And the value of the objective function after CRA should be -682.0
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be -310 MW
    And the margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 691 MW
    And the margin on cnec "fr3_fr5_CO1 - DIR - outage" after PRA should be 682 MW
    And the margin on cnec "fr4_de1_N - preventive" after PRA should be 682 MW

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.4.MW: Second most limiting CNEC belongs to operator not sharing CRAs - MW
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case4.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.5.MW: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and is improved in CRAO - MW
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case5.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 3 remedial actions are used in preventive
    And the remedial action "close_de3_de4" is used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 3 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr_cur" should be -5 after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is 31 MW
    And the value of the objective function after CRA should be -498.0
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 512 MW
    And the margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 31 MW
    And the margin on cnec "fr3_fr5_CO1 - DIR - outage" after PRA should be 682 MW
    And the margin on cnec "fr4_de1_CO1 - curative" after CRA should be 498 MW
    And the margin on cnec "fr4_de1_N - preventive" after PRA should be 682 MW

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.6.MW: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and can become most limiting in CRAO - MW
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case6.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.7.MW: Only PSTs - All CNECs belong to one operator sharing a CRA - MW
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case7.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -2 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is 420 MW
    And the value of the objective function after CRA should be -420.0
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 420 MW
    And the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 430 MW

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.8.MW: Only PSTs - Most limiting CNEC belongs to an operator not sharing CRAs - MW
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case8.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 9 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 4 after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is 91 MW
    And the value of the objective function after CRA should be -572.0
    And the margin on cnec "fr4_de1_CO1 - curative" after CRA should be 573 MW
    And the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 572 MW
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 91 MW

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.7.9.MW: Only PSTs - Second most limiting CNEC belongs to an operator not sharing CRAs - MW
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case9.xml"
    Given configuration file is "epic19/RaoParameters_maxMargin_megawatt_shareCra.json"
    When I launch search_tree_rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -2 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is 420 MW
    And the value of the objective function after CRA should be -420.0
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 420 MW
    And the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 430 MW

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.1.A.R: All CNECs belong to one operator not sharing CRAs - AMP - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic13/CBCORA_ep13us3case10.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 4 remedial actions are used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the remedial action "open_fr1_fr2" is used in preventive
    And the remedial action "close_de3_de4" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 0 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is -903 A
    And the value of the objective function after CRA should be -1269.0
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be -903 A
    And the relative margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 1081 A
    And the relative margin on cnec "fr4_de1_N - preventive" after PRA should be 1269 A
    And the relative margin on cnec "fr4_de1_CO1 - curative" after CRA should be 1269 A

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.2.A.R: All CNECs belong to one operator sharing one CRA - AMP - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case2.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"
    And the worst relative margin is 112.23 MW

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.3.A.R: Most limiting CNEC belongs to operator not sharing CRAs - AMP - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case3.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 4 remedial actions are used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the remedial action "open_fr1_fr2" is used in preventive
    And the remedial action "close_de3_de4" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -9 after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is -880 A
    And the value of the objective function after CRA should be -1228.0
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be -880 A
    And the relative margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 1228 A
    And the relative margin on cnec "fr4_de1_N - preventive" after PRA should be 1269 A
    And the relative margin on cnec "fr4_de1_CO1 - curative" after CRA should be 1309 A

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.4.A.R: Second most limiting CNEC belongs to operator not sharing CRAs - AMP - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case4.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"
    And the worst relative margin is 112.23 MW


  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.5.A.R: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and is improved in CRAO - AMP - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case5R.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 4 remedial actions are used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the remedial action "open_fr1_fr2" is used in preventive
    And the remedial action "close_de3_de4" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 3 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be -10 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr_cur" should be -10 after "CO1_fr2_fr3_1" at "curative"
    And the worst relative margin is 513 A
    And the value of the objective function after CRA should be -709.0
    And the relative margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 513 A
    And the relative margin on cnec "fr4_de1_CO1 - curative" after CRA should be 709 A
    And the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 736 A
    And the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 961 A
    And the relative margin on cnec "fr4_de1_N - preventive" after PRA should be 1269 A

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.6.A.R: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and can become most limiting in CRAO - AMP - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case6R.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"
    And the worst relative margin is 289.88 MW


  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.7.A.R: Only PSTs - All CNECs belong to one operator sharing a CRA - AMP - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case7.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra_mip.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 2 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    And the worst relative margin is 833 A
    And the value of the objective function after CRA should be -833.0
    And the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 843 A
    And the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 833 A

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.8.A.R: Only PSTs - Most limiting CNEC belongs to an operator not sharing CRAs - AMP - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case8.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra_mip.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 13 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 4 after "CO1_fr2_fr3_1" at "curative"
    And the worst relative margin is 166 A
    And the value of the objective function after CRA should be -1084.0
    And the relative margin on cnec "fr4_de1_CO1 - curative" after CRA should be 1084 A
    And the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 1097 A
    And the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 166 A

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.9.A.R: Only PSTs - Second most limiting CNEC belongs to an operator not sharing CRAs - AMP - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case9.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_ampere_shareCra_mip.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 2 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    And the worst relative margin is 833 A
    And the value of the objective function after CRA should be -833.0
    And the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 843 A
    And the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 833 A

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.1.MW.R: All CNECs belong to one operator not sharing CRAs - MW - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic13/CBCORA_ep13us3case10.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 4 remedial actions are used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the remedial action "open_fr1_fr2" is used in preventive
    And the remedial action "close_de3_de4" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 0 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is -622 MW
    And the value of the objective function after CRA should be -879.0
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be -622 MW
    And the relative margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 749 MW
    And the relative margin on cnec "fr4_de1_N - preventive" after PRA should be 879 MW
    And the relative margin on cnec "fr4_de1_CO1 - curative" after CRA should be 879 MW

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.2.MW.R: All CNECs belong to one operator sharing one CRA - MW - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case2.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"
    And the worst relative margin is 112.23 MW

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.3.MW.R: Most limiting CNEC belongs to operator not sharing CRAs - MW - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case3.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 4 remedial actions are used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the remedial action "open_fr1_fr2" is used in preventive
    And the remedial action "close_de3_de4" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -9 after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is -606 MW
    And the value of the objective function after CRA should be -851.0
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be -606 MW
    And the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 851 MW
    And the relative margin on cnec "fr4_de1_N - preventive" after PRA should be 879 MW
    And the relative margin on cnec "fr4_de1_CO1 - curative" after CRA should be 907 MW

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.4.MW.R: Second most limiting CNEC belongs to operator not sharing CRAs - MW - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case4.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"
    And the worst relative margin is 112.23 MW

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.5.MW.R: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and is improved in CRAO - MW - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case5R.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 4 remedial actions are used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the remedial action "open_fr1_fr2" is used in preventive
    And the remedial action "close_de3_de4" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 3 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be -10 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr_cur" should be -10 after "CO1_fr2_fr3_1" at "curative"
    And the worst relative margin is 355 MW
    And the value of the objective function after CRA should be -491.0
    And the relative margin on cnec "fr4_de1_CO1 - curative" after CRA should be 491 MW
    And the relative margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 355 MW

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.6.MW.R: Second most limiting CNEC after PRA belongs to operator not sharing CRAs, and can become most limiting in CRAO - MW - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case6R.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"
    And the worst relative margin is 289.88 MW

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.7.MW.R: Only PSTs - All CNECs belong to one operator sharing a CRA - MW - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case7.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra_mip.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 2 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    And the worst relative margin is 578 MW
    And the value of the objective function after CRA should be -578.0
    And the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 585 MW
    And the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 578 MW

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.8.MW.R: Only PSTs - Most limiting CNEC belongs to an operator not sharing CRAs - MW - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case8.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra_mip.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 13 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 4 after "CO1_fr2_fr3_1" at "curative"
    And the worst relative margin is 115 MW
    And the value of the objective function after CRA should be -751.0
    And the relative margin on cnec "fr4_de1_CO1 - curative" after CRA should be 751 MW
    And the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 760 MW
    And the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 115 MW

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: 19.7.9.MW.R: Only PSTs - Second most limiting CNEC belongs to an operator not sharing CRAs - MW - relative
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic19/CBCORA_ep19us7case9.xml"
    Given configuration file is "epic19/RaoParameters_maxRelMargin_megawatt_shareCra_mip.json"
    Given Glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao at "2019-01-08 12:00" after "CO1_fr2_fr3_1" at "curative"
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 2 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be -5 after "CO1_fr2_fr3_1" at "curative"
    And the worst relative margin is 578 MW
    And the value of the objective function after CRA should be -578.0
    And the relative margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 585 MW
    And the relative margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 578 MW
