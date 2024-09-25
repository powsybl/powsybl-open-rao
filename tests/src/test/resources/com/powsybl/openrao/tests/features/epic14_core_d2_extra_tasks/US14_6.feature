# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 14.6: Dangling lines

  @fast @rao @mock @dc @preventive-only
  Scenario: 14.6.1 : Dangling line with no generation, RAO in MW
    Given network file is "epic14/TestCase12NodesXnodeNoGen.uct" for CORE CC
    Given crac file is "epic14/cbcora_ep14us6.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then its security status should be "SECURED"
    Then the worst margin is 2000.0 MW
    Then 0 remedial actions are used in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: 14.6.2 : Dangling line with no generation, RAO in A
    Given network file is "epic14/TestCase12NodesXnodeNoGen.uct" for CORE CC
    Given crac file is "epic14/cbcora_ep14us6.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then its security status should be "SECURED"
    Then the worst margin is 2888.0 A
    Then 0 remedial actions are used in preventive

  @fast @rao @mock @dc @preventive-only
  Scenario: 14.6.3 : Dangling line with generation, RAO in MW
    Given network file is "epic14/TestCase12NodesXnodeWithGen.uct" for CORE CC
    Given crac file is "epic14/cbcora_ep14us6.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then its security status should be "SECURED"
    Then the worst margin is 1000.0 MW
    Then 0 remedial actions are used in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: 14.6.4 : Dangling line with generation, RAO in A
    Given network file is "epic14/TestCase12NodesXnodeWithGen.uct" for CORE CC
    Given crac file is "epic14/cbcora_ep14us6.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then its security status should be "SECURED"
    Then the worst margin is 695.0 A
    Then 0 remedial actions are used in preventive