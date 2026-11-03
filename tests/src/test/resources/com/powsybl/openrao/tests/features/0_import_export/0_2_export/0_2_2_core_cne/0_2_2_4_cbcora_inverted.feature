# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 0.2.2.4: Core CNE export for CBCORA with inverted branches
  This feature covers the export of particular CNECs in Core CNE.

  @fast @cne-export
  Scenario: 0.2.2.4.1: CBCORA with inverted branches
    Given network file is "common/network_12nodes.uct" for CORE CC
    Given crac file is "0_import_export/0_2_export/0_2_2_core_cne/cbcora_0_2_2_4_1.xml"
    Given configuration file is "common/rao_parameters_max_margin_ampere_ac_mnec_dimin30.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/rao_result_0_2_2_4_1.json"
    When I export CORE CNE at "2019-01-08 01:59"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/cne_0_2_2_4_1.xml"

  @fast @cne-export
  Scenario: 0.2.2.4.2: CBCORA with inverted branches and loopflows
    Given network file is "common/network_12nodes.uct" for CORE CC
    Given crac file is "0_import_export/0_2_export/0_2_2_core_cne/cbcora_0_2_2_4_2.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "common/ref_prog_12nodes.xml"
    Given configuration file is "common/rao_parameters_max_margin_mw_dc_lf_false_5_100_be_nl.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/rao_result_0_2_2_4_2.json"
    When I export CORE CNE at "2019-01-08 21:30"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/cne_0_2_2_4_2.xml"