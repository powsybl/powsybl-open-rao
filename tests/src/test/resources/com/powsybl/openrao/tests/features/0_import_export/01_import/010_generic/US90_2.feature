# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 90.2: Asterisks* as wildcards in UCTE CRACs importers
  This feature covers the import of network elements with asterisks as wildcard in UCTE file format.

  @fast @crac
  Scenario: US 90.2.1: Import of a valid CSE CRAC with asterisks
    Given network file is "crac90/TestCase_severalVoltageLevels_Xnodes_8characters.uct"
    Given crac file is "crac90/cseCrac_ep90us2case1.xml"
    When I import crac
    Then all the CNECs should be imported successfully
    Then all the remedial actions should be imported successfully
    Then it should have 3 network actions
    Then it should have 1 range actions
    # Then all the contingencies should be imported successfully (step not implemented)
    Then it should have 2 contingencies

  @fast @crac
  Scenario: US 90.2.2: Ambiguous CSE CRAC with filtered elements
    Given network file is "crac90/TestCase_severalVoltageLevels_Xnodes_8characters.uct"
    Given crac file is "crac90/cseCrac_ep90us2case2.xml"
    When I import crac
    Then it should have 8 cnecs
    Then the native CNEC "basecase_branch_3 - DDE1AA1* - DDE2AA1* - basecase" should not be imported
    Then the native CNEC "basecase_branch_4 - XFRDE111 - FFR2AA12 - basecase" should not be imported
    Then the native remedial action "cra_2" should not be imported because of "INCONSISTENCY_IN_DATA"
    Then the native remedial action "cra_3" should not be imported because of "ELEMENT_NOT_FOUND_IN_NETWORK"
    Then it should have 2 network actions
    Then it should have 0 range actions
    # Then all the contingencies should be imported successfully (step not implemented)
    Then it should have 2 contingencies
