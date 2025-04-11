# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 93.3: US Roxane

  @slow @rao @idcc @roxane
  Scenario: US 93.3.4: 20240224
    Given network files are in folder "idcc/20240224-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T"
    Given crac file is "idcc/20240224-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_110V1001C--00200I_CSA-COMRA-RDSTATIC-D_CORE-20240224-V001_.csv"
    Given ics series file is "_110V1001C--00200I_CSA-COMRA-RDSERIES-D_CORE-20240224-V001_.csv"
    Given ics gsk file is "_10V1001C--00200I_CSA-INDRA-GSK-D_D7-20240224-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                         |
      | 2024-02-24 00:30 | 20240224_0030_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 01:30 | 20240224_0130_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 02:30 | 20240224_0230_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 03:30 | 20240224_0330_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 04:30 | 20240224_0430_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 05:30 | 20240224_0530_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 06:30 | 20240224_0630_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 07:30 | 20240224_0730_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 08:30 | 20240224_0830_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 09:30 | 20240224_0930_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 10:30 | 20240224_1030_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 11:30 | 20240224_1130_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 12:30 | 20240224_1230_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 13:30 | 20240224_1330_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 14:30 | 20240224_1430_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 15:30 | 20240224_1530_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 16:30 | 20240224_1630_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 17:30 | 20240224_1730_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 18:30 | 20240224_1830_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 19:30 | 20240224_1930_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 20:30 | 20240224_2030_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 21:30 | 20240224_2130_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 22:30 | 20240224_2230_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-24 23:30 | 20240224_2330_2D6_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240224.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240224.zip"


  @slow @rao @idcc @thomas
  Scenario: US 93.3.6: 20240526
    Given network files are in folder "idcc/20240526-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T"
    Given crac file is "idcc/20240526-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240526-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240526-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20240526-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                         |
      | 2024-05-26 00:30 | 20240526_0030_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 01:30 | 20240526_0130_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 02:30 | 20240526_0230_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 03:30 | 20240526_0330_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 04:30 | 20240526_0430_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 05:30 | 20240526_0530_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 06:30 | 20240526_0630_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 07:30 | 20240526_0730_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 08:30 | 20240526_0830_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 09:30 | 20240526_0930_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 10:30 | 20240526_1030_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 11:30 | 20240526_1130_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 12:30 | 20240526_1230_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 13:30 | 20240526_1330_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 14:30 | 20240526_1430_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 15:30 | 20240526_1530_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 16:30 | 20240526_1630_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 17:30 | 20240526_1730_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 18:30 | 20240526_1830_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 19:30 | 20240526_1930_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 20:30 | 20240526_2030_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 21:30 | 20240526_2130_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 22:30 | 20240526_2230_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-26 23:30 | 20240526_2330_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240526.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240526.zip"

  @slow @rao @idcc @thomas
  Scenario: US 93.3.8: 20240606
    Given network files are in folder "idcc/20240606-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T"
    Given crac file is "idcc/20240606-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240606-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240606-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20240606-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                   |
      | 2024-06-06 00:30 | 20240606_0030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 01:30 | 20240606_0130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 02:30 | 20240606_0230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 03:30 | 20240606_0330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 04:30 | 20240606_0430_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 05:30 | 20240606_0530_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 06:30 | 20240606_0630_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 07:30 | 20240606_0730_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 08:30 | 20240606_0830_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 09:30 | 20240606_0930_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 10:30 | 20240606_1030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 11:30 | 20240606_1130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 12:30 | 20240606_1230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 13:30 | 20240606_1330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 14:30 | 20240606_1430_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 15:30 | 20240606_1530_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 16:30 | 20240606_1630_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 17:30 | 20240606_1730_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 18:30 | 20240606_1830_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 19:30 | 20240606_1930_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 20:30 | 20240606_2030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 21:30 | 20240606_2130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 22:30 | 20240606_2230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-06 23:30 | 20240606_2330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240606.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240606.zip"

  @slow @rao @idcc @thomas
  Scenario: US 93.3.9: 20240628
    Given network files are in folder "idcc/20240628-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T"
    Given crac file is "idcc/20240628-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240628-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240628-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20240628-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                         |
      | 2024-06-28 00:30 | 20240628_0030_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 01:30 | 20240628_0130_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 02:30 | 20240628_0230_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 03:30 | 20240628_0330_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 04:30 | 20240628_0430_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 05:30 | 20240628_0530_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 06:30 | 20240628_0630_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 07:30 | 20240628_0730_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 08:30 | 20240628_0830_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 09:30 | 20240628_0930_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 10:30 | 20240628_1030_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 11:30 | 20240628_1130_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 12:30 | 20240628_1230_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 13:30 | 20240628_1330_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 14:30 | 20240628_1430_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 15:30 | 20240628_1530_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 16:30 | 20240628_1630_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 17:30 | 20240628_1730_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 18:30 | 20240628_1830_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 19:30 | 20240628_1930_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 20:30 | 20240628_2030_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 21:30 | 20240628_2130_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 22:30 | 20240628_2230_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-28 23:30 | 20240628_2330_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240628.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240628.zip"

  @slow @rao @idcc @roxane
  Scenario: US 93.3.5: 20240502
    Given network files are in folder "idcc/20240502-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T"
    Given crac file is "idcc/20240502-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240502-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240502-V001_.csv"
    Given ics gsk file is "_10V1001C--00200I_CSA-INDRA-GSK-D_D7-20240502-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                         |
      | 2024-05-02 00:30 | 20240502_0030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 01:30 | 20240502_0130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 02:30 | 20240502_0230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 03:30 | 20240502_0330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 04:30 | 20240502_0430_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 05:30 | 20240502_0530_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 06:30 | 20240502_0630_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 07:30 | 20240502_0730_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 08:30 | 20240502_0830_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 09:30 | 20240502_0930_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 10:30 | 20240502_1030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 11:30 | 20240502_1130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 12:30 | 20240502_1230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 13:30 | 20240502_1330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 14:30 | 20240502_1430_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 15:30 | 20240502_1530_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 16:30 | 20240502_1630_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 17:30 | 20240502_1730_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 18:30 | 20240502_1830_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 19:30 | 20240502_1930_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 20:30 | 20240502_2030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 21:30 | 20240502_2130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 22:30 | 20240502_2230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-05-02 23:30 | 20240502_2330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240502.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240502.zip"


  @slow @rao @idcc @roxane
  Scenario: US 93.3.2: 20240131
    Given network files are in folder "idcc/20240131-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T"
    Given crac file is "idcc/20240131-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_110V1001C--00200I_CSA-COMRA-RDSTATIC-D_CORE-20240131-V001_.csv"
    Given ics series file is "_110V1001C--00200I_CSA-COMRA-RDSERIES-D_CORE-20240131-V001_.csv"
    Given ics gsk file is "_110V1001C--00200I_CSA-COMRA-GSK-D_CORE-20240131-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                         |
      | 2024-01-31 00:30 | 20240131_0030_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 01:30 | 20240131_0130_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 02:30 | 20240131_0230_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 03:30 | 20240131_0330_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 04:30 | 20240131_0430_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 05:30 | 20240131_0530_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 06:30 | 20240131_0630_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 07:30 | 20240131_0730_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 08:30 | 20240131_0830_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 09:30 | 20240131_0930_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 10:30 | 20240131_1030_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 11:30 | 20240131_1130_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 12:30 | 20240131_1230_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 13:30 | 20240131_1330_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 14:30 | 20240131_1430_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 15:30 | 20240131_1530_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 16:30 | 20240131_1630_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 17:30 | 20240131_1730_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 18:30 | 20240131_1830_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 19:30 | 20240131_1930_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 20:30 | 20240131_2030_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 21:30 | 20240131_2130_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 22:30 | 20240131_2230_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-01-31 23:30 | 20240131_2330_2D3_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240131.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240131.zip"

  @slow @rao @idcc @thomas
  Scenario: US 93.3.7: 20240602
    Given network files are in folder "idcc/20240602-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T"
    Given crac file is "idcc/20240602-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240602-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240602-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20240602-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                         |
      | 2024-06-02 00:30 | 20240602_0030_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 01:30 | 20240602_0130_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 02:30 | 20240602_0230_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 03:30 | 20240602_0330_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 04:30 | 20240602_0430_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 05:30 | 20240602_0530_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 06:30 | 20240602_0630_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 07:30 | 20240602_0730_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 08:30 | 20240602_0830_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 09:30 | 20240602_0930_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 10:30 | 20240602_1030_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 11:30 | 20240602_1130_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 12:30 | 20240602_1230_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 13:30 | 20240602_1330_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 14:30 | 20240602_1430_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 15:30 | 20240602_1530_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 16:30 | 20240602_1630_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 17:30 | 20240602_1730_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 18:30 | 20240602_1830_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 19:30 | 20240602_1930_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 20:30 | 20240602_2030_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 21:30 | 20240602_2130_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 22:30 | 20240602_2230_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-06-02 23:30 | 20240602_2330_2D7_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240602.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240602.zip"

  @slow @rao @idcc @thomas
  Scenario: US 93.3.10: 20240704
    Given network files are in folder "idcc/20240704-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T"
    Given crac file is "idcc/20240704-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240704-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240704-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20240704-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                         |
      | 2024-07-04 00:30 | 20240704_0030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 01:30 | 20240704_0130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 02:30 | 20240704_0230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 03:30 | 20240704_0330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 04:30 | 20240704_0430_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 05:30 | 20240704_0530_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 06:30 | 20240704_0630_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 07:30 | 20240704_0730_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 08:30 | 20240704_0830_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 09:30 | 20240704_0930_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 10:30 | 20240704_1030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 11:30 | 20240704_1130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 12:30 | 20240704_1230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 13:30 | 20240704_1330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 14:30 | 20240704_1430_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 15:30 | 20240704_1530_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 16:30 | 20240704_1630_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 17:30 | 20240704_1730_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 18:30 | 20240704_1830_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 19:30 | 20240704_1930_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 20:30 | 20240704_2030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 21:30 | 20240704_2130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 22:30 | 20240704_2230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-04 23:30 | 20240704_2330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240704.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240704.zip"

  @slow @rao @idcc @godelaine
  Scenario: US 93.3.11: 20240711
    Given network files are in folder "idcc/20240711-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T 1"
    Given crac file is "idcc/20240711-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240711-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240711-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20240711-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                   |
      | 2024-07-11 00:30 | 20240711_0030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 01:30 | 20240711_0130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 02:30 | 20240711_0230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 03:30 | 20240711_0330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 04:30 | 20240711_0430_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 05:30 | 20240711_0530_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 06:30 | 20240711_0630_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 07:30 | 20240711_0730_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 08:30 | 20240711_0830_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 09:30 | 20240711_0930_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 10:30 | 20240711_1030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 11:30 | 20240711_1130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 12:30 | 20240711_1230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 13:30 | 20240711_1330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 14:30 | 20240711_1430_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 15:30 | 20240711_1530_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 16:30 | 20240711_1630_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 17:30 | 20240711_1730_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 18:30 | 20240711_1830_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 19:30 | 20240711_1930_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 20:30 | 20240711_2030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 21:30 | 20240711_2130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 22:30 | 20240711_2230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-11 23:30 | 20240711_2330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240711.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240711.zip"


  @slow @rao @idcc @godelaine
  Scenario: US 93.3.12: 20240712
    Given network files are in folder "idcc/20240712-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T 1"
    Given crac file is "idcc/20240712-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_10V1001C–00275O_CSA-COMRA-RDSTATIC-D_CORE-20240712-V001_.csv"
    Given ics series file is "_10V1001C–00275O_CSA-COMRA-RDSERIES-D_CORE-20240712-V001_.csv"
    Given ics gsk file is "_10V1001C--00275O_CSA-INDRA-GSK-D_D7-20240712-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                   |
      | 2024-07-12 00:30 | 20240712_0030_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 01:30 | 20240712_0130_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 02:30 | 20240712_0230_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 03:30 | 20240712_0330_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 04:30 | 20240712_0430_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 05:30 | 20240712_0530_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 06:30 | 20240712_0630_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 07:30 | 20240712_0730_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 08:30 | 20240712_0830_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 09:30 | 20240712_0930_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 10:30 | 20240712_1030_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 11:30 | 20240712_1130_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 12:30 | 20240712_1230_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 13:30 | 20240712_1330_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 14:30 | 20240712_1430_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 15:30 | 20240712_1530_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 16:30 | 20240712_1630_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 17:30 | 20240712_1730_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 18:30 | 20240712_1830_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 19:30 | 20240712_1930_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 20:30 | 20240712_2030_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 21:30 | 20240712_2130_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 22:30 | 20240712_2230_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-07-12 23:30 | 20240712_2330_2D5_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240712.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240712.zip"

  @slow @rao @idcc @roxane
  Scenario: US 93.3.3: 20240222
    Given network files are in folder "idcc/20240222-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T"
    Given crac file is "idcc/20240222-FSC-ID2-CB-v1-10V1001C--00264T-to-10XFR-RTE------Q.xml"
    Given ics static file is "_110V1001C--00200I_CSA-COMRA-RDSTATIC-D_CORE-20240222-V001_.csv"
    Given ics series file is "_110V1001C--00200I_CSA-COMRA-RDSERIES-D_CORE-20240222-V001_.csv"
    Given ics gsk file is "_110V1001C--00200I_CSA-COMRA-GSK-D_CORE-20240222-V001_.csv"
    Given configuration file is "idcc/RaoParameters_idcc_low_improvement_2_max.json"
    Given intertemporal rao inputs are:
      | Timestamp        | Network                                                         |
      | 2024-02-22 00:30 | 20240222_0030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 01:30 | 20240222_0130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 02:30 | 20240222_0230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 03:30 | 20240222_0330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 04:30 | 20240222_0430_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 05:30 | 20240222_0530_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 06:30 | 20240222_0630_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 07:30 | 20240222_0730_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 08:30 | 20240222_0830_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 09:30 | 20240222_0930_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 10:30 | 20240222_1030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 11:30 | 20240222_1130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 12:30 | 20240222_1230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 13:30 | 20240222_1330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 14:30 | 20240222_1430_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 15:30 | 20240222_1530_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 16:30 | 20240222_1630_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 17:30 | 20240222_1730_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 18:30 | 20240222_1830_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 19:30 | 20240222_1930_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 20:30 | 20240222_2030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 21:30 | 20240222_2130_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 22:30 | 20240222_2230_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
      | 2024-02-22 23:30 | 20240222_2330_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct |
    When I launch marmot
    When I export marmot results to "raoresults/results_20240222.zip"
    When I export networks with PRAs to "raoresults/networkWithPras_20240222.zip"
