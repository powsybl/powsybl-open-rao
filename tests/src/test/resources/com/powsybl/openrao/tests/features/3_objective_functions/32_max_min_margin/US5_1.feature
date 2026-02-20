# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 5.1: MAX_MIN_MARGIN objective function
  This feature covers the objective-function/type MAX_MIN_MARGIN.

  @fast @rao @ac @preventive-only @secure-flow @ampere
  Scenario: US 5.1.0.a: SECURE_FLOW objective function: secure initially, no RA applied
  This test is used as a reference for positive margin objective function,for comparison with the tests with MAX_MIN_MARGIN objective function.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic5/SL_ep5us1.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 500.0 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 500.0 MW
    Then 0 remedial actions are used in preventive

  @fast @rao @dc @preventive-only @secure-flow @megawatt
  Scenario: US 5.1.0.b: use relevant number of decimals for margin and cost logging
  This test is used as a reference for positive margin stop criterion, for comparison with the tests with max margin stop criterion.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic5/SL_ep5us1b.json"
    Given configuration file is "common/RaoParameters_posMargin_megawatt_dc.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the worst margin is -0.0001 MW with a tolerance of 0.00000001 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be -0.0001 MW
    Then 0 remedial actions are used in preventive

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 5.1.1: MAX_MIN_MARGIN objective function: secure initially, optimize
  Same as US 5.1.0.a, but with MAX_MIN_MARGIN objective function.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic5/SL_ep5us1.json"
    Given configuration file is "common/RaoParameters_maxMargin_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 1000.0 MW
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 1000.0 MW
    Then 2 remedial actions are used in preventive
    Then the remedial action "Open tie-line FR1 FR2" is used in preventive
    Then the remedial action "Open tie-line FR1 FR3" is used in preventive

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 5.1.2: MAX_MIN_MARGIN objective function: maximum depth reached
  Same as US 5.1.1, but only one RA is allowed: the worst margin is lower than in 5.1.1.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic5/SL_ep5us1.json"
    Given configuration file is "epic5/RaoParameters_maxMargin_maxDepth.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 1149.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 693.0 MW
    Then 1 remedial actions are used in preventive
    Then the remedial action "Open tie-line FR1 FR2" is used in preventive

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 5.1.3: MAX_MIN_MARGIN objective function: relative minimum impact threshold not reached
  Same as US 5.1.1, but the parameter relative-minimum-impact-threshold is set at 50%. As the best RA improves the margin
  from 500 MW to 693 MW, the improvement is below 50%: no RA is activated.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic5/SL_ep5us1.json"
    Given configuration file is "epic5/RaoParameters_maxMargin_relativeMinImpact.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 871.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 500.0 MW
    Then 0 remedial actions are used in preventive

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 5.1.4.a: MAX_MIN_MARGIN objective function: absolute minimum impact threshold reached
  Same as US 5.1.1, but the parameter absolute-minimum-impact-threshold is set at 190 MW. As the best RA improves the margin
  from 500 MW to 693 MW, the improvement is more than 190 MW: the RA is activated.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic5/SL_ep5us1.json"
    Given configuration file is "epic5/RaoParameters_maxMargin_absoluteMinImpact275_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 1594.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 1000.0 MW
    Then 2 remedial actions are used in preventive
    Then the remedial action "Open tie-line FR1 FR2" is used in preventive
    Then the remedial action "Open tie-line FR1 FR3" is used in preventive

  @fast @rao @ac @preventive-only @max-min-margin @ampere
  Scenario: US 5.1.4.b: MAX_MIN_MARGIN objective function: absolute minimum impact threshold not reached
  Same as US 5.1.1, but the parameter absolute-minimum-impact-threshold is set at 195 MW. As the best RA improves the margin
  from 500 MW to 693 MW, the improvement is below 195 MW: no RA is activated.
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic5/SL_ep5us1.json"
    Given configuration file is "epic5/RaoParameters_maxMargin_absoluteMinImpact280_ampere.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 871.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - preventive" after PRA should be 500.0 MW
    Then 0 remedial actions are used in preventive