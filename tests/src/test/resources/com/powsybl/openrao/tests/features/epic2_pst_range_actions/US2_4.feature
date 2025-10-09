# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 2.4: Export the selected tap for a range RA in the basic output file

  @fast @rao @mock @ac @preventive-only
  Scenario: US 2.4.1
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic2/SL_ep2us2case1.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    Then I launch rao
    Then its security status should be "SECURED"
    Then the tap of PstRangeAction "PRA_PST_BE" should be 15 in preventive
