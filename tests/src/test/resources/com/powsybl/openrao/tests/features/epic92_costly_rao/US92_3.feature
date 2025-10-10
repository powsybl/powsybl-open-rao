# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.


Feature: US 92.3: Exhaustive costly optimization - APPROXIMATED_INTEGERS PSTs

  @fast @preventive-only @costly @rao
  Scenario: US 92.3.1: Activate one topological action and one PST in preventive
  Two ways to secure the network:
  1. move PST to tap -5 => cost of 25
  2. (optimal) close line BE-FR 2 and move PST to tap -2 => cost of 20
    Given network file is "epic92/2Nodes3ParallelLinesPST2LinesClosed.uct"
    Given crac file is "epic92/crac-92-3-1.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst.json"
    When I launch rao
    Then the worst margin is 32.13 MW
    And the value of the objective function initially should be 200000.0
    And 2 remedial actions are used in preventive
    And the remedial action "pstBeFr3" is used in preventive
    And the tap of PstRangeAction "pstBeFr3" should be -2 in preventive
    And the remedial action "closeBeFr2" is used in preventive
    And the value of the objective function after PRA should be 20.0

  @fast @costly @rao
  Scenario: US 92.3.2: Preventive and curative PST + curative topological action
  The PST is moved to tap -5 to secure the preventive perimeter for a total cost of 95
  (20 for activation + 5 * 15 for variation). Then, there are two ways to secure the curative perimeter:
  1. move PST to tap -8 => cost of 20 + 3 * 15 = 65
  2. (optimal) close line BE-FR 3 and move PST to tap -6 => cost of 10 + 20 + 1 * 15 = 45
    Given network file is "epic92/2Nodes4ParallelLinesPST3LinesClosed.uct"
    Given crac file is "epic92/crac-92-3-2.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst.json"
    When I launch rao
    # Worst margin on preventive CNEC
    Then the worst margin is 5.3 MW
    And the value of the objective function initially should be 350000.0
    And 1 remedial actions are used in preventive
    And the remedial action "pstBeFr4" is used in preventive
    And the tap of PstRangeAction "pstBeFr4" should be -5 in preventive
    # Activation of pstBeFr4 (20) + 5 taps moved (5 * 15) + overload penalty (104.54 * 1000)
    And the value of the objective function after PRA should be 104638.64
    And 2 remedial actions are used after "coBeFr2" at "curative"
    And the remedial action "pstBeFr4" is used after "coBeFr2" at "curative"
    And the tap of PstRangeAction "pstBeFr4" should be -6 after "coBeFr2" at "curative"
    And the remedial action "closeBeFr3" is used after "coBeFr2" at "curative"
    # activation of closeBeFr3 (10)
    And the value of the objective function after CRA should be 140

  @fast @costly @rao @second-preventive
  Scenario: US 92.3.3: Preventive and curative PST + curative topological action with 2nd preventive optimization
  The PST is moved to tap -6 straight from preventive to only activate the remedial action once.
    Given network file is "epic92/2Nodes4ParallelLinesPST3LinesClosed.uct"
    Given crac file is "epic92/crac-92-3-2.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst_2P.json"
    When I launch rao
    # Worst margin on curative CNEC
    Then the worst margin is 13.02 MW
    And the value of the objective function initially should be 350000.0
    And 1 remedial actions are used in preventive
    And the remedial action "pstBeFr4" is used in preventive
    And the tap of PstRangeAction "pstBeFr4" should be -6 in preventive
    # Activation of pstBeFr4 (20) + 6 taps moved (6 * 15) + overload penalty (55.46 * 10000)
    And the value of the objective function after PRA should be 55574.85
    And 1 remedial actions are used after "coBeFr2" at "curative"
    And the remedial action "closeBeFr3" is used after "coBeFr2" at "curative"
    # Activation of pstBeFr4 (20) + 6 taps moved in total (6 * 15) + activation of closeBeFr3 (10)
    And the value of the objective function after CRA should be 120.0

  @fast @costly @rao
  Scenario: US 92.3.4: Preventive and curative PST + curative topological action - 2 scenarios
    Given network file is "epic92/2Nodes5ParallelLinesPST4LinesClosed.uct"
    Given crac file is "epic92/crac-92-3-4.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst.json"
    When I launch rao
    # Worst margin on preventive CNEC
    Then the worst margin is 3.73 MW
    And the value of the objective function initially should be 233333.33
    And 1 remedial actions are used in preventive
    And the remedial action "pstBeFr5" is used in preventive
    And the tap of PstRangeAction "pstBeFr5" should be -5 in preventive
    # Activation of pstBeFr4 (20) + 5 taps moved (5 * 15) + overload penalty (69.7 * 1000)
    And the value of the objective function after PRA should be 69790.76
    And 2 remedial actions are used after "coBeFr2" at "curative"
    And the remedial action "pstBeFr5" is used after "coBeFr2" at "curative"
    And the tap of PstRangeAction "pstBeFr5" should be -6 after "coBeFr2" at "curative"
    And the remedial action "closeBeFr4" is used after "coBeFr2" at "curative"
    And 2 remedial actions are used after "coBeFr3" at "curative"
    And the remedial action "pstBeFr5" is used after "coBeFr3" at "curative"
    And the tap of PstRangeAction "pstBeFr5" should be -7 after "coBeFr3" at "curative"
    And the remedial action "closeBeFr4" is used after "coBeFr3" at "curative"
    # Activation of pstBeFr4 three times (3 * 20) + 8 taps moved in total (8 * 15) + activation of closeBeFr3 twice (2 * 10)
    And the value of the objective function after CRA should be 200.0

  @fast @costly @rao @second-preventive
  Scenario: US 92.3.5: Preventive and curative PST + curative topological action - 2 scenarios with 2nd preventive optimization
  To cut activation cost expenses, the PST is moved to tap -7 straight from preventive optimization.
    Given network file is "epic92/2Nodes5ParallelLinesPST4LinesClosed.uct"
    Given crac file is "epic92/crac-92-3-4.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst_2P.json"
    When I launch rao
    # Worst margin on curative CNEC
    Then the worst margin is 21.8 MW
    And the value of the objective function initially should be 233333.33
    And 1 remedial actions are used in preventive
    And the remedial action "pstBeFr5" is used in preventive
    And the tap of PstRangeAction "pstBeFr5" should be -7 in preventive
    # Activation of pstBeFr4 (20) + 5 taps moved (7 * 15) + overload penalty (4.26 * 10000)
    And the value of the objective function after PRA should be 4386.91
    And 1 remedial actions are used after "coBeFr2" at "curative"
    And the remedial action "closeBeFr4" is used after "coBeFr2" at "curative"
    And 1 remedial actions are used after "coBeFr3" at "curative"
    And the remedial action "closeBeFr4" is used after "coBeFr3" at "curative"
    # Activation of pstBeFr4 (20) + 7 taps moved in total (7 * 15) + activation of closeBeFr3 twice (2 * 10)
    And the value of the objective function after CRA should be 145.0