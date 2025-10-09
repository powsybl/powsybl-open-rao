# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 19.10: limit number of PSTs with a constraint directly in the optimisation

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.10.1: One PST and no topo (copy of US 19.3.3 with MIP for PSTs)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us10case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_mip.json"
    When I launch rao
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 945 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"
    And the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 1301 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.10.2: No PST and one topo (copy of US 19.3.4 with MIP for PSTs)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us10case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_mip.json"
    When I launch rao
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 840 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.10.3: One PST and one topo, one CRA, chose PST (copy of US 19.3.5 with MIP for PSTs)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us10case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_mip.json"
    When I launch rao
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 945 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"
    And the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 1301 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.10.4: Two allowed CRAs (copy of US 19.5.2 with MIP for PSTs)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us10case4.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_mip.json"
    When I launch rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -12 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 5 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 1000 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.10.5: One allowed CRA (copy of US 19.5.3 with MIP for PSTs)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us5case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_mip.json"
    When I launch rao
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 945 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.10.6: One allowed CRA, BE PST not allowed (copy of US 19.5.4 with MIP for PSTs)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us10case6.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_mip.json"
    When I launch rao
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 0 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 5 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 840 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.10.7: Two allowed TSOs - 3 TSOs in crac (copy of US 19.6.2 with MIP for PSTs)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us10case7.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_mip.json"
    When I launch rao
    Then 2 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be -12 after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 5 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 1000 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.10.8: One allowed TSO (copy of US 19.6.3 with MIP for PSTs)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us6case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_mip.json"
    When I launch rao
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_be" should be 16 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 945 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.10.9: One allowed TSO - BE PST not allowed (copy of US 19.6.4 with MIP for PSTs)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic19/SL_ep19us10case9.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_mip.json"
    When I launch rao
    Then 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "open_fr1_fr2" is used after "co1_fr2_fr3_1" at "curative"
    And the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
    And the tap of PstRangeAction "pst_fr" should be 2 after "co1_fr2_fr3_1" at "curative"
    And the worst margin is 998 A on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative"

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 19.10.10: UCTE HVDC as InjectionRangeAction and PST filtering (copy of US 15.12.7.8 with MIP for PSTs)
    Given network file is "epic15/TestCase16NodesWithUcteHvdc.uct"
    Given crac file is "epic15/cseCrac_ep15us12-5case8.xml"
    Given configuration file is "epic19/RaoParameters_19_10_10&11.json"
    Given crac creation parameters file is "epic19/us19_10_10&11.json"
    When I launch rao
    Then the setpoint of RangeAction "CRA_HVDC" should be 1406 MW after "co1_be1_fr5" at "curative"
    And the worst margin is 285 MW
    And the margin on cnec "be4_fr5_co1 - BBE4AA11->FFR5AA11  - co1_be1_fr5 - curative" after CRA should be 285 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 19.10.11: max-curative-ra : choose a PST over a HVDC
    Given network file is "epic15/TestCase16NodesWithUcteHvdc.uct"
    Given crac file is "epic19/cseCrac_ep19us10case11.xml"
    Given configuration file is "epic19/RaoParameters_19_10_10&11.json"
    Given crac creation parameters file is "epic19/us19_10_10&11.json"
    When I launch rao
    Then 1 remedial actions are used after "co1_be1_fr5" at "curative"
    And the tap of PstRangeAction "PST_CRA_PST_be_BBE2AA11 BBE3AA11 1" should be -16 after "co1_be1_fr5" at "curative"
    And the setpoint of RangeAction "CRA_HVDC" should be 0 MW after "co1_be1_fr5" at "curative"
    And the worst margin is 24.1 MW
    And the margin on cnec "be4_fr5_co1 - BBE2AA11->BBE3AA11  - co1_be1_fr5 - curative" after CRA should be 24.1 MW

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.10.12: Reference case w/o PRA limitations : 1 topo and 2 PST
    Given network file is "epic19/TestCase16Nodes_with_contingency.uct"
    Given crac file is "epic19/SL_ep19us10_ra_limits_ref.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_mip.json"
    When I launch rao
    Then 3 remedial actions are used in preventive
    And the remedial action "close_fr1_fr5" is used in preventive
    And the tap of PstRangeAction "pst_be" should be -8 in preventive
    And the tap of PstRangeAction "pst_fr" should be -4 in preventive
    And the worst margin is 999.68 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.10.13: Same case with PRA limitations: 2 RAs and only 1 Topo
    Given network file is "epic19/TestCase16Nodes_with_contingency.uct"
    Given crac file is "epic19/SL_ep19us10_ra_limits_1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_mip.json"
    When I launch rao
    Then 2 remedial actions are used in preventive
    # Asserts that the RAO is able to choose a topological action even if it as reached its limit in rootLeaf
    And the remedial action "close_fr1_fr5" is used in preventive
    And the tap of PstRangeAction "pst_be" should be -12 in preventive
    And the worst margin is 999.50 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 19.10.14: Same case with PRA limitations: 1 PST and 0 Topo
    Given network file is "epic19/TestCase16Nodes_with_contingency.uct"
    Given crac file is "epic19/SL_ep19us10_ra_limits_2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_mip.json"
    When I launch rao
    Then 1 remedial actions are used in preventive
    And the tap of PstRangeAction "pst_be" should be 16 in preventive
    And the worst margin is 945.38 A
