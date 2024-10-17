# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 13.3 : Solve a RAO for N compounds states

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.3.1: Simple case with preventive, outage and curative states
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us3case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"
    And 0 remedial actions are used in preventive
    And the worst margin is 114 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.3.2: Simple case, with 2 curative states
    # In order to have the same topological RAs used as in Osiris, we have to increase
    # the usage threshold here (absolute-network-action-minimum-impact-threshold)
    # But the minimum margin would have been the same without the increase
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us3case2.json"
    Given configuration file is "epic13/RaoParameters_maxMargin_ampere_absolute_threshold.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 9 after "co1_fr2_fr3_1" at "curative"
    And 3 remedial actions are used after "co2_be1_be3" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co2_be1_be3" at "curative"
    And the remedial action "open_be1_be4" is used after "co2_be1_be3" at "curative"
    And the remedial action "open_fr1_fr2" is used after "co2_be1_be3" at "curative"
    And the worst margin is 766 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - curative" after CRA should be 766 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 992 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - preventive" after PRA should be 1124 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 1198 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - curative" after CRA should be 1281 A
    And the value of the objective function after CRA should be -766

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.3.3: Simple case, with 2 curative states and on-contingency remedial actions
    # In order to have the same topological RAs used as in Osiris, we have to increase
    # the usage threshold here (absolute-network-action-minimum-impact-threshold)
    # But the minimum margin would have been slightly better than Osiris without the increase
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us3case3.json"
    Given configuration file is "epic13/RaoParameters_maxMargin_ampere_absolute_threshold.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr2" is used after "co1_fr2_fr3_1" at "curative"
    And 2 remedial actions are used after "co2_be1_be3" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co2_be1_be3" at "curative"
    And the remedial action "open_be1_be4" is used after "co2_be1_be3" at "curative"
    And the worst margin is 753 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - curative" after CRA should be 753 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 865 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - preventive" after PRA should be 1124 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - curative" after CRA should be 1229 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 1279 A
    And the value of the objective function after CRA should be -753

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.3.4: Complex case, with several outage/curative states, and on-contingency remedial actions
    # In order to have the same topological RAs used as in Osiris, we have to increase
    # the usage threshold here (absolute-network-action-minimum-impact-threshold)
    # But the minimum margin would have been the same without the increase
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us3case4.json"
    Given configuration file is "epic13/RaoParameters_maxMargin_ampere_absolute_threshold.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And 1 remedial actions are used after "co2_be1_be3" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co2_be1_be3" at "curative"
    And the worst margin is -184 A
    And the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - outage" after PRA should be -184 A
    And the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - curative" after CRA should be -141 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - outage" after PRA should be 332 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - curative" after CRA should be 544 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 782 A
    And the value of the objective function after CRA should be 184

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.3.5: Simple case, with two curative states, including one without CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us3case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 4 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the remedial action "open_be1_be4" is used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_de3_de4" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr2" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 469 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - curative" after CRA should be 469 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 875 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - preventive" after PRA should be 1004 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - curative" after CRA should be 1031 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 1049 A
    And the value of the objective function after CRA should be -469

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.3.6: Simple case, with two outage + curative states, including one without CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us3case6.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And 2 remedial actions are used after "co2_be1_be3" at "curative"
    And the remedial action "open_be1_be4" is used after "co2_be1_be3" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co2_be1_be3" at "curative"
    And the worst margin is -484 A
    And the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - curative" after CRA should be -484 A
    # For contingency co1_fr2_fr3_1 that has no CRA, Osiris automatically decreases the threshold in outage to match the treshold in curative state
    # Farao improves this by keeping the threshold from the crac. That is why the margin in Farao is larger by 300A than the one in Osiris.
    And the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - outage" after PRA should be -184 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - outage" after PRA should be 232 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - preventive" after PRA should be 525 A
    # For the same reason explained above, the margin in Farao is larger by 100A than the one in Osiris.
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 649 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 549 A
    And the value of the objective function after CRA should be 484

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.3.7: Simple case, with one outage and one curative state, but on two different contingencies
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us3case7.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 15 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -115 A
    And the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - curative" after CRA should be -115 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - preventive" after PRA should be 306 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - outage" after PRA should be 332 A
    And the value of the objective function after CRA should be 115

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.3.8: Complex case, with several curative / outage states, and some curative states without CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us3case8.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And the tap of PstRangeAction "pst_be" should be -16 in preventive
    And 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And 1 remedial actions are used after "co3_fr1_fr3" at "curative"
    And the remedial action "open_fr1_fr2" is used after "co3_fr1_fr3" at "curative"
    And the worst margin is 418 A
    And the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - outage" after PRA should be 418 A
    And the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - curative" after CRA should be 485 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - curative" after CRA should be 544 A
    # For the same reason explained in 13.3.6, the margin in Farao is larger by 200A than the one in Osiris.
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - outage" after PRA should be 744 A
    And the margin on cnec "FFR1AA1  FFR2AA1  1 - co3_fr1_fr3 - outage" after PRA should be 857 A
    And the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 886 A
    And the value of the objective function after CRA should be -418

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.3.9: Test case with no RA in the preventive perimeter
    # In order to have the same topological RAs used as in Osiris, we have to increase
    # the usage threshold here (absolute-network-action-minimum-impact-threshold)
    # But the minimum margin would have been better by 10A without the increase
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us3case9.json"
    Given configuration file is "epic13/RaoParameters_maxMargin_ampere_absolute_threshold_12.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -8 after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 677 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 677 A
    And the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 678 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - preventive" after PRA should be 852 A
    And the value of the objective function after CRA should be -677

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.3.10: Test case with a CBCORA file
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct"
    Given crac file is "epic13/CBCORA_ep13us3case10.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the optimization steps executed by the RAO should be "FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION"
    And the worst margin is 114 A