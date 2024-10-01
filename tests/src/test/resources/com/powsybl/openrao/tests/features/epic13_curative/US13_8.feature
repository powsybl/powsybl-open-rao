# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 13.8: cross-validation curative and relative margin

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: US 13.8.1: Full optimization in relative margin with negative margin in curative
    # Same case as 13.2.6 but with relative margin. No impact with relative margins, same optimization except one PST tap
    # in preventive, this can be explained by the fact that BE2-FR3 becomes the most limiting element due to relative margins
    # compared to initial case. In curative, remedial actions are the same because the most limiting element has negative margin
    # So it remains the same between the two cases.
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us2case6.json"
    Given configuration file is "epic13/RaoParameters_relMargin_ampere.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao
    Then 3 remedial actions are used in preventive
    And the remedial action "open_be1_be4" is used in preventive
    And the remedial action "open_fr1_fr2" is used in preventive
    And the tap of PstRangeAction "pst_be" should be -16 in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 5 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -244 A on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative"
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -244 A
    And the relative margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 278 A
    Then the relative margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 471 A
    Then the relative margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 558 A
    And the relative margin on cnec "BBE4AA1  FFR5AA1  1 - preventive" after PRA should be 513 A
    And the relative margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 543 A
    Then the relative margin on cnec "BBE4AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 693 A
    And the value of the objective function after CRA should be 244

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: US 13.8.2: Full optimization in absolute margin with positive margin in curative
    # Curative limiting element of previous case has been removed so that limiting element in curative has a positive
    # absolute margin. This case is a reference for the following one.
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us8case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 3 remedial actions are used in preventive
    And the remedial action "open_be1_be4" is used in preventive
    And the remedial action "open_fr1_fr2" is used in preventive
    And the tap of PstRangeAction "pst_be" should be -15 in preventive
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 12 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 300 A on cnec "BBE4AA1  FFR5AA1  1 - preventive"
    And the margin on cnec "BBE4AA1  FFR5AA1  1 - preventive" after PRA should be 300 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 308 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 408 A
    Then the margin on cnec "BBE4AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 411 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 470 A
    And the value of the objective function after CRA should be -300

  @fast @rao @mock @ac @contingency-scenarios @relative
  Scenario: US 13.8.3: Full optimization in relative margin with positive margin in curative
    # Optimizations in both preventive and curative are slightly different since due to PTDF factor limiting elements
    # are not the same in both situations.
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us8case2.json"
    Given configuration file is "epic13/RaoParameters_relMargin_ampere.json"
    Given loopflow glsk file is "common/glsk_proportional_16nodes.xml"
    When I launch search_tree_rao
    Then 3 remedial actions are used in preventive
    And the remedial action "open_be1_be4" is used in preventive
    And the remedial action "open_fr1_fr2" is used in preventive
    And the tap of PstRangeAction "pst_be" should be -16 in preventive
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 12 after "co1_fr2_fr3_1" at "curative"
    And the worst relative margin is 278 A on cnec "BBE2AA1  FFR3AA1  1 - preventive"
    And the relative margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 278 A
    Then the relative margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 494 A
    Then the relative margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 508 A
    And the relative margin on cnec "BBE4AA1  FFR5AA1  1 - preventive" after PRA should be 514 A
    And the relative margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 543 A
    Then the relative margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 604 A
    Then the relative margin on cnec "BBE4AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 726 A
    And the value of the objective function after CRA should be -278