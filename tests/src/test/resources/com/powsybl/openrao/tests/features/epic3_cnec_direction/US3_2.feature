# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 3.2: Handle cnec monitored in only one direction in the optimization

  @fast @rao @mock @ac @preventive-only
  Scenario: US 3.2.1: topological ra, direct cnec unsecure initially
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic3/SL_ep3us2_topo_direct.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    And 1 remedial actions are used in preventive
    And the remedial action "Open FR_DE" is used in preventive
    And the worst margin is 56.0 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 56.0 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 3.2.2: topological ra, opposite cnec secure initially
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic3/SL_ep3us2_topo_opposite.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    And 0 remedial actions are used in preventive
    And the worst margin is 779.0 A
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 779.0 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 3.2.3: pst range action, direct cnec
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic3/SL_ep3us2_pst_direct.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then its security status should be "UNSECURED"
    And 1 remedial actions are used in preventive
    And the remedial action "PST1" is used in preventive
    And the tap of PstRangeAction "PST1" should be -16 in preventive
    And the worst margin is -416.1 A
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -416.1 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 3.2.4: pst range action, opposite cnec
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic3/SL_ep3us2_pst_opposite.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    And 1 remedial actions are used in preventive
    And the remedial action "PST1" is used in preventive
    And the tap of PstRangeAction "PST1" should be 16 in preventive
    And the worst margin is 1075.0 A
    And the margin on cnec "DDE1AA1  DDE2AA1  1 - Contingency FR1 FR3 - outage" after PRA should be 1075.0 A