# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 93.2: power gradient constraints

  @fast @rao @dc @redispatching
  Scenario: US 93.2.1: Test simple gradient
    Given network files are in folder "epic93/TestCases_93_2_1"
    Given crac file is "epic93/cbcora_93_2_1.xml"
    Given ics static file is "epic93/static_93_2_1.csv"
    Given ics series file is "epic93/series_93_2_1.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network          |
      | 2019-01-08 00:30 | 12Nodes_0030.uct |
      | 2019-01-08 01:30 | 12Nodes_0130.uct |
      | 2019-01-08 02:30 | 12Nodes_0230.uct |
      | 2019-01-08 03:30 | 12Nodes_0330.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_93_2_1.zip"
    When I export marmot reports to "reports/reports_93_2_1.txt"
    When I export networks with PRAs to "raoresults/networkWithPras_93_2_1.zip"
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 00:30" is 435.18 MW
    And the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 01:30" is 10.18 MW
    And the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 02:30" is 435.18 MW
    And the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 03:30" is 408.18 MW
    And the functional cost for timestamp "2019-01-08 00:30" is 1080
    And the functional cost for timestamp "2019-01-08 01:30" is 4080
    And the functional cost for timestamp "2019-01-08 02:30" is 1080
    And the functional cost for timestamp "2019-01-08 03:30" is 0
    And the functional cost for all timestamps is 6240
    And the total cost for timestamp "2019-01-08 00:30" is 1080
    And the total cost for timestamp "2019-01-08 01:30" is 4080
    And the total cost for timestamp "2019-01-08 02:30" is 1080
    And the total cost for timestamp "2019-01-08 03:30" is 0
    And the total cost for all timestamps is 6240

  @fast @rao @dc @redispatching
  Scenario: US 93.2.2: Test simple gradient with limiting mnec and topological action
    Given network files are in folder "epic93/TestCases_93_2_2"
    Given crac file is "epic93/cbcora_93_2_2.xml"
    Given ics static file is "epic93/static_93_2_1.csv"
    Given ics series file is "epic93/series_93_2_1.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network          |
      | 2019-01-08 00:30 | 12Nodes_0030.uct |
      | 2019-01-08 01:30 | 12Nodes_0130.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_93_2_2.zip"
    When I export marmot reports to "reports/reports_93_2_2.txt"
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 00:30" is 523.04 MW
    And the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 01:30" is 259.8 MW
    And the functional cost for timestamp "2019-01-08 00:30" is 0
    And the functional cost for timestamp "2019-01-08 01:30" is 520
    And the functional cost for all timestamps is 520
    And the total cost for timestamp "2019-01-08 00:30" is 0
    And the total cost for timestamp "2019-01-08 01:30" is 520
    And the total cost for all timestamps is 520

  @fast @rao @dc @redispatching
  Scenario: US 93.2.3: Test simple gradient, PST also available
    Given network files are in folder "epic93/TestCases_93_2_1"
    Given crac file is "epic93/cbcora_93_2_3.xml"
    Given ics static file is "epic93/static_93_2_1.csv"
    Given ics series file is "epic93/series_93_2_1.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network          |
      | 2019-01-08 00:30 | 12Nodes_0030.uct |
      | 2019-01-08 01:30 | 12Nodes_0130.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_93_2_3.zip"
    When I export marmot reports to "reports/reports_93_2_3.txt"
    When I export networks with PRAs to "raoresults/networkWithPras_93_2_3.zip"
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 00:30" is 408.18 MW
    And the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 01:30" is 10.07 MW
    And the functional cost for timestamp "2019-01-08 00:30" is 0
    And the functional cost for timestamp "2019-01-08 01:30" is 1900
    And the functional cost for all timestamps is 1900
    And the total cost for timestamp "2019-01-08 00:30" is 0
    And the total cost for timestamp "2019-01-08 01:30" is 1900
    And the total cost for all timestamps is 1900

  @fast @rao @dc @redispatching
  Scenario: US 93.2.4: Test simple gradient, applied curative topo action
    Given network files are in folder "epic93/TestCases_93_2_4"
    Given crac file is "epic93/cbcora_93_2_4.xml"
    Given ics static file is "epic93/static_93_2_1.csv"
    Given ics series file is "epic93/series_93_2_1.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network          |
      | 2019-01-08 00:30 | 12Nodes_0030.uct |
      | 2019-01-08 01:30 | 12Nodes_0130.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_93_2_4.zip"
    When I export marmot reports to "reports/reports_93_2_4.txt"
    And the optimized margin on "NNL2AA1  BBE3AA1  1 - N-1 BE-FR 1 - curative" for timestamp "2019-01-08 00:30" is 408.18 MW
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 00:30" is 428.45 MW
    And the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 01:30" is 9.88 MW
    And the optimized margin on "NNL2AA1  BBE3AA1  1 - N-1 BE-FR 1 - curative" for timestamp "2019-01-08 01:30" is 276.83 MW
    And the functional cost for timestamp "2019-01-08 00:30" is 0
    And the functional cost for timestamp "2019-01-08 01:30" is 1780
    And the functional cost for all timestamps is 1780
    And the total cost for timestamp "2019-01-08 00:30" is 0
    And the total cost for timestamp "2019-01-08 01:30" is 1780
    And the total cost for all timestamps is 1780


  @fast @rao @dc @redispatching
  Scenario: US 93.2.4-b: Test simple gradient, applied curative range action
    Given network files are in folder "epic93/TestCases_93_2_4"
    Given crac file is "epic93/cbcora_93_2_4-b.xml"
    Given ics static file is "epic93/static_93_2_1.csv"
    Given ics series file is "epic93/series_93_2_1.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network          |
      | 2019-01-08 00:30 | 12Nodes_0030.uct |
      | 2019-01-08 01:30 | 12Nodes_0130.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_93_2_4-b.zip"
    When I export marmot reports to "reports/reports_93_2_4-b.txt"
    And the optimized margin on "NNL2AA1  BBE3AA1  1 - N-1 BE-FR 1 - curative" for timestamp "2019-01-08 00:30" is 408.18 MW
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 00:30" is 428.45 MW
    And the optimized margin on "NNL2AA1  BBE3AA1  1 - N-1 BE-FR 1 - curative" for timestamp "2019-01-08 01:30" is 10.07 MW
    And the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 01:30" is 12.64 MW
    And the functional cost for timestamp "2019-01-08 00:30" is 0
    And the functional cost for timestamp "2019-01-08 01:30" is 1900
    And the functional cost for all timestamps is 1900
    And the total cost for timestamp "2019-01-08 00:30" is 0
    And the total cost for timestamp "2019-01-08 01:30" is 1900
    And the total cost for all timestamps is 1900

  @fast @rao @dc @redispatching
  Scenario: US 93.2.5: Test simple gradient with topo action, mnec causes second mip optimization
    Given network files are in folder "epic93/TestCases_93_2_5"
    Given crac file is "epic93/cbcora_93_2_5.xml"
    Given ics static file is "epic93/static_93_2_5.csv"
    Given ics series file is "epic93/series_93_2_5.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network          |
      | 2019-01-08 00:30 | 12Nodes_0030.uct |
      | 2019-01-08 01:30 | 12Nodes_0130.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_93_2_5.zip"
    When I export marmot reports to "reports/reports_93_2_5.txt"
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 00:30" is 166.91 MW
    And the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 01:30" is -22.93 MW
    And the functional cost for timestamp "2019-01-08 00:30" is 1880
    And the functional cost for timestamp "2019-01-08 01:30" is 2080
    And the functional cost for all timestamps is 3960
    And the total cost for timestamp "2019-01-08 00:30" is 1880
    And the total cost for timestamp "2019-01-08 01:30" is 25010.25
    And the total cost for all timestamps is 26890.25