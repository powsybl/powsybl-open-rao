# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 20.6: Second Preventive improvements

  @fast @rao @mock @dc @second-preventive
  Scenario: US 20.6.1: Fix PST CRA setpoints in global 2nd preventive
    Given network file is "epic20/TestCase12Nodes_20_6_1.uct"
    Given crac file is "epic20/crac_ep20us6case1.json"
    Given configuration file is "epic20/RaoParameters_20_6_1.json"
    When I launch rao
    Then the worst margin is -40.5 MW
    And the tap of PstRangeAction "CRA_PST_DE" should be 0 after "Contingency NL3 BE1 2" at "curative"
    Then the execution details should be "Second preventive improved first preventive results"

  @fast @rao @mock @dc @second-preventive
  Scenario: US 20.6.2: Fallback to first preventive after 2nd preventive
    Given network file is "epic20/TestCase12Nodes_20_6_2.uct"
    Given crac file is "epic20/crac_ep20us6case2.json"
    Given configuration file is "epic20/RaoParameters_20_6_2.json"
    When I launch rao
    Then the worst margin is 100.0 MW
    And the execution details should be "Second preventive fell back to first preventive results"