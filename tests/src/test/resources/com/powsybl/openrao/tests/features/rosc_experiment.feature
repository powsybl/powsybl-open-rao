# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: ROSC experiment

  @fast @dc @rosc
  Scenario: ENTSO-E's ReliCapGrid - Dataset import
    Given network file is "/rosc/ReliCapGrid.zip"
    Given crac file is "/rosc/ReliCapGrid_CGMES_NC.zip"
    Given crac creation parameters file is "/rosc/ReliCapGrid-nc-crac-parameters.json"
    When I import crac
    Then it should have 16 contingencies
    Then it should have 660 cnecs
    Then it should have 12 range actions

  @fast @dc @rosc
  Scenario: ENTSO-E's ReliCapGrid - RAO DC
    Given network file is "/rosc/ReliCapGrid.zip"
    Given crac file is "/rosc/ReliCapGrid_CGMES_NC.zip"
    Given configuration file is "/rosc/rao_parameters_dc_2P.json"
    Given crac creation parameters file is "/rosc/ReliCapGrid-nc-crac-parameters.json"
    When I launch rao

  @fast @ac @rosc
  Scenario: ENTSO-E's ReliCapGrid - RAO AC
    Given network file is "/rosc/ReliCapGrid.zip"
    Given crac file is "/rosc/ReliCapGrid_CGMES_NC.zip"
    Given configuration file is "/rosc/rao_parameters_ac_2P.json"
    Given crac creation parameters file is "/rosc/ReliCapGrid-nc-crac-parameters.json"
    When I launch rao