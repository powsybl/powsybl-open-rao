# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 10.1: Linear RAO with relative margin

  @fast @rao @mock @dc @preventive-only @relative
  Scenario: US 10.1.1: unsecured case
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic10/ls_relative_margin_unsecure.json"
    Given configuration file is "epic10/RaoParameters_relMargin_megawatt.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    When I launch search_tree_rao
    Then its security status should be "UNSECURED"
    And the value of the objective function after CRA should be 281.0
    And the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    And the worst margin is -281.0 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    And the relative margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 2297.1 MW
    And the relative margin on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be 2475.3 MW

  @fast @rao @mock @dc @preventive-only @relative
  Scenario: US 10.1.2: secured case
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic10/ls_relative_margin.json"
    Given configuration file is "epic10/RaoParameters_relMargin_megawatt.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    And the value of the objective function after CRA should be -2385.0
    And the tap of PstRangeAction "PRA_PST_BE" should be -3 in preventive
    And the worst relative margin is 2383.0 MW on cnec "NNL2AA1  BBE3AA1  1 - preventive"
    And the absolute PTDF sum on cnec "NNL2AA1  BBE3AA1  1 - preventive" initially should be 1.455
    And the relative margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 2392.4 MW
    And the absolute PTDF sum on cnec "FFR2AA1  DDE3AA1  1 - preventive" initially should be 1.477
