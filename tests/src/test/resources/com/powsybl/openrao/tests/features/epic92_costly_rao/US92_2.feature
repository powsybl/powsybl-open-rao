# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 92.2: Costly range actions optimization - APPROXIMATED_INTEGERS PSTs

  @fast @preventive-only @costly @rao
  Scenario: US 92.2.1: Change only necessary taps on preventive PST
  The RAO can increase the minimum margin by setting the tap of the PST on position -10
  but stops at position -5 because the network is secure and this saves expenses.
    Given network file is "epic92/2Nodes2ParallelLinesPST.uct"
    Given crac file is "epic92/crac-92-2-1.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst.json"
    When I launch search_tree_rao
    Then the worst margin is 45.46 MW
    And the value of the objective function initially should be 2000000.0
    And 1 remedial actions are used in preventive
    And the remedial action "pstBeFr2" is used in preventive
    And the tap of PstRangeAction "pstBeFr2" should be -5 in preventive
    And the value of the objective function after PRA should be 55.0

  @fast @preventive-only @costly @rao
  Scenario: US 92.2.2: Two PSTs
  PST 1 has cheaper variation costs (5 per tap) but a higher activation price (100) so moving the 9 required taps would
  require a cost of 145. PST 2 is cheaper to activate (5) and more expensive to use (15 per tap) but leads to a total
  cost of 140 so it is chosen.
    Given network file is "epic92/2Nodes3ParallelLines2PSTs.uct"
    Given crac file is "epic92/crac-92-2-2.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst.json"
    When I launch search_tree_rao
    Then the worst margin is 31.15 MW
    And the value of the objective function initially should be 2633333.33
    And 1 remedial actions are used in preventive
    And the remedial action "pstBeFr3" is used in preventive
    And the tap of PstRangeAction "pstBeFr2" should be 0 in preventive
    And the tap of PstRangeAction "pstBeFr3" should be -9 in preventive
    And the value of the objective function after PRA should be 140.0

  @fast @costly @rao
  Scenario: US 92.2.3: Costly PST in preventive and curative
    Given network file is "epic92/2Nodes3ParallelLinesPST.uct"
    Given crac file is "epic92/crac-92-2-3.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst.json"
    When I launch search_tree_rao
    Then the worst margin is 11.73 MW
    And the value of the objective function initially should be 4300000.0
    And 1 remedial actions are used in preventive
    And the remedial action "pstBeFr3" is used in preventive
    And the tap of PstRangeAction "pstBeFr3" should be -3 in preventive
    # Activation of pstBeFr3 (20) + 3 taps moved (3 * 7.5) + overload penalty (282.71 * 10000)
    And the value of the objective function after PRA should be 2827226.08
    And 1 remedial actions are used after "coBeFr2" at "curative"
    And the tap of PstRangeAction "pstBeFr3" should be -9 after "coBeFr2" at "curative"
    # Activation of pstBeFr3 twice (2 * 20) + 9 taps moved in total (3 * 7.5 + 6 * 7.5)
    And the value of the objective function after CRA should be 107.5

  @fast @costly @rao
  Scenario: US 92.2.4: Free PST in preventive and curative
    Given network file is "epic92/2Nodes3ParallelLinesPST.uct"
    Given crac file is "epic92/crac-92-2-4.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst.json"
    When I launch search_tree_rao
    Then the worst margin is 11.73 MW
    And the value of the objective function initially should be 4300000.0
    And 1 remedial actions are used in preventive
    And the remedial action "pstBeFr3" is used in preventive
    And the tap of PstRangeAction "pstBeFr3" should be -3 in preventive
    # Overload penalty (282.71 * 10000)
    And the value of the objective function after PRA should be 2827171.08
    And 1 remedial actions are used after "coBeFr2" at "curative"
    And the tap of PstRangeAction "pstBeFr3" should be -9 after "coBeFr2" at "curative"
    And the value of the objective function after CRA should be 0

  @fast @costly @rao @second-preventive
  Scenario: US 92.2.5: PST in 2nd preventive optimization
  PST is moved to tap -9 straight from preventive optimization to cut curative activation costs.
    Given network file is "epic92/2Nodes3ParallelLinesPST.uct"
    Given crac file is "epic92/crac-92-2-3.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst_2P.json"
    When I launch search_tree_rao
    Then the worst margin is 11.73 MW
    And the value of the objective function initially should be 4300000.0
    And 1 remedial actions are used in preventive
    And the remedial action "pstBeFr3" is used in preventive
    And the tap of PstRangeAction "pstBeFr3" should be -9 in preventive
    # Activation of pstBeFr3 (20) + 9 taps moved (9 * 7.5)
    And the value of the objective function after PRA should be 87.5
    And 0 remedial actions are used after "coBeFr2" at "curative"
    And the value of the objective function after CRA should be 87.5

  @fast @costly @rao @multi-curative
  Scenario: US 92.2.6: Multi-curative costly optimization
  The same PST is moved in preventive optimization and at all curative states
    Given network file is "epic92/2Nodes3ParallelLinesPST.uct"
    Given crac file is "epic92/crac-92-2-6.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst.json"
    When I launch search_tree_rao
    Then the worst margin is 32.13 MW
    And the value of the objective function initially should be 4500000.0
    And 1 remedial actions are used in preventive
    And the remedial action "pstBeFr3" is used in preventive
    And the tap of PstRangeAction "pstBeFr3" should be -2 in preventive
    # Activation of pstBeFr3 (20) + 2 taps moved (2 * 5.0)
    And the value of the objective function after PRA should be 3518125.13
    And 1 remedial actions are used after "coBeFr2" at "curative-1"
    And the remedial action "pstBeFr3" is used after "coBeFr2" at "curative-1"
    And the tap of PstRangeAction "pstBeFr3" should be -7 after "coBeFr2" at "curative-1"
    And 1 remedial actions are used after "coBeFr2" at "curative-2"
    And the remedial action "pstBeFr3" is used after "coBeFr2" at "curative-2"
    And the tap of PstRangeAction "pstBeFr3" should be -9 after "coBeFr2" at "curative-2"
    And 1 remedial actions are used after "coBeFr2" at "curative-3"
    And the remedial action "pstBeFr3" is used after "coBeFr2" at "curative-3"
    And the tap of PstRangeAction "pstBeFr3" should be -10 after "coBeFr2" at "curative-3"
    # Activation of pstBeFr3 4 times (4 * 20) + 10 taps moved (10 * 5.0)
    And the value of the objective function after CRA should be 130.0

  @fast @costly @rao @second-preventive @multi-curative
  Scenario: US 92.2.6: Multi-curative costly optimization with 2P
  Same case as US 92.2.6 but with second preventive optimization.
  The PST is moved to tap -10 straight from preventive optimization to cut activation expenses.
    Given network file is "epic92/2Nodes3ParallelLinesPST.uct"
    Given crac file is "epic92/crac-92-2-6.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst_2P.json"
    When I launch search_tree_rao
    Then the worst margin is 40.77 MW
    And the value of the objective function initially should be 4500000.0
    And 1 remedial actions are used in preventive
    And the remedial action "pstBeFr3" is used in preventive
    And the tap of PstRangeAction "pstBeFr3" should be -10 in preventive
    # Activation of pstBeFr3 (20) + 10 taps moved (10 * 5.0)
    And the value of the objective function after PRA should be 70.0
    And 0 remedial actions are used after "coBeFr2" at "curative-1"
    And 0 remedial actions are used after "coBeFr2" at "curative-2"
    And 0 remedial actions are used after "coBeFr2" at "curative-3"
    # Activation of pstBeFr3 (20) + 10 taps moved (10 * 5.0)
    And the value of the objective function after CRA should be 70.0