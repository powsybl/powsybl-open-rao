# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 2.2: Optimize PST tap within given ranges

  @fast @rao @mock @ac @preventive-only
  Scenario: US 2.2.1: Optimization monitoring only the PST
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic2/SL_ep2us2case1.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then its security status should be "SECURED"
    # Osiris returns the same solution (same tap selected)
    # However, the margin calculated by Osiris is 678 A, while the one calculated
    # by Farao is 778 A. Flow values in MW are the same (-15 on the PST), but hades2
    # somehow seems to behave differently between Osiris and Farao when computing
    # Ampere values (22 A in Farao, 122 in Osiris).
    Then the worst margin is 778.0 A
    Then the tap of PstRangeAction "PRA_PST_BE" should be 15 in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: US 2.2.2: Trade-off between various constraints
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic2/SL_ep2us2case2.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then its security status should be "SECURED"
    Then the worst margin is 39.0 A
    Then the margin on cnec "FFR2AA1  DDE3AA1  1 - N-1 NL1-NL3 - outage" after PRA should be 39.0 A
    Then the tap of PstRangeAction "PRA_PST_BE" should be 4 in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: US 2.2.3: Unsecure solution
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic2/SL_ep2us2case3.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then its security status should be "UNSECURED"
    Then the worst margin is -37.0 A
    Then the margin on cnec "BBE2AA1  BBE3AA1  1 - preventive" after PRA should be -37.0 A
    Then the tap of PstRangeAction "PRA_PST_BE" should be 2 in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: US 2.2.4: Range intersection for one PST
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic2/SL_ep2us2case4.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then its security status should be "SECURED"
    Then the worst margin is 26.0 A
    Then the margin on cnec "BBE2AA1  BBE3AA1  1 - preventive" after PRA should be 26.0 A
    Then the tap of PstRangeAction "PRA_PST_BE" should be 3 in preventive

  @fast @rao @mock @ac @preventive-only
  Scenario: US 2.2.5: Handle 2 PSTs
    Given network file is "common/TestCase12Nodes2PSTs.uct"
    Given crac file is "epic2/SL_ep2us2case5.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    When I launch rao
    Then its security status should be "SECURED"

    # Once again, OSIRIS returns a solution different from Farao. With selected taps
    # 12 and -10 respectively for PSTs BE and DE.
    # Farao returns another solution. The reason why the two algorithms are not going
    # through the same path is that the linear optimisation problem has very close
    # solutions in this test case, which change depending on some parameters as the
    # pst penalty cost:
    #
    #     - tap(BE) =~ 12 and tap(DE) ~= -10, the solution apparently returned by
    #       the optimisation in OSIRIS
    #     - tap(BE) =~ 16 and tap(DE) ~= -10 the solution apparently returned by
    #       the optimisation in OSIRIS
    #
    # The solution of FARAO - once the tap are rounded to their nearest integer -
    # is slighty better than the one returned by OSIRIS.

    # Below, verification of the OSIRIS solution
    # Then the worst margin is 443 A
    # Then the tap of PstRangeAction "PRA_PST_DE" should be -10
    # Then the tap of PstRangeAction "PRA_PST_BE" should be 12

    # Below, verification of the FARAO solution
    Then the worst margin is 459.0 A
    Then the tap of PstRangeAction "PRA_PST_DE" should be -10 in preventive
    Then the tap of PstRangeAction "PRA_PST_BE" should be 16 in preventive
