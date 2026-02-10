# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 2.2: Optimize PST tap within given ranges

  ## TODO: rename everything to start with US2.1
  ## TODO: should we add @min-max-margin and @secure-flow?
  ## TODO: should we rename occurences of positive margin to secure-flow?

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 2.2.1: Optimization monitoring only the PST
  Basic case, the only RA is a PST range action - the CNECs are only defined for one network element.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic2/SL_ep2us2case1.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 778.0 A
    Then the margin on cnec "BBE2AA1  BBE3AA1  1 - preventive" after PRA should be 778.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 15 in preventive

  @fast @rao @ac @preventive-only @ampere
  Scenario: US 2.2.2: Trade-off between various constraints
  Same as US 2.2.1, except that CNECs are defined on two additional network elements.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic2/SL_ep2us2case2.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 39.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 39.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive

  @fast @rao @ac @preventive-only @ampere
  Scenario: US 2.2.3: Unsecure solution
  Same as US 2.2.2, except that the CNEC thresholds are more restrictive.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic2/SL_ep2us2case3.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the worst margin is -37.0 A
    Then the margin on cnec "BBE2AA1  BBE3AA1  1 - preventive" after PRA should be -37.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 2 in preventive

  @fast @rao @ac @preventive-only @ampere
  Scenario: US 2.2.4: Range intersection for one PST
  Same as the previous cases, except that the max value of the absolute range of the RA is restricted from 16 to 3,
    and a relativeToInitialNetwork range is added (0 to 10).
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic2/SL_ep2us2case4.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 26.0 A
    Then the margin on cnec "BBE2AA1  BBE3AA1  1 - preventive" after PRA should be 26.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 3 in preventive

  @fast @rao @ac @preventive-only @ampere
  Scenario: US 2.2.5: Handle 2 PSTs
  Two preventive range actions are available (on two different PSTs).
    Given network file is "common/TestCase12Nodes2PSTs.uct"
    Given crac file is "epic2/SL_ep2us2case5.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 459.0 A
    Then 2 remedial actions are used in preventive
    Then the remedial action "PRA_PST_DE" is used in preventive
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_DE" should be -10 in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 16 in preventive
