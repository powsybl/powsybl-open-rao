# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.10.1: Modify voltage level topology as remedial action (2 nodes case)

  @fast @crac @mock
  Scenario: US 15.10.1.1: Import succeed on basic well defined case
    Given network file is "epic15/TestCase12Nodes_forCSE.uct"
    Given crac file is "epic15/cseCrac_ep15us10-1case1.xml"
    Given crac creation parameters file is "epic15/CseCracCreationParameters_15_10_1_1.json"
    When I import crac
    Then it should have the following network actions:
      | NetworkActionId | NetworkActionName | ElementaryActions | ElementaryActionType | NetworkElementId                        | Action/Setpoint |
      | Bus bar ok test | Bus bar ok test   | 4                 | SwitchPair           | BBE1AA1W BBE1AA11 1/BBE1AA1W BBE1AA12 1 | OPEN/CLOSE      |
      | Bus bar ok test | Bus bar ok test   | 4                 | SwitchPair           | BBE1AA1X BBE1AA11 1/BBE1AA1X BBE1AA12 1 | OPEN/CLOSE      |
      | Bus bar ok test | Bus bar ok test   | 4                 | SwitchPair           | BBE1AA1Y BBE1AA11 1/BBE1AA1Y BBE1AA12 1 | OPEN/CLOSE      |
      | Bus bar ok test | Bus bar ok test   | 4                 | SwitchPair           | BBE1AA1Z BBE1AA11 1/BBE1AA1Z BBE1AA12 1 | OPEN/CLOSE      |
    Then the remedial actions should have the following usage rules:
      | RemedialActionId | UsageRules | Rule      | Method    | Instant    | ContingencyId | FlowCnecId |
      | Bus bar ok test  | 1          | OnInstant | Available | preventive |               |            |

  @fast @crac @mock
  Scenario: US 15.10.1.2: Import succeed when some lines are already on final node
    Given network file is "epic15/TestCase12Nodes_forCSE.uct"
    Given crac file is "epic15/cseCrac_ep15us10-1case2.xml"
    Given crac creation parameters file is "epic15/CseCracCreationParameters_15_10_1_2.json"
    When I import crac
    Then it should have the following network actions:
      | NetworkActionId | NetworkActionName | ElementaryActions | ElementaryActionType | NetworkElementId                        | Action/Setpoint |
      | Bus bar ok test | Bus bar ok test   | 4                 | SwitchPair           | BBE1AA1W BBE1AA12 1/BBE1AA1W BBE1AA11 1 | OPEN/CLOSE      |
      | Bus bar ok test | Bus bar ok test   | 4                 | SwitchPair           | BBE1AA1X BBE1AA12 1/BBE1AA1X BBE1AA11 1 | OPEN/CLOSE      |
      | Bus bar ok test | Bus bar ok test   | 4                 | SwitchPair           | BBE1AA1Y BBE1AA12 1/BBE1AA1Y BBE1AA11 1 | OPEN/CLOSE      |
      | Bus bar ok test | Bus bar ok test   | 4                 | SwitchPair           | BBE1AA1Z BBE1AA12 1/BBE1AA1Z BBE1AA11 1 | OPEN/CLOSE      |
    Then the remedial actions should have the following usage rules:
      | RemedialActionId | UsageRules | Rule      | Method    | Instant    | ContingencyId | FlowCnecId |
      | Bus bar ok test  | 1          | OnInstant | Available | preventive |               |            |

  @fast @crac @mock
  Scenario: US 15.10.1.3: Wrong import with missing switch in the network
    Given network file is "epic15/TestCase12Nodes_forCSE.uct"
    Given crac file is "epic15/cseCrac_ep15us10-1case1.xml"
    Given crac creation parameters file is "epic15/CseCracCreationParameters_15_10_1_3.json"
    When I import crac
    Then it should have 0 network actions
    And the native remedial action "Bus bar ok test" should not be imported because of "ELEMENT_NOT_FOUND_IN_NETWORK"

  @fast @crac @mock
  Scenario: US 15.10.1.4: Wrong import with missing creation parameter
    Given network file is "epic15/TestCase12Nodes_forCSE.uct"
    Given crac file is "epic15/cseCrac_ep15us10-1case1.xml"
    Given crac creation parameters file is "epic15/CseCracCreationParameters_15_10_1_4.json"
    When I import crac
    Then it should have 0 network actions
    And the native remedial action "Bus bar ok test" should not be imported because of "INCOMPLETE_DATA"

  @fast @crac @mock
  Scenario: US 15.10.1.5: Import succeed when some lines are already on final node
    Given network file is "epic15/TestCase12Nodes_forCSE_3nodes.uct"
    Given crac file is "epic15/cseCrac_ep15us10-1case5.xml"
    Given crac creation parameters file is "epic15/CseCracCreationParameters_15_10_1_5.json"
    When I import crac
    Then it should have the following network actions:
      | NetworkActionId | NetworkActionName | ElementaryActions | ElementaryActionType | NetworkElementId                        | Action/Setpoint |
      | RA1             | RA1               | 1                 | SwitchPair           | BBE1AA1X BBE1AA11 1/BBE1AA1X BBE1AA12 1 | OPEN/CLOSE      |
      | RA2             | RA2               | 1                 | SwitchPair           | BBE1AA1X BBE1AA12 1/BBE1AA1X BBE1AA11 1 | OPEN/CLOSE      |
    And the remedial actions should have the following usage rules:
      | RemedialActionId | UsageRules | Rule      | Method    | Instant  | ContingencyId | FlowCnecId |
      | RA1              | 1          | OnInstant | Available | curative |               |            |
      | RA2              | 1          | OnInstant | Available | curative |               |            |

  @fast @rao @mock @dc @contingency-scenarios
  Scenario: US 15.10.1.6: bus bar change in RAO
    Given network file is "epic15/TestCase12Nodes_forCSE_3nodes.uct"
    Given crac file is "epic15/cseCrac_ep15us10-1case6.xml"
    Given crac creation parameters file is "epic15/CseCracCreationParameters_15_10_1_6.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the remedial action "RA1" is used in preventive
    And 2 remedial actions are used after "co1_fr2_fr3" at "curative"
    And the remedial action "RA2" is used after "co1_fr2_fr3" at "curative"
    And the remedial action "RA3" is used after "co1_fr2_fr3" at "curative"
    And the worst margin is -1080 MW
    And the margin on cnec "fr1_fr2_co1 - FFR1AA1 ->FFR2AA1   - co1_fr2_fr3 - curative" after CRA should be -1080 MW
    And the margin on cnec "fr2_fr3_n - FFR2AA1 ->FFR3AA1  - preventive" after PRA should be -397 MW
    And the margin on cnec "fr1_fr2_n - FFR1AA1 ->FFR2AA1  - preventive" after PRA should be -224 MW
    And the margin on cnec "fr2_fr3_co1 - FFR2AA1 ->FFR3AA1   - co1_fr2_fr3 - curative" after CRA should be 950 MW
