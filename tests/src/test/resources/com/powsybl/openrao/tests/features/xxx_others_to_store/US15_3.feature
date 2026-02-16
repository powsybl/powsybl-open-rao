# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.3: Handle transformers

  @fast @rao @dc @preventive-only @max-min-margin @megawatt
  Scenario: US 15.3.1: Handle transformers on a small test case in DC
    Given network file is "epic15/TestCase12Nodes_with_2_voltage_levels_1.uct" for CORE CC
    Given crac file is "epic15/SL_ep15us3case1.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then 3 remedial actions are used in preventive
    Then the remedial action "open_be1_fr1" is used in preventive
    Then the remedial action "open_be1_be2" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be -16 in preventive
    Then the worst margin is 79.12 MW
    Then the margin on cnec "BBE2AA2  BBE2AA1  2 - preventive" after PRA should be 79.12 MW
    Then the margin on cnec "BBE2AA2  BBE2AA1  2 - co_fr - outage" after PRA should be 136.19 MW
    Then the margin on cnec "BBE1AA1  BBE1AA2  1 - preventive" after PRA should be 192.82 MW
    Then the margin on cnec "FFR3AA1  FFR3AA2  1 - preventive" after PRA should be 195.34 MW
    Then the margin on cnec "FFR3AA1  FFR3AA2  1 - co_fr - outage" after PRA should be 207.61 MW
    Then the margin on cnec "FFR1AA2  FFR1AA1  5 - preventive" after PRA should be 293.2 MW
    Then the margin on cnec "BBE1AA1  BBE1AA2  1 - co_fr - outage" after PRA should be 296.74 MW
    Then the margin on cnec "FFR1AA2  FFR1AA1  5 - co_fr - outage" after PRA should be 544 MW

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 15.3.2: Handle transformers on a small test case in AC - On side 1
    Given network file is "epic15/TestCase12Nodes_with_2_voltage_levels_2_side_1.uct" for CORE CC
    Given crac file is "epic15/SL_ep15us3case2.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 3 remedial actions are used in preventive
    Then the remedial action "open_be1_fr1" is used in preventive
    Then the remedial action "open_be1_be2" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be -15 in preventive
    Then the worst margin is 110 A
    Then the margin on cnec "FFR1AA2  FFR1AA1  5 - preventive" after PRA should be 110 A
    Then the margin on cnec "BBE2AA2  BBE2AA1  2 - preventive" after PRA should be 110.7 A
    Then the margin on cnec "BBE2AA2  BBE2AA1  2 - co_fr - outage" after PRA should be 192.7 A
    Then the margin on cnec "FFR3AA1  FFR3AA2  1 - preventive" after PRA should be 222 A
    Then the margin on cnec "FFR1AA2  FFR1AA1  5 - co_fr - outage" after PRA should be 312 A
    Then the margin on cnec "FFR3AA1  FFR3AA2  1 - co_fr - outage" after PRA should be 391 A
    Then the margin on cnec "BBE1AA1  BBE1AA2  1 - preventive" after PRA should be 452 A
    Then the margin on cnec "BBE1AA1  BBE1AA2  1 - co_fr - outage" after PRA should be 719 A
    Then the "upper" threshold on cnec "BBE2AA2  BBE2AA1  2 - preventive" should be 675 A
    Then the "upper" threshold on cnec "BBE2AA2  BBE2AA1  2 - co_fr - outage" should be 776 A
    Then the "upper" threshold on cnec "BBE1AA1  BBE1AA2  1 - preventive" should be 1778 A
    Then the "upper" threshold on cnec "BBE1AA1  BBE1AA2  1 - co_fr - outage" should be 2044 A
    Then the "upper" threshold on cnec "FFR3AA1  FFR3AA2  1 - preventive" should be 1778 A
    Then the "upper" threshold on cnec "FFR3AA1  FFR3AA2  1 - co_fr - outage" should be 2044 A
    Then the "upper" threshold on cnec "FFR1AA2  FFR1AA1  5 - preventive" should be 844 A
    Then the "upper" threshold on cnec "FFR1AA2  FFR1AA1  5 - co_fr - outage" should be 970 A

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 15.3.2: Handle transformers on a small test case in AC - On side 2
    Given network file is "epic15/TestCase12Nodes_with_2_voltage_levels_2_side_2.uct"
    Given crac file is "epic15/SL_ep15us3case2_RIGHT.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 3 remedial actions are used in preventive
    Then the remedial action "open_be1_fr1" is used in preventive
    Then the remedial action "open_be1_be2" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be 8 in preventive
    Then the worst margin is 137 A
    Then the margin on cnec "BBE2AA2  BBE2AA1  2 - preventive" after PRA should be 139 A
    Then the margin on cnec "FFR3AA1  FFR3AA2  1 - preventive" after PRA should be 137 A
    Then the margin on cnec "FFR3AA1  FFR3AA2  1 - co_fr - outage" after PRA should be 220 A
    Then the margin on cnec "FFR1AA2  FFR1AA1  5 - preventive" after PRA should be 232 A
    Then the margin on cnec "BBE1AA1  BBE1AA2  1 - preventive" after PRA should be 254 A
    Then the "upper" threshold on cnec "BBE2AA2  BBE2AA1  2 - preventive" should be 1200 A
    Then the "upper" threshold on cnec "BBE2AA2  BBE2AA1  2 - co_fr - outage" should be 1380 A
    Then the "upper" threshold on cnec "BBE1AA1  BBE1AA2  1 - preventive" should be 1000 A
    Then the "upper" threshold on cnec "BBE1AA1  BBE1AA2  1 - co_fr - outage" should be 1150 A
    Then the "upper" threshold on cnec "FFR3AA1  FFR3AA2  1 - preventive" should be 1000 A
    Then the "upper" threshold on cnec "FFR3AA1  FFR3AA2  1 - co_fr - outage" should be 1150 A
    Then the "upper" threshold on cnec "FFR1AA2  FFR1AA1  5 - preventive" should be 1500 A
    Then the "upper" threshold on cnec "FFR1AA2  FFR1AA1  5 - co_fr - outage" should be 1725 A

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 15.3.3: Handle transformers with threshold in Ampere
    Given network file is "epic15/TestCase12Nodes_with_2_voltage_levels_2_side_2.uct"
    Given crac file is "epic15/SL_ep15us3case3.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then 3 remedial actions are used in preventive
    Then the remedial action "open_be1_fr1" is used in preventive
    Then the remedial action "open_be1_be2" is used in preventive
    Then the tap of PstRangeAction "pst_be" should be 16 in preventive
    Then the worst margin is -1254 A
    Then the margin on cnec "FFR3AA1  FFR3AA2  1 - co_fr - outage" after PRA should be -1252 A
    Then the margin on cnec "FFR3AA1  FFR3AA2  1 - preventive" after PRA should be -1125 A
    Then the margin on cnec "FFR1AA2  FFR1AA1  5 - preventive" after PRA should be 48 A
    Then the margin on cnec "BBE2AA2  BBE2AA1  2 - preventive" after PRA should be 115 A
    Then the margin on cnec "FFR1AA2  FFR1AA1  5 - co_fr - outage" after PRA should be 218 A