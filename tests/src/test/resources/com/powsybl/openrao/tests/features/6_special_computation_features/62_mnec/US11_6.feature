# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 11.6: Handle MNECs in rao with a CSE CRAC

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 11.6.1: only network actions - ref run, no mnec
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic11/CSE_CRAC_11_6_1.xml"
    Given configuration file is "epic11/RaoParameters_posMargin_ampere_mnecDiminMinusInf.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then the initial margin on cnec "critical_branch - NNL2AA1 ->NNL3AA1  - preventive" should be -203.35 A
    Then the value of the objective function initially should be 203.35
    Then the remedial action "topo_remedial_action" is used in preventive
    Then the margin on cnec "critical_branch - NNL2AA1 ->NNL3AA1  - preventive" after PRA should be 759.43 A
    Then the value of the objective function after PRA should be -759.43

  @fast @rao @ac @preventive-only @mnec @secure-flow @ampere
  Scenario: US 11.6.2: only network actions - one unconstrained mnec
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic11/CSE_CRAC_11_6_2.xml"
    Given configuration file is "epic11/RaoParameters_posMargin_ampere_mnecDiminMinusInf.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then the initial margin on cnec "critical_branch - NNL2AA1 ->NNL3AA1  - preventive" should be -203.35 A
    Then the initial margin on cnec "monitored_element - FFR2AA1 ->DDE3AA1  - preventive" should be 2832.87 A
    Then the value of the objective function initially should be 203.35
    Then the remedial action "topo_remedial_action" is used in preventive
    Then the margin on cnec "critical_branch - NNL2AA1 ->NNL3AA1  - preventive" after PRA should be 759.43 A
    Then the margin on cnec "monitored_element - FFR2AA1 ->DDE3AA1  - preventive" after PRA should be 1380.43 A
    Then the value of the objective function after PRA should be -759.43

  @fast @rao @ac @preventive-only @mnec @secure-flow @ampere
  Scenario: US 11.6.3: only network actions - one constrained mnec
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic11/CSE_CRAC_11_6_3.xml"
    Given configuration file is "epic11/RaoParameters_posMargin_ampere_mnecDiminMinusInf.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then the initial margin on cnec "critical_branch - NNL2AA1 ->NNL3AA1  - preventive" should be -203.35 A
    Then the initial margin on cnec "monitored_element - FFR2AA1 ->DDE3AA1  - preventive" should be 1432.87 A
    Then the value of the objective function initially should be 203.35
    Then the remedial action "topo_remedial_action" is not used in preventive
    Then the margin on cnec "critical_branch - NNL2AA1 ->NNL3AA1  - preventive" after PRA should be -203.35 A
    Then the margin on cnec "monitored_element - FFR2AA1 ->DDE3AA1  - preventive" after PRA should be 1432.87 A
    Then the value of the objective function after PRA should be 203.35

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 11.6.4: pst range action - reference run, no mnec
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic11/CSE_CRAC_11_6_4.xml"
    Given configuration file is "epic11/RaoParameters_posMargin_ampere_mnecDiminMinusInf.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then the initial margin on cnec "critical_branch - BBE2AA1 ->BBE3AA1  - preventive" should be -162.76 A
    Then the value of the objective function initially should be 162.76
    Then the remedial action "PST_pst_remedial_action_BBE2AA1  BBE3AA1  1" is used in preventive
    Then the tap of PstRangeAction "PST_pst_remedial_action_BBE2AA1  BBE3AA1  1" should be 16 in preventive
    Then the margin on cnec "critical_branch - BBE2AA1 ->BBE3AA1  - preventive" after PRA should be 840.47 A
    Then the value of the objective function after PRA should be -840.47

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 11.6.5: pst range action - one unconstrained mnec
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic11/CSE_CRAC_11_6_5.xml"
    Given configuration file is "epic11/RaoParameters_posMargin_ampere_mnecDiminMinusInf.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then the initial margin on cnec "critical_branch - BBE2AA1 ->BBE3AA1  - preventive" should be -162.76 A
    Then the initial margin on cnec "monitored_element - BBE1AA1 ->BBE3AA1  - preventive" should be 840.55 A
    Then the value of the objective function initially should be 162.76
    Then the remedial action "PST_pst_remedial_action_BBE2AA1  BBE3AA1  1" is used in preventive
    Then the tap of PstRangeAction "PST_pst_remedial_action_BBE2AA1  BBE3AA1  1" should be 16 in preventive
    Then the margin on cnec "critical_branch - BBE2AA1 ->BBE3AA1  - preventive" after PRA should be 840.47 A
    Then the margin on cnec "monitored_element - BBE1AA1 ->BBE3AA1  - preventive" after PRA should be 88.00 A
    Then the value of the objective function after PRA should be -840.47

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 11.6.6: pst range action - one constrained mnec
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic11/CSE_CRAC_11_6_6.xml"
    Given configuration file is "epic11/RaoParameters_posMargin_ampere_mnecDiminMinusInf.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then the initial margin on cnec "critical_branch - BBE2AA1 ->BBE3AA1  - preventive" should be -162.76 A
    Then the initial margin on cnec "monitored_element - BBE1AA1 ->BBE3AA1  - preventive" should be 640.55 A
    Then the value of the objective function initially should be 162.76
    Then the remedial action "PST_pst_remedial_action_BBE2AA1  BBE3AA1  1" is used in preventive
    Then the tap of PstRangeAction "PST_pst_remedial_action_BBE2AA1  BBE3AA1  1" should be 13 in preventive
    Then the margin on cnec "critical_branch - BBE2AA1 ->BBE3AA1  - preventive" after PRA should be 652.70 A
    Then the margin on cnec "monitored_element - BBE1AA1 ->BBE3AA1  - preventive" after PRA should be 28.88 A
    Then the value of the objective function after PRA should be -652.70