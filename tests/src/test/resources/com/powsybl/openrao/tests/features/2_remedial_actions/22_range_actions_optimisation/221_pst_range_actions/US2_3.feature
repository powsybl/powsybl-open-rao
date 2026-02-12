# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 2.3: Combine range PST and NetworkAction optimization

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 2.3.1: standard optimization
  Optimization with one PST range action and one network action available: both are activated.
    Given network file is "epic2/US2-3-case1-standard.uct"
    Given crac file is "epic2/SL_ep2us3_pst_and_topological_remedial_actions.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 2 remedial actions are used in preventive
    Then the remedial action "SelectTapPST43" is used in preventive
    Then the tap of PstRangeAction "SelectTapPST43" should be -16 in preventive
    Then the remedial action "CloseLine322" is used in preventive
    Then the worst margin is 28.6 A
    Then the margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" after PRA should be 28.6 A

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 2.3.2: no network actions involved in the best solution
  Optimization with one PST range action and one network action available: only the PST range action is activated.
    Given network file is "epic2/US2-3-case2.uct"
    Given crac file is "epic2/SL_ep2us3case2.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "SelectTapPST43" is used in preventive
    Then the tap of PstRangeAction "SelectTapPST43" should be -16 in preventive
    Then the worst margin is 204.0 A
    Then the margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" after PRA should be 204.0 A

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 2.3.3: systematic sensitivity computation diverge when testing some Network Actions
  Two network actions available + one PST range action:
  - when testing "Open 432" the cost increases,
  - when testing "Open 141" the sensitivity computation diverges,
  - the PST range action can decrease the cost.
    Given network file is "epic2/US2-3-case3-networkBecomeDivergent.uct"
    Given crac file is "epic2/SL_ep2us3case3.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "SelectTapPST43" is used in preventive
    Then the tap of PstRangeAction "SelectTapPST43" should be 4 in preventive
    Then the worst margin is -820.0 A
    Then the margin on cnec "FFR1AA1  FFR2AA1  1 - preventive" after PRA should be -820.0 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - preventive" after PRA should be -820.0 A
    Then the margin on cnec "FFR4AA1  FFR3AA1  1 - preventive" after PRA should be -206.0 A
    Then the margin on cnec "FFR4AA1  FFR3AA1  2 - preventive" after PRA should be -779.0 A

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 2.3.4: the systematic sensitivity computation diverges at the root leaf of the tree
    Given network file is "epic2/US2-3-case4-networkDiverge.uct"
    Given crac file is "epic2/SL_ep2us3case4.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then the calculation fails
    Then the execution details should be "Initial sensitivity analysis failed"