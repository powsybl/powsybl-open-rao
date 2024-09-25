# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 10.6: Add minimum relative margin binary variable

  @fast @rao @mock @dc @contingency-scenarios @relative
  Scenario: US 10.6.1: Simple case, with 2 curative states and very low cnec thresholds
    #Check that even with an infeasible linear problem, the RAO goes on.
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "epic10/SL_ep10us6case1.json"
    Given configuration file is "epic10/RaoParameters_relMargin_megawatt.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    When I launch search_tree_rao
    And the worst margin is -1188.90 MW
    And the value of the objective function after CRA should be 1188.90
