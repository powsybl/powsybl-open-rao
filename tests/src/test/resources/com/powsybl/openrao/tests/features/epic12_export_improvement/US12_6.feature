# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 12.6: CORE CNE export for preventive case

  @fast @cne-export @mock
  Scenario: 12.6.2.CBCORA: Preventive only case 1
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "epic12/MergedCB_12_6_2.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given RaoResult file is "epic12/RaoResult_12_6_2.json"
    When I export CORE CNE at "2019-01-08 00:00"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_6_2.xml"

  @fast @cne-export @mock
  Scenario: 12.6.3: Preventive case with loopflows
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic12/crac_lf_rao_3_cbcora.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "epic7/refProg_12nodes.xml"
    Given configuration file is "epic7/RaoParameters_maxMargin_mw_ac_lf_false_5_100_BE_NL.json"
    Given RaoResult file is "epic12/RaoResult_12_6_3.json"
    When I export CORE CNE at "2019-01-08 21:30"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_6_3.xml"

  @fast @cne-export @mock
  Scenario: 12.6.4: Test case with a CBCORA file and a MNEC
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic12/MergedCB_12_6_4.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    Given RaoResult file is "epic12/RaoResult_12_6_4.json"
    When I export CORE CNE at "2019-01-08 01:00"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_6_4.xml"