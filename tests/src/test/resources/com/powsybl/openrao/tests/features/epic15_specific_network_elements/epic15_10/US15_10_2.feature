# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.10.2: Modify voltage level topology as remedial action (3 nodes case)

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 15.10.2.1: PRA RA1 and CRA RA3 inapplicable (1/2)
    Given network file is "epic15/TestCase12Nodes_forCSE_3nodes_uselessSwitches.uct"
    Given crac file is "epic15/cseCrac_ep15us10-1case6.xml"
    Given crac creation parameters file is "epic15/CseCracCreationParameters_15_10_2.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And 0 remedial actions are used after "co1_fr2_fr3" at "curative"

  @fast @rao @mock @dc @contingency-scenarios
  Scenario: US 15.10.2.2: PRA RA1 inapplicable, CRA RA3 applicable
    Given network file is "epic15/TestCase12Nodes_forCSE_3nodes_uselessSwitches2.uct"
    Given crac file is "epic15/cseCrac_ep15us10-1case6.xml"
    Given crac creation parameters file is "epic15/CseCracCreationParameters_15_10_2.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used in preventive
    And 1 remedial actions are used after "co1_fr2_fr3" at "curative"
    And the remedial action "RA3" is used after "co1_fr2_fr3" at "curative"

  @fast @rao @mock @dc @contingency-scenarios
  Scenario: US 15.10.2.3: PRA RA1 applicable, makes CRA RA3 inapplicable (1/2)
    Given network file is "epic15/TestCase12Nodes_forCSE_3nodes_uselessSwitches3.uct"
    Given crac file is "epic15/cseCrac_ep15us10-1case6.xml"
    Given crac creation parameters file is "epic15/CseCracCreationParameters_15_10_2.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the remedial action "RA1" is used in preventive
    And 1 remedial actions are used after "co1_fr2_fr3" at "curative"
    And the remedial action "RA2" is used after "co1_fr2_fr3" at "curative"

  @fast @rao @mock @dc @contingency-scenarios
  Scenario: US 15.10.2.4: PRA RA1 applicable, makes CRA RA3 inapplicable (2/2)
    Given network file is "epic15/TestCase12Nodes_forCSE_3nodes_uselessSwitches4.uct"
    Given crac file is "epic15/cseCrac_ep15us10-1case6.xml"
    Given crac creation parameters file is "epic15/CseCracCreationParameters_15_10_2.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used in preventive
    And the remedial action "RA1" is used in preventive
    And 1 remedial actions are used after "co1_fr2_fr3" at "curative"
    And the remedial action "RA2" is used after "co1_fr2_fr3" at "curative"
