# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 1.2.5: Complex automaton simulator cases
  This feature covers complex cases of the multi-step (automaton) optimization. It checks the
  simulation of the automatons by batches, and the treatment of auto CNECs as limiting elements,
  including when a preventive action is required to secure them.

  @fast @rao @dc @contingency-scenarios @max-min-margin
  Scenario: 1.2.5.1: Automatons simulated by batches, speed-wise
    Complex case with 5 automatons that have different speeds:
    - FR1-FR2-1 is overloaded so FR1-FR2-3 is closed
    - The previous automaton activation solved the constraint so closing FR1-FR2-4 is not triggered
    - The two PSTs are triggered as their respective monitored lines are overloaded
    - Finally, FR5-FR6-2 is closed as FR5-FR6-1 is closed is still overloaded
    Given network file is "1_multi_step_optimisation/1_2_automatons/network_1_2_5_1.uct"
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_5_1_1.json"
    Given configuration file is "common/rao_parameters_max_margin_megawatt_dc.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 0 remedial actions are used in preventive
    Then 4 remedial actions are used after "co_fr1_fr2_2" at "auto"
    Then the remedial action "close_fr1_fr2_3" is used after "co_fr1_fr2_2" at "auto"
    Then the remedial action "pst_fr3_fr4" is used after "co_fr1_fr2_2" at "auto"
    Then the tap of PstRangeAction "pst_fr3_fr4" should be 2 after "co_fr1_fr2_2" at "auto"
    Then the remedial action "pst_fr7_fr8" is used after "co_fr1_fr2_2" at "auto"
    Then the tap of PstRangeAction "pst_fr7_fr8" should be -3 after "co_fr1_fr2_2" at "auto"
    Then the remedial action "close_fr5_fr6_2" is used after "co_fr1_fr2_2" at "auto"
    Then the worst margin is 1 MW

  Scenario: 1.2.5.2: Auto CNEC is incldued in the preventive perimeter and is the limiting element - simple situation
    "cnec-auto-2" has no ARA -> the CNEC is included in the preventive perimeter -> check that it is not considered
    in the preventive security status since it cannot be secured by PRAs -> the RAO still goes on with the auto/curative
    optimization.
    The "cnec-auto-2" is secured by "accident" thanks to the ARA that is used to secure "cnec-auto-1"
    Given network file is "1_multi_step_optimisation/1_2_automatons/network_1_2_5_2.uct"
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_5_2.json"
    Given configuration file is "common/rao_parameters_max_margin_ampere_second_preventive.json"
    When I launch rao
    Then its security status should be "SECURED"
    Then the worst margin is 35.87 A
    Then the value of the objective function after CRA should be -35.87
    Then 0 remedial actions are used in preventive
    Then 1 remedial actions are used after "co_fr1_fr2_1" at "auto"
    Then the tap of PstRangeAction "pst_auto" should be -7 after "co_fr1_fr2_1" at "auto"
    Then the margin on cnec "cnec - auto - 1" after ARA should be 35.87 A
    Then the margin on cnec "cnec - auto - 2" after ARA should be 59.7 A


  Scenario: 1.2.5.3: An auto cnec needs a preventive action to be properly secured
    Same situation as 1.2.5.2, but the line 'NNL1AA1  NNL2AA1  1' is open and a preventive network action is added to the CRAC
    that close this line. As it is unprotected, the cnec-auto-2 is added to the preventive perimeter,
    the PRA is activated, and we come back to the same auto situation as 1.2.5.2, where securing "cnec-auto-1" also secure "cnec-auto-2".
    This is the "optimal solution".
    Given network file is "1_multi_step_optimisation/1_2_automatons/network_1_2_5_3.uct"
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_5_3.json"
    Given configuration file is "common/rao_parameters_max_margin_ampere_second_preventive.json"
    When I launch rao
    Then the initial margin on cnec "cnec - auto - 1" should be -280.52 A
    Then the initial margin on cnec "cnec - auto - 2" should be -89.85 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "close_nl1_nl2_1" is used in preventive
    Then the margin on cnec "cnec - auto - 1" after PRA should be -295.65 A
    Then the margin on cnec "cnec - auto - 2" after PRA should be -44.51 A
    Then 1 remedial actions are used after "co_fr1_fr2_1" at "auto"
    Then the tap of PstRangeAction "pst_auto" should be -7 after "co_fr1_fr2_1" at "auto"
    Then the worst margin is 35.87 A
    Then the value of the objective function after CRA should be -35.87
