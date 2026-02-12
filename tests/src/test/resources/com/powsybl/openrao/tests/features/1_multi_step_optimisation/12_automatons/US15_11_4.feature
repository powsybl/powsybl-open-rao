# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.11.4: ARAO with 2P

  @fast @rao @ac @second-preventive @mnec @max-min-margin @megawatt
  Scenario: US 15.11.4.1: Check that PRAO2 ignores applied ARAs from ARAO1
    Given network file is "common/TestCase12Nodes2PSTs.uct" for CORE CC
    Given crac file is "epic15/jsonCrac_ep15us11-4case1.json"
    Given configuration file is "epic15/RaoParameters_ep15us11-4.json"
    When I launch rao
    # With the recent development, the RAO falls back to 1st PRAO result
    # because 2PRA increases the cost
    # Then the RAO falls back to 1st preventive RAO result
    Then 2 remedial actions are used in preventive
    Then the remedial action "Open line NL1-NL2" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be -6 in preventive
    Then 1 remedial actions are used after "Contingency_FR1_FR3" at "auto"
    Then 0 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    Then the worst margin is -140 MW
    Then the value of the objective function after CRA should be 140
    Then the margin on cnec "FR2-FR3-O - preventive" after PRA should be -140 MW
    Then the margin on cnec "NL2-BE3-O - curative" after CRA should be 3.7 MW
    Then the execution details should be "Second preventive improved first preventive results"

  @fast @rao @ac @second-preventive @mnec @max-min-margin @megawatt
  Scenario: US 15.11.4.2: ARAO2
    Given network file is "common/TestCase12Nodes2PSTs.uct" for CORE CC
    Given crac file is "epic15/jsonCrac_ep15us11-4case2.json"
    Given configuration file is "epic15/RaoParameters_ep15us11-4.json"
    When I launch rao at "2019-01-08 12:00"
    Then 2 remedial actions are used in preventive
    Then the remedial action "Open line NL1-NL2" is used in preventive
    Then the tap of PstRangeAction "PRA_CRA_PST_BE" should be -6 in preventive
    Then 1 remedial actions are used after "Contingency_FR1_FR3" at "auto"
    Then the tap of PstRangeAction "ARA_PST_DE" should be 4 after "Contingency_FR1_FR3" at "auto"
    Then 0 remedial actions are used after "Contingency_FR1_FR3" at "curative"
    # PST PRA_CRA_PST_BE was set to 16 during first curative. But during second preventive optimization,
    # its value was not kept in order to be reoptimized, but wasn't.
    Then the remedial action "PRA_CRA_PST_BE" is not used after "Contingency_FR1_FR3" at "curative"
    Then the worst margin is -141 MW
    Then the value of the objective function after CRA should be 141
    Then the margin on cnec "NL2-BE3-O - curative" after CRA should be 7 MW
    Then the execution details should be "Second preventive improved first preventive results"