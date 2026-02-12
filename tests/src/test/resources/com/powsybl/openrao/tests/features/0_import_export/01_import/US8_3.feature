# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 8.3: Handle elementName tag
  # In UCTE file format, a line is defined by the name of two substations and either an order code or an elementName.

  ## TODO: add explicit config file?
  @fast @rao @ac @preventive-only
  Scenario: US 8.3.1.1: line defined with order code
    Given network file is "epic2/US2-3-case1-standard.uct" for CORE CC
    Given crac file is "epic8/12nodes_pst_topo_frm_cbcora.xml"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the worst margin is -24.0 MW
    Then the margin on cnec "Cnec1 - preventive" after PRA should be -24.0 MW
    Then the value of the objective function after CRA should be 24.0

  @fast @rao @ac @preventive-only
  Scenario: US 8.3.1.2: line defined with element name
    Given network file is "epic8/US2-3-case1-elementName.uct" for CORE CC
    Given crac file is "epic8/12nodes_pst_topo_frm_cbcora_elementName.xml"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the worst margin is -24.0 MW
    Then the margin on cnec "Cnec1 - preventive" after PRA should be -24.0 MW
    Then the value of the objective function after CRA should be 24.0