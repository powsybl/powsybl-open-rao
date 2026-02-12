# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 93.1: Basic redispatching actions (free and costly)

  @fast @rao @dc @redispatching @preventive-only @max-min-margin @megawatt
  Scenario: US 93.1.1.a: Extremely basic redispatching on 2 nodes network - maxMargin
  Two nodes containing one generator each, and linked by an overloaded line.
  One generator produces 1000, the other produces -1000.
  The objective is to maximize the min margin => shut down both generators.
  Redispatching action's chosen setpoint = 0: on FRR1AA1 -> setpoint * 1 = 0, on FRR2 -> setpoint * -1 = 0.
    Given network file is "epic93/2Nodes.uct"
    Given crac file is "epic93/crac-93-1-1.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then the worst margin is 300 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then the initial margin on cnec "cnecFr1Fr2Preventive" should be -700 MW
    Then 1 remedial actions are used in preventive
    Then the remedial action "redispatchingAction" is used in preventive
    Then the setpoint of RangeAction "redispatchingAction" should be 0.0 MW in preventive
    Then the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 300 MW

  @fast @rao @dc @redispatching @preventive-only @max-min-margin @megawatt
  Scenario: US 93.1.1.bis: Extremely basic redispatching on 2 nodes network - maxMargin - load
  Exact same situation but with loads instead of generators.
    Given network file is "epic93/2Nodes_load.uct"
    Given crac file is "epic93/crac-93-1-1-load.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then the worst margin is 300 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then the initial margin on cnec "cnecFr1Fr2Preventive" should be -700 MW
    Then 1 remedial actions are used in preventive
    Then the remedial action "redispatchingAction" is used in preventive
    Then the setpoint of RangeAction "redispatchingAction" should be 0.0 MW in preventive
    Then the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 300 MW

  @fast @rao @dc @redispatching @preventive-only @costly @megawatt
  Scenario: US 93.1.2: Extremely basic redispatching on 2 nodes network - minCost
  Network from US 93.1.1.a, but objective function minCost.
  The initial setpoint is 1000, it is then updated to 300.0 (because the threshold of the line FR1 FR2 is 300)
  After optim => redispatching volume = 1000 - 300 = 700 MW
  Total cost : 10 for activation + 50 * 700 MW = 35010
    Given network file is "epic93/2Nodes.uct"
    Given crac file is "epic93/crac-93-1-1.json"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_violation_0.json"
    When I launch rao
    Then the worst margin is 0 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then 1 remedial actions are used in preventive
    Then the remedial action "redispatchingAction" is used in preventive
    Then the setpoint of RangeAction "redispatchingAction" should be 300.0 MW in preventive
    Then the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 0.0 MW
    Then the value of the objective function after PRA should be 35010.0
    # TODO: suspicious result, incoherent with description -> due to shifted-violation-threshold = 10

    # TODO: can I write a 93.1.3.bis with the same situation but correctly balanced?
  # No log to explain why the RA is not applied, should there be one?
  @fast @rao @dc @redispatching @preventive-only @max-min-margin @megawatt
  Scenario: US 93.1.3: Unbalanced redispatching
  Only one redispatching action available: with a key equal to 1 on FR1 and -0.7 on FR2.
  The sum of the key do not sum up to 1. It's impossible to respect injection balance constraint with a variation != 0.
  The RAO chose to not apply the action.
    Given network file is "epic93/3Nodes.uct"
    Given crac file is "epic93/crac-93-1-3.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then the worst margin is -267 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then the initial margin on cnec "cnecFr1Fr2Preventive" should be -267 MW
    Then 0 remedial actions are used in preventive
    Then the setpoint of RangeAction "redispatchingAction" should be 1000.0 MW in preventive
    Then the margin on cnec "cnecFr1Fr2Preventive" after PRA should be -267.0 MW

  @fast @rao @dc @redispatching @preventive-only @max-min-margin @megawatt
  Scenario: US 93.1.4: Multiple redispatching actions keep network balanced - max min margin
  Both redispatching actions have distribution keys that do not sum to 0, the actions have to be activated together to
  compensate each other, and the result is optimal when all generators are shut down.
  Injection balance constraint is satisfied 1000*(1+1)-1000*(1+1)=0
    Given network file is "epic93/4Nodes.uct"
    Given crac file is "epic93/crac-93-1-4.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then the worst margin is 500 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then 2 remedial actions are used in preventive
    Then the remedial action "redispatchingActionFR1FR3" is used in preventive
    Then the remedial action "redispatchingActionFR2FR4" is used in preventive
    Then the setpoint of RangeAction "redispatchingActionFR1FR3" should be 0.0 MW in preventive
    Then the setpoint of RangeAction "redispatchingActionFR2FR4" should be 0.0 MW in preventive
    Then the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 500.0 MW

  @fast @rao @dc @redispatching @preventive-only @costly @megawatt
  Scenario: US 93.1.5: Multiple redispatching actions keep network balanced with more "complex" keys - min cost
  One redispatching action that acts on two generators with keys 1 and -0.7 and one on one generator with key 0.6.
  Injection balance constraint is respected:
      - redispatchingActionFR1FR2: initial setpoint = 1000 = 1000/1 = -700/-0.7, final = 512 => delta- = 488
      - redispatchingActionFR3: initial setpoint = -300/0.6 = -500, final = -256 => delta+ = −244
      => 488*(1-0.7)-244*0.6 = 0
  Objective function breakdown: 10+488*50+10+244*50 = 36620
    Given network file is "epic93/3Nodes.uct"
    Given crac file is "epic93/crac-93-1-5.json"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    When I launch rao
    Then the worst margin is 9.87 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then the initial margin on cnec "cnecFr1Fr2Preventive" should be -267 MW
    Then 2 remedial actions are used in preventive
    Then the setpoint of RangeAction "redispatchingActionFR1FR2" should be 512.0 MW in preventive
    Then the setpoint of RangeAction "redispatchingActionFR3" should be -256.0 MW in preventive
    Then the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 9.87 MW
    Then the value of the objective function after PRA should be 36620.0

  @fast @rao @dc @redispatching @preventive-only @costly @megawatt
  Scenario: US 93.1.6: Redispatching with disconnected generator
  A simple three nodes network where all the prod is on FFR2AA1 and all the load is on FFR1AA1.
  The generator FFR3AA1 is disconnected so the redispatchingActionFR3 won't be used so the RAO can't use redispatchingActionFR1 either
  because it won't be able to satisfy the injection balancing constraint.
    Given network file is "epic93/3Nodes_FFR3AA1_disconnected.xiidm"
    Given crac file is "epic93/crac-93-1-6.json"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    When I launch rao
    Then the worst margin is -366.67 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then the initial setpoint of RangeAction "redispatchingActionFR1" should be -1000.0
    Then the setpoint of RangeAction "redispatchingActionFR1" should be -1000.0 MW in preventive
    Then 0 remedial actions are used in preventive


