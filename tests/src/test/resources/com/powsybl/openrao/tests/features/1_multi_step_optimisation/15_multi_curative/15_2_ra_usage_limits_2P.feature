Feature: 15.2: RA Usage Limits - 2P - Multi-curative
  This feature covers the use of RA Usage Limits in 2P multi-curative situations.
  In particular it checks that the MIP in 2P (that is considering the preventive instant as well as all the curative instants at the same time)
  is able to handle the cumulative effects of limits among the curative instants.

  @fast @rao @ac @multi-curative @second-preventive
  Scenario: 15.2.1: Multi-curative CNECs with no CRA for curative1, one PST available for curative 1 and 2
  We have one network action "PRA_CLOSE_NL2_BE3_3" available in preventive and one PST available for both curative instant 1 and two.
  We monitor the line "NNL2AA1  BBE3AA1  1" at each curative instant (the threshold decrease as we move from curative 1 to 3 (500 -> 300 -> 250).
  During first preventive the network action is not used as the initial preventive perimeter is secure.
  After optimizing the curative state "Contingency DE2 DE3 1 - curative2", the PST is set to -16 (the min) but it is not enough to secure the perimeter.
  => During second preventive:
  - the RA "PRA_CLOSE_NL2_BE3_3" is used
  - move the PST to -15 in curative2
  - and then -16 in curative 3 to secure "NNL2AA1  BBE3AA1  1 - Contingency DE2 DE3 1 - curative3"
  Moving the PST to -15 in curative2 and then -16 in curative3 is considered as using two RAs at two different instants.
  Given network file is "1_multi_step_optimisation/15_multi_curative/12Nodes3ParallelLines_disconnected.uct"
  Given crac file is "1_multi_step_optimisation/15_multi_curative/crac_15_2_1.json"
  Given configuration file is "1_multi_step_optimisation/15_multi_curative/RaoParameters_case_91_12_secure_2PRAO.json"
  When I launch rao
  Then the execution details should be "Second preventive improved first preventive results"
      # Preventive
  And 1 remedial actions are used in preventive
  And the remedial action "PRA_CLOSE_NL2_BE3_3" is used in preventive
      # Curative1
  Then 0 remedial actions are used after "Contingency DE2 DE3 1" at "curative1"
      # Curative2
  And 1 remedial actions are used after "Contingency DE2 DE3 1" at "curative2"
  And the remedial action "CRA_PST_BE" is used after "Contingency DE2 DE3 1" at "curative2"
  And the tap of PstRangeAction "CRA_PST_BE" should be -15 after "Contingency DE2 DE3 1" at "curative2"
      # Curative3
  And 1 remedial actions are used after "Contingency DE2 DE3 1" at "curative3"
  And the remedial action "CRA_PST_BE" is used after "Contingency DE2 DE3 1" at "curative3"
  And the tap of PstRangeAction "CRA_PST_BE" should be -16 after "Contingency DE2 DE3 1" at "curative3"
  And the value of the objective function after CRA should be -8.65


  @fast @rao @ac @multi-curative @second-preventive
  Scenario: 15.2.2: Multi-curative - with max-ra limits and 2P
  Same case as 15.2.1 but with RA usage limitation: 0 curative1 RAs, 1 curative2 RAs, 1 curative3 RAs
  Similarly to test 91.12.5, this should test that the RAO is able to take into account the cumulative effect of the max-ra-usage-limit in multi-curative.
  However the situation is more complex than for 91.12.5, since we want to check that the limitations are also respected in second preventive where all the curative range actions are optimized at once.
  The MIP in 2P should be able to move the PST one time to -16 directly in curative2 to secure both the "Contingency DE2 DE3 1 - curative2" and "Contingency DE2 DE3 1 - curative3".
  The final situation is the same as 15.2.2.
  Given network file is "1_multi_step_optimisation/15_multi_curative/12Nodes3ParallelLines_disconnected.uct"
  Given crac file is "1_multi_step_optimisation/15_multi_curative/crac_15_2_2_with_ra_limitations.json"
  Given configuration file is "1_multi_step_optimisation/15_multi_curative/RaoParameters_case_91_12_secure_2PRAO.json"
  When I launch rao
  Then the execution details should be "Second preventive improved first preventive results"
  Then 1 remedial actions are used in preventive
  Then 0 remedial actions are used after "Contingency DE2 DE3 1" at "curative1"
  Then 1 remedial actions are used after "Contingency DE2 DE3 1" at "curative2"
  And the remedial action "CRA_PST_BE" is used after "Contingency DE2 DE3 1" at "curative2"
  And the tap of PstRangeAction "CRA_PST_BE" should be -16 after "Contingency DE2 DE3 1" at "curative2"
  Then 0 remedial actions are used after "Contingency DE2 DE3 1" at "curative3"
  And the tap of PstRangeAction "CRA_PST_BE" should be -16 after "Contingency DE2 DE3 1" at "curative3"
  And the value of the objective function after CRA should be -8.65

  @fast @rao @ac @multi-curative @second-preventive
  Scenario: 15.2.3: Multi-curative - with max-pst-per-tso limits and 2P
  Case with two PST from TSO "FR" both available in curative 1 and 2
  The best result with NO max-ra-per-tso limit after 2P:
  - worst margin: margin = 3.52 MW, element NNL2AA1  BBE3AA1  1 at state Contingency DE2 DE3 1 - curative2, CNEC ID = "NNL2AA1  BBE3AA1  1 - Contingency DE2 DE3 1 - curative2"
  - network action(s): PRA_CLOSE_NL2_BE3_3, cost: -3.52 (functional: -3.52, virtual: 0.0)
  - range action(s): CRA_PST_FR_1@Contingency DE2 DE3 1 - curative1: -16 (var: -16), CRA_PST_FR_2@Contingency DE2 DE3 1 - curative1: 16 (var: 16)
  Now we add the limit for FR: 1 in curative1 and 2 in curative2
  => can only use CRA_PST_FR_1 of the PST in curative1 because the most limiting cnec is the one in curative 1
    Given network file is "1_multi_step_optimisation/15_multi_curative/12Nodes3ParallelLines_2PST.uct"
    Given crac file is "1_multi_step_optimisation/15_multi_curative/crac_15_2_3_max_pst_per_tso.json"
    Given configuration file is "1_multi_step_optimisation/15_multi_curative/RaoParameters_case_91_12_secure_2PRAO.json"
    When I launch rao
    Then the execution details should be "Second preventive improved first preventive results"
    And the initial tap of PstRangeAction "CRA_PST_FR_1" should be 0
    And the initial tap of PstRangeAction "CRA_PST_FR_2" should be 0
    And the remedial action "PRA_CLOSE_NL2_BE3_3" is used in preventive
    Then 1 remedial actions are used after "Contingency DE2 DE3 1" at "curative1"
    And the tap of PstRangeAction "CRA_PST_FR_1" should be -16 after "Contingency DE2 DE3 1" at "curative1"
    And the tap of PstRangeAction "CRA_PST_FR_2" should be 0 after "Contingency DE2 DE3 1" at "curative1"
    Then 0 remedial actions are used after "Contingency DE2 DE3 1" at "curative2"
    And the tap of PstRangeAction "CRA_PST_FR_1" should be -16 after "Contingency DE2 DE3 1" at "curative1"
    And the tap of PstRangeAction "CRA_PST_FR_2" should be 0 after "Contingency DE2 DE3 1" at "curative1"
    And the value of the objective function after CRA should be 41.35

  @fast @rao @ac @multi-curative @second-preventive
  Scenario: 15.2.4: Multi-curative - with max-ra-per-tso limits and 2P
  Case with one PST from TSO "BE" and one PST from TSO "FR" both available in curative 1 and 2
  The best result with NO max-ra-per-tso limit after 2P:
  - worst margin: margin = 3.52 MW, element NNL2AA1  BBE3AA1  1 at state Contingency DE2 DE3 1 - curative2, CNEC ID = "NNL2AA1  BBE3AA1  1 - Contingency DE2 DE3 1 - curative2"
  - network action(s): PRA_CLOSE_NL2_BE3_3, cost: -3.52 (functional: -3.52, virtual: 0.0)
  - range action(s): CRA_PST_BE@Contingency DE2 DE3 1 - curative1: -16 (var: -16), CRA_PST_FR@Contingency DE2 DE3 1 - curative1: 16 (var: 16)
  Now we add the limit for BE: 1 in curative1 and 0 in curative2 => means that we cannot use any RA in curative1 either
  and for FR: 1 in curative1 and 1 curative2
  => The most limiting cnec is in NNL2AA1  BBE3AA1  1 - Contingency DE2 DE3 1 - curative1, so the PST_FR is used in curative1 rather than curative2
  Given network file is "1_multi_step_optimisation/15_multi_curative/12Nodes3ParallelLines_2PST.uct"
  Given crac file is "1_multi_step_optimisation/15_multi_curative/crac_15_2_4_max_ra_per_tso.json"
  Given configuration file is "1_multi_step_optimisation/15_multi_curative/RaoParameters_case_91_12_secure_2PRAO.json"
  When I launch rao
  Then the execution details should be "Second preventive improved first preventive results"
  And the initial tap of PstRangeAction "CRA_PST_FR" should be 0
  And the initial tap of PstRangeAction "CRA_PST_BE" should be 0
  And the remedial action "PRA_CLOSE_NL2_BE3_3" is used in preventive
  Then 1 remedial actions are used after "Contingency DE2 DE3 1" at "curative1"
  And the tap of PstRangeAction "CRA_PST_FR" should be 16 after "Contingency DE2 DE3 1" at "curative1"
  And the tap of PstRangeAction "CRA_PST_BE" should be 0 after "Contingency DE2 DE3 1" at "curative1"
  Then 0 remedial actions are used after "Contingency DE2 DE3 1" at "curative2"
  And the tap of PstRangeAction "CRA_PST_FR" should be 16 after "Contingency DE2 DE3 1" at "curative1"
  And the tap of PstRangeAction "CRA_PST_BE" should be 0 after "Contingency DE2 DE3 1" at "curative1"
  And the value of the objective function after CRA should be 76.23

  @fast @rao @ac @multi-curative @second-preventive
  Scenario: 15.2.5: Multi-curative - with max-elementary-action-per-tso limits and 2P
  Same case as 15.2.4, but both PST are from TSO "FR" and slightly different threshold.
  Solution without any limit:
  - network action(s) activated : PRA_CLOSE_NL2_BE3_3
  - range action(s): CRA_PST_FR_1@Contingency DE2 DE3 1 - curative1: -14 (var: -14), CRA_PST_FR_2@Contingency DE2 DE3 1 - curative1: 16 (var: 16), CRA_PST_FR_1@Contingency DE2 DE3 1 - curative2: -16 (var: -2)
  - worst margin = 3.52 MW, element NNL2AA1  BBE3AA1  1 at state Contingency DE2 DE3 1 - curative2, CNEC ID = "NNL2AA1  BBE3AA1  1 - Contingency DE2 DE3 1 - curative2"
  Now we set a max number of elementary actions per TSO for "FR": curative1 = 17 and curative2 = 30.
  We expect the 2P MIP to:
  - use the full curative1 budget by moving 17 taps in total to secure "NNL2AA1  BBE3AA1  1 - Contingency DE2 DE3 1 - curative1" as much as possible;
  - move another 10 taps so that "NNL2AA1  BBE3AA1  1 - Contingency DE2 DE3 1 - curative2"'s margin goes slightly below the curative1 cnec's margin.
  We don't reach the curative2 limit because no need to move more in curative2 since the limiting cnec's is in curative1.
  Given network file is "1_multi_step_optimisation/15_multi_curative/12Nodes3ParallelLines_2PST.uct"
  Given crac file is "1_multi_step_optimisation/15_multi_curative/crac_15_2_5_max_elementary_actions_per_tso.json"
  Given configuration file is "1_multi_step_optimisation/15_multi_curative/RaoParameters_case_91_12_secure_2PRAO.json"
  When I launch rao
  Then the execution details should be "Second preventive improved first preventive results"
  And the initial tap of PstRangeAction "CRA_PST_FR_1" should be 0
  And the initial tap of PstRangeAction "CRA_PST_FR_2" should be 0
  Then 1 remedial actions are used in preventive
  And the remedial action "PRA_CLOSE_NL2_BE3_3" is used in preventive
  Then 2 remedial actions are used after "Contingency DE2 DE3 1" at "curative1"
  And the tap of PstRangeAction "CRA_PST_FR_1" should be -16 after "Contingency DE2 DE3 1" at "curative1"
  And the tap of PstRangeAction "CRA_PST_FR_2" should be 1 after "Contingency DE2 DE3 1" at "curative1"
  Then 1 remedial actions are used after "Contingency DE2 DE3 1" at "curative2"
  And the tap of PstRangeAction "CRA_PST_FR_1" should be -16 after "Contingency DE2 DE3 1" at "curative2"
  And the tap of PstRangeAction "CRA_PST_FR_2" should be 11 after "Contingency DE2 DE3 1" at "curative2"
  And the value of the objective function after CRA should be 9.17
