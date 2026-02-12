# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 91.3: PST initially out of range

  @fast @rao @dc @preventive-only @max-min-margin @megawatt
  Scenario: US 91.3.1: initial PST tap outside of Range
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "epic91/cbcora_ep91us3case1.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the worst margin is 496.0 MW
    # margin on Cnec is improved
    Then the initial margin on cnec "be1_be2_opp - preventive" should be -53.0 MW
    Then the margin on cnec "be1_be2_opp - preventive" after PRA should be 495.0 MW
    # margin on Mnec is respected
    Then the initial margin on cnec "fr1_fr2_dir - preventive" should be 70.0 MW
    Then the margin on cnec "fr1_fr2_dir - preventive" after PRA should be 75.0 MW
    # pst_be can be used and pst_fr is not forced within its range
    Then the tap of PstRangeAction "pst_be" should be -15 in preventive

  @fast @rao @ac @contingency-scenarios @max-min-margin @megawatt
  Scenario: US 91.3.2: PST tap outside of Range at the start of the curative RAO
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "epic91/cbcora_ep91us3case2.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt_dc.json"
    When I launch rao at "2019-01-08 12:00"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "UNSECURED"
    Then the worst margin is -53.0 MW
    Then the tap of PstRangeAction "pst_fr" should be 10 in preventive
    Then the tap of PstRangeAction "pst_fr" should be 10 after "CO1" at "curative"
    Then the margin on cnec "fr1_fr2_dir_cur - curative" after CRA should be -53 MW
