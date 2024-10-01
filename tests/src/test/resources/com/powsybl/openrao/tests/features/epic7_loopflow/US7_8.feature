# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 7.8: Loopflow computation (not within the RAO)

  @fast @rao @mock @ac @preventive-only
  Scenario: US 7.8.1: optimise network action without loop flow limitation
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf_rao_2.json"
    Given configuration file is "common/RaoParameters_posMargin_ac.json"
    When I launch search_tree_rao
    Then its security status should be "SECURED"
    Then the worst margin is 92.0 MW
    Then 1 remedial actions are used in preventive

  @fast @loopflow-computation @mock @ac @loopflow
  Scenario: US 7.8.2: loopflow computation on 12 nodes network with proportional GLSKs
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf.json"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    When I launch loopflow_computation with OpenLoadFlow
    Then the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after loopflow computation should be -124.0 MW
    And the loopflow on cnec "NNL1AA1  NNL3AA1  1 - preventive" after loopflow computation should be 354.0 MW
    And the loopflow on cnec "NNL1AA1  NNL2AA1  1 - preventive" after loopflow computation should be 334.0 MW
    And the loopflow on cnec "FFR2AA1  FFR3AA1  1 - preventive" after loopflow computation should be -870.0 MW
    And the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after loopflow computation should be -124.0 MW
    And the loopflow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after loopflow computation should be -530.0 MW
    And the loopflow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after loopflow computation should be -898.0 MW
    And the loopflow on cnec "FFR1AA1  FFR3AA1  1 - preventive" after loopflow computation should be -77.0 MW
    And the loopflow on cnec "DDE2AA1  DDE3AA1  1 - preventive" after loopflow computation should be -15.0 MW
    And the loopflow on cnec "FFR1AA1  FFR2AA1  1 - preventive" after loopflow computation should be 792.0 MW
    And the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after loopflow computation should be -124.0 MW
    And the loopflow on cnec "DDE1AA1  DDE3AA1  1 - preventive" after loopflow computation should be 13.0 MW
    And the loopflow on cnec "DDE1AA1  DDE2AA1  1 - preventive" after loopflow computation should be 28.0 MW
    And the loopflow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after loopflow computation should be 20.0 MW
    And the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after loopflow computation should be -124.0 MW

  @fast @loopflow-computation @mock @ac @loopflow
  Scenario: US 7.8.3: loopflow computation on 12 nodes network with proportional GLSKs
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf.json"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    When I launch loopflow_computation with OpenLoadFlow
    Then the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after loopflow computation should be -391.0 MW
    And the loopflow on cnec "NNL1AA1  NNL3AA1  1 - preventive" after loopflow computation should be 297.0 MW
    And the loopflow on cnec "NNL1AA1  NNL2AA1  1 - preventive" after loopflow computation should be 203.0 MW
    And the loopflow on cnec "FFR2AA1  FFR3AA1  1 - preventive" after loopflow computation should be -1073.0 MW
    And the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after loopflow computation should be -391.0 MW
    And the loopflow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after loopflow computation should be -759.0 MW
    And the loopflow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after loopflow computation should be -908.0 MW
    And the loopflow on cnec "FFR1AA1  FFR3AA1  1 - preventive" after loopflow computation should be -36.0 MW
    And the loopflow on cnec "DDE2AA1  DDE3AA1  1 - preventive" after loopflow computation should be -739.0 MW
    And the loopflow on cnec "FFR1AA1  FFR2AA1  1 - preventive" after loopflow computation should be 1036.0 MW
    And the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after loopflow computation should be -391.0 MW
    And the loopflow on cnec "DDE1AA1  DDE3AA1  1 - preventive" after loopflow computation should be -869.0 MW
    And the loopflow on cnec "DDE1AA1  DDE2AA1  1 - preventive" after loopflow computation should be -131.0 MW
    And the loopflow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after loopflow computation should be 94.0 MW
    And the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after loopflow computation should be -391.0 MW