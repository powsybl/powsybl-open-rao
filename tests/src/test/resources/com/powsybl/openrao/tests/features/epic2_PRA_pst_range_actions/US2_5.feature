# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 2.5: Read PST min impact threshold from config

  @fast @rao @ac @preventive-only
  Scenario: US 2.5.1: Run a linear RAO with default PST min impact threshold (Same as epic2 - US 2.2.5)
  Two PST range actions are activated, to increase the min margin.
    Given network file is "common/TestCase12Nodes2PSTs.uct"
    Given crac file is "epic2/SL_ep2us2case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 459.0 A
    Then the tap of PstRangeAction "PRA_PST_DE" should be -10 in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 16 in preventive

  # As this test case is originally a bit tricky about PST taps rounding
  # the result here is not really what we could expect because the minimum margin
  # improved while increasing the PST min impact threshold. Anyway here we want to test that
  # the configuration file is well taken into account with this parameter so this test
  # appears to be enough.
  @fast @rao @ac @preventive-only
  Scenario: US 2.5.2: Run a linear RAO with high PST min impact threshold
    Given network file is "common/TestCase12Nodes2PSTs.uct"
    Given crac file is "epic2/SL_ep2us2case5.json"
    Given configuration file is "rao1/RaoParameters_maxMargin_ampere_highPSTcost.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 468.0 A
    Then the tap of PstRangeAction "PRA_PST_DE" should be -10 in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 11 in preventive