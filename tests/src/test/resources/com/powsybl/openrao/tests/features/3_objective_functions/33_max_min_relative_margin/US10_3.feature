# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 10.3: handle GLSK on Xnodes

  @fast @rao @dc @preventive-only @relative
  Scenario: US 10.3.1: GLSK on Xnodes
    # same test case as 10.2.1 but with two bidding zones on xnodes, giving different results
    Given network file is "common/TestCase12NodesHvdc.uct" for CORE CC
    Given crac file is "epic10/cbcora_ep10us2case1.xml"
    Given configuration file is "epic10/conf_ep10us3case1.json"
    Given loopflow glsk file is "epic10/glsk_proportional_12nodes_hvdc.xml"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then the value of the objective function after CRA should be -300
    Then the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    Then the remedial action "Open FR1 FR2" is used in preventive
    Then the relative margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 299.6 MW
    Then the relative margin on cnec "BBE2AA1  FFR3AA1  1 - preventive" after PRA should be 743.7 MW
    Then the relative margin on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be 1358.5 MW
    Then the relative margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 1677.0 MW
    Then the absolute PTDF sum on cnec "FFR2AA1  DDE3AA1  1 - preventive" initially should be 2.178
    Then the absolute PTDF sum on cnec "BBE2AA1  FFR3AA1  1 - preventive" initially should be 2.222
    Then the absolute PTDF sum on cnec "DDE2AA1  NNL3AA1  1 - preventive" initially should be 2.046
    Then the absolute PTDF sum on cnec "NNL2AA1  BBE3AA1  1 - preventive" initially should be 1.956