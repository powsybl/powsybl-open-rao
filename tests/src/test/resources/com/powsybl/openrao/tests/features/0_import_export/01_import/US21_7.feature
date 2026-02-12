# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 21.7 : Import angle CNECs

  @fast @crac
  Scenario: US 21.7.1: Import angle CNECs
    Given network file is "epic21/MicroGrid.zip"
    Given crac file is "epic21/CIM_21_7_1.xml"
    When I import crac at "2021-04-02 05:00"
    Then it should have the following angle CNECs:
      | AngleCnecId | Name            | ImportingElementId                    | ExportingElementId                    | Instant  | Contingency | Optimized | Monitored | UpperBound | LowerBound |
      | AngleCnec1  | AngleCnec1-name | _d77b61ef-61aa-4b22-95f6-b56ca080788d | _8d8a82ba-b5b0-4e94-861a-192af055f2b8 | CURATIVE | Co-1        | no        | yes       | 30         |            |
      | AngleCnec2  | AngleCnec2-name | _8d8a82ba-b5b0-4e94-861a-192af055f2b8 | _b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3 | CURATIVE | Co-2        | no        | yes       | -47        |            |
    Then it should have 1 network actions
    Then it should have the following network actions:
      | NetworkActionId | NetworkActionName                 | ElementaryActions | ElementaryActionType      | NetworkElementId                      | Action/Setpoint |
      | RA-1            | Decrease Generation and open line | 3                 | GeneratorAction           | _1dc9afba-23b5-41a0-8540-b479ed8baf4b | 0               |
      | RA-1            | Decrease Generation and open line | 3                 | GeneratorAction           | _550ebe0d-f2b2-48c1-991f-cebea43a21aa | 10              |
      | RA-1            | Decrease Generation and open line | 3                 | TerminalsConnectionAction | _ffbabc27-1ccd-4fdc-b037-e341706c8d29 | OPEN            |
    Then the remedial actions should have the following usage rules:
      | RemedialActionId | UsageRules | Rule              | Method    | Instant  | ContingencyId | AngleCnecId |
      | RA-1             | 1          | OnAngleConstraint | AVAILABLE | CURATIVE | Co-1          | AngleCnec1  |
