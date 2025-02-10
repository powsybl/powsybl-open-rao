# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.7: Injection setpoint with absolute and relative target

  @fast @rao @mock @ac @preventive-only
  Scenario: US 15.7.1: Preventive search tree RAO with absolute injectionSetpoint preventive ra
    Given network file is "epic15/TestCase12NodesWith2OpenLines.uct"
    Given crac file is "epic15/cse_crac_1_preventive_ra.xml"
    When I launch search_tree_rao at "2021-04-30 22:30"
    Then the remedial action "cra_2" is used in preventive
    Then the worst margin is 545.8 MW

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 15.7.2: Curative search tree RAO with absolute injectionSetpoint curative ra
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic15/cse_crac_1_curative_ra.xml"
    Given configuration file is "epic15/RaoParameters_ep15us7-2.json"
    When I launch search_tree_rao at "2021-04-30 22:30"
    Then 0 remedial actions are used in preventive
    And the remedial action "cra_1" is used after "outage_1" at "curative"
    And the remedial action "cra_2bis" is used after "outage_1" at "curative"
    And the margin on cnec "CNEC_1_outage1 - FFR1AA1 ->FFR2AA1   - outage_1 - curative" after CRA should be 3920 MW