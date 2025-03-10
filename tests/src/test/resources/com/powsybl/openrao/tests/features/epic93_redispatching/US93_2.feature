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
    Given network files are in folder "20240926-FID2-620-v4-10V1001C--00264T-to-10V1001C--00085T/"
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