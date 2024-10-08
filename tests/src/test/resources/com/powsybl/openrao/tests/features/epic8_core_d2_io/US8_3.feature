# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 8.3: Handle elementName tag

  @fast @rao @mock @ac @preventive-only @loopflow
  Scenario: US 8.3.1
    Given network file is "epic8/US2-3-case1-elementName.uct" for CORE CC
    Given crac file is "epic8/12nodes_pst_topo_frm_cbcora_elementName.xml"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the value of the objective function after CRA should be 44.0
    Then the margin on cnec "Cnec1 - preventive" after PRA should be -24.0 MW