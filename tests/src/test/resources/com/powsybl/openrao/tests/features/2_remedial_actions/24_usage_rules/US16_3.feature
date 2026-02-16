# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: User Story #16.3: Activate remedial actions only after a specific constraint

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 16.3.1: Preventive onConstraint RA with a constraint on the base network
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 1 remedial actions are used in preventive
    Then the remedial action "open_fr1_fr3" is used in preventive
    Then the worst margin is -135 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -135 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be -134 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be 48 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 308 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be 492 A

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 16.3.2: Preventive onConstraint RAs with a constraint triggered by another preventive RA, no reevaluation
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 1 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the worst margin is -45 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -45 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be -41 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be 81 A

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 16.3.3: Preventive onConstraint RAs with no constraint triggered
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 1 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the worst margin is -45 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -45 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be -41 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be 81 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.3.4: Curative onConstraint RA with a constraint right after applying the contingency
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case4.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 2 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is -37 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -37 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -13 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.3.5: Curative onConstraint RA with a constraint triggered by another curative RA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 2 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is -37 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -37 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -13 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.3.6: Curative onConstraint RA with no constraint triggered
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case6.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 43 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 43 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 80 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.3.7: Preventive and curative onConstraint RA with a constraint triggered on the base network
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case7.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 1 remedial actions are used in preventive
    Then the remedial action "pst_be" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be 16 in preventive
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 82 A
    Then the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 82 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 245 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 345 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.3.8: Preventive and curative onConstraint RA with a constraint triggered after applying the contingency
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case8.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 1 remedial actions are used in preventive
    Then the remedial action "pst_be" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be 16 in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -1 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 63 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 63 A
    Then the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 82 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 71 A

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 16.3.9: Preventive onConstraint RAs with no constraint triggered (CSE)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/CseCrac_16_3_9.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 1 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the worst margin is -45 A
    Then the margin on cnec "be2_fr3_n - BBE2AA1 ->FFR3AA1  - preventive" after PRA should be -45 A
    Then the margin on cnec "fr1_fr4_co1 - FFR1AA1 ->FFR4AA1   - co1_fr2_fr3_1 - curative" after PRA should be -41 A
    Then the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after PRA should be 81 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.3.10: Preventive and curative onConstraint RA with a constraint triggered on the base network (CSE)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/CseCrac_16_3_10.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 1 remedial actions are used in preventive
    Then the remedial action "PST_pst_be_pra_BBE2AA1  BBE3AA1  1" is used in preventive
    Then the tap of PstRangeAction "PST_pst_be_pra_BBE2AA1  BBE3AA1  1" should be 16 in preventive
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 82 A
    Then the margin on cnec "be1_fr5_n - BBE1AA1 ->FFR5AA1  - preventive" after PRA should be 82 A
    Then the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 245 A
    Then the margin on cnec "fr3_fr5_co1 - FFR3AA1 ->FFR5AA1   - co1_fr2_fr3_1 - curative" after CRA should be 345 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.3.11: Preventive and curative onConstraint RA with a constraint triggered after applying the contingency (CSE)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/CseCrac_16_3_11.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 1 remedial actions are used in preventive
    Then the remedial action "PST_pst_be_pra_BBE2AA1  BBE3AA1  1" is used in preventive
    Then the tap of PstRangeAction "PST_pst_be_pra_BBE2AA1  BBE3AA1  1" should be 16 in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "PST_pst_be_cra_BBE2AA1  BBE3AA1  1" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "PST_pst_be_cra_BBE2AA1  BBE3AA1  1" should be -1 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 63 A
    Then the margin on cnec "fr3_fr5_co1 - FFR3AA1 ->FFR5AA1   - co1_fr2_fr3_1 - curative" after CRA should be 63 A
    Then the margin on cnec "be1_fr5_n - BBE1AA1 ->FFR5AA1  - preventive" after PRA should be 82 A
    Then the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 72 A

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 16.3.12: Preventive onConstraint RA with constraint triggered on outage CNEC, does not re evaluate
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/CseCrac_16_3_12.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 1 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the worst margin is -45 A
    Then the margin on cnec "be2_fr3_n - BBE2AA1 ->FFR3AA1  - preventive" after PRA should be -45 A
    Then the margin on cnec "fr1_fr4_co1 - FFR1AA1 ->FFR4AA1   - co1_fr2_fr3_1 - outage" after PRA should be -41 A
    Then the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after PRA should be 81 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.3.13: Test with a maximum number of CRA 1/2
    Given network file is "epic16/TestCase16Nodes_3psts.uct"
    Given crac file is "epic16/SL_ep16us3case13.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 2 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is -37 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -37 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -13 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.3.14: Test with a maximum number of CRA 2/2
    Given network file is "epic16/TestCase16Nodes_3psts.uct"
    Given crac file is "epic16/SL_ep16us3case14.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 1 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is -5 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -5 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 3 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.3.15: OnFlowConstraint with overload on other curative state
  2 contingency scenarios but only 1 onConstraint usage rule defined.
    Given network file is "epic16/2Nodes3ParallelLines.uct"
    Given crac file is "epic16/crac_16_3_15.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    Then 1 remedial actions are used after "co_fr1_fr2_1" at "curative"
    Then the remedial action "pst_fr" is used after "co_fr1_fr2_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -9 after "co_fr1_fr2_1" at "curative"
    Then 0 remedial actions are used after "co_fr1_fr2_2" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be 0 after "co_fr1_fr2_2" at "curative"
    Then the worst margin is -221.78 A
    Then the margin on cnec "FFR1AA1  FFR2AA1  3 - co_fr1_fr2_1 - curative" after CRA should be 284.32 A
    Then the margin on cnec "FFR1AA1  FFR2AA1  3 - co_fr1_fr2_2 - curative" after CRA should be -221.78 A
    Then its security status should be "UNSECURED"

  @fast @rao @ac @contingency-scenarios @second-preventive @max-min-margin @ampere
  Scenario: US 16.3.16: OnFlowConstraint with overload on other curative state
  2 contingency scenarios but only 1 onConstraint usage rule defined.
  Because of second preventive optimization, tap is limited at position -3
  since it does not improve the objective function.
    Given network file is "epic16/2Nodes3ParallelLines.uct"
    Given crac file is "epic16/crac_16_3_15.json"
    Given configuration file is "epic16/RaoParameters_maxMargin_ampere_2P.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    Then 1 remedial actions are used after "co_fr1_fr2_1" at "curative"
    Then the remedial action "pst_fr" is used after "co_fr1_fr2_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -3 after "co_fr1_fr2_1" at "curative"
    Then 0 remedial actions are used after "co_fr1_fr2_2" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be 0 after "co_fr1_fr2_2" at "curative"
    Then the worst margin is -221.78 A
    Then the margin on cnec "FFR1AA1  FFR2AA1  3 - co_fr1_fr2_1 - curative" after CRA should be -186.28 A
    Then the margin on cnec "FFR1AA1  FFR2AA1  3 - co_fr1_fr2_2 - curative" after CRA should be -221.78 A
    Then its security status should be "UNSECURED"