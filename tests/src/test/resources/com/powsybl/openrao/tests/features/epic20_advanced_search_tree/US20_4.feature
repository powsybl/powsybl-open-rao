# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 20.4: Handle MNECs in second preventive optimization

  @fast @rao @mock @ac @second-preventive @mnec
  Scenario: US 20.4.1: MNEC constraint in curative is solved by 2P
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic20/CBCORA_ep20us4case1.xml"
    Given configuration file is "epic20/RaoParameters_20_4.json"
    When I launch rao at "2019-01-08 12:00"
    Then 2 remedial actions are used in preventive
    And the remedial action "Open line NL1-NL2" is used in preventive
    And the tap of PstRangeAction "PRA_PST_BE" should be -2 in preventive
    And 0 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    And the worst margin is -246 A
    And the value of the objective function after CRA should be 246
    And the margin on cnec "FR2-FR3-O - preventive" after PRA should be -246 A
    And the margin on cnec "NL2-BE3-O - curative" after CRA should be 7 A
    Then the execution details should be "Second preventive improved first preventive results"

  @fast @rao @mock @ac @second-preventive @mnec
  Scenario: US 20.4.2: MNEC constraint in curative is solved by CRA + 2P
    Given network file is "common/TestCase12Nodes2PSTs.uct" for CORE CC
    Given crac file is "epic20/CBCORA_ep20us4case2.xml"
    Given configuration file is "epic20/RaoParameters_20_4.json"
    When I launch rao at "2019-01-08 12:00"
    Then 2 remedial actions are used in preventive
    And the remedial action "Open line NL1-NL2" is used in preventive
    And the tap of PstRangeAction "PRA_PST_BE" should be -3 in preventive
    And 1 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    And the tap of PstRangeAction "CRA_PST_DE" should be 1 after "Contingency_FR1_FR3" at "curative"
    And the worst margin is -236 A
    And the value of the objective function after CRA should be 236
    And the margin on cnec "FR2-FR3-O - preventive" after PRA should be -236 A
    And the margin on cnec "NL2-BE3-O - curative" after CRA should be 7 A
    Then the execution details should be "Second preventive improved first preventive results"

  @fast @rao @mock @ac @second-preventive @mnec
  Scenario: US 20.4.3: MNEC constraint avoided on preventive MNEC in 2P
    Given network file is "common/TestCase12Nodes2PSTs.uct" for CORE CC
    Given crac file is "epic20/CBCORA_ep20us4case3.xml"
    Given configuration file is "epic20/RaoParameters_20_4.json"
    When I launch rao at "2019-01-08 12:00"
    Then 0 remedial actions are used in preventive
    And 0 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    And the worst margin is -264 A
    And the value of the objective function after CRA should be 264
    And the margin on cnec "NL1-NL3-D - curative" after CRA should be -264 A
    And the margin on cnec "NL2-BE3-O - curative" after CRA should be -212 A
    And the margin on cnec "FR2-FR3-O - preventive" after PRA should be -143 A
    Then the execution details should be "Second preventive improved first preventive results"