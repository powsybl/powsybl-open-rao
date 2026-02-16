# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 1.3: Security assessment with network actions

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 1.3.1: selection of topological action
    Two network actions and one PST setpoint action available, only one network action is activated.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic1/SL_ep1us2_selectionTopoRA.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 56.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 56.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - outage" after PRA should be 56.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - curative" after CRA should be 56.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "Open tie-line FR DE" is used in preventive
    Then the "upper" threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 1500.0 A
    Then the initial flow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 721.0 A on side 2
    Then the flow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -1444.0 A on side 2
    Then the "upper" threshold on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - outage" should be 1500.0 A
    Then the initial flow on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - outage" should be 652.0 A on side 2
    Then the flow on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - outage" after PRA should be -1444.0 A on side 2
    Then the initial flow on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - curative" should be 652.0 A on side 2
    Then the flow on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - curative" after PRA should be -1444.0 A on side 2
    Then the flow on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - curative" after CRA should be -1444.0 A on side 2

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 1.3.2: selection of PST setpoint remedial action
  One network action and one PST setpoint action available, only the PST setpoint action is activated.
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

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 1.3.3: selection of PST setpoint remedial action but residual constraint
  One network action and one PST setpoint action available, only the PST setpoint action is activated but the situation
  remains unsecure.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic1/SL_ep1us2_selectionTopoRA_variant2.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the worst margin is -417.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -417.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PST @1" is used in preventive
    Then the "upper" threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 1500.0 A
    Then the initial flow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 721.0 A on side 2
    Then the flow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 471.1 A on side 2
    Then the "upper" threshold on cnec "FFR2AA1  DDE3AA1  1 - Contingency FR1 FR3 - outage" should be 1500.0 A
    Then the initial flow on cnec "FFR2AA1  DDE3AA1  1 - Contingency FR1 FR3 - outage" should be 2098.0 A on side 2
    Then the flow on cnec "FFR2AA1  DDE3AA1  1 - Contingency FR1 FR3 - outage" after PRA should be 1859.0 A on side 2
    Then the flow on cnec "FFR2AA1  DDE3AA1  1 - Contingency FR1 FR3 - outage" after CRA should be 1859.0 A on side 2