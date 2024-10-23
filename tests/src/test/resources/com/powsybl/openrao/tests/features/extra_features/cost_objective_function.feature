# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

#WIP
Feature: New objective function using minimal cost

  @fast @rao @mock @ac @preventive-only
  Scenario: Preventive RA with minimal cost
    Given network file is "common/12NodesProdFR_3PST.uct"
    Given crac file is "extra_features/crac-cost-0.json"
    Given configuration file is "common/RaoParameters_minCost_megawatt_ac.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the remedial action "pst_be - TS0" is used in preventive
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive - TS0" after PRA should be 0 A
    And the tap of PstRangeAction "pst_be - TS0" should be 6 in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: Preventive RA with max min margin
    Given network file is "common/12NodesProdFR_3PST.uct"
    Given crac file is "extra_features/crac-cost-0.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_ac.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the remedial action "pst_be - TS0" is used in preventive
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive - TS0" after PRA should be 70 A
    And the tap of PstRangeAction "pst_be - TS0" should be 16 in preventive
