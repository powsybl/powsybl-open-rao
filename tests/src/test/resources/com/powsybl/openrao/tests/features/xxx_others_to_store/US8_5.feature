# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 8.5: Management of FRM during computation

  @fast @rao @ac @preventive-only @secure-flow @megawatt
  Scenario: US 8.5.1: case without FRM
    Given network file is "epic2/US2-3-case1-standard.uct" for CORE CC
    Given crac file is "epic8/12nodes_pst_topo_frm_cbcora_null_frm.xml"
    Given configuration file is "common/RaoParameters_default.json"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 25.6 MW
    Then the margin on cnec "Cnec1 - preventive" after PRA should be 25.6 MW
    Then the value of the objective function after PRA should be -25.6

  @fast @rao @ac @preventive-only @secure-flow @megawatt
  Scenario: US 8.5.2: case with a FRM of 50 MW
  Same as US 8.5.1, but the margin of the CNEC is decreased by the value of the FRM.
    Given network file is "epic2/US2-3-case1-standard.uct" for CORE CC
    Given crac file is "epic8/12nodes_pst_topo_frm_cbcora.xml"
    Given configuration file is "common/RaoParameters_default.json"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the worst margin is -24 MW
    Then the margin on cnec "Cnec1 - preventive" after PRA should be -24.0 MW
    Then the value of the objective function after CRA should be 24.0
