# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: User Story #16.5: activate remedial actions only after a constraint in a specific country

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 16.5.1: Preventive onConstraint RA with constraint on base network
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us5case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 1 remedial actions are used in preventive
    Then the remedial action "open_fr1_fr3" is used in preventive
    Then the worst margin is -135 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -135 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be -134 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be 48 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 308 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be 492 A

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 16.5.2: Preventive onConstraint RA with constraint triggered by another preventive RA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us5case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 2 remedial actions are used in preventive
    Then the remedial action "open_fr1_fr3" is used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the worst margin is 45 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 45 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be 158 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be 264 A

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 16.5.3: Preventive onConstraint RA with no constraint triggered
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us5case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 1 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the worst margin is -45 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be -45 A
    Then the margin on cnec "FFR1AA1  FFR4AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be -41 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after PRA should be 81 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.5.4: Curative onConstraint RA with constraint after contingency
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us5case4.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 2 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is -37 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -37 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -13 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.5.5: Curative onConstraint RA with constraint triggered by another curative RA
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us5case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 2 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is -37 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -37 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -13 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.5.6: Curative onConstraint RA with no constraint triggered
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us5case6.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 43 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 43 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 80 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.5.7: Preventive and curative onConstraint RA with constraint triggered on base network
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us5case7.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 1 remedial actions are used in preventive
    Then the remedial action "pst_be" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be 16 in preventive
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 82 A
    Then the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 82 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 245 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 345 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.5.8: Preventive and curative onConstraint RA with constraint triggered after contingency
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/SL_ep16us5case8.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 1 remedial actions are used in preventive
    Then the remedial action "pst_be" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be 16 in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be -1 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 63 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 63 A
    Then the margin on cnec "BBE1AA1  FFR5AA1  1 - preventive" after PRA should be 82 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be 72 A

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 16.5.9: Preventive onConstraint RA with no constraint triggered (CSE)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/CseCrac_16_5_9.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 1 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the worst margin is -45 A
    Then the margin on cnec "be2_fr3_n - BBE2AA1 ->FFR3AA1  - preventive" after PRA should be -45 A
    Then the margin on cnec "fr1_fr4_co1 - FFR1AA1 ->FFR4AA1   - co1_fr2_fr3_1 - curative" after PRA should be -41 A
    Then the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after PRA should be 81 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.5.10: Preventive and curative onConstraint RA with constraint triggered on the base network (CSE)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/CseCrac_16_5_10.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 1 remedial actions are used in preventive
    Then the remedial action "PST_pst_be_pra_BBE2AA1  BBE3AA1  1" is used in preventive
    Then the tap of PstRangeAction "PST_pst_be_pra_BBE2AA1  BBE3AA1  1" should be 16 in preventive
    Then 0 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 82 A
    Then the margin on cnec "be1_fr5_n - BBE1AA1 ->FFR5AA1  - preventive" after PRA should be 82 A
    Then the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 245 A
    Then the margin on cnec "fr3_fr5_co1 - FFR3AA1 ->FFR5AA1   - co1_fr2_fr3_1 - curative" after CRA should be 345 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.5.11: Preventive and curative onConstraint RA with constraint triggered after contingency (CSE)
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/CseCrac_16_5_11.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 1 remedial actions are used in preventive
    Then the remedial action "PST_pst_be_pra_BBE2AA1  BBE3AA1  1" is used in preventive
    Then the tap of PstRangeAction "PST_pst_be_pra_BBE2AA1  BBE3AA1  1" should be 16 in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "PST_pst_be_cra_BBE2AA1  BBE3AA1  1" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "PST_pst_be_cra_BBE2AA1  BBE3AA1  1" should be -1 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is 63 A
    Then the margin on cnec "fr3_fr5_co1 - FFR3AA1 ->FFR5AA1   - co1_fr2_fr3_1 - curative" after CRA should be 63 A
    Then the margin on cnec "be1_fr5_n - BBE1AA1 ->FFR5AA1  - preventive" after PRA should be 82 A
    Then the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after CRA should be 72 A

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 16.5.12: Preventive onConstraint RA with constraint triggered on outage CNEC
    Given network file is "common/TestCase16Nodes.uct"
    Given crac file is "epic16/CseCrac_16_5_12.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then 2 remedial actions are used in preventive
    Then the remedial action "open_fr1_fr3" is used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the worst margin is 45 A
    Then the margin on cnec "be2_fr3_n - BBE2AA1 ->FFR3AA1  - preventive" after PRA should be 45 A
    Then the margin on cnec "fr1_fr4_co1 - FFR1AA1 ->FFR4AA1   - co1_fr2_fr3_1 - outage" after PRA should be 158 A
    Then the margin on cnec "be2_fr3_co1 - BBE2AA1 ->FFR3AA1   - co1_fr2_fr3_1 - curative" after PRA should be 264 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @ampere
  Scenario: US 16.5.13: Test with a maximum number of CRA
    Given network file is "epic16/TestCase16Nodes_3psts.uct"
    Given crac file is "epic16/SL_ep16us5case13.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 0 remedial actions are used in preventive
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "pst_be" is used after "co1_fr2_fr3_1" at "curative"
    Then the tap of PstRangeAction "pst_be" should be 2 after "co1_fr2_fr3_1" at "curative"
    Then the worst margin is -37 A
    Then the margin on cnec "FFR3AA1  FFR5AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -37 A
    Then the margin on cnec "BBE2AA1  FFR3AA1  1 - co1_fr2_fr3_1 - curative" after CRA should be -13 A

  @fast @rao @ac @contingency-scenarios @max-min-margin @megawatt
  Scenario: US 16.5.14: Trigger ARA only after a given outage
    Given network file is "epic16/12Nodes3ParallelLines.uct"
    Given crac file is "epic16/crac_16_5_14.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    # An overload is created in the Netherlands only after co_nl1_nl_2_1
    # Thus, the OnFlowConstraintInCountry ARA must be triggered only after this contingency
    Then 1 remedial actions are used after "co_nl1_nl_2_1" at "auto"
    Then the remedial action "open_nl1_nl2_2" is used after "co_nl1_nl_2_1" at "auto"
    Then 0 remedial actions are used after "co_nl1_nl_2_3" at "auto"

  @fast @rao @ac @contingency-scenarios @max-min-margin @megawatt
  Scenario: US 16.5.14.bis: Sensi fail in auto, fallback
    Given network file is "epic16/12Nodes3ParallelLines.uct"
    Given crac file is "epic16/crac_16_5_14_bis.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    # An overload is created in the Netherlands only after co_nl1_nl_2_1
    # Thus, the OnFlowConstraintInCountry ARA must be triggered only after this contingency
    # But sensi fails : only cnec defined for this state matched the contigency
    # Thus, RAO fallbacks to initial solution
    Then the execution details should be "First preventive fell back to initial situation"


  @fast @rao @ac @contingency-scenarios @max-min-margin @megawatt
  Scenario: US 16.5.15: Trigger CRA only after a given outage
    Given network file is "epic16/12Nodes4ParallelLines.uct"
    Given crac file is "epic16/crac_16_5_15.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    # An overload is created in the Netherlands only after co_nl1_nl_2_1
    # Thus, the OnFlowConstraintInCountry CRA must be triggered only after this contingency
    Then 1 remedial actions are used after "co_nl1_nl_2_1" at "curative"
    Then the remedial action "close_nl1_nl2_4" is used after "co_nl1_nl_2_1" at "curative"
    Then 0 remedial actions are used after "co_nl1_nl_2_3" at "curative"
