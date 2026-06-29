Feature: 7.1: Simple CT Actions
  This features covers the CT Actions for proportional GLSK in the case where:
  - the actions doesn't saturate any generator
  - there is no other action on any generator

  @rao @counter-trading
  Scenario: 7.1.1: Preventive search tree RAO REDISPATCH
    Given network file is "counter-trading/2Nodes.uct"
    Given crac file is "counter-trading/crac-2-nodes-redispatch.json"
    Given configuration file is "counter-trading/rao-parameters-2-nodes.json"
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the remedial action "redispatchingActionFR1" is used in preventive
    Then the value of the objective function after PRA should be 20010.0
    Then the worst margin is 0 MW

  @rao @counter-trading
  Scenario: 7.1.2: Preventive search tree RAO with 2 areas of 1 node
    Given network file is "counter-trading/2Nodes.uct"
    Given crac file is "counter-trading/crac-2-nodes.json"
    Given configuration file is "counter-trading/rao-parameters-2-nodes.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes.xml"
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the remedial action "counterTradingAction" is used in preventive
    Then the value of the objective function after PRA should be 20010.0
    Then the worst margin is 0 MW
