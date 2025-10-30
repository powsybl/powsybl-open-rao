# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 1.4: Generate a basic output file after RAO computation

  @fast @rao @mock @ac @preventive-only
  Scenario: US 1.4.1: secure optimization
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic1/SL_ep1us2_selectionTopoRA.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then its security status should be "SECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "Open tie-line FR DE" is used in preventive
    Then the "upper" threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 1500.0 A
    Then the initial flow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 721.0 A
    Then the flow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -1444.0 A
    Then the "upper" threshold on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - outage" should be 1500.0 A
    Then the initial flow on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - outage" should be 652.0 A
    Then the flow on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - outage" after PRA should be -1444.0 A
    Then the initial flow on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - curative" should be 652.0 A
    Then the flow on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - curative" after PRA should be -1444.0 A
    Then the flow on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - curative" after CRA should be -1444.0 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 1.4.2: unsecure optimization
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic1/SL_ep1us4_unsecure.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then its security status should be "UNSECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "PST @1" is used in preventive
    Then the "upper" threshold on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 1500.0 A
    Then the initial flow on cnec "BBE2AA1  FFR3AA1  1 - preventive" should be 721.0 A
    Then the flow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 471.1 A
    Then the "upper" threshold on cnec "FFR2AA1  DDE3AA1  1 - Contingency FR1 FR3 - outage" should be 1500.0 A
    Then the initial flow on cnec "FFR2AA1  DDE3AA1  1 - Contingency FR1 FR3 - outage" should be 2098.0 A
    Then the flow on cnec "FFR2AA1  DDE3AA1  1 - Contingency FR1 FR3 - outage" after PRA should be 1859.0 A
    Then the flow on cnec "FFR2AA1  DDE3AA1  1 - Contingency FR1 FR3 - outage" after CRA should be 1859.0 A