# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 93.2: Time-coupled generator constraints with MARMOT with ICS files
  Presentation of the US
  ----------------------

  Simple time-coupled situations on a few timestamps illustrating fundamental concepts:
  - multiple FastRao iterations
  - multiple MARMOT iterations
  - taking into account generator constraints: Pmin, Pmax, lead time, gradients
  - application of preventive range actions
  - application of preventive network actions
  - application of curative range actions
  - application of curative network actions

  Tests 2 -> 4.b : 2-timestamp situation with 00:30 secure network situation, 01:30 overloaded network situation.

  N.B: ICS costs are in FBCracCreationParameters, for default see TimeCoupledRaoSteps.DEFAULT_CRAC_CREATION_PARAMETERS_PATH

  ----------------------

  @fast @rao @dc @redispatching @marmot @costly @megawatt
  Scenario: US 93.2.1: Test simple upward gradient
    Redispatching actions are only necessary at 01:30.
    They're applied as soon as 00:30 because of upward gradient on BBE1AA1
    They're applied at 02:30 because of upward gradient on DDE1AA1
    Given network files are in folder "epic93/TestCases_93_2_1"
    Given crac file is "epic93/cbcora_93_2_1.xml"
    Given ics static file is "epic93/static_93_2_1.csv"
    Given ics series file is "epic93/series_93_2_1.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given time-coupled rao inputs for CORE are:
      | Timestamp        | Network          |
      | 2019-01-08 00:30 | 12Nodes_0030.uct |
      | 2019-01-08 01:30 | 12Nodes_0130.uct |
      | 2019-01-08 02:30 | 12Nodes_0230.uct |
      | 2019-01-08 03:30 | 12Nodes_0330.uct |
    When I launch marmot
    # Timestamp 00:30: 0 (activation) + 10 * 54 MW * 2 (BBE1 and DDE1) (variation) = 108
    Then the functional cost for timestamp "2019-01-08 00:30" is 1080
    Then the total cost for timestamp "2019-01-08 00:30" is 1080
    Then the preventive power of generator "RO_RA_00001_BBE1AA1_GENERATOR" at timestamp "2019-01-08 00:30" is 304.0 MW
    Then the preventive power of generator "RO_RA_00002_DDE1AA1_GENERATOR" at timestamp "2019-01-08 00:30" is 196.0 MW
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 00:30" is 435.18 MW
    # Timestamp 01:30: 0 (activation) + 10 * 204 MW * 2 (BBE1 and DDE1) (variation) = 4080
    Then the functional cost for timestamp "2019-01-08 01:30" is 4080
    Then the total cost for timestamp "2019-01-08 01:30" is 4080
    Then the preventive power of generator "RO_RA_00001_BBE1AA1_GENERATOR" at timestamp "2019-01-08 01:30" is 454.0 MW
    Then the preventive power of generator "RO_RA_00002_DDE1AA1_GENERATOR" at timestamp "2019-01-08 01:30" is 46.0 MW
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 01:30" is 10.18 MW
    # Timestamp 02:30: 0 (activation) + 10 * 54 MW * 2 (BBE1 and DDE1) (variation) = 1080
    Then the functional cost for timestamp "2019-01-08 02:30" is 1080
    Then the total cost for timestamp "2019-01-08 02:30" is 1080
    Then the preventive power of generator "RO_RA_00001_BBE1AA1_GENERATOR" at timestamp "2019-01-08 02:30" is 304.0 MW
    Then the preventive power of generator "RO_RA_00002_DDE1AA1_GENERATOR" at timestamp "2019-01-08 02:30" is 196.0 MW
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 02:30" is 435.18 MW
    # Timestamp 03:30: 0
    Then the functional cost for timestamp "2019-01-08 03:30" is 0
    Then the total cost for timestamp "2019-01-08 03:30" is 0
    Then the preventive power of generator "RO_RA_00001_BBE1AA1_GENERATOR" at timestamp "2019-01-08 03:30" is 250.0 MW
    Then the preventive power of generator "RO_RA_00002_DDE1AA1_GENERATOR" at timestamp "2019-01-08 03:30" is 250.0 MW
    Then the remedial action "RO_RA_00001_RD" is not used at timestamp "2019-01-08 03:30" in preventive
    Then the remedial action "RO_RA_00002_RD" is not used at timestamp "2019-01-08 03:30" in preventive
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 03:30" is 408.18 MW
    Then the total cost for all timestamps is 6240

  @fast @rao @dc @redispatching @marmot @costly @megawatt
  Scenario: US 93.2.2: Topological action and redispatching solve overload
    At 01:30, MNEC is overloaded. During topological optimization, 2 runs of FastRAO are performed to resolve MNEC constraint,
    resulting in the application of topological action PRA_topo 1 at 01:30.
    Given network files are in folder "epic93/TestCases_93_2_2"
    Given crac file is "epic93/cbcora_93_2_2.xml"
    Given ics static file is "epic93/static_93_2_1.csv"
    Given ics series file is "epic93/series_93_2_1.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given time-coupled rao inputs for CORE are:
      | Timestamp        | Network          |
      | 2019-01-08 00:30 | 12Nodes_0030.uct |
      | 2019-01-08 01:30 | 12Nodes_0130.uct |
    When I launch marmot
     # Timestamp 00:30: 0
    Then the functional cost for timestamp "2019-01-08 00:30" is 0
    Then the total cost for timestamp "2019-01-08 00:30" is 0
    Then the remedial action "RO_RA_00001_RD" is not used at timestamp "2019-01-08 00:30" in preventive
    Then the remedial action "RO_RA_00002_RD" is not used at timestamp "2019-01-08 00:30" in preventive
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 00:30" is 523.04 MW
     # Timestamp 01:30: 0 (activation) + 10 * 26 MW * 2 (BBE1 and DDE1) (variation) = 520
    Then the functional cost for timestamp "2019-01-08 01:30" is 520
    Then the total cost for timestamp "2019-01-08 01:30" is 520
    Then the preventive power of generator "RO_RA_00001_BBE1AA1_GENERATOR" at timestamp "2019-01-08 01:30" is 276.0 MW
    Then the preventive power of generator "RO_RA_00002_DDE1AA1_GENERATOR" at timestamp "2019-01-08 01:30" is 224.0 MW
    Then the remedial action "Close Line NL2 BE3 2" is used at timestamp "2019-01-08 01:30" in preventive
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 01:30" is 259.8 MW
    Then the functional cost for all timestamps is 520
    Then the total cost for all timestamps is 520

  @fast @rao @dc @redispatching @marmot @costly @megawatt
  Scenario: US 93.2.3: PST and redispatching solve overload
    Given network files are in folder "epic93/TestCases_93_2_1"
    Given crac file is "epic93/cbcora_93_2_3.xml"
    Given ics static file is "epic93/static_93_2_1.csv"
    Given ics series file is "epic93/series_93_2_1.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given time-coupled rao inputs for CORE are:
      | Timestamp        | Network          |
      | 2019-01-08 00:30 | 12Nodes_0030.uct |
      | 2019-01-08 01:30 | 12Nodes_0130.uct |
    When I launch marmot
     # Timestamp 00:30: 0
    Then the functional cost for timestamp "2019-01-08 00:30" is 0
    Then the total cost for timestamp "2019-01-08 00:30" is 0
    Then the remedial action "RO_RA_00001_RD" is not used at timestamp "2019-01-08 00:30" in preventive
    Then the remedial action "RO_RA_00002_RD" is not used at timestamp "2019-01-08 00:30" in preventive
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 00:30" is 408.18 MW
     # Timestamp 01:30: 0 (activation) + 10 * 95 MW * 2 (BBE1 and DDE1) (variation) = 1900
    Then the functional cost for timestamp "2019-01-08 01:30" is 1900
    Then the total cost for timestamp "2019-01-08 01:30" is 1900
    Then the preventive power of generator "RO_RA_00001_BBE1AA1_GENERATOR" at timestamp "2019-01-08 01:30" is 345.0 MW
    Then the preventive power of generator "RO_RA_00002_DDE1AA1_GENERATOR" at timestamp "2019-01-08 01:30" is 155.0 MW
    Then the remedial action "PRA_PST_BE" is used at timestamp "2019-01-08 01:30" in preventive
    And the preventive tap of PstRangeAction "PRA_PST_BE" at timestamp "2019-01-08 01:30" should be -5
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 01:30" is 10.07 MW
    Then the functional cost for all timestamps is 1900
    Then the total cost for all timestamps is 1900

  @fast @rao @dc @redispatching @marmot @costly @megawatt
  Scenario: US 93.2.4: Curative topological action taken into account
    Curative topo action and redispatching actions solved 01:30 overload during topological optimization, and 
    were applied during sensi computation in global range actions optimization.
    Here, global range actions optimization doesn't change results.
    Given network files are in folder "epic93/TestCases_93_2_4"
    Given crac file is "epic93/cbcora_93_2_4.xml"
    Given ics static file is "epic93/static_93_2_1.csv"
    Given ics series file is "epic93/series_93_2_1.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given time-coupled rao inputs for CORE are:
      | Timestamp        | Network          |
      | 2019-01-08 00:30 | 12Nodes_0030.uct |
      | 2019-01-08 01:30 | 12Nodes_0130.uct |
    When I launch marmot
    # Timestamp 00:30: 0
    Then the functional cost for timestamp "2019-01-08 00:30" is 0
    Then the total cost for timestamp "2019-01-08 00:30" is 0
    Then the remedial action "RO_RA_00001_RD" is not used at timestamp "2019-01-08 00:30" in preventive
    Then the remedial action "RO_RA_00002_RD" is not used at timestamp "2019-01-08 00:30" in preventive
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 00:30" is 428.45 MW
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - N-1 BE-FR 1 - curative" for timestamp "2019-01-08 00:30" is 408.18 MW
    # Timestamp 01:30: 0 (activation) + 10 * 89 MW * 2 (BBE1 and DDE1) (variation) = 1780
    Then the functional cost for timestamp "2019-01-08 01:30" is 1780
    Then the total cost for timestamp "2019-01-08 01:30" is 1780
    Then the preventive power of generator "RO_RA_00001_BBE1AA1_GENERATOR" at timestamp "2019-01-08 01:30" is 339.0 MW
    Then the preventive power of generator "RO_RA_00002_DDE1AA1_GENERATOR" at timestamp "2019-01-08 01:30" is 161.0 MW
    Then the remedial action "Close Line NL2 BE3 2" is used at timestamp "2019-01-08 01:30" after "N-1 BE-FR 1" at "curative"
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 01:30" is 9.88 MW
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - N-1 BE-FR 1 - curative" for timestamp "2019-01-08 01:30" is 276.83 MW
    Then the functional cost for all timestamps is 1780
    Then the total cost for all timestamps is 1780

  @fast @rao @dc @redispatching @marmot @costly @megawatt
  Scenario: US 93.2.4.bis: Curative PST and preventive redispatching
    Similar to 93.2.3, but PST is curative. Overload is on curative cnec.
    Topological optimization finds a different result for redispatching: redispatching is optimized in basecase, before
    curative PST is optimized, whereas in global optimization curative PST is applied in sensi computations.
    Given network files are in folder "epic93/TestCases_93_2_4"
    Given crac file is "epic93/cbcora_93_2_4-b.xml"
    Given ics static file is "epic93/static_93_2_1.csv"
    Given ics series file is "epic93/series_93_2_1.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given time-coupled rao inputs for CORE are:
      | Timestamp        | Network          |
      | 2019-01-08 00:30 | 12Nodes_0030.uct |
      | 2019-01-08 01:30 | 12Nodes_0130.uct |
    When I launch marmot
    # Timestamp 00:30: 0
    Then the functional cost for timestamp "2019-01-08 00:30" is 0
    Then the total cost for timestamp "2019-01-08 00:30" is 0
    Then the remedial action "RO_RA_00001_RD" is not used at timestamp "2019-01-08 00:30" in preventive
    Then the remedial action "RO_RA_00002_RD" is not used at timestamp "2019-01-08 00:30" in preventive
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - N-1 BE-FR 1 - curative" for timestamp "2019-01-08 00:30" is 408.18 MW
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 00:30" is 428.45 MW
    # Timestamp 01:30: 0 (activation) + 10 * 95 MW * 2 (BBE1 and DDE1) (variation) = 1900
    Then the functional cost for timestamp "2019-01-08 01:30" is 1900
    Then the total cost for timestamp "2019-01-08 01:30" is 1900
    Then the preventive power of generator "RO_RA_00001_BBE1AA1_GENERATOR" at timestamp "2019-01-08 01:30" is 345.0 MW
    Then the preventive power of generator "RO_RA_00002_DDE1AA1_GENERATOR" at timestamp "2019-01-08 01:30" is 155.0 MW
    Then the remedial action "CRA_PST_BE" is used at timestamp "2019-01-08 01:30" after "N-1 BE-FR 1" at "curative"
    And the tap of PstRangeAction "CRA_PST_BE" at timestamp "2019-01-08 01:30" after "N-1 BE-FR 1" at "curative" should be -5
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - N-1 BE-FR 1 - curative" for timestamp "2019-01-08 01:30" is 10.07 MW
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 01:30" is 12.64 MW
    Then the functional cost for all timestamps is 1900
    Then the total cost for all timestamps is 1900

  @fast @rao @dc @redispatching @marmot @costly @megawatt
  Scenario: US 93.2.5: 2 iterations of MARMOT via MNEC overloads
    At 01:30, MNEC is overloaded. During topological optimization, 2 runs of FastRAO are performed to solve MNEC constraint,
    resulting in the application of topological action PRA_topo 1 and redispatching actions at 01:30.
    Nevertheless these redispatching actions have gradient constraints; upon entering global optimization, topological optimization
    solution is no longer viable. First MIP iteration creates constraints on MNEC at 00:30. 
    Since MNEC was only previously limiting at 01:30, it was not yet considered in MIP's considered cnecs, and MIP performs 2 iterations. 
    Given network files are in folder "epic93/TestCases_93_2_5"
    Given crac file is "epic93/cbcora_93_2_5.xml"
    Given ics static file is "epic93/static_93_2_5.csv"
    Given ics series file is "epic93/series_93_2_5.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given time-coupled rao inputs for CORE are:
      | Timestamp        | Network          |
      | 2019-01-08 00:30 | 12Nodes_0030.uct |
      | 2019-01-08 01:30 | 12Nodes_0130.uct |
    When I launch marmot
    # Timestamp 00:30: 0 (activation) + 10 * 94 MW * 2 (BBE1 and DDE1) (variation) = 1880
    Then the functional cost for timestamp "2019-01-08 00:30" is 1880
    Then the total cost for timestamp "2019-01-08 00:30" is 1880
    Then the preventive power of generator "RO_RA_00001_BBE1AA1_GENERATOR" at timestamp "2019-01-08 00:30" is 344.0 MW
    Then the preventive power of generator "RO_RA_00002_DDE1AA1_GENERATOR" at timestamp "2019-01-08 00:30" is 156.0 MW
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 00:30" is 166.91 MW
    # Timestamp 01:30: 0 (activation) + 10 * 104 MW * 2 (BBE1 and DDE1) (variation) = 2080
    Then the functional cost for timestamp "2019-01-08 01:30" is 2080
    # + 22.93 MW * 1000 (overload of 22.93 MW, shifted violation penalty of 1000)
    Then the total cost for timestamp "2019-01-08 01:30" is 25010.25
    Then the preventive power of generator "RO_RA_00001_BBE1AA1_GENERATOR" at timestamp "2019-01-08 01:30" is 354.0 MW
    Then the preventive power of generator "RO_RA_00002_DDE1AA1_GENERATOR" at timestamp "2019-01-08 01:30" is 146.0 MW
    Then the remedial action "Close Line NL2 BE3 2" is used at timestamp "2019-01-08 01:30" in preventive
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 01:30" is -22.93 MW
    Then the functional cost for all timestamps is 3960
    Then the total cost for all timestamps is 26890.25

  @fast @rao @dc @redispatching @marmot @costly @megawatt
  Scenario: US 93.2.6: Lead time and Pmin.
  The generator involved in the Belgian redispatching action has a 15 min lead time and must be operated at 1000 MW at
  2:30. Because of its lead time, it must be switched on at 1:30 and operated at its Pmin (100 MW), leading to a
  supplementary expense of 1000 in remedial actions for this generator. For grid balancing reasons, the French
  generator must be switched off thus doubling the expenses.
  StartUp is allowed for BBE2, Shutdown is allowed for FFR1.
    Given network files are in folder "epic93/TestCases_93_2_6"
    Given crac file is "epic93/cbcora_93_2_6.xml"
    Given ics static file is "epic93/static_93_2_6.csv"
    Given ics series file is "epic93/series_93_2_6.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given time-coupled rao inputs for CORE are:
      | Timestamp        | Network         |
      | 2019-01-08 00:30 | 2Nodes_0030.uct |
      | 2019-01-08 01:30 | 2Nodes_0130.uct |
      | 2019-01-08 02:30 | 2Nodes_0230.uct |
    When I launch marmot
    # Timestamp 00:30:
    Then the functional cost for timestamp "2019-01-08 00:30" is 0.0
    Then the preventive power of generator "RD_RA_BE_BBE2AA1_GENERATOR" at timestamp "2019-01-08 00:30" is 0.0 MW
    Then the preventive power of generator "RD_RA_FR_FFR1AA1_GENERATOR" at timestamp "2019-01-08 00:30" is 1000.0 MW
    # Timestamp 01:30: 10 * 1000 MW * 2 generators
    Then the functional cost for timestamp "2019-01-08 01:30" is 2000.0
    Then the preventive power of generator "RD_RA_BE_BBE2AA1_GENERATOR" at timestamp "2019-01-08 01:30" is 100.0 MW
    Then the preventive power of generator "RD_RA_FR_FFR1AA1_GENERATOR" at timestamp "2019-01-08 01:30" is 900.0 MW
    # Timestamp 02:30: 10 * 1000 MW * 2 generators
    Then the functional cost for timestamp "2019-01-08 02:30" is 20000.0
    Then the functional cost for all timestamps is 22000.0
    Then the preventive power of generator "RD_RA_BE_BBE2AA1_GENERATOR" at timestamp "2019-01-08 02:30" is 1000.0 MW
    Then the preventive power of generator "RD_RA_FR_FFR1AA1_GENERATOR" at timestamp "2019-01-08 02:30" is 0.0 MW

    # TODO to be modified when check on generator constraint is performed at import
  @fast @rao @dc @redispatching @marmot @costly @megawatt
  Scenario: US 93.2.7: Inconsistent data : BE generator's program shuts down even though shutDown is not allowed,
    FR generator's program starts up even though startUp is not allowed.
    Given network files are in folder "epic93/TestCases_93_2_6"
    Given crac file is "epic93/cbcora_93_2_6.xml"
    Given ics static file is "epic93/static_93_2_6.csv"
    Given ics series file is "epic93/series_93_2_7.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given time-coupled rao inputs for CORE are:
      | Timestamp        | Network         |
      | 2019-01-08 00:30 | 2Nodes_0030.uct |
      | 2019-01-08 01:30 | 2Nodes_0130.uct |
      | 2019-01-08 02:30 | 2Nodes_0230.uct |
    When I launch marmot
    Then its time coupled security status should be "UNSECURED"
