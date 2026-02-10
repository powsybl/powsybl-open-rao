# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 4.3: manage AC/DC modes from configuration

  @fast @rao @ac @preventive-only
  Scenario: US 4.3.1: secure with AC config
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic4/SL_ep4us3.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 38.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - curative" after PRA should be 38.0 A
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive

  @fast @rao @ac @preventive-only
  Scenario: US 4.3.2: failure with AC config without fallback
    Given network file is "epic4/US4-3-TestCase12Nodes-diverging.uct"
    Given crac file is "epic4/SL_ep4us3.json"
    Given configuration file is "epic4/RaoParameters_posMargin_ampere_ac_divergence.json"
    When I launch rao
    Then the calculation fails
    And the execution details should be "Initial sensitivity analysis failed"

  @fast @rao @dc @preventive-only
  Scenario: US 4.3.3: no failure with DC config
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic4/SL_ep4us3.json"
    Given configuration file is "common/RaoParameters_posMargin_megawatt_dc.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 32 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - curative" after PRA should be 32 MW
    Then the remedial action "PRA_PST_BE" is used in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive