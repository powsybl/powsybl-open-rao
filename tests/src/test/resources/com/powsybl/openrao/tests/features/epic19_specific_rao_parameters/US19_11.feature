# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 19.11: Handle maximum number of elementary actions per TSO

  @fast @rao @second-preventive
  Scenario: US.19.11.9: Reference case with optimal solution (no global optimization).
    Given network file is "epic19/small-network-2P.uct"
    Given crac file is "epic19/SL_ep19us11case9.json"
    Given configuration file is "epic19/RaoParameters_19_11_9.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_be" should be -10 in preventive
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 13 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -218.5 A

  @fast @rao @second-preventive
  Scenario: US.19.11.9.bis: Same case with global optimization: should have the same results
    Given network file is "epic19/small-network-2P.uct"
    Given crac file is "epic19/SL_ep19us11case9.json"
    Given configuration file is "epic19/RaoParameters_19_11_9_bis.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 13 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -218.5 A

