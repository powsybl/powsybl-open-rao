# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 13.7: Cross-validation Curative and Loop-flows

  @fast @rao @mock @dc @preventive-only @loopflow
  Scenario: US 13.7.1 : RAO with loop-flows in preventive perimeter limited by their threshold
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic13/CBCORA_ep13us7case1.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_withLoopFlows.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the worst margin is -337.0 MW
    And the worst margin is -337.0 MW on cnec "001_FR-DE - preventive"
    And the tap of PstRangeAction "PRA_PST_BE" should be -15 in preventive
    And the initial loopflow on cnec "001_FR-DE - preventive" should be -391.0 MW
    And the initial loopflow on cnec "003_FR-DE - outage" should be -660.0 MW
    And the loopflow threshold on cnec "001_FR-DE - preventive" should be 800.0 MW
    And the loopflow threshold on cnec "003_FR-DE - outage" should be 800.0 MW
    And the loopflow threshold on cnec "011_NL-BE - outage" should be 800.0 MW
    And the loopflow on cnec "001_FR-DE - preventive" after PRA should be -555.0 MW
    And the loopflow on cnec "003_FR-DE - outage" after PRA should be -796.0 MW
    And the value of the objective function after CRA should be 337

  @fast @rao @mock @dc @preventive-only @loopflow
  Scenario: US 13.7.2 : RAO with loop-flows in preventive perimeter limited by their initial value
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic13/CBCORA_ep13us7case2.xml"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_withLoopFlows.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the worst margin is -530.0 MW
    And the worst margin is -530.0 MW on cnec "001_FR-DE - preventive"
    And the tap of PstRangeAction "PRA_PST_BE" should be -9 in preventive
    And the remedial action "Open BE1 BE2" is used in preventive
    And the initial loopflow on cnec "001_FR-DE - preventive" should be -391.0 MW
    And the initial loopflow on cnec "005_DE-NL - preventive" should be -391.0 MW
    And the initial loopflow on cnec "009_NL-BE - preventive" should be -391.0 MW
    And the initial loopflow on cnec "013_BE-FR - preventive" should be -391.0 MW
    And the initial loopflow on cnec "003_FR-DE - outage" should be -660.0 MW
    And the initial loopflow on cnec "007_DE-NL - outage" should be -660.0 MW
    And the initial loopflow on cnec "011_NL-BE - outage" should be -660.0 MW
    And the initial loopflow on cnec "015_BE-FR - outage" should be -660.0 MW
    And the loopflow threshold on cnec "001_FR-DE - preventive" should be 700.0 MW
    And the loopflow threshold on cnec "005_DE-NL - preventive" should be 900.0 MW
    And the loopflow threshold on cnec "009_NL-BE - preventive" should be 800.0 MW
    And the loopflow threshold on cnec "013_BE-FR - preventive" should be 900.0 MW
    And the loopflow threshold on cnec "003_FR-DE - outage" should be 700.0 MW
    And the loopflow threshold on cnec "007_DE-NL - outage" should be 500.0 MW
    And the loopflow threshold on cnec "011_NL-BE - outage" should be 800.0 MW
    And the loopflow threshold on cnec "015_BE-FR - outage" should be 450.0 MW
    And the loopflow on cnec "001_FR-DE - preventive" after PRA should be -401.0 MW
    And the loopflow on cnec "005_DE-NL - preventive" after PRA should be -401.0 MW
    And the loopflow on cnec "009_NL-BE - preventive" after PRA should be -401.0 MW
    And the loopflow on cnec "013_BE-FR - preventive" after PRA should be -401.0 MW
    And the loopflow on cnec "003_FR-DE - outage" after PRA should be -658.0 MW
    And the loopflow on cnec "007_DE-NL - outage" after PRA should be -658.0 MW
    And the loopflow on cnec "011_NL-BE - outage" after PRA should be -658.0 MW
    And the loopflow on cnec "015_BE-FR - outage" after PRA should be -658.0 MW
    And the value of the objective function after CRA should be 530

  @fast @rao @mock @dc @contingency-scenarios @loopflow
  Scenario: US 13.7.3 : Curative RAO with loop-flows with non constraining thresholds
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic13/CBCORA_ep13us7case3.xml"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_withLoopFlows.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the worst margin is 524.0 MW
    And the margin on cnec "001_FR-DE - preventive" after PRA should be 524.0 MW
    And the margin on cnec "003_FR-DE - outage" after PRA should be 921.0 MW
    And the margin on cnec "003_FR-DE - curative" after CRA should be 1130.0 MW
    And the remedial action "Open BE1 BE3" is used in preventive
    And the tap of PstRangeAction "PRA_PST_BE" should be -16 after "CO1" at "curative"
    And the loopflow threshold on cnec "001_FR-DE - preventive" should be 1200.0 MW
    And the initial loopflow on cnec "001_FR-DE - preventive" should be -124.0 MW
    And the loopflow on cnec "001_FR-DE - preventive" after PRA should be -200.0 MW
    And the loopflow threshold on cnec "003_FR-DE - outage" should be 1600.0 MW
    And the initial loopflow on cnec "003_FR-DE - outage" should be -342.0 MW
    And the loopflow on cnec "003_FR-DE - outage" after PRA should be -396.0 MW
    And the loopflow threshold on cnec "003_FR-DE - curative" should be 1600.0 MW
    And the initial loopflow on cnec "003_FR-DE - curative" should be -342.0 MW
    And the loopflow on cnec "003_FR-DE - curative" after CRA should be -605.0 MW
    And the value of the objective function after CRA should be -524

  @fast @rao @mock @dc @contingency-scenarios @loopflow
  Scenario: US 13.7.4 : Curative RAO with loop-flows with constraining thresholds in curative
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic13/CBCORA_ep13us7case4.xml"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_withLoopFlows.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the worst margin is 524.0 MW
    And the margin on cnec "001_FR-DE - preventive" after PRA should be 524.0 MW
    And the margin on cnec "003_FR-DE - outage" after PRA should be 921.0 MW
    And the margin on cnec "003_FR-DE - curative" after CRA should be 1012.0 MW
    And the remedial action "Open BE1 BE3" is used in preventive
    And the tap of PstRangeAction "PRA_PST_BE" should be -7 after "CO1" at "curative"
    And the loopflow threshold on cnec "001_FR-DE - preventive" should be 1200.0 MW
    And the initial loopflow on cnec "001_FR-DE - preventive" should be -124.0 MW
    And the loopflow on cnec "001_FR-DE - preventive" after PRA should be -200.0 MW
    And the loopflow threshold on cnec "003_FR-DE - outage" should be 500.0 MW
    And the initial loopflow on cnec "003_FR-DE - outage" should be -342.0 MW
    And the loopflow on cnec "003_FR-DE - outage" after PRA should be -396.0 MW
    And the loopflow threshold on cnec "003_FR-DE - curative" should be 500.0 MW
    And the initial loopflow on cnec "003_FR-DE - curative" should be -342.0 MW
    And the loopflow on cnec "003_FR-DE - curative" after CRA should be -487.0 MW
    And the value of the objective function after CRA should be -524

  @fast @rao @mock @dc @contingency-scenarios @loopflow
  Scenario: US 13.7.5 : Curative RAO with loop-flows with constraining initial values in curative
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic13/CBCORA_ep13us7case5.xml"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_withLoopFlows.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the worst margin is 524.0 MW
    And the margin on cnec "001_FR-DE - preventive" after PRA should be 524.0 MW
    And the margin on cnec "003_FR-DE - curative" after CRA should be 855.0 MW
    And the remedial action "Open BE1 BE3" is used in preventive
    And the tap of PstRangeAction "PRA_PST_BE" should be 5 after "CO1" at "curative"
    And the loopflow threshold on cnec "001_FR-DE - preventive" should be 1200.0 MW
    And the initial loopflow on cnec "001_FR-DE - preventive" should be -124.0 MW
    And the loopflow on cnec "001_FR-DE - preventive" after PRA should be -200.0 MW
    And the loopflow threshold on cnec "003_FR-DE - outage" should be 500.0 MW
    And the initial loopflow on cnec "003_FR-DE - outage" should be -342.0 MW
    And the loopflow on cnec "003_FR-DE - outage" after PRA should be -396.0 MW
    And the loopflow threshold on cnec "003_FR-DE - curative" should be 250.0 MW
    And the initial loopflow on cnec "003_FR-DE - curative" should be -342.0 MW
    And the loopflow on cnec "003_FR-DE - curative" after CRA should be -331.0 MW
    And the value of the objective function after CRA should be -524

  @fast @rao @mock @dc @contingency-scenarios @loopflow
  Scenario: US 13.7.6 : Curative RAO with FIXED_PTDF loop-flow-approximation
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic13/CBCORA_ep13us7case4.xml"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    Given configuration file is "epic13/RaoParameters_ep13us7case6.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the worst margin is 524.0 MW
    And the margin on cnec "001_FR-DE - preventive" after PRA should be 524.0 MW
    And the margin on cnec "003_FR-DE - outage" after PRA should be 921.0 MW
    And the margin on cnec "003_FR-DE - curative" after CRA should be 1028.0 MW
    And the remedial action "Open BE1 BE3" is used in preventive
    And the tap of PstRangeAction "PRA_PST_BE" should be -16 after "CO1" at "curative"
    And the remedial action "Open NL1 NL3" is used after "CO1" at "curative"
    And the loopflow threshold on cnec "001_FR-DE - preventive" should be 1200.0 MW
    And the initial loopflow on cnec "001_FR-DE - preventive" should be -124.0 MW
    And the loopflow on cnec "001_FR-DE - preventive" after PRA should be -147.0 MW
    And the loopflow threshold on cnec "003_FR-DE - outage" should be 500.0 MW
    And the initial loopflow on cnec "003_FR-DE - outage" should be -342.0 MW
    And the loopflow on cnec "003_FR-DE - outage" after PRA should be -344.0 MW
    And the loopflow threshold on cnec "003_FR-DE - curative" should be 500.0 MW
    And the initial loopflow on cnec "003_FR-DE - curative" should be -342.0 MW
    And the loopflow on cnec "003_FR-DE - curative" after CRA should be -452.0 MW
    And the value of the objective function after CRA should be -524