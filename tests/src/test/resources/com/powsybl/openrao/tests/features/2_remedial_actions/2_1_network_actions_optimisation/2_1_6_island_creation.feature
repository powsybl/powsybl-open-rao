# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 2.1.6: Island creation with network actions
  This feature covers network actions that create electrical island

  There are two way of creating an island:
  - after applying one or several network actions
  - a network actions combined with a contingency

  @fast @rao @ac @contingency-scenarios @max-min-margin
  Scenario: 2.1.6.1: Simple case with two cnecs and 1 network actions that create an island
    We have here a simple case where
    - one CNEC "DDE1AA1  DDE2AA1  1 - preventive" is overloaded
    - one CNEC "NNL2AA1  NNL3AA1  1 - preventive" that is not overload and will not be in the electrical island.
    - opening the line "DDE2AA1  NNL3AA1  1" resolve this overload by creating an island (DDE1AA1, DDE2AA1 & DDE3AA1)
  -> the flow is considered equal to 0 A on the CNEC -> secure the network
    Note: we had to add at least one CNEC not in the island in the CRAC (NNL2AA1  NNL3AA1  1 - preventive) to not get a sensitivity computation error.
    Given network file is "2_remedial_actions/2_1_network_actions_optimisation/network_2_1_6_1.uct"
    Given crac file is "2_remedial_actions/2_1_network_actions_optimisation/crac_2_1_6_1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_ac.json"
    When I launch rao
    Then the initial flow on cnec "DDE1AA1  DDE2AA1  1 - preventive" should be -1203 A on side 1
    Then the remedial action "open_DDE2AA1  NNL3AA1  1" is used in preventive
    Then the margin on cnec "DDE1AA1  DDE2AA1  1 - preventive" after PRA should be 200 A
    Then the flow on cnec "DDE1AA1  DDE2AA1  1 - preventive" after PRA should be 0 A on side 1

  @fast @rao @ac @contingency-scenarios @max-min-margin
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
    We lose the line DDE3AA1 FFR2AA1 1 because of a contingency. We look at the line "DDE1AA1  DDE2AA1  1"
    that get overloaded after the contingency.
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






