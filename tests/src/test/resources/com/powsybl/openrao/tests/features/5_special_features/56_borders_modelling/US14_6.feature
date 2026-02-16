# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 14.6: Dangling lines

  @fast @rao @dc @preventive-only @max-min-margin @megawatt
  Scenario: 14.6.1 : Dangling line with no generation, RAO in MW
    Given network file is "epic14/TestCase12NodesXnodeNoGen.uct" for CORE CC
    Given crac file is "epic14/cbcora_ep14us6.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 2000.0 MW
    Then 0 remedial actions are used in preventive

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: 14.6.2 : Dangling line with no generation, RAO in A
    Given network file is "epic14/TestCase12NodesXnodeNoGen.uct" for CORE CC
    Given crac file is "epic14/cbcora_ep14us6.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 2888.0 A
    Then 0 remedial actions are used in preventive

  @fast @rao @dc @preventive-only @max-min-margin @megawatt
  Scenario: 14.6.3 : Dangling line with generation, RAO in MW
    Given network file is "epic14/TestCase12NodesXnodeWithGen.uct" for CORE CC
    Given crac file is "epic14/cbcora_ep14us6.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 1000.0 MW
    Then 0 remedial actions are used in preventive

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: 14.6.4 : Dangling line with generation, RAO in A
    Given network file is "epic14/TestCase12NodesXnodeWithGen.uct" for CORE CC
    Given crac file is "epic14/cbcora_ep14us6.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 695.0 A
    Then 0 remedial actions are used in preventive