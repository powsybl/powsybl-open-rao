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
    When I launch search_tree_rao
    # Best theoretical option is to move the PST to tap -16 (-33 MW)
    # With only three elementary actions, the best option is to move the PST to tap -3 (-462 MW)
    Then 1 remedial actions are used in preventive
    And the initial flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" should be -561.0 MW
    And the remedial action "pst_be" is used in preventive
    And the tap of PstRangeAction "pst_be" should be -3 in preventive
    And the flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after PRA should be -462.0 MW
    And the worst margin is 38 MW

  @fast @rao @preventive-only
  Scenario: US 19.11.2: Limit taps on PST with a maximum number of 7 elementary actions
    Given network file is "epic19/small-network-2P.uct"
    Given crac file is "epic19/small-crac-with-max-7-elementary-actions-pst.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete.json"
    When I launch search_tree_rao
    # Best theoretical option is to move the PST to tap -16 (-33 MW)
    # With only three elementary actions, the best option is to move the PST to tap -7 (-329 MW)
    Then 1 remedial actions are used in preventive
    And the initial flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" should be -561.0 MW
    And the remedial action "pst_be" is used in preventive
    And the tap of PstRangeAction "pst_be" should be -7 in preventive
    And the flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after PRA should be -329.0 MW
    And the worst margin is 171.0 MW

  @fast @rao @preventive-only
  Scenario: US 19.11.3: Select less efficient network action because it has less elementary actions
    Given network file is "epic19/small-network-2P-open-twin-lines.uct"
    Given crac file is "epic19/small-crac-with-max-1-elementary-action-topo.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete.json"
    When I launch search_tree_rao
    # Best theoretical option is to close both line BE1-BE3-1 and line BE1-BE3-2 (-561 MW)
    # With only three elementary actions, the best option is to simply close line BE1-BE3-1 (-572 MW)
    Then 1 remedial actions are used in preventive
    And the initial flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" should be -596.0 MW
    And the remedial action "close_be1_be3_1" is used in preventive
    And the flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after PRA should be -572.0 MW
    And the worst margin is 3.0 MW

  @fast @rao @preventive-only
  Scenario: US 19.11.4: Limit elementary actions with topos and PSTs
    Given network file is "epic19/small-network-2P-open-twin-lines.uct"
    Given crac file is "epic19/small-crac-with-max-3-elementary-actions-topo-and-pst.json"
    Given configuration file is "epic19/RaoParameters_dc_discrete.json"
    When I launch search_tree_rao
    # Best theoretical option is to close both lines and move the PST to tap -16
    # With only three elementary actions, the best three possibilities are:
    # - move the PST to tap -3 (-520 MW)
    # - close line BE1-BE3-1 and move the PST to tap -2 (-511 MW -> best option)
    # - close both line BE1-BE3-1 and line BE1-BE3-2 and move the PST to tap -1 (-528 MW)
    Then 2 remedial actions are used in preventive
    And the initial flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" should be -596.0 MW
    And the remedial action "close_be1_be3_1" is used in preventive
    And the remedial action "pst_be" is used in preventive
    And the tap of PstRangeAction "pst_be" should be -2 in preventive
    And the flow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after PRA should be -511.0 MW
    And the worst margin is 39.0 MW

  # TODO: /!\ PST moved in preventive => use previous instant's tap and not initial tap (PST prev + cur) (-5 prev, -8 curatif)
  # TODO [max-elementary-actions]: test with multi-curative
  # TODO: non PST range actions
  # TODO: second prev