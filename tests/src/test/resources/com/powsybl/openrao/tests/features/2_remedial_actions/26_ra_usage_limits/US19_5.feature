# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 19.5: Max number of CRAs
  This feature covers the  parameter of "ra-usage-limits-per-instant"/"max-ra" defined in the CRAC.
  ## TODO: test also when defined in CracCreationParameters

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.5.1: Three allowed CRAs
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -12 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 999.5 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.5.2: Two allowed CRAs
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case2.json"
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

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.5.3: One allowed CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 16 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 945 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.5.4: One allowed CRA, BE PST not allowed
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case4.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 0 after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be 5 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 840 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.5.5: No allowed CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 679 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.5.6: Three topological CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case6.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_be1_be4" is used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr2" is used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 987 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.5.7: Two topological CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case7.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr2" is used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 973 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.5.8: One topological CRA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case8.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 839 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.5.9: One topological CRA, best FR topo not allowed
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case9.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 814 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: 19.5.10: Test that the parameter is ignored in preventive
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case10.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then 3 remedial actions are used in preventive
    Then the remedial action "open_be1_be4" is used in preventive
    Then the remedial action "open_fr1_fr2" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be -15 in preventive

  @fast @rao @ac @second-preventive @contingency-scenarios
  Scenario: US 19.5.11: Max ra usage limit in curative with 2P
    Took the test from the US 20.1.8, and added a CRAC creation parameter file to limit the number of CRAs to 1.
    A network action is used in curative after "CO1_fr2_fr3_1".
    => the max-ra-usage-limit being 1 we should not optimize any CRAs in the second preventive optimization.
    Not being able to optimize CRAs in the 2P slightly worsen the worst margin : go from 721 A to 717 A.
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct"
    Given crac file is "epic13/CBCORA_ep13us3case10.xml"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    Given crac creation parameters file is "epic19/CracCreationParameters_max_ra_usage_limit.json"
    When I launch rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 3 in preventive
    Then 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the worst margin is 717 A
    Then the margin on cnec "fr4_de1_N - preventive" after PRA should be 717 A
    Then the margin on cnec "fr1_fr4_CO1 - curative" after CRA should be 751 A
    Then the margin on cnec "fr3_fr5_CO1 - OPP - curative" after CRA should be 806 A
    Then the execution details should be "Second preventive improved first preventive results"

  @fast @rao @ac @second-preventive @contingency-scenarios
  Scenario: US 19.5.12: Max ra usage limit in curative with 2P - with 2 contingency scenario
    Similar to 19.5.11, but in this test instead of one contingency scenario we have two: CO1_fr2_fr3_1 and CO2_fr1_fr2_1.
    The RA Usage limit for curative instant is still 1.
    For CO1_fr2_fr3_1, we use one network action => so no range action can be optimized in 2P
    For CO2_fr1_fr2_1, no network action used => pst_be can be optimized after "CO2_fr1_fr2_1" at "curative"
    The RAO is able to update the max RA usage limit for each curative state independently.
    Given network file is "epic19/TestCase16Nodes_19_5_12.uct"
    Given crac file is "epic19/CBCORA_ep19us5case12.xml"
    Given configuration file is "epic20/RaoParameters_maxMargin_ampere_second_preventive.json"
    Given crac creation parameters file is "epic19/CracCreationParameters_max_ra_usage_limit.json"
    When I launch rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    Then the tap of PstRangeAction "pst_fr" should be -5 in preventive
    Then 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    Then 1 remedial actions are used after "CO2_fr1_fr2_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "CO1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -16 after "CO2_fr1_fr2_1" at "curative"
    Then the worst margin is 79 A
    Then the margin on cnec "fr1_fr4_CO2 - curative" after CRA should be 79 A
    Then the execution details should be "Second preventive improved first preventive results"
