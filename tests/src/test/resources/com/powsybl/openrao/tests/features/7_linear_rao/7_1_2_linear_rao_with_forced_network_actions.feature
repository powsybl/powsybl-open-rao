# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 7.1.2: Linear RAO on existing test cases with preventive network actions
  This feature gathers the existing test cases whose CRACs contain preventive network actions (and no curative
  nor automaton remedial action), run with the linear RAO. The linear RAO does not optimize network actions:
  the ones activated by the search tree RAO in the original test are forced through the "forced-network-actions"
  parameter of the "loutre-parameters" extension, and are therefore applied before the initial sensitivity analysis.
  The assertions on the initial situation are removed from the scenarios where network actions are forced.

  @fast @rao @ac @multi-curative @secure-flow @linear-rao
  Scenario: 7.1.2.1: Multi-curative without CRAs (from 1.5.1.11)
    Given network file is "epic91/TestCase16Nodes_multi_curative.uct"
    Given crac file is "epic91/crac_91_12_noCRA.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_secure_loutre_1_5_1_11.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 50.64 A
    # Basecase / After PRA (PATL 300 MW)
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_CLOSE_BE1_BE2_1" is used after "Contingency DE2 NL3 1" at "preventive"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after PRA should be -228.0 MW on side 1
    # Outage (TATL 1000 MW)
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -667.0 MW on side 1
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after PRA should be -667.0 MW on side 1
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after PRA should be -667.0 MW on side 1
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after PRA should be -667.0 MW on side 1
    # After first curative (TATL 800 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative1"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "curative1" instant remedial actions should be -667.0 MW on side 1
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative1" instant remedial actions should be -667.0 MW on side 1
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative1" instant remedial actions should be -667.0 MW on side 1
    # After second curative (TATL 750 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative2"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative2" instant remedial actions should be -667.0 MW on side 1
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative2" instant remedial actions should be -667.0 MW on side 1
    # After third curative (TATL 700 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative3"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative3" instant remedial actions should be -667.0 MW on side 1

  @fast @rao @ac @multi-curative @secure-flow @linear-rao
  Scenario: 7.1.2.2: Multi-curative CNECs with PRAs only (from 1.5.1.18)
    This is a copy of 1.5.1.2 but CRAs are transformed into PRAs
    Curative CNECs are now part of the preventive perimeter
    -> Then the 3 RAs should be applied in preventive in order to solve curative constraints
    #
    Given network file is "1_multi_step_optimisation/1_5_multi_curative/12Nodes3ParallelLines_disconnected.uct"
    Given crac file is "epic91/crac_91_12_16.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_secure_loutre_1_5_1_18.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 107.26 A
    Then 3 remedial actions are used in preventive
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be -11 in preventive
    Then the remedial action "PRA_CLOSE_NL2_BE3_2" is used in preventive
    Then the remedial action "PRA_CLOSE_NL2_BE3_3" is used in preventive
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 654.8 MW
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - Contingency DE2 DE3 1 - curative1" after PRA should be 320.6 MW
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - Contingency DE2 DE3 1 - curative2" after PRA should be 120.6 MW
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - Contingency DE2 DE3 1 - curative3" after PRA should be 70.6 MW

  @fast @rao @ac @max-min-margin @linear-rao
  Scenario: 7.1.2.3: Simple case with two CNECs and 1 network action that create an island - island creation allowed (from 2.1.6.1)
    We have here a simple case where
    - one CNEC "DDE1AA1  DDE2AA1  1 - preventive" is overloaded
    - one CNEC "NNL2AA1  NNL3AA1  1 - preventive" that is not overloaded and will not be in the electrical island.
    - opening the line "DDE2AA1  NNL3AA1  1" resolves the overload by creating an island (DDE1AA1, DDE2AA1 & DDE3AA1)
  -> the flow is considered equal to 0 A on the CNEC "DDE1AA1  DDE2AA1  1 - preventive"
    Note: we had to add at least one CNEC not in the island in the CRAC (NNL2AA1  NNL3AA1  1 - preventive) to not get a sensitivity computation error.
    Given network file is "2_remedial_actions/2_1_network_actions_optimisation/network_2_1_6_1.uct"
    Given crac file is "2_remedial_actions/2_1_network_actions_optimisation/crac_2_1_6_1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_ac_loutre_2_1_6_1.json"
    When I launch linear rao
    Then the remedial action "open_DDE2AA1  NNL3AA1  1" is used in preventive
    Then the margin on cnec "DDE1AA1  DDE2AA1  1 - preventive" after PRA should be 1000 A
    Then the flow on cnec "DDE1AA1  DDE2AA1  1 - preventive" after PRA should be 0 A on side 1

  @fast @rao @ac @max-min-margin @linear-rao
  Scenario: 7.1.2.4: Simple case with two CNECs and 1 network action that create an island - island creation not allowed (from 2.1.6.1.bis)
    Same test as 2.1.6.1, but island creation is not allowed
    Given network file is "2_remedial_actions/2_1_network_actions_optimisation/network_2_1_6_1.uct"
    Given crac file is "2_remedial_actions/2_1_network_actions_optimisation/crac_2_1_6_1.json"
    Given configuration file is "2_remedial_actions/2_1_network_actions_optimisation/RaoParameters_maxMargin_ampere_ac_island_creation_not_allowed.json"
    When I launch linear rao
    Then the initial flow on cnec "DDE1AA1  DDE2AA1  1 - preventive" should be -1203 A on side 1
    Then 0 remedial actions are used in preventive

  @fast @rao @ac @max-min-margin @linear-rao
  Scenario: 7.1.2.5: Simple case with one CNEC and 1 network action that creates an island (from 2.1.6.2)
    Same case as 2.1.6.1, but only the CNEC "DDE1AA1  DDE2AA1  1 - preventive" is defined in the CRAC.
    The network action won't be applied, the sensitivity result status is set to FAILED because
    the status is set to SUCCESS only if at least one CNEC has a flow that is not NaN after the sensi computation.
    (If the flow or sensi value is a NaN after OLF sensi computation -> it is set to 0)
    Given network file is "2_remedial_actions/2_1_network_actions_optimisation/network_2_1_6_1.uct"
    Given crac file is "2_remedial_actions/2_1_network_actions_optimisation/crac_2_1_6_2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_ac.json"
    When I launch linear rao
    Then the initial flow on cnec "DDE1AA1  DDE2AA1  1 - preventive" should be -1203 A on side 1
    Then 0 remedial actions are used in preventive

  @fast @rao @ac @max-min-margin @linear-rao
  Scenario: 7.1.2.6: An island is created but all the production is in this island - island creation allowed (from 2.1.6.4)
    Same network architecture as as 2.1.6.1 BUT all the production is in the island that is created by the RAO
    which makes the sensi computation fail (failed to distribute slack) -> the action is not used.
    If  "slackDistributionFailureBehavior" is set to "THROW"
    Given network file is "2_remedial_actions/2_1_network_actions_optimisation/network_2_1_6_4.uct"
    Given crac file is "2_remedial_actions/2_1_network_actions_optimisation/crac_2_1_6_1.json"
    Given configuration file is "2_remedial_actions/2_1_network_actions_optimisation/raoParameters_2_1_6_2_4.json"
    When I launch linear rao
    Then the initial margin on cnec "DDE1AA1  DDE2AA1  1 - preventive" should be -1890.53 A
    Then 0 remedial actions are used in preventive
    Then the margin on cnec "DDE1AA1  DDE2AA1  1 - preventive" after PRA should be -1890.53 A

  @fast @rao @preventive-only @max-min-margin @linear-rao
  Scenario: 7.1.2.7: Select less efficient network action because it has less elementary actions (from 2.6.4.3)
    Given network file is "epic19/small-network-2P-open-twin-lines.uct"
    Given crac file is "epic19/small-crac-with-max-1-elementary-action-topo.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete_loutre_2_6_4_3.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    # Best theoretical option is to close both line BE1-BE3-1 and line BE1-BE3-2 (-561 MW)
    # With only three elementary actions, the best option is to simply close line BE1-BE3-1 (-572 MW)
    Then 1 remedial actions are used in preventive
    Then the remedial action "close_be1_be3_1" is used in preventive
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after PRA should be -572.0 MW on side 1
    Then the worst margin is 3.0 MW

  @fast @rao @preventive-only @max-min-margin @linear-rao
  Scenario: 7.1.2.8: Limit elementary actions with topos and PSTs (from 2.6.4.5)
    Given network file is "epic19/small-network-2P-open-twin-lines.uct"
    Given crac file is "epic19/small-crac-with-max-3-elementary-actions-topo-and-pst.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete_loutre_2_6_4_5.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    # Best theoretical option is to close both lines and move the PST to tap -16
    # With only three elementary actions, the best three possibilities are:
    # - move the PST to tap -3 (-520 MW)
    # - close line BE1-BE3-1 and move the PST to tap -2 (-511 MW -> best option)
    # - close both line BE1-BE3-1 and line BE1-BE3-2 and move the PST to tap -1 (-528 MW)
    Then 2 remedial actions are used in preventive
    Then the remedial action "close_be1_be3_1" is used in preventive
    Then the remedial action "pst_be" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be -2 in preventive
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after PRA should be -511.0 MW on side 1
    Then the worst margin is 39.0 MW

  @fast @preventive-only @costly @rao @linear-rao
  Scenario: 7.1.2.9: Selection of cheapest of 3 equivalent network actions (from 3.4.1.1)
  The network contains two nodes linked with 4 parallel lines: one of the lines is the overloaded CNEC,
  and the three other are open. 3 remedial actions with different costs: close one of the three lines.
    Given network file is "epic92/2Nodes4ParallelLines.uct"
    Given crac file is "epic92/crac-92-1-1.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_loutre_3_4_1_1.json"
    When I launch linear rao
    Then the worst margin is 250.0 MW
    Then its security status should be "SECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "closeBeFr4" is used in preventive
    # Overload penalty (250 * 1000)
    # Activation of closeBeFr4 (10)
    Then the value of the objective function after PRA should be 10.0

  @fast @preventive-only @costly @rao @linear-rao
  Scenario: 7.1.2.10: Selection of cheapest network action even if it does not maximize minimum margin (from 3.4.1.2)
  Line BE-FR-3 has a higher resistance than line BE-FR-2 which means that closing the latter will lead
  to a higher margin on the optimized CNEC. However, closing line BE-FR-3 is cheaper and still secures
  the CNEC so it will be chosen by the RAO.
    Given network file is "epic92/2Nodes3ParallelLines_disconnected.uct"
    Given crac file is "epic92/crac-92-1-2.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_loutre_3_4_1_2.json"
    When I launch linear rao
    Then the worst margin is 83.0 MW
    Then its security status should be "SECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "closeBeFr3" is used in preventive
    Then the value of the objective function after PRA should be 25.0

  @fast @preventive-only @max-min-margin @rao @linear-rao
  Scenario: 7.1.2.11: Duplicate of 3.4.1.2 in MAX_MIN_MARGIN mode (from 3.4.1.2.bis)
  The situation is the same as in 3.4.1.2 but the RAO maximizes the minimum margin.
  As activation costs are not taken in account, both lines will be closed.
    Given network file is "epic92/2Nodes3ParallelLines_disconnected.uct"
    Given crac file is "epic92/crac-92-1-2.json"
    Given configuration file is "epic92/RaoParameters_margin_dc_minObjective_loutre_3_4_1_2_bis.json"
    When I launch linear rao
    Then the worst margin is 464.29 MW
    Then the margin on cnec "cnecBeFrPreventive" after PRA should be 464.29 MW
    Then its security status should be "SECURED"
    Then 2 remedial actions are used in preventive
    Then the remedial action "closeBeFr2" is used in preventive
    Then the remedial action "closeBeFr3" is used in preventive
    # In MIN_MAX_MARGIN, the objective function is the opposite of the worst margin.
    Then the value of the objective function after PRA should be -464.29

  @fast @preventive-only @costly @rao @linear-rao
  Scenario: 7.1.2.12: Selection of the two cheapest network actions (from 3.4.1.3)
  Same as 3.4.1.1, but two network actions are needed to secure the CNEC.
    Given network file is "epic92/2Nodes4ParallelLines.uct"
    Given crac file is "epic92/crac-92-1-3.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_loutre_3_4_1_3.json"
    When I launch linear rao
    Then the worst margin is 66.67 MW
    Then its security status should be "SECURED"
    Then 2 remedial actions are used in preventive
    Then the remedial action "closeBeFr3" is used in preventive
    Then the remedial action "closeBeFr4" is used in preventive
    # Overload penalty (600 * 1000)
    # Activation of closeBeFr3 (500) + activation of closeBeFr4 (220)
    Then the value of the objective function after PRA should be 720.0

  @fast @preventive-only @costly @rao @linear-rao
  Scenario: 7.1.2.13: Selection of cheapest of 3 equivalent network actions but overload remains at the end of RAO (from 3.4.1.4)
  Only one network action can be used (behavior set in the RAO parameters) so the RAO chooses the cheapest
  remedial action available to reduce the overload and thus the penalty cost. The total cost is:
  100 (overload in MW) * 10000 (penalty cost in currency/MW) + 220 (cost of the chosen remedial action)
    Given network file is "epic92/2Nodes4ParallelLines.uct"
    Given crac file is "epic92/crac-92-1-3.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_maxDepth1_loutre_3_4_1_4.json"
    When I launch linear rao
    Then the worst margin is -100.0 MW
    Then its security status should be "UNSECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "closeBeFr4" is used in preventive
    Then the value of the objective function after PRA should be 100220.0

  @fast @preventive-only @costly @rao @linear-rao
  Scenario: 7.1.2.14: Sub-optimal case (from 3.4.1.5)
  Closing line BE-FR-2 costs 1000 but solves the constraint immediately. As closing BE-FR-2 looks optimal
  at depth 1, the greedy search-tree keeps it at depth 2 but there is no need to apply additional remedial
  actions since the network is already secure.
  However, the optimal case is to close lines BE-FR-3 and BE-FR-4 successively (the order does not matter)
  for a total expense of 50. Yet, because of the penalty cost for overloads, the RAO still counts an over-cost
  of 100000 because closing BE-FR-3 or BE-FR-4 alone only reduce the minimum margin to -100 MW.
    #TODO: I reordered this text, but the last sentence above is still not clear to me
    Given network file is "epic92/2Nodes4ParallelLinesDifferentResistances.uct"
    Given crac file is "epic92/crac-92-1-5.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_loutre_3_4_1_5.json"
    When I launch linear rao
    Then the worst margin is 66.67 MW
    Then its security status should be "SECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "closeBeFr2" is used in preventive
    Then the value of the objective function after PRA should be 1000.0

  @fast @preventive-only @costly @rao @linear-rao
  Scenario: 7.1.2.15: Activate one topological action and one PST in preventive (from 3.4.3.1)
  Two ways to secure the network:
  1. move PST to tap -5 => cost of 25
  2. (optimal) close line BE-FR 2 and move PST to tap -2 => cost of 20
    Given network file is "epic92/2Nodes3ParallelLinesPST2LinesClosed.uct"
    Given crac file is "epic92/crac-92-3-1.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst_loutre_3_4_3_1.json"
    When I launch linear rao
    Then the worst margin is 32.13 MW
    Then its security status should be "SECURED"
    Then 2 remedial actions are used in preventive
    Then the remedial action "pstBeFr3" is used in preventive
    Then the tap of PstRangeAction "pstBeFr3" should be -2 in preventive
    Then the remedial action "closeBeFr2" is used in preventive
    Then the value of the objective function after PRA should be 20.0
