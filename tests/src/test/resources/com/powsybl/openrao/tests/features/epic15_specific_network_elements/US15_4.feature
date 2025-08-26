# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.4: Consider two margins on tie-lines for each half-line with relative thresholds

  @fast @crac @mock
  Scenario: US 15.4.0: Import different thresholds (absolute) on two half-lines of the same tie-line
  No threshold with a PERCENT_IMAX unit is defined in the CRAC so no iMax was imported.
    Given network file is "crac7/TestCase12Nodes_with_Xnodes.uct" for CORE CC
    Given crac file is "epic15/ls-Xnodes-and-half-lines.json"
    When I import crac
    Then it should have the following flow CNECs:
      | Name            | NetworkElementId                          | Instant    | Contingency | Optimized | Monitored | ImaxLeft | ImaxRight | NominalVoltageLeft | NominalVoltageRight |
      | Cnec BE-FR Left | BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 | preventive |             | yes       | no        | NaN      | NaN       | 400.0              | 400.0               |
      | Cnec BE-FR Left | BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 | outage     | N-1 DE-NL   | yes       | no        | NaN      | NaN       | 400.0              | 400.0               |
    And the flow cnecs should have the following thresholds:
      | CnecId                                                         | Unit   | Min   | Max  | Side |
      | BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - preventive         | AMPERE | -1500 | 1500 | ONE  |
      | BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - preventive         | AMPERE | -500  | 500  | TWO  |
      | BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - N-1 DE-NL - outage | AMPERE | -1500 | 1500 | ONE  |
      | BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - N-1 DE-NL - outage | AMPERE | -500  | 500  | TWO  |
    And it should have the following PST range actions:
      | PstRangeActionId | PstRangeActionName | NetworkElementId    | InitialTap | MinTap | MaxTap | MinTapAngle | MaxTapAngle |
      | PST_BE           | PST_BE             | BBE2AA1  BBE3AA1  1 | 0          | -16    | 16     | -6.23       | 6.23        |

  @fast @rao @mock @ac @preventive-only
  Scenario: US 15.4.1: Run a rao with different thresholds (absolute) on two half-lines of the same tie-line
    Given network file is "crac7/TestCase12Nodes_with_Xnodes.uct"
    Given crac file is "epic15/ls-Xnodes-and-half-lines.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then the flow on cnec "BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - preventive" after PRA should be 721 A
    Then the margin on cnec "BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - preventive" after PRA should be -221.0 A
    Then the flow on cnec "BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - N-1 DE-NL - outage" after PRA should be 2167 A
    Then the margin on cnec "BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - N-1 DE-NL - outage" after PRA should be -1667 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 15.4.2: Run a rao with different thresholds (relative) on two half-lines of the same tie-line
    Given network file is "epic15/TestCase12Nodes_with_Xnodes_different_imax.uct"
    Given crac file is "epic15/ls-Xnodes-and-half-lines-relative.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then the flow on cnec "BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - preventive" after PRA should be 721 A
    Then the margin on cnec "BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - preventive" after PRA should be -221.0 A
    Then the flow on cnec "BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - N-1 DE-NL - outage" after PRA should be 2167 A
    Then the margin on cnec "BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1 - N-1 DE-NL - outage" after PRA should be -1667 A