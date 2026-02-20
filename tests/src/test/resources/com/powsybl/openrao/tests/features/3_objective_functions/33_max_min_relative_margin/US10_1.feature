# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 10.1: Linear RAO with relative margin
  This feature covers the objective-function/type MAX_MIN_RELATIVE_MARGIN.

  @fast @rao @ac @preventive-only @relative @max-min-relative-margin @megawatt
  Scenario: US 10.1.1: Unsecured case
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic10/ls_relative_margin_unsecure.json"
    Given configuration file is "epic10/RaoParameters_relMargin_megawatt.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the value of the objective function after CRA should be 281.02
    Then the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    Then the worst margin is -281.02 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    Then the relative margin on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 2297.1 MW
    Then the relative margin on cnec "DDE2AA1  NNL3AA1  1 - preventive" after PRA should be 2475.3 MW

  @fast @rao @ac @preventive-only @max-min-relative-margin @megawatt
  Scenario: US 10.1.2: Secured case
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic10/ls_relative_margin.json"
    Given configuration file is "epic10/RaoParameters_relMargin_megawatt.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the value of the objective function after CRA should be -2383.0
    Then the tap of PstRangeAction "PRA_PST_BE" should be -3 in preventive
    Then the worst relative margin is 2383.0 MW on cnec "NNL2AA1  BBE3AA1  1 - preventive"
    Then the absolute PTDF sum on cnec "NNL2AA1  BBE3AA1  1 - preventive" initially should be 1.455
    Then the relative margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 2392.4 MW
    Then the absolute PTDF sum on cnec "FFR2AA1  DDE3AA1  1 - preventive" initially should be 1.477

  @fast @rao @ac @preventive-only @max-min-relative-margin @megawatt
  Scenario: US 10.1.3: Secured case with open monitored branch
    Given network file is "common/TestCase12NodesWithOpenBranch.uct"
    Given crac file is "epic10/ls_relative_margin_with_open_branch.json"
    Given configuration file is "epic10/RaoParameters_relMargin_megawatt.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the value of the objective function after PRA should be -2383.0
    Then the value of the objective function after CRA should be -2385.0
    Then the tap of PstRangeAction "PRA_PST_BE" should be -3 in preventive
    Then the worst relative margin is 2383.0 MW on cnec "NNL2AA1  BBE3AA1  1 - preventive"
    Then the absolute PTDF sum on cnec "NNL2AA1  BBE3AA1  1 - preventive" initially should be 1.455
    Then the relative margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 2392.4 MW
    Then the absolute PTDF sum on cnec "FFR2AA1  DDE3AA1  1 - preventive" initially should be 1.477
