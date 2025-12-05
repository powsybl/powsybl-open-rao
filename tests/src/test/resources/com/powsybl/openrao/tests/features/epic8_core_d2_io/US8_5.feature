# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 8.5: Management of FRM during computation

  @fast @rao @mock @ac @preventive-only
  Scenario: US 8.5.1: case without FRM
    Given network file is "epic2/US2-3-case1-standard.uct" for CORE CC
    Given crac file is "epic8/12nodes_pst_topo_frm_cbcora_null_frm.xml"
    When I launch rao at "2019-01-08 12:00"
    Then the value of the objective function after PRA should be -25.6
    Then the margin on cnec "Cnec1 - preventive" after PRA should be 28 A

  @fast @rao @mock @ac @preventive-only
  Scenario: US 8.5.2: case with a FRM of 50 MW
    Given network file is "epic2/US2-3-case1-standard.uct" for CORE CC
    Given crac file is "epic8/12nodes_pst_topo_frm_cbcora.xml"
    When I launch rao at "2019-01-08 12:00"
    Then the value of the objective function after CRA should be 24.0
    Then the margin on cnec "Cnec1 - preventive" after PRA should be -24.0 MW
