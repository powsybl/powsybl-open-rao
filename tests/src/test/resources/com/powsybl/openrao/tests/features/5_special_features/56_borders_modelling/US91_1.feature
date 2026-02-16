# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 91.1: Geographic filter
  When the RAO parameter skip-actions-far-from-most-limiting-element is true, the value of
  max-number-of-boundaries-for-skipping-actions allows to filter remedial actions that are too far from the
  limiting element.

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 91.1.1: Simple case, only network actions inside country, positive margin
  The parameters mentioned in the US description only allow to use RAs inside the country of the limiting element,
  but the limiting element and the RAs are in the same country in any case.
    # TODO: this test is not so relevant because it does not test geographic filtering, as everything is in the same country
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic1/SL_ep1us2_selectionTopoRA_variant1.json"
    Given configuration file is "epic91/RaoParameters_case_91_1_1.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 56.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 56.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "Open tie-line FR DE" is used in preventive

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 91.1.2: Simple case, only network actions inside country, max margin
    # TODO: this description is written in comments, while in other files the descriptions are not inside comments
    # Before the first depth, the limiting cnec is a tie-line between FR and DE
    # So only RA "Open tie-line FR DE" can be used
    # At the end of the first depth RAO, the new limiting element is a tie-line between FR and BE
    # So the RA "PST @1" can now be used, but won't be because combining it with the first RA does not improve the solution
    # This results in a solution that is less optimal than using "PST @1" alone (83A, see test case 91.1.3 variant 1)
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic1/SL_ep1us2_selectionTopoRA_variant1.json"
    Given configuration file is "epic91/RaoParameters_case_91_1_12.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 56.0 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 56.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "Open tie-line FR DE" is used in preventive

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 91.1.3: Simple case, one boundary can be passed, max margin
  Same case as 91.1.2, but more permissive: "max-number-of-boundaries-for-skipping-actions" is set to 1 instead of 0.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic1/SL_ep1us2_selectionTopoRA_variant1.json"
    Given configuration file is "epic91/RaoParameters_case_91_1_3.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 83.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 83.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PST @1" is used in preventive

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 91.1.4: Simple case, one boundary can be passed, positive margin
  Same case as 91.1.3, but with positive margin.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic1/SL_ep1us2_selectionTopoRA_variant1.json"
    Given configuration file is "epic91/RaoParameters_case_91_1_4.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 55.92 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 55.92 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "Open tie-line FR DE" is used in preventive

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 91.1.5: Another simple case, no boundary can be passed, positive margin
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic91/sl_ep91us1case5.json"
    Given configuration file is "epic91/RaoParameters_case_91_1_1.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the worst margin is -12.0 A
    Then the margin on cnec "DDE1AA1  DDE3AA1  1 - preventive" after PRA should be -12.0 A
    Then 0 remedial actions are used in preventive

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 91.1.6: Another simple case, one boundary can be passed, positive margin
  Same as 91.1.5, but more permissive: "max-number-of-boundaries-for-skipping-actions" is set to 1 instead of 0.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic91/sl_ep91us1case5.json"
    Given configuration file is "epic91/RaoParameters_case_91_1_4.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 11.0 A
    Then the margin on cnec "DDE1AA1  DDE3AA1  1 - preventive" after PRA should be 11.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "Open internal line FR" is used in preventive

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 91.1.7: Another simple case, two boundaries can be passed, positive margin
  Same as 91.1.5, but more permissive: "max-number-of-boundaries-for-skipping-actions" is set to 2 instead of 0.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic91/sl_ep91us1case5.json"
    Given configuration file is "epic91/RaoParameters_case_91_1_7.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 71.0 A
    Then the margin on cnec "DDE1AA1  DDE3AA1  1 - preventive" after PRA should be 71.0 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "PST BE @1" is used in preventive