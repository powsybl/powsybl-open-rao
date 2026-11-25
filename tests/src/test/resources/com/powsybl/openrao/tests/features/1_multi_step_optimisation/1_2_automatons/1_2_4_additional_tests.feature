# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 1.2.4: Additional tests to check various fixes concerning automaton state
  This feature covers various fixes of the multi-step (automaton) optimization. It checks the
  application of curative CNECs after a second preventive, the second-preventive condition, the
  selection of the worst CNEC after each optimization step, and the RaoResult after PRA for curative
  CNECs with a second preventive.

  @fast @rao @dc @second-preventive @max-min-margin
  Scenario: 1.2.4.1: Test fix apply auto for curative CNECs post 2P
    Given network file is "1_multi_step_optimisation/1_2_automatons/network_1_2_4_1.uct"
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_4_1_1bis.json"
    Given configuration file is "1_multi_step_optimisation/1_2_automatons/rao_parameters_1_2_4_1.json"
    When I launch rao
    Then the execution details should be "Second preventive improved first preventive results"
    Then its security status should be "UNSECURED"
    Then the remedial action "open_de1_de2_open_nl2_be3 - prev" is used in preventive
    Then the remedial action "open_de2_nl3 - co1 - auto" is used after "co1_fr2_de3" at "auto"
    Then the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_de3" at "curative"
    Then the margin on cnec "be1_be3_co1 - BBE1AA11->BBE3AA11  - co1_fr2_de3 - curative" after CRA should be 327.73 MW

  @fast @rao @dc @second-preventive @secure-flow
  Scenario: 1.2.4.2: Test fix condition 2P
    Given network file is "1_multi_step_optimisation/1_2_automatons/network_1_2_4_4.uct"
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_4_2_2bis.json"
    Given configuration file is "1_multi_step_optimisation/1_2_automatons/rao_parameters_1_2_4_2.json"
    When I launch rao
    Then the margin on cnec "de2_nl3_co1 - DDE2AA11->NNL3AA11  - co1_de2_nl3 - curative" after PRA should be -2519.05 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"

  @fast @rao @dc @contingency-scenarios @max-min-margin
  Scenario: 1.2.4.3.1: Test get highest functional cost worst CNEC is a curative after 1PRAO
    Given network file is "1_multi_step_optimisation/1_2_automatons/network_1_2_4_4.uct"
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_4_3_1_3_1.json"
    Given configuration file is "1_multi_step_optimisation/1_2_automatons/rao_parameters_1_2_4_3.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the worst margin is -773.0 MW on cnec "be1_be3_co2 - BBE1AA11->BBE3AA11  - co2_de1_de3 - curative"

  @fast @rao @dc @contingency-scenarios @max-min-margin
  Scenario: 1.2.4.3.2: Test get highest functional cost worst CNEC is auto after 1ARAO
    Given network file is "1_multi_step_optimisation/1_2_automatons/network_1_2_4_4.uct"
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_4_3_2_3_2.json"
    Given configuration file is "1_multi_step_optimisation/1_2_automatons/rao_parameters_1_2_4_3.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the worst margin is -1945.45 MW on cnec "de2_nl3_co1 - DDE2AA11->NNL3AA11  - co1_de1_de2 - auto"

  @fast @rao @dc @contingency-scenarios @max-min-margin
  Scenario: 1.2.4.3.3: Test get highest functional cost worst CNEC is curative after 1CRAO
    Given network file is "1_multi_step_optimisation/1_2_automatons/network_1_2_4_4.uct"
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_4_3_3_3_3.json"
    Given configuration file is "1_multi_step_optimisation/1_2_automatons/rao_parameters_1_2_4_3.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the worst margin is -543.5 MW on cnec "be1_be3_co1 - BBE1AA11->BBE3AA11  - co1_de1_de2 - curative"
    
  @fast @rao @dc @second-preventive @max-min-margin
  Scenario: 1.2.4.4: RaoResult AFTER PRA fixed for curative CNECs, with 2P
    Given network file is "1_multi_step_optimisation/1_2_automatons/network_1_2_4_1.uct"
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_4_4_1.json"
    Given configuration file is "1_multi_step_optimisation/1_2_automatons/rao_parameters_1_2_4_1.json"
    When I launch rao
    Then the remedial action "open_de1_de2_open_nl2_be3 - prev" is used in preventive
    Then the remedial action "open_de2_nl3 - co1 - auto" is used after "co1_fr2_de3" at "auto"
    Then the remedial action "close_fr2_de3 - co1 - auto" is not used after "co1_fr2_de3" at "auto"
    Then the tap of PstRangeAction "pst_be" should be -16 after "co1_fr2_de3" at "curative"
    Then the margin on cnec "be1_be3_co1 - BBE1AA11->BBE3AA11  - co1_fr2_de3 - curative" after PRA should be -293.5 MW
    Then the margin on cnec "be1_be3_co1 - BBE1AA11->BBE3AA11  - co1_fr2_de3 - auto" after ARA should be -360.65 MW
    Then the margin on cnec "be1_be3_co1 - BBE1AA11->BBE3AA11  - co1_fr2_de3 - curative" after CRA should be 327.73 MW
    Then the execution details should be "Second preventive improved first preventive results"
    Then its security status should be "UNSECURED"

  @fast @rao @dc @contingency-scenarios @max-min-margin
  Scenario: 1.2.4.5: RaoResult AFTER PRA fixed for curative CNECs, without 2P
    Given network file is "1_multi_step_optimisation/1_2_automatons/network_1_2_4_1.uct"
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_4_4_1.json"
    Given configuration file is "1_multi_step_optimisation/1_2_automatons/rao_parameters_1_2_4_3.json"
    When I launch rao
    Then the remedial action "open_de1_de2_open_nl2_be3 - prev" is used in preventive
    Then the remedial action "open_de2_nl3 - co1 - auto" is used after "co1_fr2_de3" at "auto"
    Then the remedial action "close_fr2_de3 - co1 - auto" is not used after "co1_fr2_de3" at "auto"
    Then the remedial action "pst_be" is not used after "co1_fr2_de3" at "auto"
    Then the tap of PstRangeAction "pst_be" should be 0 after "co1_fr2_de3" at "curative"
    Then the margin on cnec "be1_be3_co1 - BBE1AA11->BBE3AA11  - co1_fr2_de3 - curative" after PRA should be -293.5 MW
    Then the margin on cnec "be1_be3_co1 - BBE1AA11->BBE3AA11  - co1_fr2_de3 - auto" after ARA should be -360.65 MW
    Then the margin on cnec "be1_be3_co1 - BBE1AA11->BBE3AA11  - co1_fr2_de3 - curative" after CRA should be -360.65 MW
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"

  @fast @rao @dc @second-preventive @max-min-margin
  Scenario: 1.2.4.6: Considering ARA in 2P improves 2P optimization
    Given network file is "1_multi_step_optimisation/1_2_automatons/network_1_2_4_6.uct" for CORE CC
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_4_6_2.json"
    Given configuration file is "1_multi_step_optimisation/1_2_automatons/rao_parameters_1_2_4_1.json"
    When I launch rao
    Then the remedial action "Open line BE1-BE2" is used in preventive
    Then the remedial action "Open line FR2-DE3" is used after "Contingency_FR1_FR2" at "auto"
    Then the margin on cnec "BE1-BE3 - preventive" after PRA should be 1500 MW
    Then the margin on cnec "NL3-DE2 - curative" after PRA should be -1444 MW
    Then the margin on cnec "NL3-DE2 - curative" after CRA should be 1385 MW
    Then the execution details should be "Second preventive improved first preventive results"
    Then its security status should be "SECURED"

  @fast @rao @ac @contingency-scenarios @secure-flow
  Scenario: 1.2.4.7: An initially divergent contingency is not recovered by remedial actions
  The outage of N4011-N4021 causes an initial sensitivity failure. The preventive action opens
  N1013-N1014 but does not restore convergence. Although a forced automaton could reconnect the
  outaged line, the failed contingency is not optimized: no automatic or curative action is used,
  both states remain in failure, and the sensitivity failure cost persists at every instant.
    Given network file is "0_import_export/0_2_export/0_2_3_swe_cne/nordic32.xiidm"
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_4_7.json"
    Given configuration file is "common/rao_parameters_pos_margin_ampere.json"
    When I launch rao
    Then the calculation partially fails
    Then 1 remedial actions are used in preventive
    Then the remedial action "preventive-open-line" is used in preventive
    Then the sensitivity failure cost initially should be 10000.0
    Then the sensitivity failure cost after "preventive" should be 10000.0
    Then the sensitivity failure cost after "auto" should be 10000.0
    Then the sensitivity failure cost after "curative" should be 10000.0
    Then the computation after "CO_N4011_N4021" at "auto" should be "FAILURE"
    Then the computation after "CO_N4011_N4021" at "curative" should be "FAILURE"
    Then 0 remedial actions are used after "CO_N4011_N4021" at "auto"
    Then 0 remedial actions are used after "CO_N4011_N4021" at "curative"

  @fast @rao @ac @contingency-scenarios @secure-flow
  Scenario: 1.2.4.8: Preventive remedial action resolves an initially divergent contingency
  The outage of N4042-N4043 causes an initial sensitivity failure. Preventive optimization opens
  N4043-N4044, restoring convergence and removing the sensitivity failure cost after preventive.
  Nevertheless, the initially failed contingency is not optimized: no automatic or curative action
  is used, both states remain in failure, and the sensitivity failure cost is retained for them.
    Given network file is "0_import_export/0_2_export/0_2_3_swe_cne/nordic32.xiidm"
    Given crac file is "1_multi_step_optimisation/1_2_automatons/crac_1_2_4_8.json"
    Given configuration file is "common/rao_parameters_pos_margin_ampere.json"
    When I launch rao
    Then the calculation partially fails
    Then the sensitivity failure cost initially should be 10000.0
    Then 1 remedial actions are used in preventive
    Then the remedial action "preventive-open-line" is used in preventive
    Then the sensitivity failure cost after "preventive" should be 10000.0
    Then the sensitivity failure cost after "auto" should be 10000.0
    Then the sensitivity failure cost after "curative" should be 10000.0
    Then the computation after "CO_N4042_N4043" at "auto" should be "FAILURE"
    Then the computation after "CO_N4042_N4043" at "curative" should be "FAILURE"
    Then 0 remedial actions are used after "CO_N4042_N4043" at "auto"
    Then 0 remedial actions are used after "CO_N4042_N4043" at "curative"
