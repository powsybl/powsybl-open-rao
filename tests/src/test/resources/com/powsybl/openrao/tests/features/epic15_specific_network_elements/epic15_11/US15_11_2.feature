# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.11.2: Simulate topological automatons right after preventive optimization

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 15.11.2.1: onConstraint automaton not applied
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic15/SL_ep15us11-2case1.json"
    Given configuration file is "epic15/RaoParameters_ep15us11-2.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - preventive" after PRA should be 852.4 A
    And the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 3184.3 A
    And 0 remedial actions are used after "co2_be1_be3" at "auto"
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - auto" after ARA should be 92.4 A
    And 0 remedial actions are used after "co2_be1_be3" at "curative"
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - curative" after CRA should be 92.4 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - curative" after CRA should be 858.8 A
    And 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 878.0 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 15.11.2.2: onConstraint automaton applied
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic15/SL_ep15us11-2case2.json"
    Given configuration file is "epic15/RaoParameters_ep15us11-2.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - preventive" after PRA should be 852.4 A
    And the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 3184.3 A
    And 1 remedial actions are used after "co2_be1_be3" at "auto"
    And the remedial action "open_be1_be4" is used after "co2_be1_be3" at "auto"
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - auto" after ARA should be 16.4 A
    And 1 remedial actions are used after "co2_be1_be3" at "curative"
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - curative" after CRA should be 448.8 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - curative" after CRA should be 970.5 A
    And 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 878.0 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 15.11.2.3: OnContingencyState automaton applied
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic15/SL_ep15us11-2case3.json"
    Given configuration file is "epic15/RaoParameters_ep15us11-2.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - preventive" after PRA should be 852.4 A
    And the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 3184.3 A
    And 1 remedial actions are used after "co2_be1_be3" at "auto"
    And the remedial action "open_be1_be4" is used after "co2_be1_be3" at "auto"
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - auto" after ARA should be 16.4 A
    And 1 remedial actions are used after "co2_be1_be3" at "curative"
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - curative" after CRA should be 448.8 A
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co2_be1_be3 - curative" after CRA should be 970.5 A
    And 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 878.0 A
