# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: Advanced usage rules tests

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US UR.1.1: Flow constraint in country with no contingency
    # This is a copy of test case 16.5.7
    # pst_be is available after a flow constraint in BE, no contingency defined
    # So the same results as 16.5.7 are expected
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "extra_features/Crac_UR_1_1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 1 remedial actions are used in preventive
    Then the remedial action "pst_be" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be 16 in preventive
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 82 A
    Then the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 82 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 245 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 345 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US UR.1.1: Flow constraint in country only after a given contingency
    # This is a copy of previous case but pst_be is available after a flow constraint in BE, only after contingency co1_fr2_fr3_1
    # Since only the preventive CNEC is constrained initially, the PST shall not be available
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "extra_features/Crac_UR_1_2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 0 remedial actions are used in preventive
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is -49 A
    Then the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be -49 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 693 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 79 A
