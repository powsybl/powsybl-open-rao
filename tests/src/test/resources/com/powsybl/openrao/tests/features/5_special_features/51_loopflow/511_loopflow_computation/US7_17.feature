# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 7.17: Handle LSKs for PTDF computation
  This feature covers loopflow_computation with OpenLoadFlow with LSKs in input files.

  @fast @loopflow-computation @ac @loopflow
  Scenario: 7.17.1 : LoopFLow computation with LSKs
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf.json"
    Given loopflow glsk file is "common/glsk_with_lsks.xml"
    When I launch loopflow_computation with OpenLoadFlow
    Then the loopflow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after loopflow computation should be -763.0 MW
    Then the loopflow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after loopflow computation should be -570.0 MW
    Then the loopflow on cnec "FFR1AA1  FFR2AA1  1 - preventive" after loopflow computation should be 1025.0 MW
    Then the loopflow on cnec "FFR1AA1  FFR3AA1  1 - preventive" after loopflow computation should be -25.0 MW
    Then the loopflow on cnec "FFR2AA1  FFR3AA1  1 - preventive" after loopflow computation should be -1051.0 MW
    Then the loopflow on cnec "DDE1AA1  DDE2AA1  1 - preventive" after loopflow computation should be -149.0 MW
    Then the loopflow on cnec "DDE1AA1  DDE3AA1  1 - preventive" after loopflow computation should be -663.0 MW
    Then the loopflow on cnec "DDE2AA1  DDE3AA1  1 - preventive" after loopflow computation should be -513.0 MW
    Then the loopflow on cnec "NNL1AA1  NNL2AA1  1 - preventive" after loopflow computation should be 259.0 MW
    Then the loopflow on cnec "NNL1AA1  NNL3AA1  1 - preventive" after loopflow computation should be 340.0 MW
    Then the loopflow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after loopflow computation should be 81.0 MW
    Then the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after loopflow computation should be -322.0 MW
    Then the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after loopflow computation should be -322.0 MW
    Then the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after loopflow computation should be -322.0 MW
    Then the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after loopflow computation should be -322.0 MW

  @fast @loopflow-computation @ac @loopflow
  Scenario: 7.17.2 : LoopFLow computation with LSKs and refProg
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf.json"
    Given loopflow glsk file is "common/glsk_with_lsks.xml"
    Given RefProg file is "epic7/refProg_12nodes.xml"
    When I launch loopflow_computation with OpenLoadFlow at "2019-01-08 21:30"
    Then the loopflow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after loopflow computation should be -738.0 MW
    Then the loopflow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after loopflow computation should be -561.0 MW
    Then the loopflow on cnec "FFR1AA1  FFR2AA1  1 - preventive" after loopflow computation should be 1081.0 MW
    Then the loopflow on cnec "FFR1AA1  FFR3AA1  1 - preventive" after loopflow computation should be -81.0 MW
    Then the loopflow on cnec "FFR2AA1  FFR3AA1  1 - preventive" after loopflow computation should be -1163.0 MW
    Then the loopflow on cnec "DDE1AA1  DDE2AA1  1 - preventive" after loopflow computation should be -218.0 MW
    Then the loopflow on cnec "DDE1AA1  DDE3AA1  1 - preventive" after loopflow computation should be -616.0 MW
    Then the loopflow on cnec "DDE2AA1  DDE3AA1  1 - preventive" after loopflow computation should be -398.0 MW
    Then the loopflow on cnec "NNL1AA1  NNL2AA1  1 - preventive" after loopflow computation should be 265.0 MW
    Then the loopflow on cnec "NNL1AA1  NNL3AA1  1 - preventive" after loopflow computation should be 374.0 MW
    Then the loopflow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after loopflow computation should be 109.0 MW
    Then the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after loopflow computation should be -244.0 MW
    Then the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after loopflow computation should be -544.0 MW
    Then the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after loopflow computation should be -344.0 MW
    Then the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after loopflow computation should be -144.0 MW