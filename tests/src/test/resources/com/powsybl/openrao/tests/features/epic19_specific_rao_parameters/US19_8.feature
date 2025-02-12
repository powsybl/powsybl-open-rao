# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 19.8: handle aligned PSTs when filtering range actions

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.8.1 : Do not select french pst, because of max-tso
    Given network file is "epic19/TestCase16Nodes_3PSTs_NullInitialTaps.uct"
    Given crac file is "epic19/CBCORA_ep19us8case1.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given crac creation parameters file is "epic19/us19_8_1.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 16 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_de" should be 16 after "CO1_fr2_fr3_1" at "curative"
    And the margin on cnec "fr1_fr4_CO1_DIR - curative" after CRA should be 1664 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.8.2 : Only select german pst, because of max-tso
    Given network file is "epic19/TestCase16Nodes_3PSTs_NullInitialTaps.uct"
    Given crac file is "epic19/CBCORA_ep19us8case2.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given crac creation parameters file is "epic19/us19_8_1.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_de" should be 16 after "CO1_fr2_fr3_1" at "curative"
    And the margin on cnec "fr1_fr4_CO1_DIR - curative" after CRA should be 1626 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.8.3 : Only select belgian pst, because of max-tso
    Given network file is "epic19/TestCase16Nodes_3PSTs_NullInitialTaps.uct"
    Given crac file is "epic19/CBCORA_ep19us8case3.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given crac creation parameters file is "epic19/us19_8_3&4.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 16 after "CO1_fr2_fr3_1" at "curative"
    And the margin on cnec "fr1_fr4_CO1_DIR - curative" after CRA should be 1328 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.8.4 : Only select french pst, because of max-tso
    Given network file is "epic19/TestCase16Nodes_3PSTs_NullInitialTaps.uct"
    Given crac file is "epic19/CBCORA_ep19us8case1.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given crac creation parameters file is "epic19/us19_8_3&4.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 16 after "CO1_fr2_fr3_1" at "curative"
    And the margin on cnec "fr1_fr4_CO1_DIR - curative" after CRA should be 1414 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.8.5 : Do not select french pst, because of max-curative-ra
    Given network file is "epic19/TestCase16Nodes_3PSTs_NullInitialTaps.uct"
    Given crac file is "epic19/CBCORA_ep19us8case1.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given crac creation parameters file is "epic19/us19_8_5.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 16 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_de" should be 16 after "CO1_fr2_fr3_1" at "curative"
    And the margin on cnec "fr1_fr4_CO1_DIR - curative" after CRA should be 1664 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.8.6 : Only select german pst, because of max-curative-ra
    Given network file is "epic19/TestCase16Nodes_3PSTs_NullInitialTaps.uct"
    Given crac file is "epic19/CBCORA_ep19us8case2.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given crac creation parameters file is "epic19/us19_8_5.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_de" should be 16 after "CO1_fr2_fr3_1" at "curative"
    And the margin on cnec "fr1_fr4_CO1_DIR - curative" after CRA should be 1626 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.8.7 : Only select belgian pst, because of max-curative-ra
    Given network file is "epic19/TestCase16Nodes_3PSTs_NullInitialTaps.uct"
    Given crac file is "epic19/CBCORA_ep19us8case3.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given crac creation parameters file is "epic19/us19_8_7.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 16 after "CO1_fr2_fr3_1" at "curative"
    And the margin on cnec "fr1_fr4_CO1_DIR - curative" after CRA should be 1328 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.8.8 : Only select french pst, because of max-curative-ra
    Given network file is "epic19/TestCase16Nodes_3PSTs_NullInitialTaps.uct"
    Given crac file is "epic19/CBCORA_ep19us8case1.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given crac creation parameters file is "epic19/us19_8_7.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 16 after "CO1_fr2_fr3_1" at "curative"
    And the margin on cnec "fr1_fr4_CO1_DIR - curative" after CRA should be 1414 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.8.9 : Do not select second pst, because of max-curative-pst-to-tso
    Given network file is "epic19/TestCase16Nodes_3PSTs_NullInitialTaps.uct"
    Given crac file is "epic19/CBCORA_ep19us8case9.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given crac creation parameters file is "epic19/us19_8_9.json"
    When I launch search_tree_rao
    Then 2 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be1" should be 16 after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be3" should be 16 after "CO1_fr2_fr3_1" at "curative"
    And the margin on cnec "fr1_fr4_CO1_DIR - curative" after CRA should be 1664 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.8.10 : Only select third pst, because of max-curative-pst-to-tso
    Given network file is "epic19/TestCase16Nodes_3PSTs_NullInitialTaps.uct"
    Given crac file is "epic19/CBCORA_ep19us8case10.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given crac creation parameters file is "epic19/us19_8_9.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be3" should be 16 after "CO1_fr2_fr3_1" at "curative"
    And the margin on cnec "fr1_fr4_CO1_DIR - curative" after CRA should be 1626 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.8.11 :  Only select first pst, because of max-curative-pst-to-tso
    Given network file is "epic19/TestCase16Nodes_3PSTs_NullInitialTaps.uct"
    Given crac file is "epic19/CBCORA_ep19us8case11.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given crac creation parameters file is "epic19/us19_8_11.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be1" should be 16 after "CO1_fr2_fr3_1" at "curative"
    And the margin on cnec "fr1_fr4_CO1_DIR - curative" after CRA should be 1328 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: 19.8.12 : Only select second pst, because of max-curative-pst-to-tso
    Given network file is "epic19/TestCase16Nodes_3PSTs_NullInitialTaps.uct"
    Given crac file is "epic19/CBCORA_ep19us8case9.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    Given crac creation parameters file is "epic19/us19_8_11.json"
    When I launch search_tree_rao
    Then 1 remedial actions are used after "CO1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be2" should be 16 after "CO1_fr2_fr3_1" at "curative"
    And the margin on cnec "fr1_fr4_CO1_DIR - curative" after CRA should be 1414 A