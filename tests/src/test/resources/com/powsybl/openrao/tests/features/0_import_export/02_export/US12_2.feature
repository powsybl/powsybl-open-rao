# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 12.2: Export of PTDF factors and relative margins in Core CNE
  This feature covers the export of PTDF factors and relative margins in the Core CNE.

  @fast @cne-export
  Scenario: US 12.2.1
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic12/MergedCB_12_2_1.xml"
    Given configuration file is "epic10/RaoParameters_relMargin_megawatt.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    Given RaoResult file is "epic12/RaoResult_12_2_1.json"
    When I export CORE CNE at "2019-01-08 22:59"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_2_1.xml"