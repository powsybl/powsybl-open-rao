# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 90.7: Handle Xnodes
  # CRAC #7.0 in Confluence

  @fast @crac @mock
  Scenario: US 90.7.1: Handle CRAC import with half-line IDs
    Given network file is "crac7/TestCase12Nodes_with_Xnodes.uct"
    Given crac file is "crac7/ls-Xnodes.json"
    When I import crac
    Then it should have the following flow CNECs:
      | Name       | NetworkElementId                          | Instant    | Contingency | Optimized | Monitored | ImaxLeft | ImaxRight | NominalVoltageLeft | NominalVoltageRight |
      | Cnec BE-FR | BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 | preventive |             | yes       | no        | 5000     | 5000      | 380                | 380                 |
      | Cnec BE-FR | BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 | outage     | N-1 DE-NL   | yes       | no        | 5000     | 5000      | 380                | 380                 |
      | Cnec DE-FR | DDE3AA1  X_DEFR1  1 + FFR2AA1  X_DEFR1  1 | preventive |             | yes       | no        | 5000     | 5000      | 380                | 380                 |
      | Cnec DE-FR | DDE3AA1  X_DEFR1  1 + FFR2AA1  X_DEFR1  1 | outage     | N-1 DE-NL   | yes       | no        | 5000     | 5000      | 380                | 380                 |
    And the flow cnecs should have the following thresholds:
      | CnecId                                                         | Unit   | Min   | Max  | Side  |
      | BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - preventive         | AMPERE | -1500 | 1500 | ONE  |
      | BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - N-1 DE-NL - outage | AMPERE | -5000 | 5000 | ONE  |
      | DDE3AA1  X_DEFR1  1 + FFR2AA1  X_DEFR1  1 - preventive         | AMPERE | -1500 | 1500 | TWO |
      | DDE3AA1  X_DEFR1  1 + FFR2AA1  X_DEFR1  1 - N-1 DE-NL - outage | AMPERE | -5000 | 5000 | TWO |
    And it should have 1 network actions

  @fast @rao @mock @ac @preventive-only
  Scenario: US 90.7.2: Reference run for tests with Xnodes
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "crac7/ls-ref.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then its security status should be "UNSECURED"
    Then the worst margin is -416.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -416.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 1028.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 DE-NL - outage" after PRA should be 1380.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - N-1 DE-NL - outage" after PRA should be 2832.0 A
    Then the tap of PstRangeAction "PST_BE" should be -16 in preventive
    Then 1 remedial actions are used in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: US 90.7.3: Run a rao on a simple network with one X-node but the line containing X-node is not optimized
    Given network file is "crac7/TestCase12Nodes_with_one_Xnode.uct"
    Given crac file is "crac7/ls-Xnode-ref.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    Then the worst margin is 1028.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 1028.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - N-1 DE-NL - outage" after PRA should be 2832.0 A
    Then the tap of PstRangeAction "PST_BE" should be -16 in preventive
    Then 1 remedial actions are used in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: US 90.7.4: Run a rao on a simple network with one X-node and the line containing X-node is optimized
    Given network file is "crac7/TestCase12Nodes_with_one_Xnode.uct"
    Given crac file is "crac7/ls-Xnode.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then its security status should be "UNSECURED"
    Then the worst margin is -416.0 A
    Then the margin on cnec "DDE3AA1  X_DEFR1  1 + FFR2AA1  X_DEFR1  1 - preventive" after PRA should be -416.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 1028.0 A
    Then the margin on cnec "DDE3AA1  X_DEFR1  1 + FFR2AA1  X_DEFR1  1 - N-1 DE-NL - outage" after PRA should be 1380.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - N-1 DE-NL - outage" after PRA should be 2832.0 A
    Then the tap of PstRangeAction "PST_BE" should be -16 in preventive
    Then 1 remedial actions are used in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: US 90.7.5: Run a rao on a network with X-nodes at each border and 2 lines are optimized
    Given network file is "crac7/TestCase12Nodes_with_Xnodes.uct"
    Given crac file is "crac7/ls-Xnodes.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then its security status should be "UNSECURED"
    Then the worst margin is -416.0 A
    Then the margin on cnec "DDE3AA1  X_DEFR1  1 + FFR2AA1  X_DEFR1  1 - preventive" after PRA should be -416.0 A
    Then the margin on cnec "BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - preventive" after PRA should be 1028.0 A
    Then the margin on cnec "DDE3AA1  X_DEFR1  1 + FFR2AA1  X_DEFR1  1 - N-1 DE-NL - outage" after PRA should be 1380.0 A
    Then the margin on cnec "BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - N-1 DE-NL - outage" after PRA should be 2825.0 A
    Then the tap of PstRangeAction "PST_BE" should be -16 in preventive
    Then 1 remedial actions are used in preventive


