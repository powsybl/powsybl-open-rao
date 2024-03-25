# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 20.1: enable second optimization of the preventive perimeter

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.1: Preventive network actions only
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/second_preventive_ls_1.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    Then the worst margin is 321 A
    Then 3 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be -5 in preventive
    And the tap of PstRangeAction "pst_be" should be -16 in preventive
    And the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 321 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 501 A
    Then the optimization steps executed by the RAO should be "SECOND_PREVENTIVE_IMPROVED_FIRST"

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.1_bis: Same case as US 20.1.1 with a limitation of 2 RAs in preventive
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/second_preventive_ls_1_bis.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be -5 in preventive
    And the tap of PstRangeAction "pst_be" should be 0 in preventive
    Then the worst margin is 295.6 A

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.1_ter: Same case as US 20.1.1_bis with pst_fr available in curative
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/second_preventive_ls_1_ter.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    # As pst_fr is both preventive and curative, it's excluded from the 2nd preventive.
    # But we still consider it was applied in first preventive.
    # Therefore, only 1 remedial action can be applied in 2nd preventive.
    Then 2 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be -5 in preventive
    And the tap of PstRangeAction "pst_be" should be 0 in preventive
    Then the worst margin is 295.6 A

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.2: Preventive and curative network actions 1/3
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us3case1.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    # FARAO actually has a different behavior than OSIRIS
    # During 2nd preventive, OSIRIS uses 1st preventive PRAs and deactivates some, activates some others
    # FARAO restarts from zero
    # Thus the 2 network actions are not needed, and the min margin is better than OSIRIS
    Then 2 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_fr" should be -5 in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 638 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - preventive" after PRA should be 638 A
    And the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 645 A
    Then the optimization steps executed by the RAO should be "SECOND_PREVENTIVE_IMPROVED_FIRST"

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.3: Preventive and curative network actions 2/3
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us4case2.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    # Same: FARAO has better results than OSIRIS
    Then 2 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And the tap of PstRangeAction "pst_be" should be -16 in preventive
    And 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 15 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -291 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -291 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -277 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 36 A
    Then the optimization steps executed by the RAO should be "SECOND_PREVENTIVE_IMPROVED_FIRST"

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.4: Preventive and curative network actions 3/3
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us4case4.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    # Same: FARAO has better results than OSIRIS
    Then 2 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_fr" should be 15 in preventive
    And the tap of PstRangeAction "pst_be" should be -16 in preventive
    And 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 15 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -291 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -291 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -277 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 36 A
    Then the optimization steps executed by the RAO should be "SECOND_PREVENTIVE_IMPROVED_FIRST"

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.5: Duplicated RA on the same PST, one being a PRA and the other one being a CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us5case4.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    # Same: FARAO has better results than OSIRIS
    Then 1 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_fr_pra" should be -7 in preventive
    And the tap of PstRangeAction "pst_be" should be 0 in preventive
    And 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr_cra" should be 1 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 43 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 43 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 385 A
    And the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - curative" after CRA should be 385 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 923 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 1148 A
    Then the optimization steps executed by the RAO should be "SECOND_PREVENTIVE_IMPROVED_FIRST"

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.6: Test case with a CBCORA file
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct"
    Given crac file is "epic13/CBCORA_ep13us3case10.xml"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 2 remedial actions are used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be -5 in preventive
    And 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is 638 A
    And the margin on cnec "fr4_de1_N - preventive" after PRA should be 638 A
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 645 A
    And the margin on cnec "fr4_de1_CO1 - curative" after CRA should be 738 A
    And the margin on cnec "fr4_de1_CO1 - outage" after PRA should be 848 A
    And the margin on cnec "fr3_fr5_CO1 - DIR - curative" after CRA should be 861 A
    Then the optimization steps executed by the RAO should be "SECOND_PREVENTIVE_IMPROVED_FIRST"