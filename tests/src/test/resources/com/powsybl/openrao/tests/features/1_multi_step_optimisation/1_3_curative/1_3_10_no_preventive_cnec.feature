# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 1.3.10: Solve a RAO when no preventive CNEC is defined.
  This feature covers RAO computation with no preventive CNEC defined in the CRAC

  @fast @rao @ac @contingency-scenarios @max-min-margin
  Scenario: 1.3.10.1: Simple case with curative remedial actions only and only one curative cnec
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "1_multi_step_optimisation/1_3_curative/crac_1_3_10_1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -12 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 1000 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 1000 A
    Then the value of the objective function after CRA should be -1000

  @fast @rao @ac @contingency-scenarios @max-min-margin @second-preventive
  Scenario: 1.3.10.2: Simple case with curative remedial actions only and ONLY one curative cnec WITH second preventive
  There is no preventive CNEC or remedial action, but the second preventive execution parameter is set to POSSIBLE_CURATIVE_IMPROVEMENT.
  Since cost after curative (-999.5) > cost after first preventive (-1.0E9) => the second preventive is executed
  The second preventive doesn't really improve the first preventive, we just get the same result.
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "1_multi_step_optimisation/1_3_curative/crac_1_3_10_1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch rao
    Then the execution details should be "Second preventive improved first preventive results"
    Then its security status should be "SECURED"
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -12 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 1000 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 1000 A
    Then the value of the objective function after CRA should be -1000

  @fast @rao @ac @contingency-scenarios @max-min-margin
  Scenario: 1.3.10.3: two outage + curative states, including one without CRA
  Same case as 1.3.3.6, but all the preventive CNECs were removed from the CRAC AND all the preventive remedial actions
  The curative and outage state with no RA are in the preventive perimeter (the one associated with contingency co1_fr2_fr3_1)
  as well as "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - outage" CNEC
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "1_multi_step_optimisation/1_3_curative/crac_1_3_10_3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then 0 remedial actions are used in preventive
    Then 2 remedial actions are used after "co2_be1_be3" at "curative"
    Then the remedial action "open_be1_be4" is used after "co2_be1_be3" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "co2_be1_be3" at "curative"
    Then the worst margin is -683 A
    Then the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - curative" after CRA should be -683 A
    Then the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - outage" after PRA should be -383 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - outage" after PRA should be 192 A
    Then the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 477 A
    Then the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 377 A
    Then the value of the objective function after CRA should be 683

  @fast @rao @ac @contingency-scenarios @max-min-margin
  Scenario: 1.3.10.4: two outage + curative states, including one without CRA + preventive remedial actions
  Same case as 1.3.10.3, but this time even if no preventive CNECs are defined we still have preventive remedial actions in the CRAC
  They should be used to decrease the margin of the limiting element
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "1_multi_step_optimisation/1_3_curative/crac_1_3_10_4.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 15 in preventive
    Then 2 remedial actions are used after "co2_be1_be3" at "curative"
    Then the remedial action "open_be1_be4" is used after "co2_be1_be3" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "co2_be1_be3" at "curative"
    Then the worst margin is -484 A
    Then the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - curative" after CRA should be -484 A
    Then the margin on cnec "FFR2AA1  FFR3AA1  2 - co1_fr2_fr3_1 - outage" after PRA should be -184 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co2_be1_be3 - outage" after PRA should be 232 A
    Then the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - outage" after PRA should be 649 A
    Then the margin on cnec "FFR4AA1  DDE1AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 549 A
    Then the value of the objective function after CRA should be 484

  @fast @rao @ac @contingency-scenarios @max-min-margin @second-preventive
  Scenario: 1.3.10.5: Simple case with curative remedial actions only and ONLY one curative cnec WITH second preventive
  Only one curative CNEC, one network action available in preventive and two PST available in curative.
  The parameter is set to POSSIBLE_CURATIVE_IMPROVEMENT, even if no preventive cnecs are available we still have
  preventive remedial actions available that can be used in second preventive.
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "1_multi_step_optimisation/1_3_curative/crac_1_3_10_5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch rao
    Then the execution details should be "Second preventive improved first preventive results"
    Then its security status should be "SECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -12 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 1000 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 1000 A
    Then the value of the objective function after CRA should be -1000
