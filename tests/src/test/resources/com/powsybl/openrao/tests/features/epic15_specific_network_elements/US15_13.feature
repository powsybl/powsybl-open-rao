# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.13: Handle combined RAs by configuration

  @fast @rao @mock @ac @preventive-only
  Scenario: US 15.13.1: Optimal combination in preventive, at first depth
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic15/CseCrac_ep15us13case1.xml"
    Given configuration file is "epic15/RaoParameters_ep15us13case1.json"
    When I launch search_tree_rao at "2021-04-30 22:30"
    Then 3 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the remedial action "open_be1_be4" is used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the worst margin is 97 A
    And the margin on cnec "be2_fr3_n - BBE2AA1 ->FFR3AA1  - preventive" after PRA should be 97 A
    And the margin on cnec "fr1_fr4_co1 - FFR1AA1 ->FFR4AA1   - co1_fr2_fr3_1 - curative" after CRA should be 152 A
    And the margin on cnec "fr2_de3_n - FFR2AA1 ->DDE3AA1  - preventive" after PRA should be 272 A
    And the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 311 A
    And the margin on cnec "fr2_de3_co1 - FFR2AA1 ->DDE3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 392 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 15.13.2: Optimal combination in curative, at first depth
    Given network file is "epic15/TestCase16Nodes_ep15us13case2_3_4.uct"
    Given crac file is "epic15/CseCrac_ep15us13case2_3_4.xml"
    Given configuration file is "epic15/RaoParameters_ep15us13case2.json"
    When I launch search_tree_rao at "2021-04-30 22:30"
    Then 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_be1_be4" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -309 A
    And the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after CRA should be -309 A
    And the margin on cnec "fr2_de3_co1 - FFR2AA1 ->DDE3AA1   - co1_fr2_fr3_1 - curative" after CRA should be -19 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 15.13.3: Optimal combination in curative, but not available as only 2 CRA are allowed
    Given network file is "epic15/TestCase16Nodes_ep15us13case2_3_4.uct"
    Given crac file is "epic15/CseCrac_ep15us13case2_3_4.xml"
    Given configuration file is "epic15/RaoParameters_ep15us13case2.json"
    Given crac creation parameters file is "epic15/us_15_13_3.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -336 A
    And the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after CRA should be -336 A
    And the margin on cnec "fr2_de3_co1 - FFR2AA1 ->DDE3AA1   - co1_fr2_fr3_1 - curative" after CRA should be -18 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 15.13.4: Optimal combination in curative, but not available as only the RA of 1 TSO can be used
    Given network file is "epic15/TestCase16Nodes_ep15us13case2_3_4.uct"
    Given crac file is "epic15/CseCrac_ep15us13case2_3_4.xml"
    Given configuration file is "epic15/RaoParameters_ep15us13case2.json"
    Given crac creation parameters file is "epic15/us15_13_4.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is -336 A
    And the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after CRA should be -336 A
    And the margin on cnec "fr2_de3_co1 - FFR2AA1 ->DDE3AA1   - co1_fr2_fr3_1 - curative" after CRA should be -18 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 15.13.5: Optimal subset
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic15/CseCrac_ep15us13case1.xml"
    Given configuration file is "epic15/RaoParameters_ep15us13case5.json"
    When I launch search_tree_rao at "2021-04-30 22:30"
    Then 3 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the remedial action "open_be1_be4" is used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the worst margin is 97 A
    And the margin on cnec "be2_fr3_n - BBE2AA1 ->FFR3AA1  - preventive" after PRA should be 97 A
    And the margin on cnec "fr1_fr4_co1 - FFR1AA1 ->FFR4AA1   - co1_fr2_fr3_1 - curative" after CRA should be 152 A
    And the margin on cnec "fr2_de3_n - FFR2AA1 ->DDE3AA1  - preventive" after PRA should be 272 A
    And the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 311 A
    And the margin on cnec "fr2_de3_co1 - FFR2AA1 ->DDE3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 392 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 15.13.6: Suboptimal combination, not used as it does not improve the margin as much as using individual RA (1)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic15/CseCrac_ep15us13case1.xml"
    Given configuration file is "epic15/RaoParameters_ep15us13case6.json"
    When I launch search_tree_rao at "2021-04-30 22:30"
    Then 3 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the remedial action "open_be1_be4" is used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the worst margin is 97 A
    And the margin on cnec "be2_fr3_n - BBE2AA1 ->FFR3AA1  - preventive" after PRA should be 97 A
    And the margin on cnec "fr1_fr4_co1 - FFR1AA1 ->FFR4AA1   - co1_fr2_fr3_1 - curative" after CRA should be 152 A
    And the margin on cnec "fr2_de3_n - FFR2AA1 ->DDE3AA1  - preventive" after PRA should be 272 A
    And the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 311 A
    And the margin on cnec "fr2_de3_co1 - FFR2AA1 ->DDE3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 392 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 15.13.7: Suboptimal combination, not used as it does not improve the margin as much as using individual RA (2)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic15/CseCrac_ep15us13case1.xml"
    Given configuration file is "epic15/RaoParameters_ep15us13case7.json"
    When I launch search_tree_rao at "2021-04-30 22:30"
    Then 3 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the remedial action "open_be1_be4" is used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the worst margin is 97 A
    And the margin on cnec "be2_fr3_n - BBE2AA1 ->FFR3AA1  - preventive" after PRA should be 97 A
    And the margin on cnec "fr1_fr4_co1 - FFR1AA1 ->FFR4AA1   - co1_fr2_fr3_1 - curative" after CRA should be 152 A
    And the margin on cnec "fr2_de3_n - FFR2AA1 ->DDE3AA1  - preventive" after PRA should be 272 A
    And the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 311 A
    And the margin on cnec "fr2_de3_co1 - FFR2AA1 ->DDE3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 392 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 15.13.8: Combination is better than individuals
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic15/CseCrac_ep15us13case8.xml"
    Given configuration file is "epic15/RaoParameters_ep15us13case8.json"
    When I launch search_tree_rao at "2021-04-30 22:30"
    Then 2 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the remedial action "open_be1_be4" is used in preventive
    And the worst margin is -77 A
    And the margin on cnec "be2_fr3_n - BBE2AA1 ->FFR3AA1  - preventive" after PRA should be -77 A
    And the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 106 A
    And the margin on cnec "fr2_de3_n - FFR2AA1 ->DDE3AA1  - preventive" after PRA should be 307 A
    And the margin on cnec "fr2_de3_co1 - FFR2AA1 ->DDE3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 491 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 15.13.9: onConstraint remedial action is not re evaluated after the activation of a combination of RA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic15/CseCrac_ep15us13case9.xml"
    Given configuration file is "epic15/RaoParameters_ep15us13case5.json"
    When I launch search_tree_rao at "2021-04-30 22:30"
    Then 3 remedial actions are used in preventive
    And the remedial action "open_fr1_fr3" is used in preventive
    And the remedial action "open_be1_be4" is used in preventive
    And the worst margin is 97 A
    And the margin on cnec "be2_fr3_n - BBE2AA1 ->FFR3AA1  - preventive" after PRA should be 97 A
    And the margin on cnec "fr1_fr4_co1 - FFR1AA1 ->FFR4AA1   - co1_fr2_fr3_1 - curative" after CRA should be 102 A
    And the margin on cnec "fr2_de3_n - FFR2AA1 ->DDE3AA1  - preventive" after PRA should be 272 A
    And the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 311 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 15.13.10: not imported combinations of RA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic15/CseCrac_ep15us13case10.xml"
    Given configuration file is "epic15/RaoParameters_ep15us13case10.json"
    When I launch search_tree_rao at "2021-04-30 22:30"
    Then 1 remedial actions are used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the worst margin is -44 A
    And the margin on cnec "be2_fr3_n - BBE2AA1 ->FFR3AA1  - preventive" after PRA should be -44 A
    And the margin on cnec "fr1_fr4_co1 - FFR1AA1 ->FFR4AA1   - co1_fr2_fr3_1 - curative" after CRA should be -41 A
