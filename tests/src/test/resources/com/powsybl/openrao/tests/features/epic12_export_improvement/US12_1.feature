# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 12.1: handling of curative optimization in the CORE CNE export

  @fast @cne-export @mock
  Scenario: 12.1.1: Simple curative case (copy of scenario 13.3.10)
    Given network file is "epic13/TestCase16Nodes_with_different_imax.uct" for CORE CC
    Given crac file is "epic13/CBCORA_ep13us3case10.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given RaoResult file is "epic12/RaoResult_12_1_1.json"
    When I export CORE CNE at "2019-01-08 12:59"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_1_1.xml"

  @fast @cne-export @mock
  Scenario: 12.1.2: Curative case with MNECs (copy of scenario 13.6.4)
    Given network file is "epic13/TestCase12NodesDifferentPstTap.uct" for CORE CC
    Given crac file is "epic13/MergedCB_ep13us6case4.xml"
    Given configuration file is "epic11/RaoParameters_maxMargin_megawatt_mnecDimin20.json"
    Given RaoResult file is "epic12/RaoResult_12_1_2.json"
    When I export CORE CNE at "2019-01-08 13:00"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_1_2.xml"

  @fast @cne-export @mock
  Scenario: 12.1.3 : Curative case with loopflows (copy of scenario 13.7.4)
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic12/MergedCB_12_1_3.xml"
    Given Glsk file is "common/glsk_proportional_12nodes.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_withLoopFlows.json"
    Given RaoResult file is "epic12/RaoResult_12_1_3.json"
    When I export CORE CNE at "2019-01-08 00:59"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_1_3.xml"

  @fast @cne-export @mock
  Scenario: 12.1.4.CBCORA : Curative case with 2 curative perimeters (copy of scenario 13.3.2)
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "epic12/MergedCB_12_1_4.xml"
    Given configuration file is "epic13/RaoParameters_maxMargin_ampere_absolute_threshold.json"
    Given RaoResult file is "epic12/RaoResult_12_1_4.json"
    When I export CORE CNE at "2019-01-08 13:30"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_1_4.xml"

  @fast @cne-export @mock
  Scenario: 12.1.5 : Curative case with 2 curative perimeters and relative margins
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "epic12/MergedCB_12_1_5.xml"
    Given Glsk file is "common/glsk_proportional_12nodes.xml"
    Given configuration file is "epic13/RaoParameters_relMargin_ampere.json"
    Given RaoResult file is "epic12/RaoResult_12_1_5.json"
    When I export CORE CNE at "2019-01-08 00:00"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_1_5.xml"
