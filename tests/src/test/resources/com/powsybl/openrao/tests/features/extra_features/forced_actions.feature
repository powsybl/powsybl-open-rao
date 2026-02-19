# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: Forced actions
  @fast @rao @mock @dc @preventive-only @forced-actions
  Scenario: ForcedActions.1: A clone of US 10.2.1, with absolute margins (reference for next scenario)
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic10/cbcora_ep10us2case1.xml"
    Given configuration file is "forced_actions/conf_ep10us2case1_absolute_margins.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    When I launch rao at "2019-01-08 12:00"
    Then its security status should be "SECURED"
    And 2 remedial actions are used in preventive
    And the value of the objective function before optimisation should be 114.36
    And the value of the objective function after CRA should be -241.74
    And the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    And the remedial action "Open FR1 FR2" is used in preventive
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 241.74 MW
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 1241.74 MW
    And the margin on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be 3088.15 MW
    And the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 3588.15 MW

  @fast @rao @mock @dc @preventive-only @forced-actions
  Scenario: ForcedActions.2: A clone of previous scenario, but with preventive optimal RAs forced in parameters
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic10/cbcora_ep10us2case1.xml"
    Given configuration file is "forced_actions/conf_ep10us2case1_forced_actions.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    When I launch rao at "2019-01-08 12:00"
    Then its security status should be "SECURED"
    And 0 remedial actions are used in preventive
    And the value of the objective function before optimisation should be -241.74
    And the value of the objective function after CRA should be -241.74
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 241.74 MW
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 1241.74 MW
    And the margin on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be 3088.15 MW
    And the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 3588.15 MW

  @fast @rao @mock @dc @preventive-only @forced-actions
  Scenario: ForcedActions.3: A clone of previous scenario, but with FastRAO
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic10/cbcora_ep10us2case1.xml"
    Given configuration file is "forced_actions/conf_ep10us2case1_forced_actions.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    When I launch fast rao at "2019-01-08 12:00"
    Then its security status should be "SECURED"
    And 0 remedial actions are used in preventive
    And the value of the objective function before optimisation should be -241.74
    And the value of the objective function after CRA should be -241.74
    And the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 241.74 MW
    And the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 1241.74 MW
    And the margin on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be 3088.15 MW
    And the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 3588.15 MW

  @fast @rao @dc @redispatching @preventive-only @forced-actions
  Scenario: ForcedActions.4: A clone of US 93.1.2, with optimal PRAs forced in parameters
    Given network file is "epic93/2Nodes.uct"
    Given crac file is "epic93/crac-93-1-1.json"
    Given configuration file is "forced_actions/conf_force_actions_4.json"
    When I launch rao
    And the initial margin on cnec "cnecFr1Fr2Preventive" should be 10.0 MW
    Then 0 remedial actions are used in preventive
    And the margin on cnec "cnecFr1Fr2Preventive" after PRA should be 10.0 MW
    And the value of the objective function after PRA should be 0.0

  @fast @rao @dc @redispatching @forced-actions
  Scenario: ForcedActions.5: A clone of US 93.2.2, with optimal topological PRA (Close Line NL2 BE3 2) of second TS forced in parameters
    Given network files are in folder "epic93/TestCases_93_2_2"
    Given crac file is "epic93/cbcora_93_2_2.xml"
    Given ics static file is "epic93/static_93_2_1.csv"
    Given ics series file is "epic93/series_93_2_1.csv"
    Given configuration file is "forced_actions/conf_force_actions_5.json"
    Given intertemporal rao inputs for CORE are:
      | Timestamp        | Network          |
      | 2019-01-08 00:30 | 12Nodes_0030.uct |
      | 2019-01-08 01:30 | 12Nodes_0130.uct |
    When I launch marmot
    When I export marmot results to "raoresults/ForcedActions5.zip"
    Then the initial margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 00:30" is 523.04 MW
    And the initial margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 01:30" is 252.8 MW
    And the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 00:30" is 523.04 MW
    And the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 01:30" is 252.8 MW
    And the functional cost for timestamp "2019-01-08 00:30" is 0
    And the functional cost for timestamp "2019-01-08 01:30" is 0
    And the functional cost for all timestamps is 0
    And the total cost for timestamp "2019-01-08 00:30" is 0
    And the total cost for timestamp "2019-01-08 01:30" is 0
    And the total cost for all timestamps is 0