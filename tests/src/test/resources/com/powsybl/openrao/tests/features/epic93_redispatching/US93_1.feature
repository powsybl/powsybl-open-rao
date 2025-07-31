# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 93.1: Redispatching actions

  # faire un test avec crac load/ et loads dans le réseau
  # pour les cracs generateurs => générateurs dans le réseau

  @fast @rao @dc @redispatching @preventive-only
  Scenario: US 93.1.1.a: Extremely basic redispatching on 2 nodes network - maxMargin
    Given network file is "epic93/2Nodes.uct"
    Given crac file is "epic93/crac-93-1-1.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch search_tree_rao
    And the initial margin on cnec "cnecFr1Fr2Preventive" should be -700 MW
    Then 1 remedial actions are used in preventive
    And the remedial action "redispatchingAction" is used in preventive
    # The objective function is to maximize the min margin => shut down both generator
    # Redispatching action's setpoint = 0, On FRR1AA1 -> setpoint * 1 = 0, on FRR2 -> setpoint * -1 = 0
    And the setpoint of RangeAction "redispatchingAction" should be 0.0 MW in preventive
    And the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 300 MW

  @fast @rao @dc @redispatching @preventive-only
  Scenario: US 93.1.1.b: Extremely basic redispatching on 2 nodes network - minCost
    Given network file is "epic93/2Nodes.uct"
    Given crac file is "epic93/crac-93-1-1.json"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the remedial action "redispatchingAction" is used in preventive
    And the setpoint of RangeAction "redispatchingAction" should be 300.0 MW in preventive
    And the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 0.0 MW
    # The initial setpoint is 1000, it is then updated to 300.0 (because the threshold of the line FR1 FR2 is 300
    # after optim => delta = 1000-300 = 700
    # Total cost : 10 for activation + 50 x 700 MW shift = 25010
    And the value of the objective function after PRA should be 35010.0

  @fast @rao @dc @redispatching @preventive-only
  Scenario: US 93.1.2: Unbalanced redispatching
  Only one redispatching action available, the distribution keys do not sum up to 0. The RAO chose to not apply the action.
    Given network file is "epic93/3Nodes.uct"
    Given crac file is "epic93/crac-93-1-2b.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And the setpoint of RangeAction "redispatchingAction" should be 0.0 MW in preventive
    And the margin on cnec "cnecFr1Fr3Preventive" after PRA should be -167.0 MW

  @fast @rao @dc @redispatching @preventive-only
  Scenario: US 93.1.3: Multiple redispatching actions keep network balanced
  Both redispatching actions have distribution keys that do not sum to 0, the actions should be activated together and the result is
  optimal when all generators are shut down.
    Given network file is "epic93/3Nodes.uct"
    Given crac file is "epic93/crac-93-1-3.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used in preventive
    And the remedial action "redispatchingActionFR3FR2" is used in preventive
    And the remedial action "redispatchingActionFR1FR2" is used in preventive
    And the setpoint of RangeAction "redispatchingActionFR3FR2" should be 1000.0 MW in preventive
    And the setpoint of RangeAction "redispatchingActionFR1FR2" should be -1000.0 MW in preventive
    And the margin on cnec "cnecFr1Fr3Preventive" after PRA should be 500.0 MW
