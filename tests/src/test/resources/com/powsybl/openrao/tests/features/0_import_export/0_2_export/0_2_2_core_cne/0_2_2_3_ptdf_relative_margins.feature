# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 0.2.2.3: Export of PTDF factors and relative margins in Core CNE
  This feature covers the export of PTDF factors and relative margins in the Core CNE.

  @fast @cne-export
  Scenario: 0.2.2.3.1
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "0_import_export/0_2_export/0_2_2_core_cne/MergedCB_12_2_1.xml"
    Given configuration file is "common/RaoParameters_relMargin_megawatt.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/RaoResult_0_2_2_3_1_RaoResult_12_2_1.json"
    When I export CORE CNE at "2019-01-08 22:59"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/ExpectedCNE_0_2_2_3_1_ExpectedCNE_12_2_1.xml"