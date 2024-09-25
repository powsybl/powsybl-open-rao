# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 12.9: CORE CNE export for CBCORA with inverted branches

  @fast @cne-export @mock
  Scenario: 12.9.1: CBCORA with inverted branches
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic12/MergedCB_12_6_4_withInvertedLines.xml"
    Given configuration file is "epic11/RaoParameters_maxMargin_megawatt_mnecDimin20.json"
    Given RaoResult file is "epic12/RaoResult_12_6_4_withInvertedLines.json"
    When I export CORE CNE at "2019-01-08 01:59"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_6_4_withInvertedLines.xml"

  @fast @cne-export @mock
  Scenario: 12.9.2: CBCORA with inverted branches and loopflows
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic12/MergedCB_12_9_2.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "epic7/refProg_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_ac_lf_false_5_100_BE_NL.json"
    Given RaoResult file is "epic12/RaoResult_12_9_2.json"
    When I export CORE CNE at "2019-01-08 21:30"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_9_2.xml"