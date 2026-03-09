# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.12.3: Import HVDC range action from CSE CRAC
  This feature covers the import of the HVDC range actions defined as injections from the CSE CRAC.

  @fast @crac
  Scenario: US 15.12.3.1: Import simple HVDC range actions
    Given network file is "epic15/TestCase16NodesWithUcteHvdc.uct"
    Given crac file is "epic15/cseCrac_ep15us12-3case1.xml"
    When I import crac
    Then it should have the following injection range actions:
      | RangeActionId | RangeActionName | MinRange | MaxRange | GroupId |
      | PRA_HVDC      | PRA_HVDC        | -100.0   | 2000.0   | null    |
      | CRA_HVDC      | CRA_HVDC        | -100.0   | 2000.0   | null    |
    Then the remedial actions should have the following usage rules:
      | RemedialActionId | UsageRules | Instant    | Method    | Rule      |
      | PRA_HVDC         | 1          | PREVENTIVE | AVAILABLE | OnInstant |
      | CRA_HVDC         | 1          | CURATIVE   | AVAILABLE | OnInstant |
    Then the injection range action "CRA_HVDC" should have the following injection distribution keys:
      | InjectionId        | Key  |
      | FFR3AA12_generator | -1.0 |
      | BBE2AA12_generator | 1.0  |
