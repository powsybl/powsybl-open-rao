# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 15.1: Handle the opening/closing of switches as topological actions

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 15.1: Enable the use of switches as network actions
    Given network file is "epic15/TestCase12NodesWithSwitch.uct"
    Given crac file is "epic15/SL_ep15us1_withSwitchRA.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then its security status should be "SECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA 1N poste DE3" is used in preventive
    Then the worst margin is 437.0 A