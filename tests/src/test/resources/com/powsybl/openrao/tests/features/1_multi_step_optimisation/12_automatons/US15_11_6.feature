# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.11.6: Complex automaton simulator cases

  @fast @rao @dc @contingency-scenarios @max-min-margin @megawatt
  Scenario: US 15.11.6.1: Automatons simulated by batches, speed-wise
    Complex case with 5 automatons that have different speeds:
    - FR1-FR2-1 is overloaded so FR1-FR2-3 is closed
    - The previous automaton activation solved the constraint so closing FR1-FR2-4 is not triggered
    - The two PSTs are triggered as their respective monitored lines are overloaded
    - Finally, FR5-FR6-2 is closed as FR5-FR6-1 is closed is still overloaded
    Given network file is "epic15/TestCase8Nodes_15_11_6_1.uct"
    Given crac file is "epic15/crac_15_11_6_1.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
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
