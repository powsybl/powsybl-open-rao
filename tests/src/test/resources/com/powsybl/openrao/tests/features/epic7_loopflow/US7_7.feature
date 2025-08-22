# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 7.7: Handle optimisation unfeasibility with loopflow constraints

  @fast @rao @mock @dc @preventive-only @loopflow
  Scenario: US 7.7.1: Simple search tree RAO with LF limitation and infeasible linear problem
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf_rao_3bis.json"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_dc_lf_false_5_100.json"
    When I launch loopflow search_tree_rao with default loopflow limit as 50.0 percent of pmax
    Then the worst margin is -402.0 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -402.0 MW
    Then the tap of PstRangeAction "PRA_PST_BE" should be -10 in preventive
    And the loopflow threshold on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be 500.0 MW
    And the initial loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" should be -391.0 MW
    And the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -490.0 MW