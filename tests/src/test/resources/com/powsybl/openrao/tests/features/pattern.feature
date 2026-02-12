# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

#Feature: US xx.x: Add an explicit title
#
#  # Here you can write a general description of the US (explaining the logic of the tests) if needed
#
#  @dont-run
#  Scenario: US xx.x.x: Add an explicit title - non-costly RAO example
#  Write a description here, explaining what is tested.
#  Be precise: if an element of the RaoParameters is tested, write its name explicitly.
#  If the test contains only a small difference with another test (same data but different objective function, etc.),
#  write "Same as US xx.x.x, but the xxx is xx instead of xx".
#    # Inputs
#      ## Compulsory if applicable
#    Given network file is "common/TestCase12Nodes.uct"
#    Given crac file is "epic11/ls_mnec_linearRao_ref.json"
#      ## Always define an explicit configuration file
#    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
#    # Launch
#    When I launch rao
#    # General results
#      ## Always display these lines if applicable
#    Then the execution details should be "The RAO only went through first preventive"
#    Then its security status should be "SECURED"
#    Then the worst margin is 224.0 MW
#      ## Use this instead of Then the worst margin is .. on cnec .., because there might be several cnecs with same margin
#    Then the margin on cnec "critical_branch - NNL2AA1 ->NNL3AA1  - preventive" after PRA should be 224.0 MW
#    # Specific results
#      ## RAs used: write before/after if needed, and display the value of the objective function if needed
#    Then 2 remedial actions are used in preventive
#    Then the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
#    Then PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -16
#    Then the value of the objective function after CRA should be -224.0
#      ## Optional: flows compared: if the US compares the flow on a CNEC before and after, please write both lines
#    Then the initial flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" should be 833.3 MW on side 1
#    Then the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 949.0 MW on side 1
#
#
#  @dont-run
#  Scenario: US xx.x.x: Add an explicit title - costly RAO example
#  Write a description here, explaining what is tested.
#  Be precise: if an element of the RaoParameters is tested, write its name explicitly.
#  If the test contains only a small difference with another test (same data but different objective function, etc.),
#  write "Same as US xx.x.x, but the xxx is xx instead of xx".
#
#  @dont-run
#  Scenario: US xx.x.x: Add an explicit title - time coupling (MARMOT) example
#  Write a description here, explaining what is tested.
#  Be precise: if an element of the RaoParameters is tested, write its name explicitly.
#  If the test contains only a small difference with another test (same data but different objective function, etc.),
#  write "Same as US xx.x.x, but the xxx is xx instead of xx".
