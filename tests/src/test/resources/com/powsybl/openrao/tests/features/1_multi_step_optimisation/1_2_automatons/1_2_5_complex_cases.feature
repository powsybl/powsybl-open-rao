# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 1.2.5: Complex automaton simulator cases
  ##TODO: This feature covers

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

  Scenario: 1.2.5.2: outage duplicate CNEC is the limiting element - simple situation
    "cnec-auto-2" has no ARA -> an outage duplicate is created -> check that duplicate cnec margin is not considered
    in the final cost and in the final rao result.
    The "cnec-auto-2" is secured by "accident" thanks to the ARA that is used to secure "cnec-auto-1"
    Given network file is "1_multi_step_optimisation/1_2_automatons/network_1_2_5_2.uct"
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_5_2.json"
    Given configuration file is "common/rao_parameters_max_margin_ampere_second_preventive.json"
    When I launch rao with crac validator
    Then its security status should be "SECURED"
    # The outage duplicate CNEC is not serialize in the RaoResult
    Then the worst margin is 35.87 A
    Then the value of the objective function after CRA should be -35.87
    Then 1 remedial actions are used after "co_fr1_fr2_1" at "auto"
    Then the tap of PstRangeAction "pst_auto" should be -7 after "co_fr1_fr2_1" at "auto"
    Then the margin on cnec "cnec - auto - 1" after ARA should be 35.87 A
    Then the margin on cnec "cnec - auto - 2" after ARA should be 59.7 A


  Scenario: 1.2.5.3.a: An auto cnec needs a preventive action to be properly secured - with outage duplicate
    Same situation as 1.2.5.2, but the line 'NNL1AA1  NNL2AA1  1' is open and a preventive network action is added to the CRAC
    that close this line. With the outage cnec duplicate mechanism, the cnec-auto-2 is added to the preventive perimeter,
    the PRA is activated, and we come back to the same auto situation as 1.2.5.2, where securing "cnec-auto-1" also secure "cnec-auto-2".
    This is the "optimal solution".
    Given network file is "1_multi_step_optimisation/1_2_automatons/network_1_2_5_3.uct"
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_5_3.json"
    Given configuration file is "common/rao_parameters_max_margin_ampere_second_preventive.json"
    When I launch rao with crac validator
    Then the initial margin on cnec "cnec - auto - 1" should be -280.52 A
    Then the initial margin on cnec "cnec - auto - 2" should be -89.85 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "close_nl1_nl2_1" is used in preventive
    Then the margin on cnec "cnec - auto - 1" after PRA should be -295.65 A
    Then the margin on cnec "cnec - auto - 2" after PRA should be -44.51 A
    Then 1 remedial actions are used after "co_fr1_fr2_1" at "auto"
    Then the tap of PstRangeAction "pst_auto" should be -7 after "co_fr1_fr2_1" at "auto"
       # The outage duplicate CNEC is not serialize in the RaoResult
    Then the worst margin is 35.87 A
    Then the value of the objective function after CRA should be -35.87

  Scenario: 1.2.5.3.b: An auto cnec needs a preventive action to be properly secured - no outage duplicate but with 2P
  Same situation as 1.2.5.3, but we deactivate the outage duplicate mechanism
  In the first preventive, no PRA is used because there is no CNEC in the preventive perimeter.
  In auto, "pst_auto" is activated to secure "cnec-auto-1" BUT we only need to go to tap -6 now to secure "cnec-auto-1" which is not enough to secure "cnec-auto-2".
  In second preventive, the search tree try the network action "close_nl1_nl2_1" but it is not retained because the cnec-auto-1 becomes unsecure (margin= -11.46 A)
    Given network file is "1_multi_step_optimisation/1_2_automatons/network_1_2_5_3.uct"
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_5_3.json"
    Given configuration file is "common/rao_parameters_max_margin_ampere_second_preventive.json"
    When I launch rao
    Then the initial margin on cnec "cnec - auto - 1" should be -280.52 A
    Then the initial margin on cnec "cnec - auto - 2" should be -89.85 A
    Then 0 remedial actions are used in preventive
    Then the margin on cnec "cnec - auto - 1" after PRA should be -280.52 A
    Then the margin on cnec "cnec - auto - 2" after PRA should be -89.85 A
    Then 1 remedial actions are used after "co_fr1_fr2_1" at "auto"
    Then the tap of PstRangeAction "pst_auto" should be -6 after "co_fr1_fr2_1" at "auto"
    Then the margin on cnec "cnec - auto - 1" after ARA should be 5.04 A
    Then the margin on cnec "cnec - auto - 2" after ARA should be -4.59 A
    Then the worst margin is -4.59 A
    Then the value of the objective function after CRA should be 4.59
