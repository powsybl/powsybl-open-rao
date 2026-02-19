# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 13.6: cross validation curative optimization and MNECs

  @fast @rao @mock @ac @contingency-scenarios @mnec
  Scenario: US 13.6.1: Simple case with a mix of preventive and curative remedial actions and a MNEC in preventive limited by threshold
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "epic13/SL_ep13us2case5_with_mnec.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 2 remedial actions are used in preventive
    And the remedial action "open_be1_be4" is used in preventive
    # Without MNEC pst_fr is set to -5
    And the tap of PstRangeAction "pst_fr" should be 2 in preventive
    # Margin of the limiting CNEC is slightly lower than in the original test case without MNEC
    And the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 1483 A
    And the initial flow on cnec "FFR1AA1  FFR2AA1  1 - preventive" should be 430 MW on side 1
    And the initial margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" should be 70 MW
    And the margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" after PRA should be 5 MW
    And 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 14 after "co1_fr2_fr3_1" at "curative"
    And the value of the objective function after CRA should be -999

  @fast @rao @mock @ac @contingency-scenarios @mnec
  Scenario: US 13.6.2: CBCORA - Curative MNECs should have a positive margin
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic13/MergedCB_ep13us6case2.xml"
    Given configuration file is "epic13/RaoParameters_13_6_2.json"
    When I launch rao at "2019-01-08 12:00"
    Then the margin on cnec "NL2-BE3-O - outage" after PRA should be 8 A
    And 2 remedial actions are used in preventive
    And the remedial action "Open line NL1-NL2" is used in preventive
    And the tap of PstRangeAction "PRA_PST_BE" should be -6 in preventive
    And the margin on cnec "FR2-FR3-O - preventive" after PRA should be -206.14 A
   # Note that he chosen CRA does not improve the functional cost (it only improves the virtual cost), so if in the future it changes,
    # it would be OK to change the test as long as the MNEC is properly treated in the curative RAO
    Then the margin on cnec "NL2-BE3-O - curative" after CRA should be 18.28 A
    And 1 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    And the remedial action "Open line DE1-DE2" is used after "Contingency_FR1_FR3" at "curative"
    And the margin on cnec "NL1-NL3-D - curative" after CRA should be 0.22 A
    And the value of the objective function after CRA should be 206.1

  @fast @rao @mock @ac @contingency-scenarios @mnec
  Scenario: US 13.6.3: CBCORA - Curative MNECs limited by their initial margin - CRAs only
    Given network file is "epic13/TestCase12NodesDifferentPstTap.uct" for CORE CC
    Given crac file is "epic13/MergedCB_ep13us6case3.xml"
    Given configuration file is "epic11/RaoParameters_maxMargin_ampere_ac_mnecDimin30.json"
    When I launch rao at "2019-01-08 12:00"
    Then the margin on cnec "NL2-BE3-O - outage" after PRA should be -83.0 MW
    And the margin on cnec "NL2-BE3-O - curative" after CRA should be -103.0 MW
    And 2 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    And the remedial action "Open line DE1-DE2" is used after "Contingency_FR1_FR3" at "curative"
    And the tap of PstRangeAction "PRA_PST_BE" should be 10 after "Contingency_FR1_FR3" at "curative"
    And the margin on cnec "FR2-FR3-OO - curative" after CRA should be -98 MW

  @fast @rao @mock @ac @contingency-scenarios @mnec
  Scenario: US 13.6.4: CBCORA - Curative MNECs limited by their initial margin - PRAs and CRAs
    Given network file is "epic13/TestCase12NodesDifferentPstTap.uct" for CORE CC
    Given crac file is "epic13/MergedCB_ep13us6case4.xml"
    Given configuration file is "epic11/RaoParameters_maxMargin_ampere_ac_mnecDimin30.json"
    When I launch rao at "2019-01-08 12:00"
    Then the margin on cnec "NL2-BE3-O - outage" after PRA should be -103.0 MW
    And 1 remedial actions are used in preventive
    And the tap of PstRangeAction "PRA_PST_BE" should be 14 in preventive
    And the margin on cnec "FR2-FR3-O - preventive" after PRA should be -267 MW
    # Note that he chosen CRA does not improve the functional cost (it only improves the virtual cost), so if in the future it changes,
    # it would be OK to change the test as long as the MNEC is properly treated in the curative RAO
    Then the margin on cnec "NL2-BE3-O - curative" after CRA should be -63.0 MW
    And 1 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    And the remedial action "Open line DE1-DE2" is used after "Contingency_FR1_FR3" at "curative"
    And the margin on cnec "FR2-FR3-OO - curative" after CRA should be -137.0 MW
    And the value of the objective function after CRA should be 402

  @fast @rao @mock @ac @contingency-scenarios @mnec
  Scenario: US 13.6.5: Simple case with a mix of preventive and curative remedial actions and a MNEC in preventive limited by threshold
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "epic13/SL_ep13us2case5_with_mnec.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 2 remedial actions are used in preventive
    And the remedial action "open_be1_be4" is used in preventive
    # Without MNEC pst_fr is set to -5
    And the tap of PstRangeAction "pst_fr" should be 2 in preventive
    # Margin of the limiting CNEC is slightly lower than in the original test case without MNEC
    And the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 1483 A
    And the initial margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" should be 70 MW
    And the margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" after PRA should be 5 MW
    And 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 14 after "co1_fr2_fr3_1" at "curative"
    And the value of the objective function after CRA should be -999

  @fast @rao @mock @ac @contingency-scenarios @mnec
  Scenario: US 13.6.6: Simple case with a mix of preventive and curative remedial actions and MNECs in preventive and curative limited by threshold
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "epic13/SL_ep13us2case6_with_mnec_curative.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 2 remedial actions are used in preventive
    And the remedial action "open_be1_be4" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 2 in preventive
    And the initial margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" should be 70 MW
    And the margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" after PRA should be 5 MW
    # Flow is -572 MW without RA, and threshold -700 MW.
    And the initial margin on cnec "BBE1AA1  BBE2AA1  1 - co1_fr2_fr3_1 - curative" should be 127 MW
    # Here the margin should not be negative because the branch is a MNEC and initial margin was positive.
    # Flow is -643 MW with PRA and CRA (actually no CRA were activated in this test case), and threshold -700 MW. Margin is positive.
    And the margin on cnec "BBE1AA1  BBE2AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 57 MW
    # 2 Remedial actions would have been used if the MNEC was not limiting
    And 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 612 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"
    And the value of the objective function after CRA should be -612

  @fast @rao @mock @ac @contingency-scenarios @mnec
  Scenario: US 13.6.7: Simple case with a mix of preventive and curative remedial actions and MNECs in preventive and curative limited by initial value
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "epic13/SL_ep13us2case7_with_mnec_curative_initial.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_mip.json"
    When I launch rao
    Then 2 remedial actions are used in preventive
    And the remedial action "open_be1_be4" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 2 in preventive
    And the initial margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" should be 70 MW
    And the margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" after PRA should be 5 MW
    # Flow is -572 MW without RA, and threshold -500 MW.
    And the initial margin on cnec "BBE1AA1  BBE2AA1  1 - co1_fr2_fr3_1 - curative" should be -72 MW
    # Here the margin should not be below -122 MW because the initial margin is -72 MW (taking acceptable diminution parameter into account).
    # Flow is -643 MW with PRA and CRA, and threshold -700 MW. Margin is positive.
    And the margin on cnec "BBE1AA1  BBE2AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -105 MW
    # Curative RA need to be used in order to respect the MNEC constraint (the MNEC is violated by 30 MW in the root leaf of the curative perimeter)
    And 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -2 after "co1_fr2_fr3_1" at "curative"
    # The min margin is lower than in the previous case as the MNEC threshold has been tightened
    And the worst margin is 705 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"
    And the value of the objective function after CRA should be -705

  @fast @rao @mock @ac @contingency-scenarios @mnec
  Scenario: US 13.6.8: Curative perimeter with pure MNECs only
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "epic13/MergedCB_ep13us6case8.xml"
    Given configuration file is "epic13/RaoParameters_ep13us6case8.json"
    When I launch rao at "2019-01-08 12:00"
    Then the worst margin is 390 MW
    # "NNL2AA1  BBE3AA1  1 - preventive" is the only MNEC.
    # With a temporary threshold in outage of 1385 MW, and a permanent threshold in curative of 1350 MW
    And the initial flow on cnec "NL2-BE3-D - outage" should be -1338 MW on side 1
    And the flow on cnec "NL2-BE3-D - outage" after PRA should be -1382 MW on side 1
    And the flow on cnec "NL2-BE3-O - curative" after CRA should be -1283 MW on side 1
    # Remedial actions are used in preventive to increase the margin of the CNECs
    # Remedial actions are used in curative to respect the constraint on the MNEC (there are no optimized CNECs in curative)
    And 1 remedial actions are used in preventive
    And the tap of PstRangeAction "PRA_PST_FR" should be 12 in preventive
    And 1 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    And the remedial action "Open line DE1-DE2" is used after "Contingency_FR1_FR3" at "curative"
    And the value of the objective function initially should be -328
    And the value of the objective function after PRA should be -71
    And the value of the objective function after ARA should be -71
    And the value of the objective function after CRA should be -390

  @fast @rao @mock @ac @contingency-scenarios @mnec
  Scenario: US 13.6.11: Curative with pure MNECs only - PST CRA should remove MNEC constraint
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic13/CBCORA_ep13us6case11.xml"
    Given configuration file is "epic13/RaoParameters_13_6_11.json"
    When I launch rao at "2019-01-08 12:00"
    # The only curative element is a MNEC is NL2-BE3-0 - curative with a temporary threshold of 2298 A and a permanent one at 2237 A
    # Margin after preventive perimeter optimization -> 1 A OK (2298 A-2297 A). But in curative the margin is -60 A (2237 A - 2297 A).
    # "CRA_PST_BE" applied to remove MNEC violation.
    Then 1 remedial actions are used in preventive
    And the tap of PstRangeAction "PRA_PST_BE" should be -4 in preventive
    And 1 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    And the tap of PstRangeAction "CRA_PST_BE" should be 0 after "Contingency_FR1_FR3" at "curative"