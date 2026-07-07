# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 2.1.6: Island creation because of network actions
  This feature covers the issue of network actions that create electrical islands

  There are three ways to create an island:
  - after applying one or several network actions
  - a network action combined with a contingency:
      -> a network action is used in curative and combined with the contingency we create an island.
      -> a network action is applied in preventive, but combined with a contingency we get an island.

  @fast @rao @ac @max-min-margin
  Scenario: 2.1.6.1: Simple case with two CNECs and 1 network action that create an island
    We have here a simple case where
    - one CNEC "DDE1AA1  DDE2AA1  1 - preventive" is overloaded
    - one CNEC "NNL2AA1  NNL3AA1  1 - preventive" that is not overloaded and will not be in the electrical island.
    - opening the line "DDE2AA1  NNL3AA1  1" resolves the overload by creating an island (DDE1AA1, DDE2AA1 & DDE3AA1)
  -> the flow is considered equal to 0 A on the CNEC
    Note: we had to add at least one CNEC not in the island in the CRAC (NNL2AA1  NNL3AA1  1 - preventive) to not get a sensitivity computation error.
    Given network file is "2_remedial_actions/2_1_network_actions_optimisation/network_2_1_6_1.uct"
    Given crac file is "2_remedial_actions/2_1_network_actions_optimisation/crac_2_1_6_1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_ac.json"
    When I launch rao
    Then the initial flow on cnec "DDE1AA1  DDE2AA1  1 - preventive" should be -1203 A on side 1
    Then the remedial action "open_DDE2AA1  NNL3AA1  1" is used in preventive
    Then the margin on cnec "DDE1AA1  DDE2AA1  1 - preventive" after PRA should be 1000 A
    Then the flow on cnec "DDE1AA1  DDE2AA1  1 - preventive" after PRA should be 0 A on side 1

  @fast @rao @ac @max-min-margin
  Scenario: 2.1.6.2: Simple case with one cnecs and 1 network actions that create an island
    Same case as 2.1.6.1, but only the CNEC "DDE1AA1  DDE2AA1  1 - preventive" is defined in the CRAC.
    The network action won't be applied, the sensitivity result status is set to FAILED because
    the status is set to SUCCESS only if at least one CNEC has a flow that is not NaN after the sensi computation.
    (If the flow or sensi value is 0 after OLF sensi computation -> it is set to 0)
    Given network file is "2_remedial_actions/2_1_network_actions_optimisation/network_2_1_6_1.uct"
    Given crac file is "2_remedial_actions/2_1_network_actions_optimisation/crac_2_1_6_2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_ac.json"
    When I launch rao
    Then the initial flow on cnec "DDE1AA1  DDE2AA1  1 - preventive" should be -1203 A on side 1
    Then 0 remedial actions are used in preventive

  @fast @rao @ac @contingency-scenarios @max-min-margin
  Scenario: 2.1.6.3: An island is created after a contingency
    Same case as 2.1.6.1, but initially all the lines are closed.
    We loose the line DDE3AA1 FFR2AA1 1 because of a contingency. We look at the line "DDE1AA1  DDE2AA1  1"
    that get overloaded after the contingency and using the network action "open_DDE2AA1  NNL3AA1  1" "solves" the overload by creating an island.
    Given network file is "2_remedial_actions/2_1_network_actions_optimisation/network_2_1_6_3.uct"
    Given crac file is "2_remedial_actions/2_1_network_actions_optimisation/crac_2_1_6_3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_ac.json"
    When I launch rao
    Then the initial margin on cnec "DDE1AA1  DDE2AA1  1 - preventive" should be 518 A
    Then the initial margin on cnec "DDE1AA1  DDE2AA1  1 - curative" should be -203 A
    Then 0 remedial actions are used in preventive
    Then 1 remedial actions are used after "CO_0001" at "curative"
    Then the remedial action "open_DDE2AA1  NNL3AA1  1" is used after "CO_0001" at "curative"
    Then the margin on cnec "DDE1AA1  DDE2AA1  1 - curative" after CRA should be 1000 A

  @fast @rao @ac @max-min-margin
  Scenario: 2.1.6.4: An island is created but all the production is in this island
  Same network architecture as as 2.1.6.1 BUT all the production is in the island that is created by the RAO
    which make the sensi computation fail (failed to distribute slack) -> the action is not used.
    If  "slackDistributionFailureBehavior" is set to "THROW"
    Given network file is "2_remedial_actions/2_1_network_actions_optimisation/network_2_1_6_4.uct"
    Given crac file is "2_remedial_actions/2_1_network_actions_optimisation/crac_2_1_6_1.json"
    Given configuration file is "2_remedial_actions/2_1_network_actions_optimisation/raoParameters_2_1_6_2_4.json"
    When I launch rao
    Then the initial margin on cnec "DDE1AA1  DDE2AA1  1 - preventive" should be -1890.53 A
    Then 0 remedial actions are used in preventive
    Then the margin on cnec "DDE1AA1  DDE2AA1  1 - preventive" after PRA should be -1890.53 A

  @fast @rao @ac @contingency-scenarios @max-min-margin
  Scenario: 2.1.6.5: An island is created in the curative perimeter because of a network action taken in preventive
    Same network as 2.1.6.3, in preventive the network action "open_FFR2AA1  DDE3AA1  1" is used to solve the overload on "FFR2AA1  FFR3AA1  1 - preventive"
    However applying this network action on top of the contingency on  DDE2AA1  NNL3AA1  1 create an island.
    Given network file is "2_remedial_actions/2_1_network_actions_optimisation/network_2_1_6_3.uct"
    Given crac file is "2_remedial_actions/2_1_network_actions_optimisation/crac_2_1_6_5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_ac.json"
    When I launch rao
    Then the initial margin on cnec "FFR2AA1  FFR3AA1  1 - preventive" should be -1007.99 A
    Then 1 remedial actions are used in preventive
    Then the remedial action "open_FFR2AA1  DDE3AA1  1" is used in preventive
    Then the margin on cnec "FFR2AA1  FFR3AA1  1 - preventive" after PRA should be 437.45 A
    Then the margin on cnec "DDE1AA1  DDE2AA1  1 - preventive" after PRA should be 96.87 A
    # After contingency N-1 on DDE2AA1  NNL3AA1  1, the DE area is isolated from the rest of the network
    Then the margin on cnec "FFR2AA1  FFR3AA1  1 - curative - CO_0001" after CRA should be 36.3 A
    Then the flow on cnec "DDE1AA1  DDE2AA1  1 - curative - CO_0001" after CRA should be 0 A on side 1 and 0 A on side 2
    Then the margin on cnec "DDE1AA1  DDE2AA1  1 - curative - CO_0001" after CRA should be 1000.00 A


