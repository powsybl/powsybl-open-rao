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
    When I launch marmot
    When I export marmot results to "raoresults/results_93_2_1.zip"
    Then the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 00:30" is 426.18 MW
    And the optimized margin on "NNL2AA1  BBE3AA1  1 - preventive" for timestamp "2019-01-08 01:30" is 1.18 MW
    And the functional cost for timestamp "2019-01-08 00:30" is 720
    And the functional cost for timestamp "2019-01-08 01:30" is 3720
    And the functional cost for all timestamps is 4440
    And the total cost for timestamp "2019-01-08 00:30" is 720
    And the total cost for timestamp "2019-01-08 01:30" is 3720
    And the total cost for all timestamps is 4440

#
#  @fast @rao @dc @redispatching @preventive-only
  Scenario: US 93.2.1: Test for CORE IDCC
    Given network files are in folder "20250101-TestCase12Nodes2PSTs"
    Given crac files are in folder "20250101"
#    Given crac file is "epic11/ls_mnec_networkAction_ref.json"
    Given ics static file is "20250101_STATIC_.csv"
    Given ics series file is "20250101_SERIES_.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                | Crac               |
      | 2025-01-01 02:30 | 20250101_0230_TestCase12Nodes2PSTs.uct | 20250101_0230.json |
      | 2025-01-01 03:30 | 20250101_0330_TestCase12Nodes2PSTs.uct | 20250101_0330.json |
      | 2025-01-01 04:30 | 20250101_0430_TestCase12Nodes2PSTs.uct | 20250101_0430.json |
    When I launch marmot
#
#  @fast @rao @dc @redispatching @preventive-only
#  Scenario: US 93.2.2: Test for CORE IDCC 2
#    Given network files are in folder "20240926-FID2-620-v4-10V1001C--00264T-to-10V1001C--00085T"
#    Given crac file is "20240926-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
#    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240926-V001_.csv"
#    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240926-V001_.csv"
#    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D2-20240602-V004_.csv"
#    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
#    Given intertemporal rao inputs are:
#      | Timestamp        | Network                                                         |
#      | 2024-09-26 00:30 | 20240926_0030_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 01:30 | 20240926_0130_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 02:30 | 20240926_0230_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 03:30 | 20240926_0330_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 04:30 | 20240926_0430_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 05:30 | 20240926_0530_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 06:30 | 20240926_0630_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 07:30 | 20240926_0730_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 08:30 | 20240926_0830_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 09:30 | 20240926_0930_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 10:30 | 20240926_1030_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 11:30 | 20240926_1130_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 12:30 | 20240926_1230_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 13:30 | 20240926_1330_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 14:30 | 20240926_1430_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 15:30 | 20240926_1530_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 16:30 | 20240926_1630_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 17:30 | 20240926_1730_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 18:30 | 20240926_1830_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 19:30 | 20240926_1930_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 20:30 | 20240926_2030_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 21:30 | 20240926_2130_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 22:30 | 20240926_2230_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 23:30 | 20240926_2330_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#    When I launch marmot
#
#  @fast @rao @dc @redispatching @preventive-only
#  Scenario: US 93.2.3: Test for CORE IDCC 3
#    Given network files are in folder "20240926-FID2-620-v4-10V1001C--00264T-to-10V1001C--00085T"
#    Given crac file is "20240926-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
#    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240926-V001_.csv"
#    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240926-V001_.csv"
#    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D2-20240602-V004_.csv"
#    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
#    Given intertemporal rao inputs are:
#      | Timestamp        | Network                                                         |
#      | 2024-09-26 00:30 | 20240926_0030_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 01:30 | 20240926_0130_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-09-26 02:30 | 20240926_0230_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#    When I launch marmot
#
#
#  @fast @rao @dc @redispatching @preventive-only
#  Scenario: US 93.2.2: Test for CORE IDCC 1 TS
#    Given network files are in folder "20240926-FID2-620-v4-10V1001C--00264T-to-10V1001C--00085T"
#    Given crac file is "20240926-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
#    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240926-V001_.csv"
#    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240926-V001_.csv"
#    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D2-20240602-V004_.csv"
#    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
#    Given intertemporal rao inputs are:
#      | Timestamp        | Network                                                         |
#      | 2024-09-26 00:30 | 20240926_0030_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#    When I launch marmot
#
#  @fast @rao @dc @redispatching @preventive-only
#  Scenario: US 93.2.4: Test for core idcc 0602
#    Given network files are in folder "20240602-FID2-620-v4-10V1001C--00264T-to-10V1001C--00085T"
#    Given crac file is "20240602-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
#    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240602-V001_.csv"
#    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240602-V001_.csv"
#    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D2-20240602-V004_.csv"
#    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
#    Given intertemporal rao inputs are:
#      | Timestamp        | Network                                                         |
#      | 2024-06-02 00:30 | 20240602_0030_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 01:30 | 20240602_0130_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 02:30 | 20240602_0230_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 03:30 | 20240602_0330_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 04:30 | 20240602_0430_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 05:30 | 20240602_0530_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 06:30 | 20240602_0630_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 07:30 | 20240602_0730_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 08:30 | 20240602_0830_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 09:30 | 20240602_0930_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 10:30 | 20240602_1030_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 11:30 | 20240602_1130_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 12:30 | 20240602_1230_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 13:30 | 20240602_1330_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 14:30 | 20240602_1430_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 15:30 | 20240602_1530_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 16:30 | 20240602_1630_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 17:30 | 20240602_1730_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 18:30 | 20240602_1830_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 19:30 | 20240602_1930_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 20:30 | 20240602_2030_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 21:30 | 20240602_2130_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 22:30 | 20240602_2230_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#      | 2024-06-02 23:30 | 20240602_2330_2D7_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
#    When I launch marmot