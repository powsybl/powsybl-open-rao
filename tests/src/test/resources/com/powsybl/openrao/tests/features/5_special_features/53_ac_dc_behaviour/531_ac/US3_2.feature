# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 3.2: Handle CNEC monitored in only one direction in the optimization
  This feature tests the behaviour of the algorithm when only either the "min" or the "max" threshold is defined
  on the CNEC (but not both) in the json CRAC.

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 3.2.1: topological RA, direct CNEC unsecure initially
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic3/SL_ep3us2_topo_direct.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 56.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 56.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "Open FR_DE" is used in preventive

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 3.2.2: topological RA, opposite CNEC secure initially
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic3/SL_ep3us2_topo_opposite.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 779.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 779.0 A
    Then 0 remedial actions are used in preventive

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 3.2.3: PST range action, direct CNEC
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic3/SL_ep3us2_pst_direct.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the worst margin is -416.1 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -416.1 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PST1" is used in preventive
    Then the tap of PstRangeAction "PST1" should be -16 in preventive

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 3.2.4: PST range action, opposite CNEC
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic3/SL_ep3us2_pst_opposite.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 1075.0 A
    Then the margin on cnec "DDE1AA1  DDE2AA1  1 - Contingency FR1 FR3 - outage" after PRA should be 1075.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PST1" is used in preventive
    Then the tap of PstRangeAction "PST1" should be 16 in preventive