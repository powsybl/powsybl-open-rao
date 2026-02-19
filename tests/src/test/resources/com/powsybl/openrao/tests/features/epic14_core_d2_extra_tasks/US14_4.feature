# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 14.4: HVDC

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 14.4.1 : Outage HVDC modelling 1 (CORE's Cobra)
    Given network file is "epic14/TestCase12NodesHvdc.uct" for CORE CC
    Given crac file is "epic14/cbcora_ep14us4case1.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2019-01-08 12:00"
    Then the initial margin on cnec "004_FR-DE - outage" should be 501.0 MW
    And the initial margin on cnec "003_FR-DE - curative" should be 501.0 MW
    And the initial margin on cnec "002_FR-DE - preventive" should be 932.0 MW
    And the worst margin is 691.0 MW
    And the margin on cnec "004_FR-DE - outage" after PRA should be 691.0 MW
    And the margin on cnec "003_FR-DE - curative" after CRA should be 857.0 MW
    And the margin on cnec "002_FR-DE - preventive" after PRA should be 1102.0 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 14.4.2 : Outage HVDC modelling 2 (CORE's Alegro)
    Given network file is "epic14/TestCase12NodesHvdc.uct" for CORE CC
    Given crac file is "epic14/cbcora_ep14us4case2.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2019-01-08 12:00"
    Then the initial margin on cnec "004_FR-DE - outage" should be 501.0 MW
    And the initial margin on cnec "003_FR-DE - curative" should be 501.0 MW
    And the initial margin on cnec "002_FR-DE - preventive" should be 932.0 MW
    And the worst margin is 691.0 MW
    And the margin on cnec "004_FR-DE - outage" after PRA should be 691.0 MW
    And the margin on cnec "003_FR-DE - curative" after CRA should be 857.0 MW
    And the margin on cnec "002_FR-DE - preventive" after PRA should be 1102.0 MW

  @fast @rao @mock @dc @contingency-scenarios @hvdc
  Scenario: US 14.4.3 : German HVDC Preventive (CORE's german HVDC)
    Given network file is "epic14/TestCase12NodesUltranet_v2.uct" for CORE CC
    Given crac file is "epic14/crac_hvdc_preventive.xml"
    Given crac creation parameters file is "epic14/ccp.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2026-01-27 17:00"
    Then the remedial action "D7_RA_99991_D4_RA_99991" is used in preventive
    And the remedial action "D7_RA_99992_D4_RA_99992" is used in preventive
    And the initial margin on cnec "CB0 - outage" should be 4246.1 MW
    And the initial margin on cnec "CB0 - curative" should be 4246.1 MW
    And the initial setpoint of RangeAction "D7_RA_99991_D4_RA_99991" should be 0
    And the initial setpoint of RangeAction "D7_RA_99992_D4_RA_99992" should be 0
    And the margin on cnec "CB0 - outage" after PRA should be 5646.1 MW
    And the margin on cnec "CB0 - curative" after PRA should be 5646.1 MW
    And the setpoint of RangeAction "D7_RA_99991_D4_RA_99991" should be 700 MW in preventive
    And the setpoint of RangeAction "D7_RA_99992_D4_RA_99992" should be 700 MW in preventive
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
  Scenario: US 14.4.4 : German HVDC Curative (CORE's german HVDC)
    Given network file is "epic14/TestCase12NodesUltranet_v2.uct" for CORE CC
    Given crac file is "epic14/crac_hvdc_curative.xml"
    Given crac creation parameters file is "epic14/ccp.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2026-01-27 17:00"
    Then the remedial action "D7_RA_99991_D4_RA_99991" is not used in preventive
    And the remedial action "D7_RA_99992_D4_RA_99992" is not used in preventive
    And the remedial action "D7_RA_99991_D4_RA_99991" is used after "OUTAGE_1" at "curative"
    And the remedial action "D7_RA_99992_D4_RA_99992" is used after "OUTAGE_1" at "curative"
    And the initial margin on cnec "CB0 - outage" should be 4246.1 MW
    And the initial margin on cnec "CB0 - curative" should be 4246.1 MW
    And the initial setpoint of RangeAction "D7_RA_99991_D4_RA_99991" should be 0
    And the initial setpoint of RangeAction "D7_RA_99992_D4_RA_99992" should be 0
    And the margin on cnec "CB0 - outage" after PRA should be 4246.1 MW
    And the margin on cnec "CB0 - curative" after PRA should be 4246.1 MW
    And the setpoint of RangeAction "D7_RA_99991_D4_RA_99991" should be 0 MW in preventive
    And the setpoint of RangeAction "D7_RA_99992_D4_RA_99992" should be 0 MW in preventive
    And the margin on cnec "CB0 - outage" after CRA should be 4246.1 MW
    And the margin on cnec "CB0 - curative" after CRA should be 5646.1 MW
    And the setpoint of RangeAction "D7_RA_99991_D4_RA_99991" should be 700 MW after "OUTAGE_1" at "curative"
    And the setpoint of RangeAction "D7_RA_99992_D4_RA_99992" should be 700 MW after "OUTAGE_1" at "curative"
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
  Scenario: US 14.4.5 : German HVDC with no alignment of generators (CORE's german HVDC)
    Given network file is "epic14/TestCase12NodesUltranet_v2.uct" for CORE CC
    Given crac file is "epic14/crac_hvdc_preventive_and_curative.xml"
    Given crac creation parameters file is "epic14/ccp.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2026-01-27 17:00"
    Then the remedial action "D7_RA_99991_D4_RA_99991" is used in preventive
    And the remedial action "D7_RA_99992_D4_RA_99992" is used after "OUTAGE_1" at "curative"
    And the initial margin on cnec "CB0 - outage" should be 4246.1 MW
    And the initial margin on cnec "CB0 - curative" should be 4246.1 MW
    And the initial setpoint of RangeAction "D7_RA_99991_D4_RA_99991" should be 0
    And the initial setpoint of RangeAction "D7_RA_99992_D4_RA_99992" should be 0
    And the margin on cnec "CB0 - outage" after PRA should be 5246.1 MW
    And the margin on cnec "CB0 - curative" after PRA should be 5246.1 MW
    And the setpoint of RangeAction "D7_RA_99991_D4_RA_99991" should be 1000 MW in preventive
    And the setpoint of RangeAction "D7_RA_99992_D4_RA_99992" should be 0 MW in preventive
    And the margin on cnec "CB0 - outage" after CRA should be 5246.1 MW
    And the margin on cnec "CB0 - curative" after CRA should be 5946.1 MW
    And the setpoint of RangeAction "D7_RA_99991_D4_RA_99991" should be 1000 MW after "OUTAGE_1" at "curative"
    And the setpoint of RangeAction "D7_RA_99992_D4_RA_99992" should be 700 MW after "OUTAGE_1" at "curative"
    And the generator "D7AAA11A_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D7AAA21A_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D4AAA11B_generator" should have a targetP of 0.0 MW at initial state
    And the generator "D4AAA21B_generator" should have a targetP of 0.0 MW at initial state
    # RA have different usage rules => there is no alignment of the generators
    # Generators of the same RA have opposed targetP: D7AAA11A_generator & D4AAA11B_generator ; D4AAA21B_generator & D7AAA21A_generator
    And the generator "D7AAA11A_generator" should have a targetP of -1000.0 MW after PRA
    And the generator "D7AAA21A_generator" should have a targetP of 0.0 MW after PRA
    And the generator "D4AAA11B_generator" should have a targetP of 1000.0 MW after PRA
    And the generator "D4AAA21B_generator" should have a targetP of 0.0 MW after PRA
    And the generator "D7AAA11A_generator" should have a targetP of -1000.0 MW after CRA
    And the generator "D7AAA21A_generator" should have a targetP of -700.0 MW after CRA
    And the generator "D4AAA11B_generator" should have a targetP of 1000.0 MW after CRA
    And the generator "D4AAA21B_generator" should have a targetP of 700.0 MW after CRA
    And the worst margin is 5246.1 MW
