# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 91.11: optimize computations for SECURE stop criterion

  @fast @rao @mock @dc @preventive-only
  Scenario: US 91.11.1: Interrupt search tree depth early
    Given network file is "epic91/TestCase4Nodes.uct"
    Given crac file is "epic91/CBCORA_interrupt_search_tree.xml"
    Given configuration file is "epic91/RaoParameters_interrupt_search_tree.json"
    When I launch search_tree_rao at "2019-01-08 12:00" on preventive state
    Then 1 remedial actions are used in preventive
    And the margin on cnec "be1_fr1_N - preventive" after PRA should be 2340 A

  @fast @rao @mock @dc @preventive-only
  Scenario: US 91.11.2: Interrupt search tree depth early two threads
    Given network file is "epic91/TestCase4Nodes.uct"
    Given crac file is "epic91/CBCORA_interrupt_search_tree.xml"
    Given configuration file is "epic91/RaoParameters_interrupt_search_tree_2_threads.json"
    When I launch search_tree_rao at "2019-01-08 12:00" on preventive state
    Then 1 remedial actions are used in preventive
    And the margin on cnec "be1_fr1_N - preventive" after PRA should be 2340 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 91.11.3: Skip curative optimization
    Given network file is "epic13/TestCase12NodesForCurative.uct"
    Given crac file is "epic91/CBCORA_ep91us11case3.xml"
    Given configuration file is "common/RaoParameters_posMargin_ac.json"
    When I launch search_tree_rao at "2019-01-08 00:30"
    Then its security status should be "UNSECURED"
    And 0 remedial actions are used in preventive
    And 0 remedial actions are used after "Contingency" at "curative"
    And the worst margin is -70.9 MW
    # Check that curative CNECs' results are available
    And the margin on cnec "CnecCurativeDir - curative" after PRA should be 1359.6 MW
    And the margin on cnec "CnecCurativeDir - curative" after ARA should be 1359.6 MW
    And the margin on cnec "CnecCurativeDir - curative" after CRA should be 1359.6 MW
    And the margin on cnec "CnecCurativeOppo - curative" after PRA should be 1931.3 MW
    And the margin on cnec "CnecCurativeOppo - curative" after ARA should be 1931.3 MW
    And the margin on cnec "CnecCurativeOppo - curative" after CRA should be 1931.3 MW
