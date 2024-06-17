# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 1.2: Import of simple network actions as preventive remedial action free to use

  @fast @crac @mock
  Scenario: US 1.2.1
    Given crac file is "epic1/SL_ep1us2_selectionTopoRA.json"
    Given network file is "common/TestCase12Nodes.uct"
    When I import crac
    Then it should have the following flow CNECs:
      | Name           | NetworkElementId    | Instant    | Contingency         | Optimized | Monitored | ImaxLeft | ImaxRight | NominalVoltageLeft | NominalVoltageRight |
      | Tie-line FR BE | BBE2AA1  FFR3AA1  1 | preventive |                     | yes       | no        | 5000.0   | 5000.0    | 400.0              | 400.0               |
      | Tie-line FR BE | BBE2AA1  FFR3AA1  1 | curative   | Contingency FR1 FR3 | yes       | no        | 5000.0   | 5000.0    | 400.0              | 400.0               |
      | Tie-line FR BE | BBE2AA1  FFR3AA1  1 | outage     | Contingency FR1 FR3 | yes       | no        | 5000.0   | 5000.0    | 400.0              | 400.0               |
      | Tie-line FR DE | FFR2AA1  DDE3AA1  1 | preventive |                     | yes       | no        | 5000.0   | 5000.0    | 400.0              | 400.0               |
      | Tie-line FR DE | FFR2AA1  DDE3AA1  1 | curative   | Contingency FR1 FR3 | yes       | no        | 5000.0   | 5000.0    | 400.0              | 400.0               |
      | Tie-line FR DE | FFR2AA1  DDE3AA1  1 | outage     | Contingency FR1 FR3 | yes       | no        | 5000.0   | 5000.0    | 400.0              | 400.0               |
    Then the flow cnecs should have the following thresholds:
      | CnecId                                               | Unit   | Min   | Max  | Side  |
      | BBE2AA1  FFR3AA1  1 - preventive                     | AMPERE | -1500 | 1500 | RIGHT |
      | BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - outage   | AMPERE | -1500 | 1500 | RIGHT |
      | BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - curative | AMPERE | -1500 | 1500 | RIGHT |
      | FFR2AA1  DDE3AA1  1 - preventive                     | AMPERE | -1500 | 1500 | RIGHT |
      | FFR2AA1  DDE3AA1  1 - Contingency FR1 FR3 - outage   | AMPERE | -1500 | 1500 | RIGHT |
      | FFR2AA1  DDE3AA1  1 - Contingency FR1 FR3 - curative | AMPERE | -1500 | 1500 | RIGHT |
    Then it should have the following network actions:
      | NetworkActionId      | NetworkActionName    | ElementaryActions | ElementaryActionType             | NetworkElementId    | Action/Setpoint |
      | Open tie-line FR DE  | Open tie-line FR DE  | 1                 | TerminalsConnectionAction        | FFR2AA1  DDE3AA1  1 | OPEN            |
      | Close tie-line FR BE | Close tie-line FR BE | 1                 | TerminalsConnectionAction        | BBE2AA1  FFR3AA1  1 | CLOSE           |
      | PST @1               | PST @1               | 1                 | PhaseTapChangerTapPositionAction | BBE2AA1  BBE3AA1  1 | -16             |
    Then the remedial actions should have the following usage rules:
      | RemedialActionId     | UsageRules | Rule      | Method    | Instant    | ContingencyId | FlowCnecId |
      | Open tie-line FR DE  | 1          | OnInstant | Available | preventive |               |            |
      | Close tie-line FR BE | 1          | OnInstant | Available | preventive |               |            |
      | PST @1               | 1          | OnInstant | Available | preventive |               |            |