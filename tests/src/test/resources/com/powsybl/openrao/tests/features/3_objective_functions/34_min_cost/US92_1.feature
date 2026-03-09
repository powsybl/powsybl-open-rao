# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 92.1: Costly network actions optimization
  This feature covers the objective-function type MIN_COST with network actions.

  @fast @preventive-only @costly @rao @megawatt
  Scenario: US 92.1.1: Selection of cheapest of 3 equivalent network actions
  The network contains two nodes linked with 4 parallel lines: one of the lines is the overloaded CNEC,
  and the three other are open. 3 remedial actions with different costs: close one of the three lines.
    Given network file is "epic92/2Nodes4ParallelLines.uct"
    Given crac file is "epic92/crac-92-1-1.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective.json"
    When I launch rao
    Then the worst margin is 250.0 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "closeBeFr4" is used in preventive
    # Overload penalty (250 * 1000)
    Then the value of the objective function initially should be 250000.0
    # Activation of closeBeFr4 (10)
    Then the value of the objective function after PRA should be 10.0

  @fast @preventive-only @costly @rao @megawatt
  Scenario: US 92.1.2: Selection of cheapest network action even if it does not maximize minimum margin
  Line BE-FR-3 has a higher resistance than line BE-FR-2 which means that closing the latter will lead
  to a higher margin on the optimized CNEC. However, closing line BE-FR-3 is cheaper and still secures
  the CNEC so it will be chosen by the RAO.
    Given network file is "epic92/2Nodes3ParallelLines_disconnected.uct"
    Given crac file is "epic92/crac-92-1-2.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective.json"
    When I launch rao
    Then the worst margin is 83.0 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "closeBeFr3" is used in preventive
    Then the value of the objective function initially should be 250000.0
    Then the value of the objective function after PRA should be 25.0

  @fast @preventive-only @max-min-margin @rao @megawatt
  Scenario: US 92.1.2.bis: Duplicate of 92.1.2 in MAX_MIN_MARGIN mode
  The situation is the same as in US 92.1.2 but the RAO maximizes the minimum margin.
  As activation costs are not taken in account, both lines will be closed.
    Given network file is "epic92/2Nodes3ParallelLines_disconnected.uct"
    Given crac file is "epic92/crac-92-1-2.json"
    Given configuration file is "epic92/RaoParameters_margin_dc_minObjective.json"
    When I launch rao
    Then the worst margin is 464.29 MW
    Then the margin on cnec "cnecBeFrPreventive" after PRA should be 464.29 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 2 remedial actions are used in preventive
    Then the remedial action "closeBeFr2" is used in preventive
    Then the remedial action "closeBeFr3" is used in preventive
    Then the value of the objective function initially should be 250.0
    # In MIN_MAX_MARGIN, the objective function is the opposite of the worst margin.
    Then the value of the objective function after PRA should be -464.29

  @fast @preventive-only @costly @rao @megawatt
  Scenario: US 92.1.3: Selection of the two cheapest network actions
  Same as US 92.1.1, but two network actions are needed to secure the CNEC.
    Given network file is "epic92/2Nodes4ParallelLines.uct"
    Given crac file is "epic92/crac-92-1-3.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective.json"
    When I launch rao
    Then the worst margin is 66.67 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 2 remedial actions are used in preventive
    Then the remedial action "closeBeFr3" is used in preventive
    Then the remedial action "closeBeFr4" is used in preventive
    # Overload penalty (600 * 1000)
    Then the value of the objective function initially should be 600000.0
    # Activation of closeBeFr3 (500) + activation of closeBeFr4 (220)
    Then the value of the objective function after PRA should be 720.0

  @fast @preventive-only @costly @rao @megawatt
  Scenario: US 92.1.4: Selection of cheapest of 3 equivalent network actions but overload remains at the end of RAO
  Only one network action can be used (behavior set in the RAO parameters) so the RAO chooses the cheapest
  remedial action available to reduce the overload and thus the penalty cost. The total cost is:
  100 (overload in MW) * 10000 (penalty cost in currency/MW) + 220 (cost of the chosen remedial action)
    Given network file is "epic92/2Nodes4ParallelLines.uct"
    Given crac file is "epic92/crac-92-1-3.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_maxDepth1.json"
    When I launch rao
    Then the worst margin is -100.0 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "closeBeFr4" is used in preventive
    Then the value of the objective function initially should be 600000.0
    Then the value of the objective function after PRA should be 100220.0

  @fast @preventive-only @costly @rao @megawatt
  Scenario: US 92.1.5: Sub-optimal case
  Closing line BE-FR-2 costs 1000 but solves the constraint immediately. As closing BE-FR-2 looks optimal
  at depth 1, the greedy search-tree keeps it at depth 2 but there is no need to apply additional remedial
  actions since the network is already secure.
  However, the optimal case is to close lines BE-FR-3 and BE-FR-4 successively (the order does not matter)
  for a total expense of 50. Yet, because of the penalty cost for overloads, the RAO still counts an over-cost
  of 100000 because closing BE-FR-3 or BE-FR-4 alone only reduce the minimum margin to -100 MW.
    #TODO: I reordered this text, but the last sentence above is still not clear to me
    Given network file is "epic92/2Nodes4ParallelLinesDifferentResistances.uct"
    Given crac file is "epic92/crac-92-1-5.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective.json"
    When I launch rao
    Then the worst margin is 66.67 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "closeBeFr2" is used in preventive
    Then the value of the objective function initially should be 600000.0
    Then the value of the objective function after PRA should be 1000.0

  @fast @costly @rao @megawatt
  Scenario: US 92.1.6: Preventive and auto optimization - 1 scenario
  Overload in preventive (activation of 1 RA), no overload in auto (activation of a 2nd RA)
    Given network file is "epic92/2Nodes4ParallelLines2LinesClosed.uct"
    Given crac file is "epic92/crac-92-1-6.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective.json"
    When I launch rao
    Then the worst margin is 66.67 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the value of the objective function initially should be 600000.0
    Then 1 remedial actions are used in preventive
    Then the remedial action "closeBeFr3" is used in preventive
    # Activation of closeBeFr3 (200) + overload penalty (100 * 1000)
    Then the value of the objective function after PRA should be 100200.0
    Then 1 remedial actions are used after "coBeFr2" at "auto"
    Then the remedial action "closeBeFr4" is used after "coBeFr2" at "auto"
    # Activation of closeBeFr3 (200) + activation of closeBeFr4 (850)
    Then the value of the objective function after ARA should be 1050.0

  @fast @costly @rao @megawatt
  Scenario: US 92.1.7: Preventive and auto optimization - 2 scenarios
  Each scenario secured by a different RA in auto.
    Given network file is "epic92/2Nodes4ParallelLines3LinesClosed.uct"
    Given crac file is "epic92/crac-92-1-7.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective.json"
    When I launch rao
    Then the worst margin is 66.67 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the value of the objective function initially should be 100000.0
    Then 1 remedial actions are used after "coBeFr2" at "auto"
    Then the remedial action "closeBeFr4" is used after "coBeFr2" at "auto"
    Then 1 remedial actions are used after "coBeFr3" at "auto"
    Then the remedial action "closeBeFr4" is used after "coBeFr3" at "auto"
    # Activation of closeBeFr4 after coBeFr2 (850) + activation of closeBeFr4 after coBeFr3 (850)
    Then the value of the objective function after ARA should be 1700.0

  @fast @costly @rao @megawatt
  Scenario: US 92.1.8: Preventive and auto optimization - 2 scenarios with PRA
  Each scenario secured by a different RA in auto, after one PRA.
    Given network file is "epic92/2Nodes5ParallelLines3LinesClosed.uct"
    Given crac file is "epic92/crac-92-1-8.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective.json"
    When I launch rao
    Then the worst margin is 10.0 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the value of the objective function initially should be 240000.0
    Then 1 remedial actions are used in preventive
    Then the remedial action "closeBeFr4" is used in preventive
    # Activation of closeBeFr4 (325) + overload penalty (73.33 * 1000)
    Then the value of the objective function after PRA should be 73658.0
    Then 1 remedial actions are used after "coBeFr2" at "auto"
    Then the remedial action "closeBeFr5" is used after "coBeFr2" at "auto"
    Then 1 remedial actions are used after "coBeFr3" at "auto"
    Then the remedial action "closeBeFr5" is used after "coBeFr3" at "auto"
    # Preventive cost (325) + activation of closeBeFr4 after coBeFr2 (850) + activation of closeBeFr4 after coBeFr3 (850)
    Then the value of the objective function after ARA should be 2025.0

  @fast @costly @rao @megawatt
  Scenario: US 92.1.9: Preventive and curative optimization - 1 scenario - no auto instant
  The curative scenario is secured by one PRA and one CRA.
    Given network file is "epic92/2Nodes4ParallelLines2LinesClosed.uct"
    Given crac file is "epic92/crac-92-1-9.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective.json"
    When I launch rao
    Then the worst margin is 66.67 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the value of the objective function initially should be 600000.0
    Then 1 remedial actions are used in preventive
    Then the remedial action "closeBeFr3" is used in preventive
    # Activation of closeBeFr3 (200) + overload penalty (100 * 1000)
    Then the value of the objective function after PRA should be 100200.0
    Then 1 remedial actions are used after "coBeFr2" at "curative"
    Then the remedial action "closeBeFr4" is used after "coBeFr2" at "curative"
    # Activation of closeBeFr3 (200) + activation of closeBeFr4 (850)
    Then the value of the objective function after CRA should be 1050.0

  @fast @costly @rao @megawatt
  Scenario: US 92.1.9.bis: Preventive and curative optimization - 1 scenario - with auto instant
  The curative scenario is secured by one PRA and one CRA, but instants auto and preventive are unsecure.
    Given network file is "epic92/2Nodes4ParallelLines2LinesClosed.uct"
    Given crac file is "epic92/crac-92-1-9-bis.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective.json"
    When I launch rao
    Then the worst margin is 66.67 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the value of the objective function initially should be 600000.0
    Then 1 remedial actions are used in preventive
    Then the remedial action "closeBeFr3" is used in preventive
    # Activation of closeBeFr3 (200) + overload penalty (100 * 1000)
    Then the value of the objective function after PRA should be 100200.0
    Then the value of the objective function after ARA should be 100200.0
    Then 1 remedial actions are used after "coBeFr2" at "curative"
    Then the remedial action "closeBeFr4" is used after "coBeFr2" at "curative"
    # Activation of closeBeFr3 (200) + activation of closeBeFr4 (850)
    Then the value of the objective function after CRA should be 1050.0

  @fast @costly @rao @megawatt
  Scenario: US 92.1.10: Preventive, auto and curative optimization - 1 scenario
  One RA applied after each instant, only curative is secure.
    Given network file is "epic92/2Nodes5ParallelLines2LinesClosed.uct"
    Given crac file is "epic92/crac-92-1-10.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective.json"
    When I launch rao
    Then the worst margin is 16.67 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the value of the objective function initially should be 700000.0
    Then 1 remedial actions are used in preventive
    Then the remedial action "closeBeFr3" is used in preventive
    # Activation of closeBeFr2 (200) + overload penalty for curative cnec (200 * 1000)
    Then the value of the objective function after PRA should be 200200.0
    Then 1 remedial actions are used after "coBeFr2" at "auto"
    Then the remedial action "closeBeFr4" is used after "coBeFr2" at "auto"
    # Activation of closeBeFr2 (200) + activation of closeBeFr3 (1350) + overload penalty for curative cnec (33.33 * 1000)
    Then the value of the objective function after ARA should be 34883.33
    Then 1 remedial actions are used after "coBeFr2" at "curative"
    Then the remedial action "closeBeFr5" is used after "coBeFr2" at "curative"
    # Activation of closeBeFr2 (200) + activation of closeBeFr3 (1350) + activation of closeBeFr5 (850)
    Then the value of the objective function after CRA should be 2400.0

  @fast @costly @rao @megawatt
  Scenario: US 92.1.11: Preventive, auto and curative optimization - 4 comprehensive scenarios
  4 scenarios are optimized in parallel:
  - scenario 1: no ARA and no CRA -> 50 MW overload
  - scenario 2: forced ARA and no CRA
  - scenario 3: no ARA and available CRA
  - scenario 4: forced ARA and available CRA
    Given network file is "epic92/2Nodes8ParallelLines5LinesClosed.uct"
    Given crac file is "epic92/crac-92-1-11.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective.json"
    When I launch rao
    Then the worst margin is -50.00 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the value of the objective function initially should be 100000.0
    Then 1 remedial actions are used in preventive
    Then the remedial action "closeBeFr6" is used in preventive
    # Activation of closeBeFr6 (2500) + overload penalty (50 * 1000)
    Then the value of the objective function after PRA should be 52500.0
    Then 0 remedial actions are used after "coBeFr2" at "auto"
    Then 1 remedial actions are used after "coBeFr3" at "auto"
    Then the remedial action "closeBeFr7" is used after "coBeFr3" at "auto"
    Then 0 remedial actions are used after "coBeFr4" at "auto"
    Then 1 remedial actions are used after "coBeFr5" at "auto"
    Then the remedial action "closeBeFr7" is used after "coBeFr5" at "auto"
    # Activation of closeBeFr6 (2500) + activation of closeBeFr7 twice (2 * 60) + overload penalty (50 * 1000)
    Then the value of the objective function after ARA should be 52620.0
    Then 0 remedial actions are used after "coBeFr2" at "curative"
    Then 0 remedial actions are used after "coBeFr3" at "curative"
    Then 1 remedial actions are used after "coBeFr4" at "curative"
    Then the remedial action "closeBeFr8" is used after "coBeFr4" at "curative"
    Then 1 remedial actions are used after "coBeFr5" at "curative"
    Then the remedial action "closeBeFr8" is used after "coBeFr5" at "curative"
    # Activation of closeBeFr6 (2500) + activation of closeBeFr7 twice (2 * 60) + activation of closeBeFr8 twice (2 * 735) + overload penalty (50 * 1000)
    Then the value of the objective function after CRA should be 54090.0

  @fast @costly @rao @megawatt
  Scenario: US 92.1.12: Preventive and auto optimization - curative overload
  4 scenarios are optimized in parallel with a curative overload each time:
  - scenario 1: no ARA and no CRA -> 50 MW overload
  - scenario 2: forced ARA and no CRA -> 66.67 MW overload
  - scenario 3: no ARA and available CRA -> 41.67 MW overload
  - scenario 4: forced ARA and available CRA -> 10 MW overload
    Given network file is "epic92/2Nodes8ParallelLines5LinesClosed.uct"
    Given crac file is "epic92/crac-92-1-12.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective.json"
    When I launch rao
    Then the worst margin is -66.67 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the value of the objective function initially should be 150000.0
    Then 1 remedial actions are used in preventive
    Then the remedial action "closeBeFr6" is used in preventive
    # Activation of closeBeFr6 (2500) + overload penalty (100 * 1000) on cnecBeFrCurative - coBeFr3
    Then the value of the objective function after PRA should be 102500.0
    Then 0 remedial actions are used after "coBeFr2" at "auto"
    Then 1 remedial actions are used after "coBeFr3" at "auto"
    Then the remedial action "closeBeFr7" is used after "coBeFr3" at "auto"
    Then 0 remedial actions are used after "coBeFr4" at "auto"
    Then 1 remedial actions are used after "coBeFr5" at "auto"
    Then the remedial action "closeBeFr7" is used after "coBeFr5" at "auto"
    # Activation of closeBeFr6 (2500) + activation of closeBeFr7 twice (2 * 60) + overload penalty on cnecBeFrCurative - coBeFr4 (75 * 1000)
    Then the value of the objective function after ARA should be 77620.0
    Then 0 remedial actions are used after "coBeFr2" at "curative"
    Then 0 remedial actions are used after "coBeFr3" at "curative"
    Then 1 remedial actions are used after "coBeFr4" at "curative"
    Then the remedial action "closeBeFr8" is used after "coBeFr4" at "curative"
    Then 1 remedial actions are used after "coBeFr5" at "curative"
    Then the remedial action "closeBeFr8" is used after "coBeFr5" at "curative"
    # Activation of closeBeFr6 (2500) + activation of closeBeFr7 twice (2 * 60) + activation of closeBeFr8 twice (2 * 735) + overload penalty on cnecBeFrCurative - coBeFr3 (66.67 * 1000)
    Then the value of the objective function after CRA should be 70756.67
