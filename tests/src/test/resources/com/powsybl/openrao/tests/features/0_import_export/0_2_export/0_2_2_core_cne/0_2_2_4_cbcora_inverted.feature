# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 0.2.2.4: Core CNE export for CBCORA with inverted branches
  This feature covers the export of particular CNECs in Core CNE.

  @fast @cne-export
  Scenario: 0.2.2.4.1: CBCORA with inverted branches
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "0_import_export/0_2_export/0_2_2_core_cne/MergedCB_12_6_4_withInvertedLines.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_ac_mnecDimin30.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/RaoResult_0_2_2_4_1_RaoResult_12_6_4_withInvertedLines.json"
    When I export CORE CNE at "2019-01-08 01:59"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/ExpectedCNE_0_2_2_4_1_ExpectedCNE_12_6_4_withInvertedLines.xml"

  @fast @cne-export
  Scenario: 0.2.2.4.2: CBCORA with inverted branches and loopflows
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "0_import_export/0_2_export/0_2_2_core_cne/MergedCB_12_9_2.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "common/refProg_12nodes.xml"
    Given configuration file is "common/RaoParameters_maxMargin_mw_dc_lf_false_5_100_BE_NL.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/RaoResult_0_2_2_4_2_RaoResult_12_9_2.json"
    When I export CORE CNE at "2019-01-08 21:30"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/ExpectedCNE_0_2_2_4_2_ExpectedCNE_12_9_2.xml"