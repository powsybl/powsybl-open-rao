# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 93.1: Redispatching actions

  @fast @rao @dc @redispatching @preventive-only
  Scenario: US 93.1.1.a: Extremely basic redispatching on 2 nodes network - maxMargin
    The objective function is to maximize the min margin => shut down both generator
    Redispatching action's setpoint = 0, On FRR1AA1 -> setpoint * 1 = 0, on FRR2 -> setpoint * -1 = 0
    Given network file is "epic93/2Nodes.uct"
    Given crac file is "epic93/crac-93-1-1.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch search_tree_rao
    And the initial margin on cnec "cnecFr1Fr2Preventive" should be -700 MW
    Then 1 remedial actions are used in preventive
    And the remedial action "redispatchingAction" is used in preventive

    And the setpoint of RangeAction "redispatchingAction" should be 0.0 MW in preventive
    And the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 300 MW

  @fast @rao @dc @redispatching @preventive-only
  Scenario: US 93.1.1.bis: Extremely basic redispatching on 2 nodes network - maxMargin - load
    Exact same situation but with loads instead of generator
    Given network file is "epic93/2Nodes_load.uct"
    Given crac file is "epic93/crac-93-1-1-load.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch search_tree_rao
    And the initial margin on cnec "cnecFr1Fr2Preventive" should be -700 MW
    Then 1 remedial actions are used in preventive
    And the remedial action "redispatchingAction" is used in preventive
    And the setpoint of RangeAction "redispatchingAction" should be 0.0 MW in preventive
    And the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 300 MW

  @fast @rao @dc @redispatching @preventive-only
  Scenario: US 93.1.2: Extremely basic redispatching on 2 nodes network - minCost
    The initial setpoint is 1000, it is then updated to 300.0 (because the threshold of the line FR1 FR2 is 300
    after optim => delta = 1000-300 = 700
    Total cost : 10 for activation + 50 x 700 MW shift = 25010
    Given network file is "epic93/2Nodes.uct"
    Given crac file is "epic93/crac-93-1-1.json"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the remedial action "redispatchingAction" is used in preventive
    And the setpoint of RangeAction "redispatchingAction" should be 300.0 MW in preventive
    And the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 0.0 MW

    And the value of the objective function after PRA should be 35010.0

  @fast @rao @dc @redispatching @preventive-only
  Scenario: US 93.1.3: Unbalanced redispatching
  Only one redispatching action available on FR1 and FR2 with a key equal to 1 and another to -0.7
  The sum of the key do not sum up to 1. It's impossible to respect injection balance constraint with a variation != 0.
  The RAO chose to not apply the action.
    Given network file is "epic93/3Nodes.uct"
    Given crac file is "epic93/crac-93-1-3.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And the setpoint of RangeAction "redispatchingAction" should be 1000.0 MW in preventive
    And the margin on cnec "cnecFr1Fr2Preventive" after PRA should be -267.0 MW

  @fast @rao @dc @redispatching @preventive-only
  Scenario: US 93.1.4: Multiple redispatching actions keep network balanced
  Both redispatching actions have distribution keys that do not sum to 0, the actions should be activated together and the result is
  optimal when all generators are shut down. The constraint is satisfied 1000*(1+1)-1000*(1+1)
    Given network file is "epic93/4Nodes.uct"
    Given crac file is "epic93/crac-93-1-4.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used in preventive
    And the remedial action "redispatchingActionFR1FR3" is used in preventive
    And the remedial action "redispatchingActionFR2FR4" is used in preventive
    And the setpoint of RangeAction "redispatchingActionFR1FR3" should be 0.0 MW in preventive
    And the setpoint of RangeAction "redispatchingActionFR2FR4" should be 0.0 MW in preventive
    And the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 500.0 MW
