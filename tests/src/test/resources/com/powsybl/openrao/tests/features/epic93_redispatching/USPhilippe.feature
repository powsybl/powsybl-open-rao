# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 93.3: US Philippe

  @slow @rao @idcc @done-philippe
  Scenario: US 93.3.14: 20240718
    Given network files are in folder "idcc/20240718-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T"
    Given crac file is "idcc/20240718-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240718-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240718-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20240718-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                                 |
      | 2024-07-18 00:30 | 20240718_0030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 01:30 | 20240718_0130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 02:30 | 20240718_0230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 03:30 | 20240718_0330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 04:30 | 20240718_0430_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 05:30 | 20240718_0530_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 06:30 | 20240718_0630_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 07:30 | 20240718_0730_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 08:30 | 20240718_0830_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 09:30 | 20240718_0930_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 10:30 | 20240718_1030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 11:30 | 20240718_1130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 12:30 | 20240718_1230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 13:30 | 20240718_1330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 14:30 | 20240718_1430_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 15:30 | 20240718_1530_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 16:30 | 20240718_1630_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 17:30 | 20240718_1730_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 18:30 | 20240718_1830_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 19:30 | 20240718_1930_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 20:30 | 20240718_2030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 21:30 | 20240718_2130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 22:30 | 20240718_2230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-18 23:30 | 20240718_2330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240718.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240718.zip"
    When I export F711 for business date "20240718"

  @slow @rao @idcc @godelaine
  Scenario: US 93.3.16: 20240730
    Given network files are in folder "idcc/20240730-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T 1"
    Given crac file is "idcc/20240730-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240730-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240730-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20240730-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                                 |
      | 2024-07-30 00:30 | 20240730_0030_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 01:30 | 20240730_0130_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 02:30 | 20240730_0230_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 03:30 | 20240730_0330_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 04:30 | 20240730_0430_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 05:30 | 20240730_0530_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 06:30 | 20240730_0630_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 07:30 | 20240730_0730_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 08:30 | 20240730_0830_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 09:30 | 20240730_0930_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 10:30 | 20240730_1030_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 11:30 | 20240730_1130_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 12:30 | 20240730_1230_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 13:30 | 20240730_1330_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 14:30 | 20240730_1430_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 15:30 | 20240730_1530_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 16:30 | 20240730_1630_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 17:30 | 20240730_1730_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 18:30 | 20240730_1830_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 19:30 | 20240730_1930_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 20:30 | 20240730_2030_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 21:30 | 20240730_2130_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 22:30 | 20240730_2230_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-30 23:30 | 20240730_2330_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240730.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240730.zip"
    When I export F711 for business date "20240730"

  @slow @rao @idcc @philippe
  Scenario: US 93.3.17: 20240826
    Given network files are in folder "idcc/20240826-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T 1"
    Given crac file is "idcc/20240826-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240826-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240826-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20240826-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                                 |
      | 2024-08-26 00:30 | 20240826_0030_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 01:30 | 20240826_0130_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 02:30 | 20240826_0230_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 03:30 | 20240826_0330_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 04:30 | 20240826_0430_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 05:30 | 20240826_0530_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 06:30 | 20240826_0630_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 07:30 | 20240826_0730_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 08:30 | 20240826_0830_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 09:30 | 20240826_0930_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 10:30 | 20240826_1030_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 11:30 | 20240826_1130_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 12:30 | 20240826_1230_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 13:30 | 20240826_1330_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 14:30 | 20240826_1430_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 15:30 | 20240826_1530_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 16:30 | 20240826_1630_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 17:30 | 20240826_1730_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 18:30 | 20240826_1830_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 19:30 | 20240826_1930_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 20:30 | 20240826_2030_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 21:30 | 20240826_2130_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 22:30 | 20240826_2230_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-08-26 23:30 | 20240826_2330_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240826.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240826.zip"
    When I export F711 for business date "20240826"

  @slow @rao @idcc @philippe
  Scenario: US 93.3.18: 20240906
    Given network files are in folder "idcc/20240906-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T 1"
    Given crac file is "idcc/20240906-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240906-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240906-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20240906-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                                 |
      | 2024-09-06 00:30 | 20240906_0030_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 01:30 | 20240906_0130_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 02:30 | 20240906_0230_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 03:30 | 20240906_0330_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 04:30 | 20240906_0430_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 05:30 | 20240906_0530_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 06:30 | 20240906_0630_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 07:30 | 20240906_0730_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 08:30 | 20240906_0830_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 09:30 | 20240906_0930_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 10:30 | 20240906_1030_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 11:30 | 20240906_1130_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 12:30 | 20240906_1230_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 13:30 | 20240906_1330_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 14:30 | 20240906_1430_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 15:30 | 20240906_1530_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 16:30 | 20240906_1630_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 17:30 | 20240906_1730_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 18:30 | 20240906_1830_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 19:30 | 20240906_1930_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 20:30 | 20240906_2030_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 21:30 | 20240906_2130_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 22:30 | 20240906_2230_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-06 23:30 | 20240906_2330_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240906.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240906.zip"
    When I export F711 for business date "20240906"

  @slow @rao @idcc @godelaine
  Scenario: US 93.3.13: 20240716
    Given network files are in folder "idcc/20240716-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T 1"
    Given crac file is "idcc/20240716-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240716-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240716-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20240716-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                                 |
      | 2024-07-16 00:30 | 20240716_0030_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 01:30 | 20240716_0130_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 02:30 | 20240716_0230_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 03:30 | 20240716_0330_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 04:30 | 20240716_0430_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 05:30 | 20240716_0530_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 06:30 | 20240716_0630_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 07:30 | 20240716_0730_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 08:30 | 20240716_0830_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 09:30 | 20240716_0930_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 10:30 | 20240716_1030_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 11:30 | 20240716_1130_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 12:30 | 20240716_1230_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 13:30 | 20240716_1330_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 14:30 | 20240716_1430_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 15:30 | 20240716_1530_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 16:30 | 20240716_1630_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 17:30 | 20240716_1730_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 18:30 | 20240716_1830_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 19:30 | 20240716_1930_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 20:30 | 20240716_2030_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 21:30 | 20240716_2130_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 22:30 | 20240716_2230_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-16 23:30 | 20240716_2330_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240716.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240716.zip"
    When I export F711 for business date "20240716"

  @slow @rao @idcc @philippe
  Scenario: US 93.3.19: 20240910
    Given network files are in folder "idcc/20240910-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T 1"
    Given crac file is "idcc/20240910-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240910-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240910-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20240910-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                         |
      | 2024-09-10 00:30 | 20240910_0030_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 01:30 | 20240910_0130_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 02:30 | 20240910_0230_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 03:30 | 20240910_0330_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 04:30 | 20240910_0430_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 05:30 | 20240910_0530_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 06:30 | 20240910_0630_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 07:30 | 20240910_0730_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 08:30 | 20240910_0830_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 09:30 | 20240910_0930_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 10:30 | 20240910_1030_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 11:30 | 20240910_1130_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 12:30 | 20240910_1230_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 13:30 | 20240910_1330_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 14:30 | 20240910_1430_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 15:30 | 20240910_1530_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 16:30 | 20240910_1630_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 17:30 | 20240910_1730_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 18:30 | 20240910_1830_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 19:30 | 20240910_1930_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 20:30 | 20240910_2030_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 21:30 | 20240910_2130_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 22:30 | 20240910_2230_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-10 23:30 | 20240910_2330_2D2_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240910.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240910.zip"
    When I export F711 for business date "20240910"

  @slow @rao @idcc @philippe
  Scenario: US 93.3.22: 20241028
    Given network files are in folder "idcc/20241028-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T 1"
    Given crac file is "idcc/20241028-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20241028-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20241028-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20241028-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                                 |
      | 2024-10-28 00:30 | 20241028_0030_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 01:30 | 20241028_0130_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 02:30 | 20241028_0230_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 03:30 | 20241028_0330_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 04:30 | 20241028_0430_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 05:30 | 20241028_0530_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 06:30 | 20241028_0630_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 07:30 | 20241028_0730_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 08:30 | 20241028_0830_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 09:30 | 20241028_0930_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 10:30 | 20241028_1030_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 11:30 | 20241028_1130_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 12:30 | 20241028_1230_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 13:30 | 20241028_1330_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 14:30 | 20241028_1430_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 15:30 | 20241028_1530_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 16:30 | 20241028_1630_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 17:30 | 20241028_1730_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 18:30 | 20241028_1830_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 19:30 | 20241028_1930_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 20:30 | 20241028_2030_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 21:30 | 20241028_2130_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 22:30 | 20241028_2230_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-28 23:30 | 20241028_2330_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20241028.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20241028.zip"
    When I export F711 for business date "20241028"



  @slow @rao @idcc @philippe
  Scenario: US 93.3.20: 20240926
    Given network files are in folder "idcc/20240926-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T 1"
    Given crac file is "idcc/20240926-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240926-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240926-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20240926-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                                 |
      | 2024-09-26 00:30 | 20240926_0030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 01:30 | 20240926_0130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 02:30 | 20240926_0230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 03:30 | 20240926_0330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 04:30 | 20240926_0430_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 05:30 | 20240926_0530_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 06:30 | 20240926_0630_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 07:30 | 20240926_0730_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 08:30 | 20240926_0830_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 09:30 | 20240926_0930_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 10:30 | 20240926_1030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 11:30 | 20240926_1130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 12:30 | 20240926_1230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 13:30 | 20240926_1330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 14:30 | 20240926_1430_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 15:30 | 20240926_1530_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 16:30 | 20240926_1630_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 17:30 | 20240926_1730_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 18:30 | 20240926_1830_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 19:30 | 20240926_1930_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 20:30 | 20240926_2030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 21:30 | 20240926_2130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 22:30 | 20240926_2230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-09-26 23:30 | 20240926_2330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240926.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240926.zip"
    When I export F711 for business date "20240926"


  @slow @rao @idcc @philippe
  Scenario: US 93.3.21: 20241013
    Given network files are in folder "idcc/20241013-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T 1"
    Given crac file is "idcc/20241013-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20241013-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20241013-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20241013-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                                 |
      | 2024-10-13 00:30 | 20241013_0030_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 01:30 | 20241013_0130_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 02:30 | 20241013_0230_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 03:30 | 20241013_0330_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 04:30 | 20241013_0430_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 05:30 | 20241013_0530_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 06:30 | 20241013_0630_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 07:30 | 20241013_0730_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 08:30 | 20241013_0830_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 09:30 | 20241013_0930_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 10:30 | 20241013_1030_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 11:30 | 20241013_1130_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 12:30 | 20241013_1230_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 13:30 | 20241013_1330_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 14:30 | 20241013_1430_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 15:30 | 20241013_1530_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 16:30 | 20241013_1630_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 17:30 | 20241013_1730_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 18:30 | 20241013_1830_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 19:30 | 20241013_1930_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 20:30 | 20241013_2030_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 21:30 | 20241013_2130_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 22:30 | 20241013_2230_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-10-13 23:30 | 20241013_2330_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20241013.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20241013.zip"
    When I export F711 for business date "20241013"

  @slow @rao @idcc @roxane
  Scenario: US 93.3.1: 20240122
    Given network files are in folder "idcc/20240122-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T"
    Given crac file is "idcc/20240122-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_110V1001C--00200I_CSA-COMRA-RDSTATIC-D_CORE-20240122-V001_.csv"
    Given ics series file is "_110V1001C--00200I_CSA-COMRA-RDSERIES-D_CORE-20240122-V001_.csv"
    Given ics gsk file is "_110V1001C--00200I_CSA-COMRA-GSK-D_CORE-20240122-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                                 |
      | 2024-01-22 00:30 | 20240122_0030_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 01:30 | 20240122_0130_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 02:30 | 20240122_0230_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 03:30 | 20240122_0330_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 04:30 | 20240122_0430_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 05:30 | 20240122_0530_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 06:30 | 20240122_0630_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 07:30 | 20240122_0730_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 08:30 | 20240122_0830_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 09:30 | 20240122_0930_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 10:30 | 20240122_1030_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 11:30 | 20240122_1130_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 12:30 | 20240122_1230_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 13:30 | 20240122_1330_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 14:30 | 20240122_1430_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 15:30 | 20240122_1530_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 16:30 | 20240122_1630_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 17:30 | 20240122_1730_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 18:30 | 20240122_1830_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 19:30 | 20240122_1930_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 20:30 | 20240122_2030_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 21:30 | 20240122_2130_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 22:30 | 20240122_2230_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-22 23:30 | 20240122_2330_2D1_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240122.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240122.zip"
    When I export F711 for business date "20240122"