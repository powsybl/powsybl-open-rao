# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 90.2: asterisks* as wildcards in UCTE CRACs importers

  @fast @crac @mock
  Scenario: US 90.2.1: Import of a valid CSE CRAC with asterisks
    Given network file is "crac90/TestCase_severalVoltageLevels_Xnodes_8characters.uct"
    Given crac file is "crac90/cseCrac_ep90us2case1.xml"
    When I import crac
    Then all the CNECs should be imported successfully
    And all the remedial actions should be imported successfully
    # And all the contingencies should be imported successfully (step not implemented)
    And it should have 2 contingencies

  @fast @crac @mock
  Scenario: US 90.2.2: Ambiguous CSE CRAC with filtered elements
    Given network file is "crac90/TestCase_severalVoltageLevels_Xnodes_8characters.uct"
    Given crac file is "crac90/cseCrac_ep90us2case2.xml"
    When I import crac
    Then it should have 11 cnecs
    And the native CNEC "basecase_branch_3 - DDE1AA1* - DDE2AA1* - basecase" should not be imported
    And the native CNEC "basecase_branch_4 - XFRDE111 - FFR2AA12 - basecase" should not be imported
    And the native remedial action "cra_2" should not be imported because of "INCONSISTENCY_IN_DATA"
    And the native remedial action "cra_3" should not be imported because of "ELEMENT_NOT_FOUND_IN_NETWORK"
    And it should have 2 network actions
    And it should have 0 range actions
    # And all the contingencies should be imported successfully (step not implemented)
    And it should have 2 contingencies
