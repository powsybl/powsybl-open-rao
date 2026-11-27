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
    Covers: reference case with a redispatching (injection) remedial action instead of CT, on 2 nodes.
    Situation: one overloaded border, a redispatching action available on an FR generator, preventive only.
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
    Covers: CT actions on 2 areas (FR, BE), each with a single node, proportional GLSK.
    Situation: the same overload as 7.1.1, now solved by counter trading instead of redispatch.
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
    Covers: CT actions on 3 areas, each with a single node, proportional GLSK.
    Situation: the same overload as 7.1.2, now balanced by three counterparts instead of two.
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
    Covers: CT on an area with two generators (FFR1, FFR2) and a second area with one node (BE).
    Situation: the same 400 MW exchange as 7.1.2, but FR's reduction must be split between two generators.
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
    Covers: same as 7.1.4, with equal GLSK weights on the two FR generators.
    Situation: FFR1 and FFR2 have the same weight, so the FR setpoint should be split evenly.
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
    Covers: same as 7.1.4, with the GLSK weights taken from the generators' initial targetP (different sizes).
    Situation: FFR1 is larger than FFR2, so it must carry a larger share of the FR setpoint.
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
    Covers: saturation of one FR generator at its lower limit, with the GLSK split of 7.1.4.
    Situation: FFR2 would have to go down to 0 MW or below to follow its GLSK share, which is not allowed.
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
    Covers: saturation of FFR2 at a non-zero limit (250 MW), with the same GLSK split as 7.1.6.
    Situation: FFR2 cannot go below 250 MW, so its proportional share cannot be fully applied.
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
    Covers: a CT action defined on FR only, with no counterpart on BE.
    Situation: the 2-node overload of 7.1.2, with only FR having a CT action (1 iteration).
    Given network file is "counter-trading/2Nodes.uct"
    Given crac file is "counter-trading/crac-2-nodes-fr-only.json"
    Given configuration file is "counter-trading/rao-parameters-ct-1-iteration.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes.xml"
    When I launch rao at "2021-04-30 22:30"
    Then the remedial action "counterTradingActionFR" is not used in preventive
    Then its security status should be "UNSECURED"

  @rao @counter-trading
  Scenario: 7.1.10: Two areas exchange through a third area which has no CT action
    Covers: exchange between BE and ES on 3 areas, where FR (in between) has no CT action.
    Situation: 400 MW must come from ES to BE, but the path would cross FR.
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
    Covers: 3 areas where ES has no CT action; FR is the only counterpart of BE.
    Situation: BE needs 400 MW of counter trading, ES keeps producing its initial 800 MW.
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
    Covers: merit order between counterparts: the cheapest one is limited by its range.
    Situation: BE needs 400 MW; ES costs 10/MW but its range is +/-100 MW; FR costs 50/MW with a wider range.
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
    Covers: same as 7.1.12, but the cheapest counterpart's generator is limited by its Pmax.
    Situation: ES Pmax is 850 MW, so ES can add only 50 MW of its 100 MW range.
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
    Covers: 3 areas with an extra BE-ES line (x = 25), so the border exchanges form a loop.
    Situation: after an N-1, the remaining FR-BE line carries 3/4 of BE's production and 1/8 of ES's, symmetrically for FR-ES.
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
    Covers: GLSK split on FR with four special generators, one per edge case.
    Situation: FR must produce 400 MW less. FR1_disconnected is left out of the GLSK; FR1_zero has targetP 0; FR1_fixed has Pmin = Pmax.
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
    Covers: a GLSK file that lists a node (FFR9AA1) which does not exist in the network.
    Situation: same case as 7.1.4 with the extra unknown node in the GLSK.
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
    Covers: a CT range that is too narrow in the direction needed to relieve the overload.
    Situation: 400 MW are needed, but FR's range is [-200, 1000], so FR can only go down by 200 MW.
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
    Covers: a CT range defined relative to the initial network, with no initialNetPosition given.
    Situation: the relative range is +/-300 MW, and the overload needs 400 MW.
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
    Covers: a topological remedial action (closing line 3, cost 100) combined with CT.
    Situation: closing line 3 leaves 3/4 of the exchange on the remaining line after an N-1, so the limit becomes 800 MW.
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
    Covers: CT under the MAX_MIN_MARGIN objective, where costs are ignored.
    Situation: the 2-node overload of 7.1.2, with the margin as the only criterion.
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
    Covers: downward saturation of one FR generator among three, with a 2:1 ratio between the two others.
    Situation: FR must go down by 400 MW; one of the three generators hits its lower limit.
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
    Covers: the whole FR area saturating downwards before the CT range is reached.
    Situation: FR generators can only go down by 200 + 80 + 20 = 300 MW, while 400 MW are needed.
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
    Covers: upward saturation of one FR generator, and a generator at its Pmin that must still be able to go up.
    Situation: FR imports 1000 MW from BE and consumes 2000 MW, so FR must produce 400 MW more. FR1_c can only go up by 10 MW; FR1_a is at its Pmin.
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
    Covers: mirror of 7.1.13 with reversed flows: the cheapest counterpart is limited by its Pmin.
    Situation: FR produces 1800 MW, BE imports 1000 MW and ES 800 MW; BE must produce 400 MW more. ES is the cheapest (range +/-100 MW) but its Pmin is 750 MW.
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
