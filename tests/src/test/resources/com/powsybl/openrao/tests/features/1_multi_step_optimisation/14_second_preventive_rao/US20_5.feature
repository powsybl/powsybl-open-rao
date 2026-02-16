# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 20.5: Advanced 2nd preventive run conditions

  @fast @rao @ac @second-preventive @max-min-margin @ampere
  Scenario: US 20.5.1: Cost has not increased during RAO, do not fall back to initial solution (copy of 20.1.1)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/second_preventive_ls_1.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_forbid_cost_increase.json"
    When I launch rao
    Then the worst margin is -144 A
    Then the value of the objective function after CRA should be 144
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"

  @fast @rao @ac @second-preventive @max-min-margin @ampere
  Scenario: US 20.5.2: Cost has increased during RAO, fall back to initial solution (copy of 20.1.2)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/SL_ep20us5case2.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_forbid_cost_increase.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 0 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 113 A
    Then the value of the objective function after CRA should be -113
    Then the execution details should be "First preventive fell back to initial situation"
    Then its security status should be "SECURED"

  @fast @rao @ac @second-preventive @max-min-margin @ampere
  Scenario: US 20.5.3: Cost has not increased during RAO, do not run 2P (copy of 20.1.1)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/second_preventive_ls_1.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_2p_if_cost_increase.json"
    When I launch rao
    Then the worst margin is -144 A
    Then the value of the objective function after CRA should be 144
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"

  @fast @rao @ac @second-preventive @max-min-margin @ampere
  Scenario: US 20.5.4: Cost has increased during RAO, run 2P (copy of 20.1.2)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/SL_ep20us5case2.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_2p_if_cost_increase.json"
    When I launch rao
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 2 in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 795 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 795 A
    Then the margin on cnec "FFR4AA1  DDE1AA1  1 - preventive" after CRA should be 800 A
    Then the execution details should be "Second preventive improved first preventive results"
    Then its security status should be "SECURED"

  @fast @rao @ac @second-preventive @max-min-margin @ampere
  Scenario: US 20.5.5: Not enough time to run 2P (copy of 20.1.1)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/second_preventive_ls_1.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch rao with a time limit of -1 seconds
    Then the worst margin is -144 A
    Then the value of the objective function after CRA should be 144
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"

  @fast @rao @ac @second-preventive @max-min-margin @ampere
  Scenario: US 20.5.6: Enough time to run 2P (copy of 20.1.1)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic20/second_preventive_ls_1.json"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    When I launch rao with a time limit of 600 seconds
    Then the worst margin is 321 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 321 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 501 A
    Then the execution details should be "Second preventive improved first preventive results"
    Then its security status should be "SECURED"