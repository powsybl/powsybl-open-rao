# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 90.3: Inversion of PSTs in remedial actions

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 90.3.1: Inverted PstRangeAction in Security Limit
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic90/SL_ep90us3case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the margin on cnec "BBE2AA1  BBE3AA1  1 - preventive" after PRA should be 26.0 A
    Then the worst margin is 26.0 A
    Then the tap of PstRangeAction "PRA_PST_BE" should be 3 in preventive

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 90.3.2: Inverted PstSetpoint in Security Limit
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic1/SL_ep1us2_selectionTopoRA_variant1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 83.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 83.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PST @1" is used in preventive

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 90.3.3: Inverted PstRangeAction in CBCORA
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic90/cbcora_ep90us3case3.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao at "2019-01-08 21:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the worst margin is -372.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -372.0 A
    Then 2 remedial actions are used in preventive
    Then the remedial action "Open FR1 FR2" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be -5 in preventive

  @fast @cne-export @max-min-margin @ampere
  Scenario: US 90.3.3.CNE: Inverted PstRangeAction in CBCORA (CNE export)
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic90/cbcora_ep90us3case3.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given RaoResult file is "epic90/RaoResult_90_3_3.json"
    When I export CORE CNE at "2019-01-08 21:30"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is similar to "epic90/ExpectedCNE_90_3_3.xml"

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 90.3.4: Inverted PstRangeAction in CSE Crac
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic90/cseCrac_ep90us3case4.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 2876.0 A
    Then the margin on cnec "basecase_branch_1 - NNL2AA1 ->NNL3AA1  - preventive" after PRA should be 2876.0 A
    Then 2 remedial actions are used in preventive
    Then the remedial action "PRA_2" is used in preventive
    Then the tap of PstRangeAction "PST_PRA_3_BBE2AA1  BBE3AA1  1" should be 4 in preventive
