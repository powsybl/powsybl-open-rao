# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 0.1.2.1: HVDC
  This feature covers the behaviour of a computation with specific HVDC equivalent models.

  @fast @rao @dc @contingency-scenarios @hvdc @max-min-margin
  Scenario: 0.1.2.1.1 : Outage HVDC modelling 1 (CORE's Cobra)
    Given network file is "common/TestCase12NodesHvdc.uct" for CORE CC
    Given crac file is "epic14/cbcora_ep14us4case1.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the initial margin on cnec "004_FR-DE - outage" should be 501.0 MW
    Then the initial margin on cnec "003_FR-DE - curative" should be 501.0 MW
    Then the initial margin on cnec "002_FR-DE - preventive" should be 932.0 MW
    Then the worst margin is 691.0 MW
    Then the margin on cnec "004_FR-DE - outage" after PRA should be 691.0 MW
    Then the margin on cnec "003_FR-DE - curative" after CRA should be 857.0 MW
    Then the margin on cnec "002_FR-DE - preventive" after PRA should be 1102.0 MW

  @fast @rao @dc @contingency-scenarios @hvdc @max-min-margin
  Scenario: 0.1.2.1.2 : Outage HVDC modelling 2 (CORE's Alegro)
    Given network file is "common/TestCase12NodesHvdc.uct" for CORE CC
    Given crac file is "epic14/cbcora_ep14us4case2.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the initial margin on cnec "004_FR-DE - outage" should be 501.0 MW
    Then the initial margin on cnec "003_FR-DE - curative" should be 501.0 MW
    Then the initial margin on cnec "002_FR-DE - preventive" should be 932.0 MW
    Then the worst margin is 691.0 MW
    Then the margin on cnec "004_FR-DE - outage" after PRA should be 691.0 MW
    Then the margin on cnec "003_FR-DE - curative" after CRA should be 857.0 MW
    Then the margin on cnec "002_FR-DE - preventive" after PRA should be 1102.0 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: 0.1.2.1.3 : German HVDC Preventive (CORE's german HVDC)
  German HVDC are set as preventive remedial actions and should be used at preventive state.
  Aligned generators (generators of the same station) should be moved together and have the same value.
  The two generators involved in one HVDC line should have opposite values.
    Given network file is "epic14/TestCase12NodesUltranet_v2.uct" for CORE CC
    Given crac file is "epic14/crac_hvdc_preventive.xml"
    Given crac creation parameters file is "epic14/ccp.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2026-01-27 17:00"
    Then the remedial action "D7_RA_99991 + D4_RA_99991" is used in preventive
    And the remedial action "D7_RA_99992 + D4_RA_99992" is used in preventive
    And the initial margin on cnec "CB0 - outage" should be 4246.1 MW
    And the initial margin on cnec "CB0 - curative" should be 4246.1 MW
    And the initial setpoint of RangeAction "D7_RA_99991 + D4_RA_99991" should be 0
    And the initial setpoint of RangeAction "D7_RA_99992 + D4_RA_99992" should be 0
    And the margin on cnec "CB0 - outage" after PRA should be 5646.1 MW
    And the margin on cnec "CB0 - curative" after PRA should be 5646.1 MW
    And the setpoint of RangeAction "D7_RA_99991 + D4_RA_99991" should be 700 MW in preventive
    And the setpoint of RangeAction "D7_RA_99992 + D4_RA_99992" should be 700 MW in preventive
    And the generator "D7AAA11A_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D7AAA21A_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D4AAA11B_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D4AAA21B_generator" should have a targetP of 0.0 MW at initial state
    # Aligned generators have the same targetP: D7AAA11A_generator & D7AAA21A_generator ; D4AAA11B_generator & D4AAA21B_generator
    # Generators of the same RA have opposed targetP: D7AAA11A_generator & D4AAA11B_generator ; D4AAA21B_generator & D7AAA21A_generator
    And the generator "D7AAA11A_generator" should have a targetP of -700.0 MW after PRA
    And the generator "D7AAA21A_generator" should have a targetP of -700.0 MW after PRA
    And the generator "D4AAA11B_generator" should have a targetP of 700.0 MW after PRA
    And the generator "D4AAA21B_generator" should have a targetP of 700.0 MW after PRA
    And the worst margin is 5646.1 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: 0.1.2.1.3b : German HVDC Preventive (CORE's german HVDC) with VirtualHubs file as input
  Same test as scenario 0.1.2.1.3 but using a VirtualHubs file as input (from which the internal-hvdcs data will be extracted)
  instead of a CracCreationContext where internal-hvdcs data would already be stored
    Given network file is "epic14/TestCase12NodesUltranet_v2.uct" for CORE CC
    Given crac file is "epic14/crac_hvdc_preventive.xml"
    Given Virtual hubs configuration file is "conf_virtual_hub_internal_hvdc.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2026-01-27 17:00"
    Then the remedial action "D7_RA_99991 + D4_RA_99991" is used in preventive
    And the remedial action "D7_RA_99992 + D4_RA_99992" is used in preventive
    And the initial margin on cnec "CB0 - outage" should be 4246.1 MW
    And the initial margin on cnec "CB0 - curative" should be 4246.1 MW
    And the initial setpoint of RangeAction "D7_RA_99991 + D4_RA_99991" should be 0
    And the initial setpoint of RangeAction "D7_RA_99992 + D4_RA_99992" should be 0
    And the margin on cnec "CB0 - outage" after PRA should be 5646.1 MW
    And the margin on cnec "CB0 - curative" after PRA should be 5646.1 MW
    And the setpoint of RangeAction "D7_RA_99991 + D4_RA_99991" should be 700 MW in preventive
    And the setpoint of RangeAction "D7_RA_99992 + D4_RA_99992" should be 700 MW in preventive
    And the generator "D7AAA11A_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D7AAA21A_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D4AAA11B_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D4AAA21B_generator" should have a targetP of 0.0 MW at initial state
    # Aligned generators have the same targetP: D7AAA11A_generator & D7AAA21A_generator ; D4AAA11B_generator & D4AAA21B_generator
    # Generators of the same RA have opposed targetP: D7AAA11A_generator & D4AAA11B_generator ; D4AAA21B_generator & D7AAA21A_generator
    And the generator "D7AAA11A_generator" should have a targetP of -700.0 MW after PRA
    And the generator "D7AAA21A_generator" should have a targetP of -700.0 MW after PRA
    And the generator "D4AAA11B_generator" should have a targetP of 700.0 MW after PRA
    And the generator "D4AAA21B_generator" should have a targetP of 700.0 MW after PRA
    And the worst margin is 5646.1 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: 0.1.2.1.4 : German HVDC Curative (CORE's german HVDC)
  German HVDC are set as curative remedial actions and should be used at curative state, but not at preventive state.
  Aligned generators (generators of the same station) should be moved together and have the same value.
  The two generators involved in one HVDC line should have opposite values.
    Given network file is "epic14/TestCase12NodesUltranet_v2.uct" for CORE CC
    Given crac file is "epic14/crac_hvdc_curative.xml"
    Given crac creation parameters file is "epic14/ccp.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2026-01-27 17:00"
    Then the remedial action "D7_RA_99991 + D4_RA_99991" is not used in preventive
    And the remedial action "D7_RA_99992 + D4_RA_99992" is not used in preventive
    And the remedial action "D7_RA_99991 + D4_RA_99991" is used after "OUTAGE_1" at "curative"
    And the remedial action "D7_RA_99992 + D4_RA_99992" is used after "OUTAGE_1" at "curative"
    And the initial margin on cnec "CB0 - outage" should be 4246.1 MW
    And the initial margin on cnec "CB0 - curative" should be 4246.1 MW
    And the initial setpoint of RangeAction "D7_RA_99991 + D4_RA_99991" should be 0
    And the initial setpoint of RangeAction "D7_RA_99992 + D4_RA_99992" should be 0
    And the margin on cnec "CB0 - outage" after PRA should be 4246.1 MW
    And the margin on cnec "CB0 - curative" after PRA should be 4246.1 MW
    And the setpoint of RangeAction "D7_RA_99991 + D4_RA_99991" should be 0 MW in preventive
    And the setpoint of RangeAction "D7_RA_99992 + D4_RA_99992" should be 0 MW in preventive
    And the margin on cnec "CB0 - outage" after CRA should be 4246.1 MW
    And the margin on cnec "CB0 - curative" after CRA should be 5646.1 MW
    And the setpoint of RangeAction "D7_RA_99991 + D4_RA_99991" should be 700 MW after "OUTAGE_1" at "curative"
    And the setpoint of RangeAction "D7_RA_99992 + D4_RA_99992" should be 700 MW after "OUTAGE_1" at "curative"
    # Remedial actions are curative, so there should be no change in generators on preventive state
    And the generator "D7AAA11A_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D7AAA21A_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D4AAA11B_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D4AAA21B_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D7AAA11A_generator" should have a targetP of 0.0 MW after PRA
    And the generator "D7AAA21A_generator" should have a targetP of 0.0 MW after PRA
    And the generator "D4AAA11B_generator" should have a targetP of 0.0 MW after PRA
    And the generator "D4AAA21B_generator" should have a targetP of 0.0 MW after PRA
    # Aligned generators have the same targetP: D7AAA11A_generator & D7AAA21A_generator ; D4AAA11B_generator & D4AAA21B_generator
    # Generators of the same RA have opposed targetP: D7AAA11A_generator & D4AAA11B_generator ; D4AAA21B_generator & D7AAA21A_generator
    And the generator "D7AAA11A_generator" should have a targetP of -700.0 MW after CRA
    And the generator "D7AAA21A_generator" should have a targetP of -700.0 MW after CRA
    And the generator "D4AAA11B_generator" should have a targetP of 700.0 MW after CRA
    And the generator "D4AAA21B_generator" should have a targetP of 700.0 MW after CRA
    And the worst margin is 2746.1 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: 0.1.2.1.4b : German HVDC Curative (CORE's german HVDC) with VirtualHubs file as input
  Same test as scenario 0.1.2.1.4 but using a VirtualHubs file as input (from which the internal-hvdcs data will be extracted)
  instead of a CracCreationContext where internal-hvdcs data would already be stored
    Given network file is "epic14/TestCase12NodesUltranet_v2.uct" for CORE CC
    Given crac file is "epic14/crac_hvdc_curative.xml"
    Given Virtual hubs configuration file is "conf_virtual_hub_internal_hvdc.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2026-01-27 17:00"
    Then the remedial action "D7_RA_99991 + D4_RA_99991" is not used in preventive
    And the remedial action "D7_RA_99992 + D4_RA_99992" is not used in preventive
    And the remedial action "D7_RA_99991 + D4_RA_99991" is used after "OUTAGE_1" at "curative"
    And the remedial action "D7_RA_99992 + D4_RA_99992" is used after "OUTAGE_1" at "curative"
    And the initial margin on cnec "CB0 - outage" should be 4246.1 MW
    And the initial margin on cnec "CB0 - curative" should be 4246.1 MW
    And the initial setpoint of RangeAction "D7_RA_99991 + D4_RA_99991" should be 0
    And the initial setpoint of RangeAction "D7_RA_99992 + D4_RA_99992" should be 0
    And the margin on cnec "CB0 - outage" after PRA should be 4246.1 MW
    And the margin on cnec "CB0 - curative" after PRA should be 4246.1 MW
    And the setpoint of RangeAction "D7_RA_99991 + D4_RA_99991" should be 0 MW in preventive
    And the setpoint of RangeAction "D7_RA_99992 + D4_RA_99992" should be 0 MW in preventive
    And the margin on cnec "CB0 - outage" after CRA should be 4246.1 MW
    And the margin on cnec "CB0 - curative" after CRA should be 5646.1 MW
    And the setpoint of RangeAction "D7_RA_99991 + D4_RA_99991" should be 700 MW after "OUTAGE_1" at "curative"
    And the setpoint of RangeAction "D7_RA_99992 + D4_RA_99992" should be 700 MW after "OUTAGE_1" at "curative"
    # Remedial actions are curative, so there should be no change in generators on preventive state
    And the generator "D7AAA11A_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D7AAA21A_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D4AAA11B_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D4AAA21B_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D7AAA11A_generator" should have a targetP of 0.0 MW after PRA
    And the generator "D7AAA21A_generator" should have a targetP of 0.0 MW after PRA
    And the generator "D4AAA11B_generator" should have a targetP of 0.0 MW after PRA
    And the generator "D4AAA21B_generator" should have a targetP of 0.0 MW after PRA
    # Aligned generators have the same targetP: D7AAA11A_generator & D7AAA21A_generator ; D4AAA11B_generator & D4AAA21B_generator
    # Generators of the same RA have opposed targetP: D7AAA11A_generator & D4AAA11B_generator ; D4AAA21B_generator & D7AAA21A_generator
    And the generator "D7AAA11A_generator" should have a targetP of -700.0 MW after CRA
    And the generator "D7AAA21A_generator" should have a targetP of -700.0 MW after CRA
    And the generator "D4AAA11B_generator" should have a targetP of 700.0 MW after CRA
    And the generator "D4AAA21B_generator" should have a targetP of 700.0 MW after CRA
    And the worst margin is 2746.1 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: 0.1.2.1.5 : German HVDC with no alignment of generators (CORE's german HVDC)
  This case tests an inconsistent situation that should not happen in real life:
  Two parallel German HVDC lines are defined but one is set as preventive and the other is set as curative.
  In such a situation, generators alignment would not work and generators could be moved separately.
  To avoid inconsistent behaviour, inconsistent RAs are removed from Crac before the RAO is launched to avoid using them during the optimisation.
    Given network file is "epic14/TestCase12NodesUltranet_v2.uct" for CORE CC
    Given crac file is "epic14/crac_hvdc_preventive_and_curative.xml"
    Given crac creation parameters file is "epic14/ccp.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2026-01-27 17:00"
    # HVDC remedial actions have different usage rules => they should not be used during optimisation
    Then 0 remedial actions are used in preventive
    And 0 remedial actions are used after "OUTAGE_1" at "curative"
    And the worst margin is 4246.1 MW
