# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.17: cnecs in series with Pst range actions

  @fast @rao @mock @ac @preventive-only
  Scenario: US 15.17.1: Functional test case : PST tap change secures margin on CNEC_IN_SERIES - sensi(CNEC_IN_SERIES, PST) > 0
    Given network file is "epic15/TestCnecSeriesPst.uct"
    Given crac file is "epic15/CIM_15_17_noCo_largePSTTaps.xml"
    Given configuration file is "epic15/raoParametersSweIDCC_withoutPsts.json"
    Given crac creation parameters file is "validationTests/crac_swe_parameters.json"
    When I launch search_tree_rao at "2021-04-02 05:00"
    And the worst margin is 341.8 A
    Then 1 remedial actions are used in preventive
    And the remedial action "PST" is used in preventive
    And the tap of PstRangeAction "PST" should be -10 in preventive
    And the initial margin on cnec "CNEC_IN_SERIES - DIRECT - preventive" should be -181.22 A
    And the initial margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" should be 981.22 A
    And the margin on cnec "CNEC_IN_SERIES - DIRECT - preventive" after PRA should be 341.8 A
    And the margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" after PRA should be 458.2 A
    And the value of the objective function initially should be 181.22
    And the value of the objective function after PRA should be -341.8

  @fast @rao @mock @ac @preventive-only @cnec-series-pst
  Scenario: US 15.17.2: Functional test case : PST has enough taps to absorb margin deficit on CNEC_IN_SERIES - sensi(CNEC_IN_SERIES, PST) > 0
    Given network file is "epic15/TestCnecSeriesPst.uct"
    Given crac file is "epic15/CIM_15_17_noCo_largePSTTaps.xml"
    Given configuration file is "epic15/RaoParameters_ep15us17case2.json"
    When I launch search_tree_rao at "2021-04-02 05:00"
    Then 0 remedial actions are used in preventive
    And the worst margin is -181.22 A
    And the tap of PstRangeAction "PST" should be 0 in preventive
    And the initial margin on cnec "CNEC_IN_SERIES - DIRECT - preventive" should be -181.22 A
    And the initial margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" should be 981.22 A
    And the margin on cnec "CNEC_IN_SERIES - DIRECT - preventive" after PRA should be -181.22 A
    And the margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" after PRA should be 981.22 A
    And the value of the objective function initially should be -981.22
    And the value of the objective function after PRA should be -981.22

  @fast @rao @mock @ac @preventive-only
  Scenario: US 15.17.3: PST tap change improves (without securing) margin on CNEC_IN_SERIES - sensi(CNEC_IN_SERIES, PST) > 0
    Given network file is "epic15/TestCnecSeriesPst.uct"
    Given crac file is "epic15/CIM_15_17_noCo_lowPSTTaps.xml"
    Given configuration file is "epic15/raoParametersSweIDCC_withoutPsts.json"
    Given crac creation parameters file is "validationTests/crac_swe_parameters.json"
    When I launch search_tree_rao at "2021-04-02 05:00"
    And the worst margin is -76.56 A
    Then 1 remedial actions are used in preventive
    And the remedial action "PST" is used in preventive
    And the tap of PstRangeAction "PST" should be -2 in preventive
    And the initial margin on cnec "CNEC_IN_SERIES - DIRECT - preventive" should be -181.22 A
    And the initial margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" should be 981.22 A
    And the margin on cnec "CNEC_IN_SERIES - DIRECT - preventive" after PRA should be -76.56 A
    And the margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" after PRA should be 876.56 A
    And the value of the objective function initially should be 181.22
    And the value of the objective function after PRA should be 76.56

  @fast @rao @mock @ac @preventive-only @cnec-series-pst
  Scenario: US 15.17.4: Functional test case : PST doesn't have enough taps to absorb margin deficit on CNEC_IN_SERIES - sensi(CNEC_IN_SERIES, PST) > 0
    Given network file is "epic15/TestCnecSeriesPst.uct"
    Given crac file is "epic15/CIM_15_17_noCo_lowPSTTaps.xml"
    Given configuration file is "epic15/RaoParameters_ep15us17case2.json"
    Given crac creation parameters file is "validationTests/crac_swe_parameters.json"
    When I launch search_tree_rao at "2021-04-02 05:00"
    And the worst margin is -76.56 A
    Then 1 remedial actions are used in preventive
    And the remedial action "PST" is used in preventive
    And the tap of PstRangeAction "PST" should be -2 in preventive
    And the initial margin on cnec "CNEC_IN_SERIES - DIRECT - preventive" should be -181.22 A
    And the initial margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" should be 981.22 A
    And the margin on cnec "CNEC_IN_SERIES - DIRECT - preventive" after PRA should be -76.56 A
    And the margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" after PRA should be 876.56 A
    And the value of the objective function initially should be 181.22
    And the value of the objective function after PRA should be 76.56

  @fast @rao @mock @ac @preventive-only
  Scenario: US 15.17.5: Functional test case : PST tap change secures margin on CNEC_IN_SERIES - sensi(CNEC_IN_SERIES, PST) < 0
    Given network file is "epic15/TestCnecSeriesOppositePst.uct"
    Given crac file is "epic15/CIM_15_17_noCo_largePSTTaps_oppositePST.xml"
    Given configuration file is "epic15/raoParametersSweIDCC_withoutPsts.json"
    Given crac creation parameters file is "validationTests/crac_swe_parameters.json"
    When I launch search_tree_rao at "2021-04-02 05:00"
    And the worst margin is 539.04 A
    Then 1 remedial actions are used in preventive
    And the remedial action "PST" is used in preventive
    And the tap of PstRangeAction "PST" should be -10 in preventive
    And the initial margin on cnec "CNEC_IN_SERIES - OPPOSITE - preventive" should be -88.71 A
    And the initial margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" should be 1077.45 A
    And the margin on cnec "CNEC_IN_SERIES - OPPOSITE - preventive" after PRA should be 539.04 A
    And the margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" after PRA should be 1391.29 A
    And the value of the objective function initially should be 88.71
    And the value of the objective function after PRA should be -539.04

  @fast @rao @mock @ac @preventive-only @cnec-series-pst
  Scenario: US 15.17.6: Functional test case : PST has enough taps to absorb margin deficit on CNEC_IN_SERIES - sensi(CNEC_IN_SERIES, PST) < 0
    Given network file is "epic15/TestCnecSeriesOppositePst.uct"
    Given crac file is "epic15/CIM_15_17_noCo_largePSTTaps_oppositePST.xml"
    Given configuration file is "epic15/RaoParameters_ep15us17case6.json"
    When I launch search_tree_rao at "2021-04-02 05:00"
    Then 0 remedial actions are used in preventive
    And the worst margin is -88.71 A
    And the tap of PstRangeAction "PST" should be 0 in preventive
    And the initial margin on cnec "CNEC_IN_SERIES - OPPOSITE - preventive" should be -88.71 A
    And the initial margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" should be 1077.45 A
    And the margin on cnec "CNEC_IN_SERIES - OPPOSITE - preventive" after PRA should be -88.71 A
    And the margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" after PRA should be 1077.45 A
    And the value of the objective function initially should be -1077.45
    And the value of the objective function after PRA should be -1077.45

  @fast @rao @mock @ac @preventive-only @cnec-series-pst
  Scenario: US 15.17.7: PST tap change improves (without securing) margin on CNEC_IN_SERIES - sensi(CNEC_IN_SERIES, PST) < 0
    Given network file is "epic15/TestCnecSeriesOppositePst.uct"
    Given crac file is "epic15/CIM_15_17_noCo_lowPSTTaps_oppositePST.xml"
    Given configuration file is "epic15/raoParametersSweIDCC_withoutPsts.json"
    Given crac creation parameters file is "validationTests/crac_swe_parameters.json"
    When I launch search_tree_rao at "2021-04-02 05:00"
    And the worst margin is -25.89 A
    Then 1 remedial actions are used in preventive
    And the remedial action "PST" is used in preventive
    And the tap of PstRangeAction "PST" should be -1 in preventive
    And the initial margin on cnec "CNEC_IN_SERIES - OPPOSITE - preventive" should be -88.71 A
    And the initial margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" should be 1077.45 A
    And the margin on cnec "CNEC_IN_SERIES - OPPOSITE - preventive" after PRA should be -25.89 A
    And the margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" after PRA should be 1108.85 A
    And the value of the objective function initially should be 88.71
    And the value of the objective function after PRA should be 25.89

  @fast @rao @mock @ac @preventive-only @cnec-series-pst
  Scenario: US 15.17.8: Functional test case : PST doesn't have enough taps to absorb margin deficit on CNEC_IN_SERIES - sensi(CNEC_IN_SERIES, PST) < 0
    Given network file is "epic15/TestCnecSeriesOppositePst.uct"
    Given crac file is "epic15/CIM_15_17_noCo_lowPSTTaps_oppositePST.xml"
    Given configuration file is "epic15/RaoParameters_ep15us17case6.json"
    When I launch search_tree_rao at "2021-04-02 05:00"
    And the worst margin is -25.89 A
    Then 1 remedial actions are used in preventive
    And the remedial action "PST" is used in preventive
    And the tap of PstRangeAction "PST" should be -1 in preventive
    And the initial margin on cnec "CNEC_IN_SERIES - OPPOSITE - preventive" should be -88.71 A
    And the initial margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" should be 1077.45 A
    And the margin on cnec "CNEC_IN_SERIES - OPPOSITE - preventive" after PRA should be -25.89 A
    And the margin on cnec "CNEC_NOT_IN_SERIES - DIRECT - preventive" after PRA should be 1108.85 A
    And the value of the objective function initially should be 88.71
    And the value of the objective function after PRA should be 25.89
