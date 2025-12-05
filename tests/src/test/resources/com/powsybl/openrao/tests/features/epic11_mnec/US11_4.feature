# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 11.4: Handle mnecs in search tree with range actions and network actions

  @fast @rao @mock @ac @preventive-only
  Scenario: US 11.4.1: reference run, no mnec
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mixed_ref.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_ac.json"
    When I launch search_tree_rao
    Then the remedial action "Open line NL1-NL2" is used in preventive
    And line "NNL1AA1  NNL2AA1  1" in network file with PRA has connection status to "false"
    And the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    And PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -16
    And 2 remedial actions are used in preventive
    And the worst margin is -106.6 A
    And the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 1648.7 A
    And the flow on cnec "DDE1AA1  DDE2AA1  1 - Contingency FR1 FR3 - curative" after CRA should be -568.2 A
    And the flow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -2372.5 A

  @fast @rao @mock @ac @preventive-only @mnec
  Scenario: US 11.4.2: margin on MNEC should stay positive
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mixed_4_2.json"
    Given configuration file is "epic11/RaoParameters_maxMargin_ampere_ac_mnecDimin30.json"
    When I launch search_tree_rao
    Then the remedial action "Open line NL1-NL2" is used in preventive
    And line "NNL1AA1  NNL2AA1  1" in network file with PRA has connection status to "false"
    And the tap of PstRangeAction "PRA_PST_BE" should be -12 in preventive
    And PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -12
    And 2 remedial actions are used in preventive
    And the worst margin is -146.3 A on cnec "FFR2AA1  FFR3AA1  1 - preventive"
    And the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 1589.1 A

  @fast @rao @mock @ac @preventive-only @mnec
  Scenario: US 11.4.3: Search Tree RAO - 2 MNECs with one curative
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mixed_4_3.json"
    Given configuration file is "epic11/RaoParameters_maxMargin_ampere_ac_mnecDimin30.json"
    When I launch search_tree_rao
    Then the remedial action "Open line NL1-NL2" is used in preventive
    And line "NNL1AA1  NNL2AA1  1" in network file with PRA has connection status to "false"
    And the tap of PstRangeAction "PRA_PST_BE" should be -9 in preventive
    And PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -9
    And 2 remedial actions are used in preventive
    And the worst margin is -176.0 A on cnec "FFR2AA1  FFR3AA1  1 - preventive"
    And the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 1544.0 A
    And the flow on cnec "DDE1AA1  DDE2AA1  1 - Contingency FR1 FR3 - curative" after PRA should be -535.0 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 11.4.4.a: reference run on CBCORA, no mnec
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/MergedCB_ref.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_ac.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the remedial action "Open line NL1-NL2" is used in preventive
    And line "NNL1AA1  NNL2AA1  1" in network file with PRA has connection status to "false"
    And the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    And PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -16
    And 2 remedial actions are used in preventive
    And the worst margin is -106.6 A
    And the flow on cnec "NL2-NL3-D - preventive" after PRA should be 1648.7 A
    And the flow on cnec "DE1-DE2-DO - curative" after PRA should be -568.2 A
    And the flow on cnec "NL2-BE3-D - preventive" after PRA should be -2372.5 A

  @fast @rao @mock @ac @preventive-only @mnec
  Scenario: US 11.4.4.b: margin on MNEC should stay positive
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/MergedCB_4_4.xml"
    Given configuration file is "epic11/RaoParameters_maxMargin_ampere_ac_mnecDimin30.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    # The mnec is NL2-BE3-D - preventive
    Then the remedial action "Open line NL1-NL2" is used in preventive
    And line "NNL1AA1  NNL2AA1  1" in network file with PRA has connection status to "false"
    And the tap of PstRangeAction "PRA_PST_BE" should be -11 in preventive
    And PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -11
    And 2 remedial actions are used in preventive
    And the worst margin is -156.3 A on cnec "FR2-FR3-O - preventive"
    And the flow on cnec "NL1-NL3-D - preventive" after PRA should be 721.8 A
    And the flow on cnec "NL2-BE3-D - preventive" after PRA should be -2297.8 A