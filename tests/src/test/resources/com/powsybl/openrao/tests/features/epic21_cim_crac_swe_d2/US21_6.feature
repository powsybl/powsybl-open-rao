# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 21.6 : Import HVDC Range Actions

  @fast @crac @mock
  Scenario: US 21.6.1: Import HVDC automaton from CIM CRAC
    Given network file is "epic21/TestCase16NodesWith2Hvdc.xiidm"
    Given crac file is "epic21/CIM_21_6_1.xml"
    Given crac creation parameters file is "epic21/CimCracCreationParameters_21_6_1.json"
    When I import crac
    Then its name should be "CORESO-20210402-SWECCD2-F011"
    And its id should be "CORESO-20210402-SWECCD2-F011"
    And it should have 2 range actions
    And it should have the following HVDC range actions:
      | HvdcRangeActionId                                       | HvdcRangeActionName                                     | NetworkElementId    | GroupId                                   | InitialSetpoint |
      | HVDC-direction1 + HVDC-direction2 - BBE2AA11 FFR3AA11 1 | HVDC-direction1 + HVDC-direction2 - BBE2AA11 FFR3AA11 1 | BBE2AA11 FFR3AA11 1 | BBE2AA11 FFR3AA11 1 + BBE2AA12 FFR3AA12 1 | 0.0             |
      | HVDC-direction1 + HVDC-direction2 - BBE2AA12 FFR3AA12 1 | HVDC-direction1 + HVDC-direction2 - BBE2AA12 FFR3AA12 1 | BBE2AA12 FFR3AA12 1 | BBE2AA11 FFR3AA11 1 + BBE2AA12 FFR3AA12 1 | 0.0             |
    And the HVDC range actions should have the following ranges:
      | HvdcRangeActionId                                       | Ranges | RangeType | Min     | Max    |
      | HVDC-direction1 + HVDC-direction2 - BBE2AA11 FFR3AA11 1 | 1      | ABSOLUTE  | -1000.0 | 1500.0 |
      | HVDC-direction1 + HVDC-direction2 - BBE2AA12 FFR3AA12 1 | 1      | ABSOLUTE  | -1000.0 | 1500.0 |
    And the remedial actions should have the following usage rules:
      | RemedialActionId                                        | UsageRules | Rule               | Method | Instant | ContingencyId | FlowCnecId |
      | HVDC-direction1 + HVDC-direction2 - BBE2AA11 FFR3AA11 1 | 1          | OnContingencyState | Forced | auto    | Co-1          |            |
      | HVDC-direction1 + HVDC-direction2 - BBE2AA12 FFR3AA12 1 | 1          | OnContingencyState | Forced | auto    | Co-1          |            |

  @fast @crac @mock
  Scenario: US 21.6.2: Import on flow constraint in country with country from CIM CRAC
    Given network file is "epic21/TestCase16NodesWith2Hvdc.xiidm"
    Given crac file is "epic21/CIM_21_6_2.xml"
    Given crac creation parameters file is "epic21/CimCracCreationParameters_21_6_1.json"
    When I import crac
    Then its name should be "CORESO-20210402-SWECCD2-F011"
    And its id should be "CORESO-20210402-SWECCD2-F011"
    And it should have 1 network actions
    And it should have the following network actions:
      | NetworkActionId | NetworkActionName | ElementaryActions | ElementaryActionType | NetworkElementId   | Action/Setpoint |
      | Auto RA Gen FR  | Auto RA Gen FR    | 1                 | GeneratorAction      | FFR1AA11_generator | 0               |
    And the remedial actions should have the following usage rules:
      | RemedialActionId | UsageRules | Rule                      | Method | Instant | ContingencyId | Country |
      | Auto RA Gen FR   | 1          | OnFlowConstraintInCountry | Forced | auto    | Co-1          | FR      |
