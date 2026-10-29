# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 0.2.2.2: Handling of curative optimization in the Core CNE export
  This feature covers the export of optimization results Core CNE.

  @fast @cne-export
  Scenario: 0.2.2.2.1: Simple curative case (copy of scenario 1.3.3.10)
    Given network file is "common/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "common/cbcora_2_6_3_11.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/RaoResult_0_2_2_2_1_RaoResult_12_1_1.json"
    When I export CORE CNE at "2019-01-08 12:59"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/ExpectedCNE_0_2_2_2_1_ExpectedCNE_12_1_1.xml"

  @fast @cne-export
  Scenario: 0.2.2.2.2: Curative case with MNECs (copy of scenario 1.3.6.4)
    Given network file is "common/TestCase12NodesDifferentPstTap.uct" for CORE CC
    Given crac file is "common/MergedCB_ep13us6case4.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_ac_mnecDimin30.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/RaoResult_0_2_2_2_2_RaoResult_12_1_2.json"
    When I export CORE CNE at "2019-01-08 13:00"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/ExpectedCNE_0_2_2_2_2_ExpectedCNE_12_1_2.xml"

  @fast @cne-export
  Scenario: 0.2.2.2.3: Curative case with loopflows (copy of scenario 1.3.7.4)
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "0_import_export/0_2_export/0_2_2_core_cne/MergedCB_12_1_3.xml"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc_withLoopFlows.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/RaoResult_0_2_2_2_3_RaoResult_12_1_3.json"
    When I export CORE CNE at "2019-01-08 00:59"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/ExpectedCNE_0_2_2_2_3_ExpectedCNE_12_1_3.xml"

  @fast @cne-export
  Scenario: 0.2.2.2.4.CBCORA: Curative case with 2 curative perimeters (copy of scenario 1.3.3.2)
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "0_import_export/0_2_export/0_2_2_core_cne/MergedCB_12_1_4.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_absolute_threshold.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/RaoResult_0_2_2_2_4_RaoResult_12_1_4.json"
    When I export CORE CNE at "2019-01-08 13:30"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/ExpectedCNE_0_2_2_2_4_ExpectedCNE_12_1_4.xml"

  @fast @cne-export
  Scenario: 0.2.2.2.5: Curative case with 2 curative perimeters and relative margins
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "0_import_export/0_2_export/0_2_2_core_cne/MergedCB_12_1_5.xml"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    Given configuration file is "common/RaoParameters_relMargin_ampere.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/RaoResult_0_2_2_2_5_RaoResult_12_1_5.json"
    When I export CORE CNE at "2019-01-08 00:00"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/ExpectedCNE_0_2_2_2_5_ExpectedCNE_12_1_5.xml"

  @fast @cne-export @hvdc
  Scenario: 0.2.2.2.6 : Curative case with German HVDC
    Given network file is "0_import_export/TestCase12NodesUltranet_v2.uct" for CORE CC
    Given crac file is "0_import_export/crac_0_2_2_2_6_crac_hvdc_curative.xml"
    Given crac creation parameters file is "0_import_export/CimCracCreationParameters_default.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/RaoResult_0_2_2_2_6_RaoResult_12_1_6.json"
    When I export CORE CNE at "2026-01-27 14:00"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/ExpectedCNE_0_2_2_2_6_ExpectedCNE_12_1_6.xml"
