# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: User Story #16.3: Activate remedial actions only after a specific constraint

  @fast @rao @mock @ac @preventive-only
  Scenario: US 16.3.1: Preventive onConstraint RA with a constraint on the base network
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the worst margin is -135 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -135 A
    And the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be -134 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be 48 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 308 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be 492 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 16.3.2: Preventive onConstraint RAs with a constraint triggered by another preventive RA
  At depth 1 close_fr1_fr5 is applied. The overload on curative CNEC FR1 FR4 is still present so
  close_fr1_fr5 is used as well.
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the worst margin is 45.17 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 45.17 A
    And the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be 158.45 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be 264.2 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 16.3.3: Preventive onConstraint RAs with no constraint triggered
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the worst margin is -45 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -45 A
    And the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be -41 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be 81 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 16.3.4: Curative onConstraint RA with a constraint right after applying the contingency
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case4.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 2 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -37 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -37 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -13 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 16.3.5: Curative onConstraint RA with a constraint triggered by another curative RA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 2 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -37 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -37 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -13 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 16.3.6: Curative onConstraint RA with no constraint triggered
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case6.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 43 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 43 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 80 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 16.3.7: Preventive and curative onConstraint RA with a constraint triggered on the base network
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case7.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the remedial action "pst_be" is used in preventive
    And the tap of PstRangeAction "pst_be" should be 16 in preventive
    And 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 82 A
    And the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 82 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 245 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 345 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 16.3.8: Preventive and curative onConstraint RA with a constraint triggered after applying the contingency
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us3case8.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the remedial action "pst_be" is used in preventive
    And the tap of PstRangeAction "pst_be" should be 16 in preventive
    And 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -1 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 63 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 63 A
    And the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 82 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 72 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 16.3.9: Preventive onConstraint RAs with no constraint triggered (CSE)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/CseCrac_16_3_9.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the worst margin is -45 A
    And the margin on cnec "be2_fr3_n - BBE2AA1 ->FFR3AA1  - preventive" after PRA should be -45 A
    And the margin on cnec "fr1_fr4_co1 - FFR1AA1 ->FFR4AA1   - co1_fr2_fr3_1 - curative" after PRA should be -41 A
    And the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after PRA should be 81 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 16.3.10: Preventive and curative onConstraint RA with a constraint triggered on the base network (CSE)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/CseCrac_16_3_10.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the remedial action "PST_pst_be_pra_BBE2AA1  BBE3AA1  1" is used in preventive
    And the tap of PstRangeAction "PST_pst_be_pra_BBE2AA1  BBE3AA1  1" should be 16 in preventive
    And 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 82 A
    And the margin on cnec "be1_fr5_n - BBE1AA1 ->FFR5AA1  - preventive" after PRA should be 82 A
    And the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 245 A
    And the margin on cnec "fr3_fr5_co1 - FFR3AA1 ->FFR5AA1   - co1_fr2_fr3_1 - curative" after CRA should be 345 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 16.3.11: Preventive and curative onConstraint RA with a constraint triggered after applying the contingency (CSE)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/CseCrac_16_3_11.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the remedial action "PST_pst_be_pra_BBE2AA1  BBE3AA1  1" is used in preventive
    And the tap of PstRangeAction "PST_pst_be_pra_BBE2AA1  BBE3AA1  1" should be 16 in preventive
    And 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "PST_pst_be_cra_BBE2AA1  BBE3AA1  1" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "PST_pst_be_cra_BBE2AA1  BBE3AA1  1" should be -1 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 63 A
    And the margin on cnec "fr3_fr5_co1 - FFR3AA1 ->FFR5AA1   - co1_fr2_fr3_1 - curative" after CRA should be 63 A
    And the margin on cnec "be1_fr5_n - BBE1AA1 ->FFR5AA1  - preventive" after PRA should be 82 A
    And the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 72 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 16.3.12: Preventive onConstraint RA with constraint triggered on outage CNEC
  Same case as 16.3.2 with a CSE CRAC and an onFlowConstraint usage rule based on an outage CNEC
  instead of a curative one.
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/CseCrac_16_3_12.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the worst margin is 45.17 A
    And the margin on cnec "be2_fr3_n - BBE2AA1 ->FFR3AA1  - preventive" after PRA should be 45.17 A
    And the margin on cnec "fr1_fr4_co1 - FFR1AA1 ->FFR4AA1   - co1_fr2_fr3_1 - outage" after PRA should be 158.45 A
    And the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after PRA should be 264.2 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 16.3.13: Test with a maximum number of CRA 1/2
    Given network file is "epic16/TestCase16Nodes_3psts.uct"
    Given crac file is "epic16/SL_ep16us3case13.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 2 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -37 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -37 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -13 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 16.3.14: Test with a maximum number of CRA 2/2
    Given network file is "epic16/TestCase16Nodes_3psts.uct"
    Given crac file is "epic16/SL_ep16us3case14.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 1 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -5 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -5 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 3 A