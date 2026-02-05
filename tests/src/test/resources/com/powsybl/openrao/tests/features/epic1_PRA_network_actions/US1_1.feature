# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 1.1: Security assessment without any remedial action

  @fast @rao @ac @preventive-only
  Scenario: US 1.1.1
  No remedial action, several unsecure CNECs.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic1/SL_ep1us0_withoutRA.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the worst margin is -667.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -667.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - Defaut FR1 FR3 - outage" after PRA should be -598.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - Defaut FR1 FR3 - curative" after CRA should be -598.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - Defaut FR1 FR2 - outage" after PRA should be -389.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - Defaut FR1 FR2 - curative" after CRA should be -389.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 779.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - Defaut FR1 FR3 - outage" after PRA should be 848.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - Defaut FR1 FR3 - curative" after CRA should be 848.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - Defaut FR1 FR2 - outage" after PRA should be 1056.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - Defaut FR1 FR2 - curative" after CRA should be 1056.0 A