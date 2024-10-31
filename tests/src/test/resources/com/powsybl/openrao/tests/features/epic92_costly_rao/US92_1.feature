# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 92.1: Costly network actions optimization

  @fast @preventive-only @costly @rao
  Scenario: US 92.1.1: Selection of cheapest of 3 equivalent network actions
    Given network file is "epic92/2Nodes4ParallelLines.uct"
    Given crac file is "epic92/crac-92-1.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective.json"
    When I launch search_tree_rao
    Then the worst margin is 250.0 MW
    And 1 remedial actions are used in preventive
    And the remedial action "closeBeFr4" is used in preventive
    And the value of the objective function after PRA should be 10.0
