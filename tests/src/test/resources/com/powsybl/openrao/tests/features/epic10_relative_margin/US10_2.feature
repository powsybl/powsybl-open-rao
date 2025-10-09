# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 10.2: define ptdfBoundaries with EIcode instead of Country codes

  @fast @rao @mock @dc @preventive-only @relative
  Scenario: US 10.2.1: Compute relative margins with PTDFs on bidding-zones which are countries
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic10/cbcora_ep10us2case1.xml"
    Given configuration file is "epic10/conf_ep10us2case1.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    When I launch rao at "2019-01-08 12:00"
    Then its security status should be "SECURED"
    And the value of the objective function after CRA should be -164
    And the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    And the remedial action "Open FR1 FR2" is used in preventive
    And the relative margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 164.2 MW
    And the relative margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 816.4 MW
    And the relative margin on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be 1997.8 MW
    And the relative margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 2466.4 MW
    And the absolute PTDF sum on cnec "FFR2AA1  DDE3AA1  1 - preventive" initially should be 1.477
    And the absolute PTDF sum on cnec "BBE2AA1  FFR3AA1  1 - preventive" initially should be 1.522
    And the absolute PTDF sum on cnec "DDE2AA1  NNL3AA1  1 - preventive" initially should be 1.546
    And the absolute PTDF sum on cnec "NNL2AA1  BBE3AA1  1 - preventive" initially should be 1.455

  @fast @rao @mock @dc @preventive-only @relative
  Scenario: US 10.2.2: Compute relative margins with PTDFs on bidding-zones which are NOT countries
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic10/cbcora_ep10us2case1.xml"
    Given configuration file is "epic10/conf_ep10us2case2.json"
    Given loopflow glsk file is "epic10/glsk_ep10us2case2.xml"
    When I launch rao at "2019-01-08 12:00"
    Then its security status should be "SECURED"
    And the value of the objective function after CRA should be -164
    And the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    And the remedial action "Open FR1 FR2" is used in preventive
    And the relative margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 164.2 MW
    And the relative margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 816.4 MW
    And the relative margin on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be 1997.8 MW
    And the relative margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 2466.4 MW
    And the absolute PTDF sum on cnec "FFR2AA1  DDE3AA1  1 - preventive" initially should be 1.477
    And the absolute PTDF sum on cnec "BBE2AA1  FFR3AA1  1 - preventive" initially should be 1.522
    And the absolute PTDF sum on cnec "DDE2AA1  NNL3AA1  1 - preventive" initially should be 1.546
    And the absolute PTDF sum on cnec "NNL2AA1  BBE3AA1  1 - preventive" initially should be 1.455