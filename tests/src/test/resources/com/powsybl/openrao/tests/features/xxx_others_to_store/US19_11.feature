# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 19.11: Handle maximum number of elementary actions per TSO

  @fast @rao @preventive-only
  Scenario: US 19.11.1: Limit taps on PST with a maximum number of 3 elementary actions
    Given network file is "epic19/small-network-2P.uct"
    Given crac file is "epic19/small-crac-with-max-3-elementary-actions-pst.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete.json"
    When I launch rao
    # Best theoretical option is to move the PST to tap -16 (-33 MW)
    # With only three elementary actions, the best option is to move the PST to tap -3 (-462 MW)
    Then 1 remedial actions are used in preventive
    Then the initial flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" should be -561.0 MW on side 1
    Then the remedial action "pst_be" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be -3 in preventive
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after PRA should be -462.0 MW on side 1
    Then the worst margin is 38 MW

  @fast @rao @preventive-only
  Scenario: US 19.11.2: Limit taps on PST with a maximum number of 7 elementary actions
    Given network file is "epic19/small-network-2P.uct"
    Given crac file is "epic19/small-crac-with-max-7-elementary-actions-pst.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete.json"
    When I launch rao
    # Best theoretical option is to move the PST to tap -16 (-33 MW)
    # With only three elementary actions, the best option is to move the PST to tap -7 (-329 MW)
    Then 1 remedial actions are used in preventive
    Then the initial flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" should be -561.0 MW on side 1
    Then the remedial action "pst_be" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be -7 in preventive
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after PRA should be -329.0 MW on side 1
    Then the worst margin is 171.0 MW

  @fast @rao @preventive-only
  Scenario: US 19.11.3: Select less efficient network action because it has less elementary actions
    Given network file is "epic19/small-network-2P-open-twin-lines.uct"
    Given crac file is "epic19/small-crac-with-max-1-elementary-action-topo.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete.json"
    When I launch rao
    # Best theoretical option is to close both line BE1-BE3-1 and line BE1-BE3-2 (-561 MW)
    # With only three elementary actions, the best option is to simply close line BE1-BE3-1 (-572 MW)
    Then 1 remedial actions are used in preventive
    Then the initial flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" should be -596.0 MW on side 1
    Then the remedial action "close_be1_be3_1" is used in preventive
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after PRA should be -572.0 MW on side 1
    Then the worst margin is 3.0 MW

  @fast @rao @preventive-only @multi-curative
  Scenario: US 19.11.4: Limit elementary actions with PSTs and 3 curative instants
    Given network file is "epic19/small-network-2P-open-twin-lines.uct"
    Given crac file is "epic19/small-crac-with-max-elementary-actions-pst-3-curative-instants.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete.json"
    When I launch rao
    # At each curative instant, the PST could theoretically go down to tap -16
    # But the tap decreases more slowly over the 3 curative instants
    Then the initial flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" should be -596.0 MW on side 1
    Then 1 remedial actions are used after "co1_fr1_fr3_1" at "curative1"
    Then the remedial action "pst_be" is used after "co1_fr1_fr3_1" at "curative1"
    Then the tap of PstRangeAction "pst_be" should be -1 after "co1_fr1_fr3_1" at "curative1"
    Then 1 remedial actions are used after "co1_fr1_fr3_1" at "curative2"
    Then the remedial action "pst_be" is used after "co1_fr1_fr3_1" at "curative2"
    Then the tap of PstRangeAction "pst_be" should be -3 after "co1_fr1_fr3_1" at "curative2"
    Then 1 remedial actions are used after "co1_fr1_fr3_1" at "curative3"
    Then the remedial action "pst_be" is used after "co1_fr1_fr3_1" at "curative3"
    Then the tap of PstRangeAction "pst_be" should be -7 after "co1_fr1_fr3_1" at "curative3"

  @fast @rao @preventive-only
  Scenario: US 19.11.5: Limit elementary actions with topos and PSTs
    Given network file is "epic19/small-network-2P-open-twin-lines.uct"
    Given crac file is "epic19/small-crac-with-max-3-elementary-actions-topo-and-pst.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete.json"
    When I launch rao
    # Best theoretical option is to close both lines and move the PST to tap -16
    # With only three elementary actions, the best three possibilities are:
    # - move the PST to tap -3 (-520 MW)
    # - close line BE1-BE3-1 and move the PST to tap -2 (-511 MW -> best option)
    # - close both line BE1-BE3-1 and line BE1-BE3-2 and move the PST to tap -1 (-528 MW)
    Then 2 remedial actions are used in preventive
    Then the initial flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" should be -596.0 MW on side 1
    Then the remedial action "close_be1_be3_1" is used in preventive
    Then the remedial action "pst_be" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be -2 in preventive
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after PRA should be -511.0 MW on side 1
    Then the worst margin is 39.0 MW

  @fast @rao @preventive-only
  Scenario: US 19.11.6: Limit elementary actions with PST also used in preventive
    Given network file is "epic19/small-network-2P-open-twin-lines.uct"
    Given crac file is "epic19/small-crac-with-max-3-elementary-actions-pst-in-curative.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete.json"
    When I launch rao
    Then 1 remedial actions are used in preventive
    Then the remedial action "pst_be_prev" is used in preventive
    Then the tap of PstRangeAction "pst_be_prev" should be -5 in preventive
    # The initial tap of the CRA is set to 0 but the preventive optimization moves the tap to -5
    # We check that the optimized curative tap is -8 and not -3
    Then 1 remedial actions are used after "co1_fr1_fr3_1" at "curative"
    Then the remedial action "pst_be_cur" is used after "co1_fr1_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be_cur" should be -8 after "co1_fr1_fr3_1" at "curative"
    Then the worst margin is 1.0 MW

  @fast @rao @preventive-only @multi-curative
  Scenario: US 19.11.7: Limit elementary actions with PST and topology actions in multi-curative situation
    Given network file is "epic19/small-network-2P-open-twin-lines.uct"
    Given crac file is "epic19/small-crac-with-max-elementary-actions-topo-and-pst-2-curative-instants.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete.json"
    When I launch rao
    Then 1 remedial actions are used after "co1_fr1_fr3_1" at "curative1"
    Then the remedial action "pst_be" is used after "co1_fr1_fr3_1" at "curative1"
    Then the tap of PstRangeAction "pst_be" should be -1 after "co1_fr1_fr3_1" at "curative1"
    Then 2 remedial actions are used after "co1_fr1_fr3_1" at "curative2"
    Then the remedial action "close_be1_be3_1" is used after "co1_fr1_fr3_1" at "curative2"
    Then the remedial action "pst_be" is used after "co1_fr1_fr3_1" at "curative2"
    Then the tap of PstRangeAction "pst_be" should be -2 after "co1_fr1_fr3_1" at "curative2"

  @fast @rao @preventive-only
  Scenario: US 19.11.8: Limit elementary actions for multiple TSOs
    Given network file is "epic19/small-network-2P.uct"
    Given crac file is "epic19/small-crac-with-max-elementary-actions-multiple-tsos.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete.json"
    When I launch rao
    Then 2 remedial actions are used in preventive
    Then the remedial action "pst_be" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be -8 in preventive
    Then the remedial action "pst_fr" is used in preventive
    Then the tap of PstRangeAction "pst_fr" should be 10 in preventive
    Then the worst margin is 7.0 MW

  @fast @rao @second-preventive
  Scenario: US.19.11.9: Same case with global optimization: should have the same results
    Given network file is "epic19/small-network-2P.uct"
    Given crac file is "epic19/SL_ep19us11case9.json"
    Given configuration file is "epic19/RaoParameters_19_11_9.json"
    When I launch rao
    Then 1 remedial actions are used in preventive
    # It doesn't really matter what's the preventive tap, as long as it's <= -6, in order for the tap -16
    # to be achievable in curative (the PST has a maximum variation of -10 taps from preventive to curative), allowing
    # the minimum margin to be maximized (on the curative limiting element).
    # However, minimizing PST usage (due to penalty costs) should lead to -6 in preventive
    Then the tap of PstRangeAction "pst_be" should be -6 in preventive
    Then the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_fr" should be 13 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is -218.5 A

