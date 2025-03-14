# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 93.1: power gradient constraints

  @fast @rao @dc @redispatching @preventive-only
  Scenario: US 93.2.1: Test for CORE IDCC
    Given network files are in folder "20241028-FID2-620-v4-10V1001C--00264T-to-10V1001C--00085T/"
    Given crac file is "20241028-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20241028-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20241028-V001_.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                         |
      | 2024-10-28 00:30 | 20241028_0030_2D1_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-10-28 01:30 | 20241028_0130_2D1_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-10-28 02:30 | 20241028_0230_2D1_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
    When I launch marmot

  @fast @rao @dc @redispatching @preventive-only
  Scenario: US 93.2.2: Test for CORE IDCC 2
    Given network files are in folder "20240926-FID2-620-v4-10V1001C--00264T-to-10V1001C--00085T"
    Given crac file is "20240926-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240926-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240926-V001_.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                         |
      | 2024-09-26 00:30 | 20240926_0030_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 01:30 | 20240926_0130_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 02:30 | 20240926_0230_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 03:30 | 20240926_0330_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 04:30 | 20240926_0430_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 05:30 | 20240926_0530_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 06:30 | 20240926_0630_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 07:30 | 20240926_0730_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 08:30 | 20240926_0830_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 09:30 | 20240926_0930_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 10:30 | 20240926_1030_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 11:30 | 20240926_1130_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 12:30 | 20240926_1230_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 13:30 | 20240926_1330_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 14:30 | 20240926_1430_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 15:30 | 20240926_1530_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 16:30 | 20240926_1630_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 17:30 | 20240926_1730_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 18:30 | 20240926_1830_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 19:30 | 20240926_1930_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 20:30 | 20240926_2030_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 21:30 | 20240926_2130_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 22:30 | 20240926_2230_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 23:30 | 20240926_2330_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
    When I launch marmot

  @fast @rao @dc @redispatching @preventive-only
  Scenario: US 93.2.2: Test for CORE IDCC 3
    Given network files are in folder "20240926-FID2-620-v4-10V1001C--00264T-to-10V1001C--00085T"
    Given crac file is "20240926-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240926-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240926-V001_.csv"
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                         |
      | 2024-09-26 00:30 | 20240926_0030_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 01:30 | 20240926_0130_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
      | 2024-09-26 02:30 | 20240926_0230_2D4_UX0_FEXPORTGRIDMODEL_CGM_10V1001C--00264T.uct |
    When I launch marmot