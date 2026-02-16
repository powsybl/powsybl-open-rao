# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 13.2: Solve a RAO for two consecutive states (preventive THEN curative)

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 13.2.1: Simple case with preventive remedial actions only
    # Osiris does not find the global optimal solution, thus the test cas has been modified
    # If left alone, it would decide to use open_fr1_fr2, close_de3_de4, pst_fr, and pst_be to generate a minimum margin of 988 A
    # If we impose the usage of close_fr1_fr5, it would then only add pst_be and generate a minimum margin of 999 A
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us2case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 2 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 5 in preventive
    Then the tap of PstRangeAction "pst_be" should be -12 in preventive
    Then the worst margin is 999 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"
    Then the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 1064 A
    Then the value of the objective function after CRA should be -999

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 13.2.2: Simple case with curative remedial actions only
    # FARAO finds a better solution than OSIRIS
    # OSIRIS has worst margin 986 A with "open_fr1_fr2" and pst_be at 16 and pst_fr at 15
    # This solution exists in FARAO (we can see it in the logs) but a better solution is found
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us6basecase.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -12 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 1000 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 1000 A
    Then the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 1301 A
    Then the value of the objective function after CRA should be -1000

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 13.2.3: Simple case with a mix of preventive and curative remedial actions
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us2case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    # In preventive exactly the same results as OSIRIS
    Then 2 remedial actions are used in preventive
    Then the remedial action "open_be1_be4" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be -5 in preventive
    # FARAO PST tap is 15 whereas OSIRIS is 16, not a big difference
    # OSIRIS worst margin is 989 A and FARAO is actually 992 A, slightly better
    # I put the same values as in OSIRIS that are accepted due to acceptance margin in Cucumber
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 15 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 992 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 992 A
    Then the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 1495 A
    Then the value of the objective function after CRA should be -992

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 13.2.4: Complex case with preventive remedial actions only
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us2case4.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 4 remedial actions are used in preventive
    Then the remedial action "open_be1_be4" is used in preventive
    Then the remedial action "open_fr1_fr3" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be -5 in preventive
    Then the tap of PstRangeAction "pst_be" should be -15 in preventive
    Then the worst margin is 302 A on cnec "BBE4AA1  FFR5AA1  1 - preventive"
    Then the margin on cnec "BBE4AA1  FFR5AA1  1 - preventive" after PRA should be 302 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 311 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 319 A
    Then the margin on cnec "BBE4AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 376 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 437 A
    Then the value of the objective function after CRA should be -302

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 13.2.5: Complex case with curative remedial actions only
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us2case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -5 after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is -196 A on cnec "BBE2AA1  FFR3AA1  1 - preventive"
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -196 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 316 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 321 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 441 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 501 A
    Then the value of the objective function after CRA should be 196

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 13.2.6: Complex case with a mix of preventive and curative remedial actions (1/3)
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "epic13/SL_ep13us2case6.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 3 remedial actions are used in preventive
    Then the remedial action "open_be1_be4" is used in preventive
    Then the remedial action "open_fr1_fr2" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be -15 in preventive
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -5 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is -244 A on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative"
    Then the margin on cnec "BBE4AA1  FFR5AA1  1 - preventive" after PRA should be 300 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 308 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -244 A
    Then the margin on cnec "BBE4AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 366 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 417 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 469 A
    Then the value of the objective function after CRA should be 244

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 13.2.7: Complex case with a mix of preventive and curative remedial actions (2/3)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us2case6.json"
    Given configuration file is "epic13/RaoParameters_maxMargin_ampere_absolute_threshold.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 2 remedial actions are used in preventive
    Then the remedial action "open_be1_be4" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be -15 in preventive
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -5 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 298 A on cnec "BBE4AA1  FFR5AA1  1 - preventive"
    Then the margin on cnec "BBE4AA1  FFR5AA1  1 - preventive" after PRA should be 298 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 304 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 405 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 319 A
    Then the margin on cnec "BBE4AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 376 A
    Then the value of the objective function after CRA should be -298

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 13.2.8: Complex case with a mix of preventive and curative remedial actions (3/3)
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "epic13/SL_ep13us2case7.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 3 remedial actions are used in preventive
    Then the remedial action "open_fr1_fr2" is used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be -5 in preventive
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    # Difference with OSIRIS on PST tap explained below
    Then the tap of PstRangeAction "pst_be" should be 16 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 433 A on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative"
    Then the margin on cnec "FFR4AA1  DDE1AA1  1 - preventive" after PRA should be 597 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 601 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 643 A
    # Results after curative optimization are dramatically different between FARAO and OSIRIS because they are
    # not on the same cneces but the worst margin are similar (slightly better for FARAO +14 A)
    # After a comparing study between OSIRIS and FARAO logs, remedial actions are taken the same way and for
    # identical remedial actions results are the same (so flows are well computed and RAs are well applied even
    # taking preventive RAs into account). For a reason I ignore when OSIRIS make the linear optimization after applying
    # "open_fr1_fr3" it doesn't find the FARAO solution which is better (and verified through simple loadflow computation
    # in Convergence)
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 433 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 440 A
    Then the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 571 A
    Then the value of the objective function after CRA should be -433