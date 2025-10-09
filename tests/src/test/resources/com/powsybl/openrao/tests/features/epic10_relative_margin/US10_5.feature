# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 10.5: GLSK on disconnected Xnode

  @fast @rao @mock @dc @preventive-only @relative
  Scenario: US 10.5.1: XNode disconnected in initial network
    Given network file is "epic10/TestCase12NodesDisconnectedHvdc.uct" for CORE CC
    Given crac file is "epic10/cbcora_ep10us2case1.xml"
    Given configuration file is "epic10/conf_ep10us4case1.json"
    Given loopflow glsk file is "epic10/glsk_proportional_12nodes_hvdc.xml"
    When I launch rao at "2019-01-08 12:00"
    And the value of the objective function after CRA should be -122
    And the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    And the remedial action "Open FR1 FR2" is used in preventive
    And the relative margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 122.6 MW
    And the relative margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 614.5 MW
    And the relative margin on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be 1509.0 MW
    And the relative margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 1834.5 MW
    And the absolute PTDF sum on cnec "FFR2AA1  DDE3AA1  1 - preventive" initially should be 1.977
    And the absolute PTDF sum on cnec "BBE2AA1  FFR3AA1  1 - preventive" initially should be 2.020
    And the absolute PTDF sum on cnec "DDE2AA1  NNL3AA1  1 - preventive" initially should be 2.046
    And the absolute PTDF sum on cnec "NNL2AA1  BBE3AA1  1 - preventive" initially should be 1.956

  @fast @rao @mock @dc @preventive-only @relative
  Scenario: US 10.5.2: XNode disconnected by a contingency
    Given network file is "common/TestCase12NodesHvdc.uct" for CORE CC
    Given crac file is "epic10/cbcora_ep10us5case2.xml"
    Given configuration file is "epic10/conf_ep10us4case1.json"
    Given loopflow glsk file is "epic10/glsk_proportional_12nodes_hvdc.xml"
    When I launch rao at "2019-01-08 12:00"
    And the value of the objective function after CRA should be -122
    And the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    And the remedial action "Open FR1 FR2" is used in preventive
    And the relative margin on cnec "CB_FR-DE_2 - outage" after PRA should be 122.3 MW
    And the relative margin on cnec "CB_FR-DE_1 - preventive" after PRA should be 413.6 MW
    And the relative margin on cnec "CB_BE-FR_2 - outage" after PRA should be 614.5 MW
    And the absolute PTDF sum on cnec "CB_FR-DE_2 - outage" initially should be 1.977
    And the absolute PTDF sum on cnec "CB_FR-DE_1 - preventive" initially should be 1.578
    And the absolute PTDF sum on cnec "CB_BE-FR_2 - outage" initially should be 2.021