# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 7.1.1: Linear RAO on existing test cases
  This feature gathers the existing test cases whose CRACs contain no network action and either
  whose RAO parameters enable the second preventive optimization (7.1.1.1 to 7.1.1.6), or whose
  CRACs contain no curative nor automaton remedial action (from 7.1.1.7), run with the linear RAO.
  The expected results are the same as with the search tree RAO.

  @fast @rao @dc @redispatching @preventive-only @costly
  Scenario: 7.1.1.1: Extremely basic redispatching on 2 nodes network - minCost (from 2.3.1.2)
  Network from 2.3.1.1.a, but objective function minCost.
  The initial setpoint is 1000, it is then updated to 300.0 (because the threshold of the line FR1 FR2 is 300)
  After optim => redispatching volume = 1000 - 300 = 700 MW
  Total cost : 10 for activation + 50 * 700 MW = 35010
    Given network file is "epic93/2Nodes.uct"
    Given crac file is "epic93/crac-93-1-1.json"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    When I launch linear rao
    Then the worst margin is 10 MW
    Then its security status should be "SECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "redispatchingAction" is used in preventive
    Then the setpoint of RangeAction "redispatchingAction" should be 290.0 MW in preventive
    Then the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 10.0 MW
    Then the value of the objective function after PRA should be 35010.0

  @fast @rao @dc @redispatching @preventive-only @costly
  Scenario: 7.1.1.2: Multiple redispatching actions keep network balanced with more "complex" keys - min cost (from 2.3.1.5)
  One redispatching action that acts on two generators with keys 1 and -0.7 and one on one generator with key 0.6.
  Injection balance constraint is respected:
      - redispatchingActionFR1FR2: initial setpoint = 1000 = 1000/1 = -700/-0.7, final = 512 => delta- = 488
      - redispatchingActionFR3: initial setpoint = -300/0.6 = -500, final = -256 => delta+ = −244
      => 488*(1-0.7)-244*0.6 = 0
  Objective function breakdown: 10+488*50+10+244*50 = 36620
    Given network file is "epic93/3Nodes.uct"
    Given crac file is "epic93/crac-93-1-5.json"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    When I launch linear rao
    Then the worst margin is 9.87 MW
    Then its security status should be "SECURED"
    Then the initial margin on cnec "cnecFr1Fr2Preventive" should be -267 MW
    Then 2 remedial actions are used in preventive
    Then the setpoint of RangeAction "redispatchingActionFR1FR2" should be 512.0 MW in preventive
    Then the setpoint of RangeAction "redispatchingActionFR3" should be -256.0 MW in preventive
    Then the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 9.87 MW
    Then the value of the objective function after PRA should be 36620.0

  @fast @rao @dc @redispatching @preventive-only @costly
  Scenario: 7.1.1.3: Redispatching with disconnected generator (from 2.3.1.6)
  A simple three nodes network where all the prod is on FFR2AA1 and all the load is on FFR1AA1.
  The generator FFR3AA1 is disconnected so the redispatchingActionFR3 won't be used so the RAO can't use redispatchingActionFR1 either
  because it won't be able to satisfy the injection balancing constraint.
    Given network file is "epic93/3Nodes_FFR3AA1_disconnected.xiidm"
    Given crac file is "epic93/crac-93-1-6.json"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    When I launch linear rao
    Then the worst margin is -366.67 MW
    Then its security status should be "UNSECURED"
    Then the initial setpoint of RangeAction "redispatchingActionFR1" should be -1000.0
    Then the setpoint of RangeAction "redispatchingActionFR1" should be -1000.0 MW in preventive
    Then 0 remedial actions are used in preventive

  @fast @rao @ac @contingency-scenarios @second-preventive @max-min-margin
  Scenario: 7.1.1.4: OnFlowConstraint with overload on other curative state (from 2.4.1.16)
  2 contingency scenarios but only 1 onConstraint usage rule defined.
  Because of second preventive optimization, tap is limited at position -3
  since it does not improve the objective function.
    Given network file is "epic16/2Nodes3ParallelLines.uct"
    Given crac file is "epic16/crac_16_3_15.json"
    Given configuration file is "epic16/RaoParameters_maxMargin_ampere_2P.json"
    When I launch linear rao
    Then 0 remedial actions are used in preventive
    Then 1 remedial actions are used after "co_fr1_fr2_1" at "curative"
    Then the remedial action "pst_fr" is used after "co_fr1_fr2_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be -3 after "co_fr1_fr2_1" at "curative"
    Then 0 remedial actions are used after "co_fr1_fr2_2" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be 0 after "co_fr1_fr2_2" at "curative"
    Then the worst margin is -221.78 A
    Then the margin on cnec "FFR1AA1  FFR2AA1  3 - co_fr1_fr2_1 - curative" after CRA should be -186.28 A
    Then the margin on cnec "FFR1AA1  FFR2AA1  3 - co_fr1_fr2_2 - curative" after CRA should be -221.78 A
    Then its security status should be "UNSECURED"

  @fast @costly @rao @second-preventive
  Scenario: 7.1.1.5: PST in 2nd preventive optimization (from 3.4.2.5)
  PST is moved to tap -9 straight from preventive optimization to cut curative activation costs.
  Same case as 3.4.2.3 but with 2nd preventive optimization allowed.
    Given network file is "epic92/2Nodes3ParallelLinesPST_v2.uct"
    Given crac file is "epic92/crac-92-2-3.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst_2P.json"
    When I launch linear rao
    Then the worst margin is 11.73 MW
    Then its security status should be "SECURED"
    Then the value of the objective function initially should be 430000.0
    Then 1 remedial actions are used in preventive
    Then the remedial action "pstBeFr3" is used in preventive
    Then the tap of PstRangeAction "pstBeFr3" should be -9 in preventive
    # Activation of pstBeFr3 (20) + 9 taps moved (9 * 7.5)
    Then the value of the objective function after PRA should be 87.5
    Then 0 remedial actions are used after "coBeFr2" at "curative"
    Then the value of the objective function after CRA should be 87.5

  @fast @costly @rao @second-preventive @multi-curative
  Scenario: 7.1.1.6: Multi-curative costly optimization with 2P (from 3.4.2.7)
  Same case as 3.4.2.6 but with second preventive optimization.
  The PST is moved to tap -10 straight from preventive optimization to cut activation expenses.
    Given network file is "epic92/2Nodes3ParallelLinesPST_v2.uct"
    Given crac file is "epic92/crac-92-2-6.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst_2P.json"
    When I launch linear rao
    Then the worst margin is 40.77 MW
    Then its security status should be "SECURED"
    Then the value of the objective function initially should be 450000.0
    Then 1 remedial actions are used in preventive
    Then the remedial action "pstBeFr3" is used in preventive
    Then the tap of PstRangeAction "pstBeFr3" should be -10 in preventive
    # Activation of pstBeFr3 (20) + 10 taps moved (10 * 5.0)
    Then the value of the objective function after PRA should be 70.0
    Then 0 remedial actions are used after "coBeFr2" at "curative-1"
    Then 0 remedial actions are used after "coBeFr2" at "curative-2"
    Then 0 remedial actions are used after "coBeFr2" at "curative-3"
    # Activation of pstBeFr3 (20) + 10 taps moved (10 * 5.0)
    Then the value of the objective function after CRA should be 70.0

  @fast @rao @ac @preventive-only @max-min-margin
  Scenario: 7.1.1.7: Inverted PstRangeAction in Security Limit (from 0.1.3.4.1)
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic90/SL_ep90us3case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 26.0 A
    Then the margin on cnec "BBE2AA1  BBE3AA1  1 - preventive" after PRA should be 26.0 A
    Then the tap of PstRangeAction "PRA_PST_BE" should be 3 in preventive

    ## TODO: is this relevant as security limits are not used anymore?

  @fast @rao @ac @preventive-only @secure-flow
  Scenario: 7.1.1.8: No remedial action (from 2.1.1.1)
  No remedial action, several unsecure CNECs.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic1/SL_ep1us0_withoutRA.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch linear rao
    Then its security status should be "UNSECURED"
    Then the worst margin is -667.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -667.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - Defaut FR1 FR3 - outage" after PRA should be -598.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - Defaut FR1 FR3 - curative" after CRA should be -598.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - Defaut FR1 FR2 - outage" after PRA should be -389.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - Defaut FR1 FR2 - curative" after CRA should be -389.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 779.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - Defaut FR1 FR3 - outage" after PRA should be 848.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - Defaut FR1 FR3 - curative" after CRA should be 848.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - Defaut FR1 FR2 - outage" after PRA should be 1056.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - Defaut FR1 FR2 - curative" after CRA should be 1056.0 A

  @fast @rao @ac @preventive-only @secure-flow
  Scenario: 7.1.1.9: Optimization monitoring only the PST (from 2.2.1.1.1)
  Basic case, the only RA is a PST range action - the CNECs are only defined for one network element.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic2/SL_ep2us2case1.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 778.0 A
    Then the margin on cnec "BBE2AA1  BBE3AA1  1 - preventive" after PRA should be 778.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 15 in preventive

  @fast @rao @ac @preventive-only @secure-flow
  Scenario: 7.1.1.10: Trade-off between various constraints (from 2.2.1.1.2)
  Same as 2.2.1.1.1, except that CNECs are defined on two additional network elements.
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic2/SL_ep2us2case2.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 39.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 39.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive

  @fast @rao @ac @preventive-only @secure-flow
  Scenario: 7.1.1.11: Unsecure solution (from 2.2.1.1.3)
  Same as 2.2.1.1.2, except that the CNEC thresholds are more restrictive.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic2/SL_ep2us2case3.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch linear rao
    Then its security status should be "UNSECURED"
    Then the worst margin is -37.0 A
    Then the margin on cnec "BBE2AA1  BBE3AA1  1 - preventive" after PRA should be -37.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 2 in preventive

  @fast @rao @ac @preventive-only @secure-flow
  Scenario: 7.1.1.12: Range intersection for one PST (from 2.2.1.1.4)
  Same as the previous cases, except that the max value of the absolute range of the RA is restricted from 16 to 3,
    and a relativeToInitialNetwork range is added (0 to 10).
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic2/SL_ep2us2case4.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 26.0 A
    Then the margin on cnec "BBE2AA1  BBE3AA1  1 - preventive" after PRA should be 26.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 3 in preventive

  @fast @rao @ac @preventive-only @secure-flow
  Scenario: 7.1.1.13: Handle 2 PSTs (from 2.2.1.1.5)
  Two preventive range actions are available (on two different PSTs).
    Given network file is "common/TestCase12Nodes2PSTs.uct"
    Given crac file is "epic2/SL_ep2us2case5.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 459.0 A
    Then 2 remedial actions are used in preventive
    Then the remedial action "PRA_PST_DE" is used in preventive
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_DE" should be -10 in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 16 in preventive

  @fast @rao @ac @preventive-only @max-min-margin
  Scenario: 7.1.1.14: Run a linear RAO with default PST min impact threshold (Same as 2.2.5) (from 2.2.1.6.1)
  Two PST range actions are activated, to increase the min margin.
    Given network file is "common/TestCase12Nodes2PSTs.uct"
    Given crac file is "epic2/SL_ep2us2case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 459.0 A
    Then the tap of PstRangeAction "PRA_PST_DE" should be -10 in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 16 in preventive

  # As this test case is originally a bit tricky about PST taps rounding
  # the result here is not really what we could expect because the minimum margin
  # improved while increasing the PST min impact threshold. Anyway here we want to test that
  # the configuration file is well taken into account with this parameter so this test
  # appears to be enough.

  @fast @rao @ac @preventive-only @max-min-margin
  Scenario: 7.1.1.15: Run a linear RAO with high PST min impact threshold (from 2.2.1.6.2)
    Given network file is "common/TestCase12Nodes2PSTs.uct"
    Given crac file is "epic2/SL_ep2us2case5.json"
    Given configuration file is "rao1/RaoParameters_maxMargin_ampere_highPSTcost.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 468.0 A
    Then the tap of PstRangeAction "PRA_PST_DE" should be -10 in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 11 in preventive

  @fast @rao @dc @preventive-only @hvdc @max-min-margin
  Scenario: 7.1.1.16: HVDC range action with one preventive CNEC, no impact on worst CNEC (from 2.2.2.3.12)
    Given network file is "epic15/TestCase16NodesWithHvdcAcEmulation_HvdcCnec.xiidm"
    Given crac file is "epic15/jsonCrac_ep15us17case12.json"
    Given configuration file is "epic15/RaoParameters_ep15us17case12.json"
    When I launch linear rao
    Then its security status should be "UNSECURED"
    Then 0 remedial actions are used in preventive
    Then the initial flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" should be 878 A on side 1
    Then the flow on cnec "be2_be5_n - BBE2AA11->BBE5AA11 - preventive" after PRA should be 878 A on side 1

  @fast @rao @dc @redispatching @preventive-only @max-min-margin
  Scenario: 7.1.1.17: Extremely basic redispatching on 2 nodes network - maxMargin (from 2.3.1.1.a)
  Two nodes containing one generator each, and linked by an overloaded line.
  One generator produces 1000, the other produces -1000.
  The objective is to maximize the min margin => shut down both generators.
  Redispatching action's chosen setpoint = 0: on FRR1AA1 -> setpoint * 1 = 0, on FRR2 -> setpoint * -1 = 0.
    Given network file is "epic93/2Nodes.uct"
    Given crac file is "epic93/crac-93-1-1.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch linear rao
    Then the worst margin is 300 MW
    Then its security status should be "SECURED"
    Then the initial margin on cnec "cnecFr1Fr2Preventive" should be -700 MW
    Then 1 remedial actions are used in preventive
    Then the remedial action "redispatchingAction" is used in preventive
    Then the setpoint of RangeAction "redispatchingAction" should be 0.0 MW in preventive
    Then the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 300 MW

  @fast @rao @dc @redispatching @preventive-only @max-min-margin
  Scenario: 7.1.1.18: Extremely basic redispatching on 2 nodes network - maxMargin - load (from 2.3.1.1.bis)
  Exact same situation but with loads instead of generators.
    Given network file is "epic93/2Nodes_load.uct"
    Given crac file is "epic93/crac-93-1-1-load.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch linear rao
    Then the worst margin is 300 MW
    Then its security status should be "SECURED"
    Then the initial margin on cnec "cnecFr1Fr2Preventive" should be -700 MW
    Then 1 remedial actions are used in preventive
    Then the remedial action "redispatchingAction" is used in preventive
    Then the setpoint of RangeAction "redispatchingAction" should be 0.0 MW in preventive
    Then the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 300 MW

  @fast @rao @dc @redispatching @preventive-only @max-min-margin
  Scenario: 7.1.1.19: Unbalanced redispatching (from 2.3.1.3)
  Only one redispatching action available: with a key equal to 1 on FR1 and -0.7 on FR2.
  The sum of the key do not sum up to 1. It's impossible to respect injection balance constraint with a variation != 0.
  The RAO chose to not apply the action.
    Given network file is "epic93/3Nodes.uct"
    Given crac file is "epic93/crac-93-1-3.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch linear rao
    Then the worst margin is -267 MW
    Then its security status should be "UNSECURED"
    Then the initial margin on cnec "cnecFr1Fr2Preventive" should be -267 MW
    Then 0 remedial actions are used in preventive
    Then the setpoint of RangeAction "redispatchingAction" should be 1000.0 MW in preventive
    Then the margin on cnec "cnecFr1Fr2Preventive" after PRA should be -267.0 MW

  @fast @rao @dc @redispatching @preventive-only @max-min-margin
  Scenario: 7.1.1.20: Multiple redispatching actions keep network balanced - max min margin (from 2.3.1.4)
  Both redispatching actions have distribution keys that do not sum to 0, the actions have to be activated together to
  compensate each other, and the result is optimal when all generators are shut down.
  Injection balance constraint is satisfied 1000*(1+1)-1000*(1+1)=0
    Given network file is "epic93/4Nodes.uct"
    Given crac file is "epic93/crac-93-1-4.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch linear rao
    Then the worst margin is 500 MW
    Then its security status should be "SECURED"
    Then 2 remedial actions are used in preventive
    Then the remedial action "redispatchingActionFR1FR3" is used in preventive
    Then the remedial action "redispatchingActionFR2FR4" is used in preventive
    Then the setpoint of RangeAction "redispatchingActionFR1FR3" should be 0.0 MW in preventive
    Then the setpoint of RangeAction "redispatchingActionFR2FR4" should be 0.0 MW in preventive
    Then the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 500.0 MW

  @fast @rao @ac @contingency-scenarios @max-min-margin
  Scenario: 7.1.1.21: Flow constraint in country with no contingency (from 2.4.3.1)
    # This is a copy of test case 2.4.2.7
    # pst_be is available after a flow constraint in BE, no contingency defined
    # So the same results as 2.4.2.7 are expected
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "extra_features/Crac_UR_1_1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "pst_be" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be 16 in preventive
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 82 A
    Then the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 82 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 245 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 345 A

  @fast @rao @ac @contingency-scenarios @max-min-margin
  Scenario: 7.1.1.22: Flow constraint in country only after a given contingency (from 2.4.3.2)
    # This is a copy of previous case but pst_be is available after a flow constraint in BE, only after contingency co1_fr2_fr3_1
    # Since only the preventive CNEC is constrained initially, the PST shall not be available
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "extra_features/Crac_UR_1_2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch linear rao
    Then its security status should be "UNSECURED"
    Then 0 remedial actions are used in preventive
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is -49 A
    Then the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be -49 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 693 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 79 A

  @fast @rao @preventive-only @max-min-margin
  Scenario: 7.1.1.23: Limit taps on PST with a maximum number of 3 elementary actions (from 2.6.4.1)
    Given network file is "epic19/small-network-2P.uct"
    Given crac file is "epic19/small-crac-with-max-3-elementary-actions-pst.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    # Best theoretical option is to move the PST to tap -16 (-33 MW)
    # With only three elementary actions, the best option is to move the PST to tap -3 (-462 MW)
    Then 1 remedial actions are used in preventive
    Then the initial flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" should be -561.0 MW on side 1
    Then the remedial action "pst_be" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be -3 in preventive
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after PRA should be -462.0 MW on side 1
    Then the worst margin is 38 MW

  @fast @rao @preventive-only @max-min-margin
  Scenario: 7.1.1.24: Limit taps on PST with a maximum number of 7 elementary actions (from 2.6.4.2)
    Given network file is "epic19/small-network-2P.uct"
    Given crac file is "epic19/small-crac-with-max-7-elementary-actions-pst.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    # Best theoretical option is to move the PST to tap -16 (-33 MW)
    # With only three elementary actions, the best option is to move the PST to tap -7 (-329 MW)
    Then 1 remedial actions are used in preventive
    Then the initial flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" should be -561.0 MW on side 1
    Then the remedial action "pst_be" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be -7 in preventive
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after PRA should be -329.0 MW on side 1
    Then the worst margin is 171.0 MW

  @fast @rao @preventive-only @max-min-margin
  Scenario: 7.1.1.25: Limit elementary actions for multiple TSOs (from 2.6.4.8)
    Given network file is "epic19/small-network-2P.uct"
    Given crac file is "epic19/small-crac-with-max-elementary-actions-multiple-tsos.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then 2 remedial actions are used in preventive
    Then the remedial action "pst_be" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be -8 in preventive
    Then the remedial action "pst_fr" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 10 in preventive
    Then the worst margin is 7.0 MW

  @fast @rao @dc @preventive-only @secure-flow
  Scenario: 7.1.1.26: use relevant number of decimals for margin and cost logging (from 3.2.1.0.b)
  This test is used as a reference for positive margin stop criterion, for comparison with the tests with max margin stop criterion.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic5/SL_ep5us1b.json"
    Given configuration file is "common/RaoParameters_posMargin_megawatt_dc.json"
    When I launch linear rao
    Then its security status should be "UNSECURED"
    Then the worst margin is -0.0001 MW with a tolerance of 0.00000001 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -0.0001 MW
    Then 0 remedial actions are used in preventive

  @fast @rao @ac @preventive-only @relative @max-min-relative-margin
  Scenario: 7.1.1.27: Unsecured case (from 3.3.1.1)
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic10/ls_relative_margin_unsecure.json"
    Given configuration file is "epic10/RaoParameters_relMargin_megawatt.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    When I launch linear rao
    Then its security status should be "UNSECURED"
    Then the value of the objective function after CRA should be 281.02
    Then the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    Then the worst margin is -281.02 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    Then the relative margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 2297.1 MW
    Then the relative margin on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be 2475.3 MW

  @fast @rao @ac @preventive-only @max-min-relative-margin
  Scenario: 7.1.1.28: Secured case (from 3.3.1.2)
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic10/ls_relative_margin.json"
    Given configuration file is "epic10/RaoParameters_relMargin_megawatt.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the value of the objective function after CRA should be -2383.0
    Then the tap of PstRangeAction "PRA_PST_BE" should be -3 in preventive
    Then the worst relative margin is 2383.0 MW on cnec "NNL2AA1  BBE3AA1  1 - preventive"
    Then the absolute PTDF sum on cnec "NNL2AA1  BBE3AA1  1 - preventive" initially should be 1.455
    Then the relative margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 2392.4 MW
    Then the absolute PTDF sum on cnec "FFR2AA1  DDE3AA1  1 - preventive" initially should be 1.477

  @fast @rao @ac @preventive-only @max-min-relative-margin
  Scenario: 7.1.1.29: Secured case with open monitored branch (from 3.3.1.3)
    Given network file is "common/TestCase12NodesWithOpenBranch.uct"
    Given crac file is "epic10/ls_relative_margin_with_open_branch.json"
    Given configuration file is "epic10/RaoParameters_relMargin_megawatt.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the value of the objective function after PRA should be -2383.0
    Then the value of the objective function after CRA should be -2385.0
    Then the tap of PstRangeAction "PRA_PST_BE" should be -3 in preventive
    Then the worst relative margin is 2383.0 MW on cnec "NNL2AA1  BBE3AA1  1 - preventive"
    Then the absolute PTDF sum on cnec "NNL2AA1  BBE3AA1  1 - preventive" initially should be 1.455
    Then the relative margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 2392.4 MW
    Then the absolute PTDF sum on cnec "FFR2AA1  DDE3AA1  1 - preventive" initially should be 1.477

  @fast @preventive-only @costly @rao
  Scenario: 7.1.1.30: Change only necessary taps on preventive PST (from 3.4.2.1)
  The RAO can increase the minimum margin by setting the tap of the PST on position -10
  but stops at position -5 because the network is secure and this saves expenses.
    Given network file is "epic92/2Nodes2ParallelLinesPST.uct"
    Given crac file is "epic92/crac-92-2-1.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst.json"
    When I launch linear rao
    Then the worst margin is 45.46 MW
    Then its security status should be "SECURED"
    Then the value of the objective function initially should be 200000.0
    Then 1 remedial actions are used in preventive
    Then the remedial action "pstBeFr2" is used in preventive
    Then the tap of PstRangeAction "pstBeFr2" should be -5 in preventive
    Then the value of the objective function after PRA should be 55.0

  @fast @preventive-only @costly @rao
  Scenario: 7.1.1.31: Two PSTs (from 3.4.2.2)
  PST 1 has cheaper variation costs (5 per tap) but a higher activation price (100) so moving the 9 required taps would
  require a cost of 145. PST 2 is cheaper to activate (5) and more expensive to use (15 per tap) but leads to a total
  cost of 140 so it is chosen.
    Given network file is "epic92/2Nodes3ParallelLines2PSTs_v2.uct"
    Given crac file is "epic92/crac-92-2-2.json"
    Given configuration file is "epic92/RaoParameters_dc_minObjective_discretePst.json"
    When I launch linear rao
    Then the worst margin is 31.15 MW
    Then its security status should be "SECURED"
    Then the value of the objective function initially should be 263333.33
    Then 1 remedial actions are used in preventive
    Then the remedial action "pstBeFr3" is used in preventive
    Then the tap of PstRangeAction "pstBeFr2" should be 0 in preventive
    Then the tap of PstRangeAction "pstBeFr3" should be -9 in preventive
    Then the value of the objective function after PRA should be 140.0

  @fast @rao @dc @preventive-only @max-min-margin
  Scenario: 7.1.1.32: linear RAO without LF limitation (from 5.1.2.3.1)
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf_rao_1.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 218.0 MW
    Then the margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" after PRA should be 218.0 MW
    Then the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive

  @fast @rao @dc @preventive-only @max-min-margin
  Scenario: 7.1.1.33: reference run, no MNEC (from 5.2.1.1)
  The flow on CNEC "NNL2AA1  NNL3AA1  1 - preventive" is increased because the CNEC is not limiting.
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_linearRao_ref.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    Then the value of the objective function after CRA should be -224.0
    Then the worst margin is 224.0 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    Then the initial flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" should be 833.3 MW on side 1
    Then the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 949.0 MW on side 1

  @fast @rao @dc @preventive-only @mnec @max-min-margin
  Scenario: 7.1.1.34: margin on MNEC should stay positive (initial margin > 50MW) (from 5.2.1.2)
  Same as 5.2.1.1, except that "NNL2AA1  NNL3AA1  1 - preventive" is a MNEC (threshold 900 MW) and not a CNEC (5000 MW).
  Unlike in the previous US, the flow of the MNEC cannot be increased above 900 MW, so the tap of the PST range action
  is adapted. The margin of the MNEC was positive initially, it must remain positive.
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_linearRao_1_2.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the tap of PstRangeAction "PRA_PST_BE" should be -9 in preventive
    Then the worst margin is 199.0 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    Then the initial flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" should be 833.3 MW on side 1
    Then the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 898.0 MW on side 1
    Then the margin on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 1.4 MW

  @fast @rao @dc @preventive-only @mnec @max-min-margin
  Scenario: 7.1.1.35: margin on MNEC should stay above initial value -50 MW [1] (initial margin < 0MW) (from 5.2.1.3)
  Same as 5.2.1.2, except that the threshold of the MNEC is "NNL2AA1  NNL3AA1  1 - preventive" is 800 MW (vs 900 MW).
  As the initial flow is 833 MW, the MNEC is overloaded. Its flow cannot be decreased "too much" by the RAO. The maximum
  decrease is set at 50 MW by the parameter "acceptable-margin-decrease" in "mnec-parameters".
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_linearRao_1_3.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the tap of PstRangeAction "PRA_PST_BE" should be -7 in preventive
    Then the worst margin is 192.0 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    Then the initial flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" should be 833.3 MW on side 1
    Then the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 884.1 MW on side 1
    Then the margin on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be -84.0 MW

  @fast @rao @dc @preventive-only @mnec @max-min-margin
  Scenario: 7.1.1.36: margin on MNEC should stay above initial value -50 MW [2] (50MW > initial margin > 0MW) (from 5.2.1.4)
  Same as 5.2.1.2, except that the threshold of the MNEC is "NNL2AA1  NNL3AA1  1 - preventive" is 850 MW (vs 900 MW).
  As the initial flow is 833 MW, the MNEC is not overloaded. However, its flow can still be decreased by the RAO, even
  if the margin becomes negative. The maximum decrease is set at 50 MW by the parameter "acceptable-margin-decrease"
  in "mnec-parameters".
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_linearRao_1_4.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the tap of PstRangeAction "PRA_PST_BE" should be -7 in preventive
    Then the worst margin is 192.0 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    Then the initial flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" should be 833.3 MW on side 1
    Then the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 884.1 MW on side 1
    Then the margin on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be -34.0 MW

  @fast @rao @ac @preventive-only @max-min-margin
  Scenario: 7.1.1.37: PST range action, direct CNEC (from 5.3.1.2.3)
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic3/SL_ep3us2_pst_direct.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch linear rao
    Then its security status should be "UNSECURED"
    Then the worst margin is -416.1 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -416.1 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PST1" is used in preventive
    Then the tap of PstRangeAction "PST1" should be -16 in preventive

  @fast @rao @ac @preventive-only @max-min-margin
  Scenario: 7.1.1.38: PST range action, opposite CNEC (from 5.3.1.2.4)
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic3/SL_ep3us2_pst_opposite.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 1075.0 A
    Then the margin on cnec "DDE1AA1  DDE2AA1  1 - Contingency FR1 FR3 - outage" after PRA should be 1075.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PST1" is used in preventive
    Then the tap of PstRangeAction "PST1" should be 16 in preventive

  @fast @rao @dc @preventive-only @max-min-margin
  Scenario: 7.1.1.39: MW thresholds in DC mode and min margin in MW (from 5.3.2.1.1.1)
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic4/SL_ep4us2_4MR_MW.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 22 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - curative" after PRA should be 22 MW
    Then the value of the objective function after CRA should be -22.0
    Then the tap of PstRangeAction "PRA_PST_BE" should be 5 in preventive
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 22.4 MW
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 24.1 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 44.0 MW

  @fast @rao @dc @preventive-only @max-min-margin
  Scenario: 7.1.1.40: MW thresholds in AC mode and min margin in MW (from 5.3.2.1.1.2)
  Same data as 5.3.2.1.1, but the computation is in AC.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic4/SL_ep4us2_4MR_MW.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 18.0 MW
    Then the value of the objective function after CRA should be -18.0
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - curative" after PRA should be 18 MW
    Then the value of the objective function after CRA should be -18.0
    Then the tap of PstRangeAction "PRA_PST_BE" should be 5 in preventive
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 22.4 MW
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 24.1 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 44.0 MW

  @fast @rao @dc @preventive-only @max-min-margin
  Scenario: 7.1.1.41: A thresholds in DC mode and min margin in MW (from 5.3.2.1.2.1)
  Same inputs as 5.3.2.1.1.1, but the thresholds are defined in A in the CRAC.
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic4/SL_ep4us2_4MR_A.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 15.07 MW
    Then the value of the objective function after CRA should be -15.07
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 15.07 MW
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 23.38 MW
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 15.07 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 45.12 MW

  @fast @rao @ac @preventive-only @max-min-margin
  Scenario: 7.1.1.42: A thresholds in AC mode and min margin in A (from 5.3.2.1.2.2)
  Same data as 5.3.2.1.2.1, but the computation is in AC.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic4/SL_ep4us2_4MR_A.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 19.0 A
    Then the value of the objective function after CRA should be -19.0
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 19.0 A
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 19.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 31.0 A
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 51.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 63.0 A

  @fast @rao @dc @preventive-only @max-min-margin
  Scenario: 7.1.1.43: mixed thresholds in DC mode and min margin in MW (from 5.3.2.1.3.1)
  Same inputs as 5.3.2.1.1.1, but some thresholds are defined in A in the CRAC (and others in MW).
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic4/SL_ep4us2_4MR_mixed.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 18.52 MW
    Then the value of the objective function after CRA should be -18.52
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 18.52 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 23.38 MW

  @fast @rao @ac @preventive-only @max-min-margin
  Scenario: 7.1.1.44: mixed thresholds in AC mode and min margin in A (from 5.3.2.1.3.2)
  Same data as 5.3.2.1.3.1, but the computation is in AC.
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic4/SL_ep4us2_4MR_mixed.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_ac.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 24 A
    Then the value of the objective function after CRA should be -24
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 24.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 31.5 A

  @fast @rao @ac @preventive-only @secure-flow
  Scenario: 7.1.1.45: secure with AC config (from 5.3.2.2.1.1)
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic4/SL_ep4us3.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 38.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - curative" after PRA should be 38.0 A
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive

  @fast @rao @dc @preventive-only @secure-flow
  Scenario: 7.1.1.46: secure with DC config (from 5.3.2.2.1.2)
  Same as 5.3.2.2.1 but computation in DC
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic4/SL_ep4us3.json"
    Given configuration file is "common/RaoParameters_posMargin_megawatt_dc.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 32 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - curative" after PRA should be 32 MW
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive

  @fast @rao @ac @preventive-only @secure-flow
  Scenario: 7.1.1.47: failure with AC config without fallback (from 5.3.2.2.2.1)
    Given network file is "epic4/US4-3-TestCase12Nodes-diverging.uct"
    Given crac file is "epic4/SL_ep4us3.json"
    Given configuration file is "epic4/RaoParameters_posMargin_ampere_ac_divergence.json"
    When I launch linear rao
    Then the calculation fails
    Then its security status should be "UNSECURED"

  @fast @rao @dc @preventive-only @secure-flow
  Scenario: 7.1.1.48: no failure with DC config (from 5.3.2.2.2.2)
  Same case as 4.3.2.1 but in DC.
    Given network file is "epic4/US4-3-TestCase12Nodes-diverging.uct"
    Given crac file is "epic4/SL_ep4us3.json"
    Given configuration file is "common/RaoParameters_posMargin_megawatt_dc.json"
    When I launch linear rao
    Then its security status should be "SECURED"
    Then the worst margin is 32.0 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - curative" after PRA should be 32.0 MW
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive
