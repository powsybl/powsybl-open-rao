# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 20.1: enable second optimization of the preventive perimeter

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.1.1: Preventive network actions only
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
    Then the execution details should be "Second preventive improved first preventive results"

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.1.2: Same case as US 20.1.1 with a limitation of 2 RAs in preventive
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/second_preventive_ls_1_2.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be -5 in preventive
    And the tap of PstRangeAction "pst_be" should be 0 in preventive
    Then the worst margin is 295.6 A

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.1.3: Same case as US 20.1.1.1 with pst_fr available in curative
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/second_preventive_ls_1_3.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    # We can re-optimize pst_fr even if it is preventive and curative for 2 reasons.
    # 1- It has no range limitations based on previous instants.
    # 2- There are no ra usage limits in curative.
    # Therefore, we obtain better results.
    Then 2 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be 5 in preventive
    And the tap of PstRangeAction "pst_be" should be -16 in preventive
    Then the worst margin is 321 A

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.1.4: Same case as US 20.1.1.3 with pst_fr limits relative to preventive.
    # We should have the same results as 20.1.1.2
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/second_preventive_ls_1_4.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    # We can not re-optimize pst_fr as its range limitation type is RELATIVE_TO_PREVIOUS_INSTANT.
    Then 2 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be -5 in preventive
    And the tap of PstRangeAction "pst_be" should be 0 in preventive
    Then the worst margin is 295.6 A

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.1.5: Same case as US 20.1.1.3 with curative maxRa limits.
    # We should have the same results as 20.1.1.2
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/second_preventive_ls_1_5.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    # We can not re-optimize pst_fr for 2 combined reasons:
    # 1- It has the same taps in preventive and in curative.
    # 2- There are maxRa usage limits for curative instant.
    Then 2 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be -5 in preventive
    And the tap of PstRangeAction "pst_be" should be 0 in preventive
    Then the worst margin is 295.6 A

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.1.6: Same case as US 20.1.1.3 with curative maxPstPerTso limits.
    # We should have the same results as 20.1.1.2
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/second_preventive_ls_1_6.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    # We can not re-optimize pst_fr for 2 combined reasons:
    # 1- It has the same taps in preventive and in curative.
    # 2- There are maxPstPerTso usage limits for curative instant.
    Then 2 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be -5 in preventive
    And the tap of PstRangeAction "pst_be" should be 0 in preventive
    Then the worst margin is 295.6 A

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.1.7: Same case as US 20.1.1.3 with curative maxRaPerTso limits.
    # We should have the same results as 20.1.1.2
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/second_preventive_ls_1_7.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    # We can not re-optimize pst_fr for 2 combined reasons:
    # 1- It has the same taps in preventive and in curative.
    # 2- There are maxRaPerTso usage limits for curative instant.
    Then 2 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the tap of PstRangeAction "pst_fr" should be -5 in preventive
    And the tap of PstRangeAction "pst_be" should be 0 in preventive
    Then the worst margin is 295.6 A

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.1.8: Same case as US 20.1.1.3 with curative maxTso limits.
    # We should have the same results as 20.1.1.2
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/second_preventive_ls_1_8.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    # We can not re-optimize pst_fr for 2 combined reasons:
    # 1- It has the same taps in preventive and in curative.
    # 2- There are maxTso usage limits for curative instant.
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
    Then 1 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_fr" should be 3 in preventive
    And 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 718 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - preventive" after PRA should be 718 A
    And the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 752 A
    Then the execution details should be "Second preventive improved first preventive results"

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.3: Preventive and curative network actions 2/3
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us4case2.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_be" should be -16 in preventive
    And 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -462 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -462 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -87 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 236 A
    Then the execution details should be "Second preventive improved first preventive results"

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.4: Preventive and curative network actions 3/3
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic13/SL_ep13us4case4.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_be" should be -16 in preventive
    And 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -462 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -462 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -87 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 236 A
    Then the execution details should be "Second preventive improved first preventive results"

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.5: Duplicated RA on the same PST, one being a PRA and the other one being a CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/crac_ep20us1case1_5.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    # Same: FARAO has better results than OSIRIS
    Then 1 remedial actions are used in preventive
    And the remedial action "pst_fr_pra" is used in preventive
    And the tap of PstRangeAction "pst_fr_pra" should be -7 in preventive
    And 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr_cra" should be 8 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 43 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 43 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 385 A
    And the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - curative" after CRA should be 535 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 963 A
    And the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 1148 A
    Then the execution details should be "Second preventive improved first preventive results"

  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.6: Same test as 20.1.5 but the CRA has a relative to previous instant range
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/crac_ep20us1case1_6.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao
    # Same: FARAO has better results than OSIRIS
    Then 1 remedial actions are used in preventive
    And the remedial action "pst_fr_pra" is used in preventive
    And the tap of PstRangeAction "pst_fr_pra" should be -7 in preventive
    And 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr_cra" should be 1 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    # The margin is way worse as we cannot re-optimize the preventive PST
    Then the worst margin is -184 A
    Then the execution details should be "Second preventive improved first preventive results"


  @fast @rao @mock @ac @second-preventive
  Scenario: US 20.1.7: Test case with a CBCORA file
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct"
    Given crac file is "epic13/CBCORA_ep13us3case10.xml"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_fr" should be 3 in preventive
    And 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    And the worst margin is 717 A
    And the margin on cnec "fr4_de1_N - preventive" after PRA should be 717 A
    And the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 752 A
    And the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 806 A
    And the margin on cnec "fr3_fr5_CO1 - OPP - outage" after CRA should be 874 A
    And the margin on cnec "fr4_de1_CO1 - curative" after CRA should be 1002 A
    Then the execution details should be "Second preventive improved first preventive results"