# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 0.2.2.1: Core CNE export for preventive case
  This feature covers the export of Core CNE for preventive computation.

  @fast @cne-export
  Scenario: 0.2.2.1.1.CBCORA: Preventive only case 1
    Given network file is "common/network_16nodes.uct" for CORE CC
    Given crac file is "0_import_export/0_2_export/0_2_2_core_cne/cbcora_0_2_2_1_1.xml"
    Given configuration file is "common/rao_parameters_max_margin_ampere.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/rao_result_0_2_2_1_1.json"
    When I export CORE CNE at "2019-01-08 00:00"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/cne_0_2_2_1_1.xml"

  @fast @cne-export
  Scenario: 0.2.2.1.2: Preventive case with loopflows
    Given network file is "common/network_12nodes.uct" for CORE CC
    Given crac file is "0_import_export/0_2_export/0_2_2_core_cne/cbcora_0_2_2_1_2.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given RefProg file is "common/ref_prog_12nodes.xml"
    Given configuration file is "common/rao_parameters_max_margin_mw_dc_lf_false_5_100_be_nl.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/rao_result_0_2_2_1_2.json"
    When I export CORE CNE at "2019-01-08 21:30"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/cne_0_2_2_1_2.xml"

  @fast @cne-export
  Scenario: 0.2.2.1.3: Test case with a CBCORA file and a MNEC
    Given network file is "common/network_12nodes.uct" for CORE CC
    Given crac file is "0_import_export/0_2_export/0_2_2_core_cne/cbcora_0_2_2_1_3.xml"
    Given configuration file is "common/rao_parameters_max_margin_megawatt_dc.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/rao_result_0_2_2_1_3.json"
    When I export CORE CNE at "2019-01-08 01:00"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/cne_0_2_2_1_3.xml"

  @fast @cne-export @hvdc
  Scenario: 0.2.2.1.4 : Preventive case with German HVDC
    Given network file is "0_import_export/network_12nodes_ultranet_v2.uct" for CORE CC
    Given crac file is "0_import_export/crac_0_2_2_1_4.xml"
    Given crac creation parameters file is "0_import_export/cim_crac_creation_parameters_default.json"
    Given configuration file is "common/rao_parameters_max_margin_megawatt_dc.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/rao_result_0_2_2_1_4.json"
    When I export CORE CNE at "2026-01-27 14:00"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/cne_0_2_2_1_4.xml"
