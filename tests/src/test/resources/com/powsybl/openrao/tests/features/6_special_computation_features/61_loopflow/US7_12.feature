# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 7.12: Compute loop-flows for N-1 Cnecs

  @fast @loopflow-computation @ac @loopflow
  Scenario: 7.12.1 : loop-flow computation on 12 nodes network on N and N-1 states
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic7/crac_lf_2.json"
    Given loopflow glsk file is "common/glsk_lots_of_lf_12nodes.xml"
    When I launch loopflow_computation with OpenLoadFlow at "2019-01-08 21:30"

    Then the loopflow on cnec "DDE2AA1  NNL3AA1  1 - preventive" after loopflow computation should be -391.0 MW
    Then the loopflow on cnec "NNL1AA1  NNL3AA1  1 - preventive" after loopflow computation should be 297.0 MW
    Then the loopflow on cnec "NNL1AA1  NNL2AA1  1 - preventive" after loopflow computation should be 203.0 MW
    Then the loopflow on cnec "FFR2AA1  FFR3AA1  1 - preventive" after loopflow computation should be -1073.0 MW
    Then the loopflow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after loopflow computation should be -391.0 MW
    Then the loopflow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after loopflow computation should be -759.0 MW
    Then the loopflow on cnec "BBE1AA1  BBE2AA1  1 - preventive" after loopflow computation should be -908.0 MW
    Then the loopflow on cnec "FFR1AA1  FFR3AA1  1 - preventive" after loopflow computation should be -36.0 MW
    Then the loopflow on cnec "DDE2AA1  DDE3AA1  1 - preventive" after loopflow computation should be -739.0 MW
    Then the loopflow on cnec "FFR1AA1  FFR2AA1  1 - preventive" after loopflow computation should be 1036.0 MW
    Then the loopflow on cnec "FFR2AA1  DDE3AA1  1 - preventive" after loopflow computation should be -391.0 MW
    Then the loopflow on cnec "DDE1AA1  DDE3AA1  1 - preventive" after loopflow computation should be -869.0 MW
    Then the loopflow on cnec "DDE1AA1  DDE2AA1  1 - preventive" after loopflow computation should be -131.0 MW
    Then the loopflow on cnec "NNL2AA1  NNL3AA1  1 - preventive" after loopflow computation should be 94.0 MW
    Then the loopflow on cnec "BBE2AA1  FFR3AA1  1 - preventive" after loopflow computation should be -391.0 MW

    Then the loopflow on cnec "DDE2AA1  NNL3AA1  1 - Contingency FR1 FR3 - outage" after loopflow computation should be -396.0 MW
    Then the loopflow on cnec "NNL1AA1  NNL3AA1  1 - Contingency FR1 FR3 - outage" after loopflow computation should be 299.0 MW
    Then the loopflow on cnec "NNL1AA1  NNL2AA1  1 - Contingency FR1 FR3 - outage" after loopflow computation should be 201.0 MW
    Then the loopflow on cnec "FFR2AA1  FFR3AA1  1 - Contingency FR1 FR3 - outage" after loopflow computation should be -1104.0 MW
    Then the loopflow on cnec "NNL2AA1  BBE3AA1  1 - Contingency FR1 FR3 - outage" after loopflow computation should be -396.0 MW
    Then the loopflow on cnec "BBE1AA1  BBE3AA1  1 - Contingency FR1 FR3 - outage" after loopflow computation should be -757.0 MW
    Then the loopflow on cnec "BBE1AA1  BBE2AA1  1 - Contingency FR1 FR3 - outage" after loopflow computation should be -910.0 MW
    Then the loopflow on cnec "DDE2AA1  DDE3AA1  1 - Contingency FR1 FR3 - outage" after loopflow computation should be -736.0 MW
    Then the loopflow on cnec "FFR1AA1  FFR2AA1  1 - Contingency FR1 FR3 - outage" after loopflow computation should be 1000.0 MW
    Then the loopflow on cnec "FFR2AA1  DDE3AA1  1 - Contingency FR1 FR3 - outage" after loopflow computation should be -396.0 MW
    Then the loopflow on cnec "DDE1AA1  DDE3AA1  1 - Contingency FR1 FR3 - outage" after loopflow computation should be -868.0 MW
    Then the loopflow on cnec "DDE1AA1  DDE2AA1  1 - Contingency FR1 FR3 - outage" after loopflow computation should be -132.0 MW
    Then the loopflow on cnec "NNL2AA1  NNL3AA1  1 - Contingency FR1 FR3 - outage" after loopflow computation should be 98.0 MW
    Then the loopflow on cnec "BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - outage" after loopflow computation should be -396.0 MW