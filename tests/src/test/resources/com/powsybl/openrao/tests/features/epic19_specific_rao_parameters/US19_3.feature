# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 19.3: Handle maximum CRA and maximum curative PSTs per TSO

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.3.1: One PST and one topo, two CRAs
    # <!> All RAs are hypothetically operated by "be"
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us3case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -12 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 1000 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"
    And the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 1301 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.3.2: Two PSTs and no topo
    # <!> All RAs are hypothetically operated by "be"
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us3case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 16 after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_fr" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 15 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 972 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"
    And the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 1301 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.3.3: One PST and no topo
    # <!> All RAs are hypothetically operated by "be"
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us10case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 945 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"
    And the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 1301 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.3.4: No PST and one topo
    # <!> All RAs are hypothetically operated by "be"
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us10case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 840 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.3.5: One PST and one topo, one CRA, chose PST
    # <!> All RAs are hypothetically operated by "be"
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us10case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 945 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"
    And the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 1301 A

  @fast @rao @mock @ac @contingency-scenarios @search-tree-rao
  Scenario: US 19.3.6: One PST and one topo, one CRA, chose topo
    # <!> All RAs are hypothetically operated by "be"
    Given network file is "epic19/TestCase16Nodes_19_3_6.uct"
    Given crac file is "epic19/SL_ep19us3case6.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao after "co1_fr2_fr3_1" at "curative"
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr2" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 986 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.3.7: Two topo
    # <!> All RAs are hypothetically operated by "be"
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us3case7.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr2" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 973 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.3.8: Two operators, no PST and one topo per operator
    # <!> All RAs are hypothetically operated by "be", except for "open_be1_be4" operated by "fr"
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us3case8.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_be1_be4" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 876 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @preventive-only @search-tree-rao
  Scenario: US 19.3.9: Test that the parameters are ignored in preventive
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us3case9.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao on preventive state
    Then 3 remedial actions are used in preventive
    And the remedial action "open_be1_be4" is used in preventive
    And the remedial action "open_fr1_fr2" is used in preventive
    And the tap of PstRangeAction "pst_be" should be -15 in preventive

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.3.10: One PST, limiting element changes
    # <!> All RAs are hypothetically operated by "be"
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us3case10.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr2" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 11 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 5 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 399 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"
    And the margin on cnec "FFR1AA1  FFR2AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 900 A
