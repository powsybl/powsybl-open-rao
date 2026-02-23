# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 4.2: Computation in MAX_MIN_MARGIN: optimization in A/MW, thresholds in A/MW, computation in AC/DC
  This feature covers the parameter load-flow-parameters/dc, from the RaoParameters.

  @fast @rao @dc @preventive-only @max-min-margin @megawatt
  Scenario: US 4.2.1.1: MW thresholds in DC mode and min margin in MW
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic4/SL_ep4us2_4MR_MW.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 22 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - curative" after PRA should be 22 MW
    Then the value of the objective function after CRA should be -22.0
    Then the tap of PstRangeAction "PRA_PST_BE" should be 5 in preventive
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 22.4 MW
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 24.1 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 44.0 MW

  @fast @rao @dc @preventive-only @max-min-margin @megawatt
  Scenario: US 4.2.1.2: MW thresholds in AC mode and min margin in MW
  Same data as US 4.2.1, but the computation is in AC.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic4/SL_ep4us2_4MR_MW.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 18.0 MW
    Then the value of the objective function after CRA should be -18.0
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - curative" after PRA should be 18 MW
    Then the value of the objective function after CRA should be -18.0
    Then the tap of PstRangeAction "PRA_PST_BE" should be 5 in preventive
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 22.4 MW
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 24.1 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 44.0 MW

  @fast @rao @dc @preventive-only @max-min-margin @megawatt
  Scenario: US 4.2.2.1: A thresholds in DC mode and min margin in MW
  Same inputs as US 4.2.1.1, but the thresholds are defined in A in the CRAC.
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic4/SL_ep4us2_4MR_A.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 15.07 MW
    Then the value of the objective function after CRA should be -15.07
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 15.07 MW
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 23.38 MW
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 15.07 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 45.12 MW

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 4.2.2.2: A thresholds in AC mode and min margin in A
  Same data as US 4.2.2.1, but the computation is in AC.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic4/SL_ep4us2_4MR_A.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 19.0 A
    Then the value of the objective function after CRA should be -19.0
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 19.0 A
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 19.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 31.0 A
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 51.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 63.0 A

  @fast @rao @dc @preventive-only @max-min-margin @megawatt
  Scenario: US 4.2.3.1: mixed thresholds in DC mode and min margin in MW
  Same inputs as US 4.2.1.1, but some thresholds are defined in A in the CRAC (and others in MW).
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic4/SL_ep4us2_4MR_mixed.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 18.52 MW
    Then the value of the objective function after CRA should be -18.52
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 18.52 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 23.38 MW

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 4.2.3.2: mixed thresholds in AC mode and min margin in A
  Same data as US 4.2.3.1, but the computation is in AC.
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic4/SL_ep4us2_4MR_mixed.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_ac.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 24 A
    Then the value of the objective function after CRA should be -24
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive
    Then the margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 24.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 31.5 A