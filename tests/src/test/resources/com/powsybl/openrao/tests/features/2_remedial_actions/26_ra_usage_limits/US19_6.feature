# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 19.6: Handle maximum number of TSOs using RAs in curative optimization
  This feature covers the parameter "ra-usage-limits-per-instant"/"max-tso", defined in the CRAC.
    ## TODO: test also when defined in CracCreationParameters

  @fast @rao @ac @contingency-scenarios @max-min-margin
  Scenario: US 19.6.1: Two allowed TSOs - 2 TSOs in crac
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us6case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -12 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 999 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @ac @contingency-scenarios @max-min-margin
  Scenario: US 19.6.2: Two allowed TSOs - 3 TSOs in crac
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us6case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -12 after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be 5 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 1000 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @ac @contingency-scenarios @max-min-margin
  Scenario: US 19.6.4: One allowed TSO - BE PST not allowed
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us10case9.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr2" is used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be 2 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 998 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @ac @contingency-scenarios @max-min-margin
  Scenario: US 19.6.8: No CRA allowed
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us6case8.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 680 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @ac @contingency-scenarios @max-min-margin
  Scenario: US 19.6.10: No CRA allowed - check parameter is ignored in preventive
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us6case10.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then 3 remedial actions are used in preventive
    Then the remedial action "open_be1_be4" is used in preventive
    Then the remedial action "open_fr1_fr2" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be -15 in preventive