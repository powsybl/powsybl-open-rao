Feature: 7.1: Simple CT Actions
  This features covers preventive CT Actions defined per area (proportional GLSK).
  7.1.1 to 7.1.9: nominal cases on 2 and 3 areas, with and without generator saturation.
  7.1.10 to 7.1.26: robustness and edge cases (multi-area, generators, GLSK, CRAC definition, other remedial actions).
  Reading the expected values:
  - a positive CT setpoint means the area produces more, a negative one that it produces less;
  - CT cost = activation cost + variation cost (up or down) x |setpoint|;
  - an N-1 on one of two parallel lines puts the whole exchange on the remaining line.

  @rao @counter-trading
  Scenario: 7.1.1: Preventive search tree RAO REDISPATCH
    Given network file is "counter-trading/2Nodes.uct"
    Given crac file is "counter-trading/crac-2-nodes-redispatch.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
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
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes.xml"
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the remedial action "counterTradingActionFR" is used in preventive
    Then the remedial action "counterTradingActionBE" is used in preventive
    Then the value of the objective function after PRA should be 20010.0

  @rao @counter-trading
  Scenario: 7.1.3: Preventive search tree RAO with 3 areas of 1 node
    Given network file is "counter-trading/3Nodes.uct"
    Given crac file is "counter-trading/crac-3-nodes.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_3nodes.xml"
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the remedial action "counterTradingAction_FR" is used in preventive
    Then the remedial action "counterTradingAction_BE" is used in preventive
    Then the remedial action "counterTradingAction_ES" is used in preventive
    Then the generator "FFR1AA1 _generator" should have a targetP of -1050 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of 600 MW after PRA
    Then the generator "EES1AA1 _generator" should have a targetP of 450 MW after PRA

  @rao @counter-trading
  Scenario: 7.1.4: Preventive search tree RAO with 2 areas and 2 generators in an area
    Given network file is "counter-trading/2NodesFR1NodeBE.uct"
    Given crac file is "counter-trading/crac-2-nodes-fr-1-node-be.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes_fr_1node_be.xml"
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the remedial action "counterTradingActionFR" is used in preventive
    Then the remedial action "counterTradingActionBE" is used in preventive
    Then the generator "FFR1AA1 _generator" should have a targetP of 220 MW after PRA
    Then the generator "FFR2AA1 _generator" should have a targetP of 380 MW after PRA
    Then the value of the objective function after PRA should be 20010.0

  @rao @counter-trading
  Scenario: 7.1.5: Preventive search tree RAO with 2 areas and 2 generators in an area with equal proportions
    Given network file is "counter-trading/2NodesFR1NodeBE.uct"
    Given crac file is "counter-trading/crac-2-nodes-fr-1-node-be.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes_fr_1node_be_equalProportions.xml"
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the remedial action "counterTradingActionFR" is used in preventive
    Then the remedial action "counterTradingActionBE" is used in preventive
    Then the generator "FFR1AA1 _generator" should have a targetP of 300 MW after PRA
    Then the generator "FFR2AA1 _generator" should have a targetP of 300 MW after PRA
    Then the value of the objective function after PRA should be 20010.0


  @rao @counter-trading
  Scenario: 7.1.6: Preventive search tree RAO with 2 areas and 2 generators in an area with different proportions
    Given network file is "counter-trading/2NodesFR1NodeBE_differentProportions.uct"
    Given crac file is "counter-trading/crac-2-nodes-fr-1-node-be.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes_fr_1node_be.xml"
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the remedial action "counterTradingActionFR" is used in preventive
    Then the remedial action "counterTradingActionBE" is used in preventive
    Then the generator "FFR1AA1 _generator" should have a targetP of 420 MW after PRA
    Then the generator "FFR2AA1 _generator" should have a targetP of 180 MW after PRA
    Then the value of the objective function after PRA should be 20010.0

  @rao @counter-trading
  Scenario: 7.1.7: Preventive search tree RAO with 2 areas and 2 generators in an area, one of them reaches saturation
    Given network file is "counter-trading/2NodesFR1NodeBE_saturationFR2.uct"
    Given crac file is "counter-trading/crac-2-nodes-fr-1-node-be.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes_fr_1node_be.xml"
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the remedial action "counterTradingActionFR" is used in preventive
    Then the remedial action "counterTradingActionBE" is used in preventive
    Then the generator "FFR1AA1 _generator" should have a targetP of 600 MW after PRA
    Then the generator "FFR2AA1 _generator" should have a targetP of 0 MW after PRA
    Then the value of the objective function after PRA should be 20010.0


  @rao @counter-trading
  Scenario: 7.1.8: Preventive search tree RAO with 2 areas and 2 generators in an area, one of them reaches saturation
    Given network file is "counter-trading/2NodesFR1NodeBE_saturationFR2_0.uct"
    Given crac file is "counter-trading/crac-2-nodes-fr-1-node-be.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes_fr_1node_be.xml"
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the remedial action "counterTradingActionFR" is used in preventive
    Then the remedial action "counterTradingActionBE" is used in preventive
    Then the generator "FFR1AA1 _generator" should have a targetP of 350 MW after PRA
    Then the generator "FFR2AA1 _generator" should have a targetP of 250 MW after PRA
    Then the value of the objective function after PRA should be 20010.0

  @rao @counter-trading
  Scenario: 7.1.9: CT action on one area only, its neighbour has none
    Given network file is "counter-trading/2Nodes.uct"
    Given crac file is "counter-trading/crac-2-nodes-fr-only.json"
    Given configuration file is "counter-trading/rao-parameters-ct-1-iteration.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes.xml"
    When I launch rao at "2021-04-30 22:30"
    Then the remedial action "counterTradingActionFR" is not used in preventive
    Then its security status should be "UNSECURED"

  @rao @counter-trading
  Scenario: 7.1.10: Two areas exchange through a third area which has no CT action
    FR has no CT action, so its balance must stay at 0: the 400 MW BE must not come from ES, through FR.
    Given network file is "counter-trading/3Nodes.uct"
    Given crac file is "counter-trading/crac-3-nodes-no-ct-fr.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_3nodes.xml"
    When I launch rao at "2021-04-30 22:30"
    Then the remedial action "counterTradingAction_BE" is not used in preventive
    Then the remedial action "counterTradingAction_ES" is not used in preventive
    Then its security status should be "UNSECURED"

  @rao @counter-trading
  Scenario: 7.1.11: A border area without CT action is left untouched
    ES has no CT action: it keeps producing 800 MW and FR is the only counterpart of BE.
    FR only has an activation cost (variation cost 0): 10 + 0 x 400 + 5 + 50 x 400 = 20015.
    Given network file is "counter-trading/3Nodes.uct"
    Given crac file is "counter-trading/crac-3-nodes-no-ct-es.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_3nodes.xml"
    When I launch rao at "2021-04-30 22:30"
    Then its security status should be "SECURED"
    Then the generator "FFR1AA1 _generator" should have a targetP of -1400 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of 600 MW after PRA
    Then the generator "EES1AA1 _generator" should have a targetP of 800 MW after PRA
    Then the setpoint of RangeAction "counterTradingAction_FR" should be 400 MW in preventive
    Then the setpoint of RangeAction "counterTradingAction_BE" should be -400 MW in preventive
    Then the value of the objective function after PRA should be 20015.0

  @rao @counter-trading
  Scenario: 7.1.12: The cheapest counterpart is used up to its range, the next one takes the rest
    ES costs 10 per MW but its range is limited to +/-100 MW, FR costs 50 per MW: ES takes 100 MW, FR the 300 MW left.
    Given network file is "counter-trading/3Nodes.uct"
    Given crac file is "counter-trading/crac-3-nodes-es-cheap-limited.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_3nodes.xml"
    When I launch rao at "2021-04-30 22:30"
    Then its security status should be "SECURED"
    Then the generator "FFR1AA1 _generator" should have a targetP of -1500 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of 600 MW after PRA
    Then the generator "EES1AA1 _generator" should have a targetP of 900 MW after PRA
    Then the setpoint of RangeAction "counterTradingAction_BE" should be -400 MW in preventive
    Then the setpoint of RangeAction "counterTradingAction_ES" should be 100 MW in preventive
    Then the setpoint of RangeAction "counterTradingAction_FR" should be 300 MW in preventive
    Then the value of the objective function after PRA should be 36020.0

  @rao @counter-trading
  Scenario: 7.1.13: The cheapest counterpart saturates at its Pmax before reaching its range
    Same CRAC as 7.1.12, but the ES generator has Pmax = 850 MW: only 50 MW of the 100 MW range can be used.
    Given network file is "counter-trading/3Nodes_ESPmax850.uct"
    Given crac file is "counter-trading/crac-3-nodes-es-cheap-limited.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_3nodes.xml"
    When I launch rao at "2021-04-30 22:30"
    Then its security status should be "SECURED"
    Then the generator "FFR1AA1 _generator" should have a targetP of -1450 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of 600 MW after PRA
    Then the generator "EES1AA1 _generator" should have a targetP of 850 MW after PRA
    Then the setpoint of RangeAction "counterTradingAction_BE" should be -400 MW in preventive
    Then the setpoint of RangeAction "counterTradingAction_ES" should be 50 MW in preventive
    Then the setpoint of RangeAction "counterTradingAction_FR" should be 350 MW in preventive
    Then the value of the objective function after PRA should be 38020.0

  @rao @counter-trading
  Scenario: 7.1.14: Three areas connected in a triangle
    Same case as 7.1.3 with an extra BE-ES line (x = 25): the border exchanges form a loop.
    After an N-1, a remaining FR-BE line carries 3/4 of BE's production + 1/8 of ES's, and symmetrically for FR-ES.
    Given network file is "counter-trading/3Nodes_triangle.uct"
    Given crac file is "counter-trading/crac-3-nodes.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_3nodes.xml"
    When I launch rao at "2021-04-30 22:30"
    Then the worst margin is 0 MW
    Then the generator "FFR1AA1 _generator" should have a targetP of -1200 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of 720 MW after PRA
    Then the generator "EES1AA1 _generator" should have a targetP of 480 MW after PRA
    Then the setpoint of RangeAction "counterTradingAction_FR" should be 600 MW in preventive
    Then the setpoint of RangeAction "counterTradingAction_BE" should be -280 MW in preventive
    Then the setpoint of RangeAction "counterTradingAction_ES" should be -320 MW in preventive
    Then the value of the objective function after PRA should be 60020.0

  @rao @counter-trading
  Scenario: 7.1.16: Proportional GLSK on an area with special generators
    Checks that, without GLSK file, the CT effort only goes to generators able to move. FR must produce 400 MW less.
    Each FR generator tests one case: FR1_disconnected is left out of the GLSK, FR1_zero (targetP 0) gets a negligible share,
    FR1_fixed (Pmin = Pmax) saturates at once and its share goes to the others.
    Given network file is "counter-trading/2Nodes_FRSeveralGenerators.xiidm"
    Given crac file is "counter-trading/crac-2-nodes.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    When I launch rao at "2021-04-30 22:30"
    Then its security status should be "SECURED"
    Then the generator "FR1_big" should have a targetP of 300 MW after PRA
    Then the generator "FR1_small" should have a targetP of 100 MW after PRA
    Then the generator "FR1_zero" should have a targetP of 0 MW after PRA
    Then the generator "FR1_fixed" should have a targetP of 200 MW after PRA
    Then the generator "FR1_disconnected" should have a targetP of 300 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of -600 MW after PRA
    Then the value of the objective function after PRA should be 20010.0

  @rao @counter-trading
  Scenario: 7.1.17: GLSK referring to a node absent from the network
    Same case as 7.1.4 with an extra FR node FFR9AA1 that does not exist: it is ignored and the result is the one of 7.1.4.
    Given network file is "counter-trading/2NodesFR1NodeBE.uct"
    Given crac file is "counter-trading/crac-2-nodes-fr-1-node-be.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes_fr_1node_be_unknownNode.xml"
    When I launch rao at "2021-04-30 22:30"
    Then its security status should be "SECURED"
    Then the generator "FFR1AA1 _generator" should have a targetP of 220 MW after PRA
    Then the generator "FFR2AA1 _generator" should have a targetP of 380 MW after PRA
    Then the value of the objective function after PRA should be 20010.0

  @rao @counter-trading
  Scenario: 7.1.18: CT range too small in the needed direction
    The FR range is [-200, 1000]: FR can only go down by 200 MW, the remaining 200 MW overload cannot be solved.
    Given network file is "counter-trading/2Nodes.uct"
    Given crac file is "counter-trading/crac-2-nodes-fr-range-too-small.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes.xml"
    When I launch rao at "2021-04-30 22:30"
    Then its security status should be "UNSECURED"
    Then the worst margin is -200 MW
    Then the generator "FFR1AA1 _generator" should have a targetP of 800 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of -800 MW after PRA
    Then the setpoint of RangeAction "counterTradingActionFR" should be -200 MW in preventive
    Then the setpoint of RangeAction "counterTradingActionBE" should be 200 MW in preventive

  @rao @counter-trading
  Scenario: 7.1.19: CT range relative to the initial network
    Checks that a CT range relative to the initial network is centred on 0: the CT initial setpoint is always 0 when initialNetPosition is not specified.
    The relative range +/-300 therefore behaves as the absolute range [-300, 300]: FR uses its full range and the border stays 100 MW overloaded.
    Given network file is "counter-trading/2Nodes.uct"
    Given crac file is "counter-trading/crac-2-nodes-relative-range.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes.xml"
    When I launch rao at "2021-04-30 22:30"
    Then its security status should be "UNSECURED"
    Then the worst margin is -100 MW
    Then the generator "FFR1AA1 _generator" should have a targetP of 700 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of -700 MW after PRA
    Then the setpoint of RangeAction "counterTradingActionFR" should be -300 MW in preventive
    Then the setpoint of RangeAction "counterTradingActionBE" should be 300 MW in preventive
    Then the value of the objective function after PRA should be 115010.0

  @rao @counter-trading
  Scenario: 7.1.21: CT combined with a topological action
    Closing line 3 (x = 30, cost 100) leaves 3/4 of the exchange on the remaining line after an N-1: the limit becomes 800 MW.
    CT covers the last 200 MW. Cost: 100 + 2 x (5 + 25 x 200) = 10110, cheaper than CT alone (20010).
    Given network file is "counter-trading/2Nodes_openLine3.uct"
    Given crac file is "counter-trading/crac-2-nodes-ct-and-topo.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes.xml"
    When I launch rao at "2021-04-30 22:30"
    Then its security status should be "SECURED"
    Then the remedial action "closeLine3" is used in preventive
    Then the generator "FFR1AA1 _generator" should have a targetP of 800 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of -800 MW after PRA
    Then the setpoint of RangeAction "counterTradingActionFR" should be -200 MW in preventive
    Then the setpoint of RangeAction "counterTradingActionBE" should be 200 MW in preventive
    Then the value of the objective function after PRA should be 10110.0

  @rao @counter-trading
  Scenario: 7.1.22: CT with the MAX_MIN_MARGIN objective
    Costs are ignored: the margin is maximal when no power flows between FR and BE, i.e. CT of 1000 MW (full range).
    Given network file is "counter-trading/2Nodes.uct"
    Given crac file is "counter-trading/crac-2-nodes.json"
    Given configuration file is "counter-trading/rao-parameters-ct-max-min-margin.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes.xml"
    When I launch rao at "2021-04-30 22:30"
    Then its security status should be "SECURED"
    Then the generator "FFR1AA1 _generator" should have a targetP of 0 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of 0 MW after PRA
    Then the setpoint of RangeAction "counterTradingActionFR" should be -1000 MW in preventive
    Then the setpoint of RangeAction "counterTradingActionBE" should be 1000 MW in preventive

  @rao @counter-trading
  Scenario: 7.1.23: Downward saturation of one generator, the two others keep their 2:1 proportion
    Given network file is "counter-trading/2Nodes_FR3Generators_down.xiidm"
    Given crac file is "counter-trading/crac-2-nodes.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    When I launch rao at "2021-04-30 22:30"
    Then the worst margin is 0 MW
    Then the generator "FR1_a" should have a targetP of 340 MW after PRA
    Then the generator "FR1_b" should have a targetP of 170 MW after PRA
    Then the generator "FR1_c" should have a targetP of 90 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of -600 MW after PRA
    Then the setpoint of RangeAction "counterTradingActionFR" should be -400 MW in preventive
    Then the setpoint of RangeAction "counterTradingActionBE" should be 400 MW in preventive
    Then the value of the objective function after PRA should be 20010.0

  @rao @counter-trading
  Scenario: 7.1.24: The whole area saturates downwards before reaching its range
    FR generators can only go down 200 + 80 + 20 = 300 MW while 400 MW are needed: CT is limited by the Pmin, not by the range.
    Given network file is "counter-trading/2Nodes_FR3Generators_down_areaSaturated.xiidm"
    Given crac file is "counter-trading/crac-2-nodes.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    When I launch rao at "2021-04-30 22:30"
    Then its security status should be "UNSECURED"
    Then the worst margin is -100 MW
    Then the generator "FR1_a" should have a targetP of 400 MW after PRA
    Then the generator "FR1_b" should have a targetP of 220 MW after PRA
    Then the generator "FR1_c" should have a targetP of 80 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of -700 MW after PRA
    Then the setpoint of RangeAction "counterTradingActionFR" should be -300 MW in preventive
    Then the setpoint of RangeAction "counterTradingActionBE" should be 300 MW in preventive
    Then the value of the objective function after PRA should be 115010.0

  @rao @counter-trading
  Scenario: 7.1.25: Upward saturation of one generator
    FR consumes 2000 MW and imports 1000 MW from BE: FR must produce 400 MW more.
    FR1_c can only go up 10 MW, the 300 MW left are shared 2:1. FR1_a is at its Pmin, which must not prevent it from going up.
    Cost: 5 + 25 x 400 + 5 + 25 x 400 = 20010.
    Given network file is "counter-trading/2Nodes_FR3Generators_up.xiidm"
    Given crac file is "counter-trading/crac-2-nodes.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    When I launch rao at "2021-04-30 22:30"
    Then the worst margin is 0 MW
    Then the generator "FR1_a" should have a targetP of 860 MW after PRA
    Then the generator "FR1_b" should have a targetP of 430 MW after PRA
    Then the generator "FR1_c" should have a targetP of 110 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of 600 MW after PRA
    Then the setpoint of RangeAction "counterTradingActionFR" should be 400 MW in preventive
    Then the setpoint of RangeAction "counterTradingActionBE" should be -400 MW in preventive
    Then the value of the objective function after PRA should be 20010.0

  @rao @counter-trading
  Scenario: 7.1.26: The cheapest counterpart saturates at its Pmin before reaching its range, mirror of 7.1.13
    Exchanges of 3Nodes.uct are reversed: FR produces 1800 MW, BE imports 1000 MW and ES 800 MW. BE must produce 400 MW more.
    ES is the cheapest counterpart (range +/-100 MW) but its Pmin = 750 MW: only 50 MW can be used, FR takes the 350 MW left.
    Cost: BE 5 + 50 x 400, ES 5 + 10 x 50, FR 10 + 50 x 350 = 38020.
    Given network file is "counter-trading/3Nodes_mirror_ESPmin750.uct"
    Given crac file is "counter-trading/crac-3-nodes-es-cheap-limited.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_3nodes.xml"
    When I launch rao at "2021-04-30 22:30"
    Then the worst margin is 0 MW
    Then the generator "FFR1AA1 _generator" should have a targetP of 1450 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of 1400 MW after PRA
    Then the generator "EES1AA1 _generator" should have a targetP of 750 MW after PRA
    Then the setpoint of RangeAction "counterTradingAction_BE" should be 400 MW in preventive
    Then the setpoint of RangeAction "counterTradingAction_ES" should be -50 MW in preventive
    Then the setpoint of RangeAction "counterTradingAction_FR" should be -350 MW in preventive
    Then the value of the objective function after PRA should be 38020.0
