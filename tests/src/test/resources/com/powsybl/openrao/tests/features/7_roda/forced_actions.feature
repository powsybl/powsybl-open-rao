# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: Forced actions in RODA
  @fast @rao @dc @redispatching @forced-actions
  Scenario: ForcedActions.1: A clone of test 4.1.2, with optimal topological PRA (Close Line NL2 BE3 2) of second TS forced in parameters
    Given network files are in folder "epic93/TestCases_93_2_2"
    Given crac file is "epic93/cbcora_93_2_2.xml"
    Given ics static file is "epic93/static_93_2_1.csv"
    Given ics series file is "epic93/series_93_2_1.csv"
    Given configuration file is "roda/conf_force_actions.json"
    Given time-coupled rao inputs for CORE are:
      | Timestamp        | Network          |
      | 2019-01-08 00:30 | 12Nodes_0030.uct |
      | 2019-01-08 01:30 | 12Nodes_0130.uct |
    When I launch roda
    When I export marmot results to "raoresults/ForcedActions.zip"
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