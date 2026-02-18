# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 20.4: Handle MNECs in second preventive optimization
  This feature covers the parameters of "second-preventive-rao" ("execution-condition" : "POSSIBLE_CURATIVE_IMPROVEMENT"),
  with MNECs defined in the CRAC file, from the RaoParameters.

  @fast @rao @ac @second-preventive @mnec @max-min-margin @megawatt
  Scenario: US 20.4.1: MNEC constraint in curative is solved by 2P
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic20/CBCORA_ep20us4case1.xml"
    Given configuration file is "epic20/RaoParameters_20_4.json"
    When I launch rao at "2019-01-08 12:00"
    Then 2 remedial actions are used in preventive
    Then the execution details should be "Second preventive improved first preventive results"
    Then its security status should be "UNSECURED"
    Then the remedial action "Open line NL1-NL2" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be -2 in preventive
    Then 0 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    Then the worst margin is -168 MW
    Then the value of the objective function after CRA should be 168
    Then the margin on cnec "FR2-FR3-O - preventive" after PRA should be -168 MW
    Then the margin on cnec "NL2-BE3-O - curative" after CRA should be 7 MW

  @fast @rao @ac @second-preventive @mnec @max-min-margin @megawatt
  Scenario: US 20.4.2: MNEC constraint in curative is solved by CRA + 2P
    Given network file is "common/TestCase12Nodes2PSTs.uct" for CORE CC
    Given crac file is "epic20/CBCORA_ep20us4case2.xml"
    Given configuration file is "epic20/RaoParameters_20_4.json"
    When I launch rao at "2019-01-08 12:00"
    Then 2 remedial actions are used in preventive
    Then the execution details should be "Second preventive improved first preventive results"
    Then its security status should be "UNSECURED"
    Then the remedial action "Open line NL1-NL2" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be -3 in preventive
    Then 1 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    Then the tap of PstRangeAction "CRA_PST_DE" should be 1 after "Contingency_FR1_FR3" at "curative"
    Then the worst margin is -161 MW
    Then the value of the objective function after CRA should be 161
    Then the margin on cnec "FR2-FR3-O - preventive" after PRA should be -161 MW
    Then the margin on cnec "NL2-BE3-O - curative" after CRA should be 7 MW

  @fast @rao @ac @second-preventive @mnec @max-min-margin @megawatt
  Scenario: US 20.4.3: MNEC constraint avoided on preventive MNEC in 2P
    Given network file is "common/TestCase12Nodes2PSTs.uct" for CORE CC
    Given crac file is "epic20/CBCORA_ep20us4case3.xml"
    Given configuration file is "epic20/RaoParameters_20_4.json"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "Second preventive improved first preventive results"
    Then its security status should be "UNSECURED"
    Then 0 remedial actions are used in preventive
    Then 0 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    Then the worst margin is -182 MW
    Then the value of the objective function after CRA should be 182
    Then the margin on cnec "NL1-NL3-D - curative" after CRA should be -182 MW
    Then the margin on cnec "NL2-BE3-O - curative" after CRA should be -145 MW
    Then the margin on cnec "FR2-FR3-O - preventive" after PRA should be -96 MW