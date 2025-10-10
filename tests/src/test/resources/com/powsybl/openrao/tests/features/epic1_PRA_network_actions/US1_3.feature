# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 1.3: Security assessment with network actions

  @fast @rao @mock @ac @preventive-only
  Scenario: US 1.3.1: selection of topological action
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic1/SL_ep1us2_selectionTopoRA.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then its security status should be "SECURED"
    Then the worst margin is 56.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 56.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - outage" after PRA should be 56.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - curative" after CRA should be 56.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "Open tie-line FR DE" is used in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: US 1.3.2: selection of PST setpoint remedial action
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic1/SL_ep1us2_selectionTopoRA_variant1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then its security status should be "SECURED"
    Then the worst margin is 83.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 83.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PST @1" is used in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: US 1.3.3: selection of PST setpoint remedial action but residual constraint
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic1/SL_ep1us2_selectionTopoRA_variant2.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then its security status should be "UNSECURED"
    Then the worst margin is -417.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -417.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PST @1" is used in preventive