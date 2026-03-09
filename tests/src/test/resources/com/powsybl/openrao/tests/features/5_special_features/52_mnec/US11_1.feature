# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 11.1: Handle MNECs in linear RAO
  # TODO: This feature covers

  @fast @rao @dc @preventive-only @max-min-margin @megawatt
  Scenario: US 11.1.1: reference run, no MNEC
  The flow on CNEC "NNL2AA1  NNL3AA1  1 - preventive" is increased because the CNEC is not limiting.
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_linearRao_ref.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the tap of PstRangeAction "PRA_PST_BE" should be -16 in preventive
    Then PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -16
    Then the value of the objective function after CRA should be -224.0
    Then the worst margin is 224.0 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    Then the initial flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" should be 833.3 MW on side 1
    Then the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 949.0 MW on side 1

  @fast @rao @dc @preventive-only @mnec @max-min-margin @megawatt
  Scenario: US 11.1.2: margin on MNEC should stay positive (initial margin > 50MW)
  Same as US 11.1.1, except that "NNL2AA1  NNL3AA1  1 - preventive" is a MNEC (threshold 900 MW) and not a CNEC (5000 MW).
  Unlike in the previous US, the flow of the MNEC cannot be increased above 900 MW, so the tap of the PST range action
  is adapted. The margin of the MNEC was positive initially, it must remain positive.
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_linearRao_1_2.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the tap of PstRangeAction "PRA_PST_BE" should be -9 in preventive
    Then PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -9
    Then the worst margin is 199.0 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    Then the initial flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" should be 833.3 MW on side 1
    Then the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 898.0 MW on side 1
    Then the margin on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 1.4 MW

  @fast @rao @dc @preventive-only @mnec @max-min-margin @megawatt
  Scenario: US 11.1.3: margin on MNEC should stay above initial value -50 MW [1] (initial margin < 0MW)
  Same as US 11.1.2, except that the threshold of the MNEC is "NNL2AA1  NNL3AA1  1 - preventive" is 800 MW (vs 900 MW).
  As the initial flow is 833 MW, the MNEC is overloaded. Its flow cannot be decreased "too much" by the RAO. The maximum
  decrease is set at 50 MW by the parameter "acceptable-margin-decrease" in "mnec-parameters".
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_linearRao_1_3.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the tap of PstRangeAction "PRA_PST_BE" should be -7 in preventive
    Then PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -7
    Then the worst margin is 192.0 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    Then the initial flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" should be 833.3 MW on side 1
    Then the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 884.1 MW on side 1
    Then the margin on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be -84.0 MW

  @fast @rao @dc @preventive-only @mnec @max-min-margin @megawatt
  Scenario: US 11.1.4: margin on MNEC should stay above initial value -50 MW [2] (50MW > initial margin > 0MW)
  Same as US 11.1.2, except that the threshold of the MNEC is "NNL2AA1  NNL3AA1  1 - preventive" is 850 MW (vs 900 MW).
  As the initial flow is 833 MW, the MNEC is not overloaded. However, its flow can still be decreased by the RAO, even
  if the margin becomes negative. The maximum decrease is set at 50 MW by the parameter "acceptable-margin-decrease"
  in "mnec-parameters".
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic11/ls_mnec_linearRao_1_4.json"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the tap of PstRangeAction "PRA_PST_BE" should be -7 in preventive
    Then PST "BBE2AA1  BBE3AA1  1" in network file with PRA is on tap -7
    Then the worst margin is 192.0 MW on cnec "FFR1AA1  FFR2AA1  1 - preventive"
    Then the initial flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" should be 833.3 MW on side 1
    Then the flow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be 884.1 MW on side 1
    Then the margin on cnec "NNL2AA1  NNL3AA1  1 - preventive" after PRA should be -34.0 MW