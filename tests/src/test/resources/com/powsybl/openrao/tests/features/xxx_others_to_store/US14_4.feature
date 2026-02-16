# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 14.4: HVDC

  @fast @rao @dc @contingency-scenarios @hvdc @max-min-margin @megawatt
  Scenario: US 14.4.1 : Outage HVDC modelling 1 (CORE's Cobra)
    Given network file is "common/TestCase12NodesHvdc.uct" for CORE CC
    Given crac file is "epic14/cbcora_ep14us4case1.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then the initial margin on cnec "004_FR-DE - outage" should be 501.0 MW
    Then the initial margin on cnec "003_FR-DE - curative" should be 501.0 MW
    Then the initial margin on cnec "002_FR-DE - preventive" should be 932.0 MW
    Then the worst margin is 691.0 MW
    Then the margin on cnec "004_FR-DE - outage" after PRA should be 691.0 MW
    Then the margin on cnec "003_FR-DE - curative" after CRA should be 857.0 MW
    Then the margin on cnec "002_FR-DE - preventive" after PRA should be 1102.0 MW

  @fast @rao @dc @contingency-scenarios @hvdc @max-min-margin @megawatt
  Scenario: US 14.4.2 : Outage HVDC modelling 2 (CORE's Alegro)
    Given network file is "common/TestCase12NodesHvdc.uct" for CORE CC
    Given crac file is "epic14/cbcora_ep14us4case2.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then the initial margin on cnec "004_FR-DE - outage" should be 501.0 MW
    Then the initial margin on cnec "003_FR-DE - curative" should be 501.0 MW
    Then the initial margin on cnec "002_FR-DE - preventive" should be 932.0 MW
    Then the worst margin is 691.0 MW
    Then the margin on cnec "004_FR-DE - outage" after PRA should be 691.0 MW
    Then the margin on cnec "003_FR-DE - curative" after CRA should be 857.0 MW
    Then the margin on cnec "002_FR-DE - preventive" after PRA should be 1102.0 MW