# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 91.1.1: Read PST penalty cost from config
  # RAO #1.1 in confluence

  @fast @rao @mock @ac @preventive-only
  Scenario: US 91.1.1.1: Run a linear RAO with default PST penalty cost (Same as epic2 - US 2.2.5)
    Given network file is "common/TestCase12Nodes2PSTs.uct"
    Given crac file is "epic2/SL_ep2us2case5.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then its security status should be "SECURED"
    Then the worst margin is 459.0 A
    Then the tap of PstRangeAction "PRA_PST_DE" should be -10 in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 16 in preventive

  # As this test case is originally a bit tricky about PST taps rounding
  # the result here is not really what we could expect because the minimum margin
  # improved while increasing the PST penalty cost. Anyway here we want to test that
  # the configuration file is well taken into account with this parameter so this test
  # appears to be enough.
  @fast @rao @mock @ac @preventive-only
  Scenario: US 91.1.1.2: Run a linear RAO with high PST penalty cost
    Given network file is "common/TestCase12Nodes2PSTs.uct"
    Given crac file is "epic2/SL_ep2us2case5.json"
    Given configuration file is "rao1/RaoParameters_maxMargin_ampere_highPSTcost.json"
    When I launch rao
    Then its security status should be "SECURED"
    Then the worst margin is 468.0 A
    Then the tap of PstRangeAction "PRA_PST_DE" should be -10 in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 11 in preventive