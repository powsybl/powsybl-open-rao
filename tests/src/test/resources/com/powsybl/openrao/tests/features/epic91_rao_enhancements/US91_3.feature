# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 91.3: pst initially out of range

  @fast @rao @mock @dc @preventive-only
  Scenario: US 91.3.1: initial PST tap outside of Range
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "epic91/cbcora_ep91us3case1.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the worst margin is 496.0 MW
    # margin on Cnec is improved
    And the initial margin on cnec "be1_be2_opp - preventive" should be -53.0 MW
    And the margin on cnec "be1_be2_opp - preventive" after PRA should be 495.0 MW
    # margin on Mnec is respected
    And the initial margin on cnec "fr1_fr2_dir - preventive" should be 70.0 MW
    And the margin on cnec "fr1_fr2_dir - preventive" after PRA should be 75.0 MW
    # pst_be can be used and pst_fr is not forced within its range
    And the tap of PstRangeAction "pst_be" should be -15 in preventive

  @fast @rao @mock @ac @contingency-scenarios
  Scenario: US 91.3.2: PST tap outside of Range at the start of the curative RAO
    Given network file is "common/TestCase16Nodes.uct" for CORE CC
    Given crac file is "epic91/cbcora_ep91us3case2.xml"
    Given configuration file is "common/RaoParameters_maxMargin_megawatt.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then the tap of PstRangeAction "pst_fr" should be 10 in preventive
    And the tap of PstRangeAction "pst_fr" should be 10 after "CO1" at "curative"
    And the margin on cnec "fr1_fr2_dir_cur - curative" after CRA should be -53 MW
