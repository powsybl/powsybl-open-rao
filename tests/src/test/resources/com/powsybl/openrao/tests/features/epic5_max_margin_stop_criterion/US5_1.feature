# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 5.1: Maximum margin stop criterion

  @fast @rao @mock @ac @preventive-only
  Scenario: US 5.1.0.a: positive margin stop criterion: secure initially, no ra applied
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic5/SL_ep5us1.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    Then the worst margin is 500.0 MW
    Then 0 remedial actions are used in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: US 5.1.0.b: use relevant number of decimals for margin and cost logging
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic5/SL_ep5us1b.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch search_tree_rao
    Then its security status should be "UNSECURED"
    Then the worst margin is -0.0001 A with a tolerance of 1E-8 A
    Then 0 remedial actions are used in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: US 5.1.1: maximum margin stop criterion: secure initially, optimize
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic5/SL_ep5us1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    Then the worst margin is 1000.0 MW
    Then 2 remedial actions are used in preventive
    Then the remedial action "Open tie-line FR1 FR2" is used in preventive
    Then the remedial action "Open tie-line FR1 FR3" is used in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: US 5.1.2: maximum margin stop criterion: maximum depth reached
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic5/SL_ep5us1.json"
    Given configuration file is "epic5/RaoParameters_maxMargin_maxDepth.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    Then the worst margin is 693.0 MW
    Then 1 remedial actions are used in preventive
    Then the remedial action "Open tie-line FR1 FR2" is used in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: US 5.1.3: maximum margin stop criterion: relative minimum impact threshold not reached
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic5/SL_ep5us1.json"
    Given configuration file is "epic5/RaoParameters_maxMargin_relativeMinImpact.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    Then the worst margin is 500.0 MW
    Then 0 remedial actions are used in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: US 5.1.4.a: maximum margin stop criterion: absolute minimum impact threshold reached
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic5/SL_ep5us1.json"
    Given configuration file is "epic5/RaoParameters_maxMargin_absoluteMinImpact190.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    Then the worst margin is 1000.0 MW
    Then 2 remedial actions are used in preventive
    Then the remedial action "Open tie-line FR1 FR2" is used in preventive
    Then the remedial action "Open tie-line FR1 FR3" is used in preventive

  @fast @rao @mock @dc @preventive-only
  Scenario: US 5.1.4.b: maximum margin stop criterion: absolute minimum impact threshold not reached
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic5/SL_ep5us1.json"
    Given configuration file is "epic5/RaoParameters_maxMargin_absoluteMinImpact195.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    Then the worst margin is 500.0 MW
    Then 0 remedial actions are used in preventive