# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 0.2.2.2: Handling of curative optimization in the Core CNE export
  This feature covers the export of optimization results Core CNE.

  @fast @cne-export
  Scenario: 0.2.2.2.1: Simple curative case (copy of scenario 1.3.3.10)
    Given network file is "common/network_16nodes_with_different_imax.uct" for CORE CC
    Given crac file is "common/cbcora_2_6_3_11.xml"
    Given configuration file is "common/rao_parameters_max_margin_ampere.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/rao_result_0_2_2_2_1.json"
    When I export CORE CNE at "2019-01-08 12:59"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/cne_0_2_2_2_1.xml"

  @fast @cne-export
  Scenario: 0.2.2.2.2: Curative case with MNECs (copy of scenario 1.3.6.4)
    Given network file is "common/network_12nodes_different_pst_tap.uct" for CORE CC
    Given crac file is "common/cbcora_common_merged_cb.xml"
    Given configuration file is "common/rao_parameters_max_margin_ampere_ac_mnec_dimin30.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/rao_result_0_2_2_2_2.json"
    When I export CORE CNE at "2019-01-08 13:00"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/cne_0_2_2_2_2.xml"

  @fast @cne-export
  Scenario: 0.2.2.2.3: Curative case with loopflows (copy of scenario 1.3.7.4)
    Given network file is "common/network_12nodes.uct" for CORE CC
    Given crac file is "0_import_export/0_2_export/0_2_2_core_cne/cbcora_0_2_2_2_3.xml"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    Given configuration file is "common/rao_parameters_max_margin_megawatt_dc_with_loop_flows.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/rao_result_0_2_2_2_3.json"
    When I export CORE CNE at "2019-01-08 00:59"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/cne_0_2_2_2_3.xml"

  @fast @cne-export
  Scenario: 0.2.2.2.4.CBCORA: Curative case with 2 curative perimeters (copy of scenario 1.3.3.2)
    Given network file is "common/network_16nodes.uct" for CORE CC
    Given crac file is "0_import_export/0_2_export/0_2_2_core_cne/cbcora_0_2_2_2_4.xml"
    Given configuration file is "common/rao_parameters_max_margin_ampere_absolute_threshold.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/rao_result_0_2_2_2_4.json"
    When I export CORE CNE at "2019-01-08 13:30"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/cne_0_2_2_2_4.xml"

  @fast @cne-export
  Scenario: 0.2.2.2.5: Curative case with 2 curative perimeters and relative margins
    Given network file is "common/network_16nodes.uct" for CORE CC
    Given crac file is "0_import_export/0_2_export/0_2_2_core_cne/cbcora_0_2_2_2_5.xml"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    Given configuration file is "common/rao_parameters_rel_margin_ampere.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/rao_result_0_2_2_2_5.json"
    When I export CORE CNE at "2019-01-08 00:00"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/cne_0_2_2_2_5.xml"

  @fast @cne-export @hvdc
  Scenario: 0.2.2.2.6 : Curative case with German HVDC
    Given network file is "0_import_export/network_12nodes_ultranet_v2.uct" for CORE CC
    Given crac file is "0_import_export/crac_0_2_2_2_6.xml"
    Given crac creation parameters file is "0_import_export/cim_crac_creation_parameters_default.json"
    Given configuration file is "common/rao_parameters_max_margin_megawatt_dc.json"
    Given RaoResult file is "0_import_export/0_2_export/0_2_2_core_cne/rao_result_0_2_2_2_6.json"
    When I export CORE CNE at "2026-01-27 14:00"
    Then the CORE CNE file is xsd-compliant
    Then the exported CNE file is the same as "0_import_export/0_2_export/0_2_2_core_cne/cne_0_2_2_2_6.xml"
