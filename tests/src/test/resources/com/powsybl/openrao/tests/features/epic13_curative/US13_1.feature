# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 13.1: solve a RAO for a single preventive or curative state

  @fast @rao @mock @ac @preventive-only
  Scenario: US 13.1.1: Solve preventive perimeter alone
    Given network file is "epic13/TestCase12NodesForCurative.uct"
    Given crac file is "epic13/12nodes_pst_topo_frm_cbcora_curative.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_ac.json"
    When I launch search_tree_rao at "2019-01-08 00:30" on preventive state
    Then the tap of PstRangeAction "SelectTapPSTPrev" should be 12 in preventive
    Then the initial flow on cnec "CnecPreventiveDir - preventive" should be -400.0 A
    # Then the flow on cnec "DDE1AA1  DDE3AA1  1 - preventive" after PRA should be 121.0 MW
    # Previously there was a mistake it was optimizing with all cnecs and not only on preventive state
    Then the flow on cnec "CnecPreventiveDir - preventive" after PRA should be -8 A

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 13.1.2: Solve curative perimeter alone at curative instant
    Given network file is "epic13/TestCase12NodesForCurative.uct"
    Given crac file is "epic13/12nodes_pst_topo_frm_cbcora_curative.xml"
    Given configuration file is "common/RaoParameters_maxMargin_ampere_ac.json"
    When I launch search_tree_rao at "2019-01-08 00:30" after "Contingency" at "curative"
    Then the tap of PstRangeAction "SelectTapPSTCur" should be -9 after "Contingency" at "curative"
    Then the initial flow on cnec "CnecCurativeDir - curative" should be 412.6 A
    Then the flow on cnec "CnecCurativeDir - curative" after CRA should be -13.4 A
