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

  @fast @preventive-only @costly @rao
  Scenario: US 92.1.2: Selection of cheapest network action even if it does not maximize minimum margin
    Line BE-FR-3 has a higher resistance than line BE-FR-2 which means that closing the latter will lead
    to a higher margin on the optimized CNEC. However, closing line BE-FR-3 is cheaper and still secures
    the CNEC so it will be chosen by the RAO.
    Given network file is "epic92/2Nodes3ParallelLines.uct"
    Given crac file is "epic92/crac-92-2.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective.json"
    When I launch search_tree_rao
    Then the worst margin is 83.0 MW
    And 1 remedial actions are used in preventive
    And the remedial action "closeBeFr3" is used in preventive
    And the value of the objective function after PRA should be 25.0

  @fast @preventive-only @costly @rao
  Scenario: US 92.1.2.bis: Duplicate of 92.1.2 in MAX_MIN_MARGIN mode
    The situation is the same as in US 92.1.2 but the RAO maximizes the minimum margin.
    As activation costs are not taken in account, both lines will be closed.
    Given network file is "epic92/2Nodes3ParallelLines.uct"
    Given crac file is "epic92/crac-92-2.json"
    Given configuration file is "epic92/RaoParameters_margin_dc_minObjective.json"
    When I launch search_tree_rao
    Then the worst margin is 464.29 MW
    And 2 remedial actions are used in preventive
    And the remedial action "closeBeFr2" is used in preventive
    And the remedial action "closeBeFr3" is used in preventive
    And the value of the objective function after PRA should be -464.29
