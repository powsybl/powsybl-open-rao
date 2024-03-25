# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 91.12: Multi-curative

  @fast @rao @ac
  Scenario: US 91.12.1: All combinations of auto and curative states
    # Test state tree algorithm
    Given network file is "epic91/TestCase12Nodes_16ParallelLines.uct"
    Given crac file is "epic91/crac_12Nodes_16ParallelLines.json"
    Given configuration file is "epic91/RaoParameters_case_91_1_12.json"
    When I launch search_tree_rao
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - Contingency NL2 BE3 5 - auto" after CRA should be -279.74 A
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - Contingency NL2 BE3 6 - auto" after CRA should be -279.74 A
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - Contingency NL2 BE3 7 - auto" after CRA should be -279.74 A
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - Contingency NL2 BE3 8 - auto" after CRA should be -279.74 A
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - Contingency NL2 BE3 D - auto" after CRA should be 0 A
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - Contingency NL2 BE3 E - auto" after CRA should be 0 A
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - Contingency NL2 BE3 F - auto" after CRA should be 0 A
    Then the flow on cnec "BBE1AA1  BBE2AA1  1 - Contingency NL2 BE3 G - auto" after CRA should be 0 A
    Then the flow on cnec "DDE1AA1  DDE2AA1  1 - Contingency NL2 BE3 2 - curative" after CRA should be 201.41 A
    Then the flow on cnec "DDE1AA1  DDE2AA1  1 - Contingency NL2 BE3 6 - curative" after CRA should be 201.41 A
    Then the flow on cnec "DDE1AA1  DDE2AA1  1 - Contingency NL2 BE3 A - curative" after CRA should be 216.79 A
    Then the flow on cnec "DDE1AA1  DDE2AA1  1 - Contingency NL2 BE3 E - curative" after CRA should be 216.79 A
    Then the flow on cnec "DDE1AA1  DDE2AA1  1 - Contingency NL2 BE3 8 - curative" after CRA should be 0 A
    Then the flow on cnec "DDE1AA1  DDE2AA1  1 - Contingency NL2 BE3 4 - curative" after CRA should be 0 A
    Then the flow on cnec "DDE1AA1  DDE2AA1  1 - Contingency NL2 BE3 C - curative" after CRA should be 0 A
    Then the flow on cnec "DDE1AA1  DDE2AA1  1 - Contingency NL2 BE3 G - curative" after CRA should be 0 A

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.2: Multi-curative with PST range actions and Topological actions
    # The network has three parallel lines. Initially, two of them are opened and the third one is a CNEC
    # The flow on the CNEC is initially smaller than the PATL and the first TATL so no PRA is needed
    # 1. First a PST is activated to go under the first curative TATL (500 MW)
    # 2. One of the two other lines is parallel is closed to divide the flow by 2 and make it go under the second curative TATL (300 MW)
    # 3. The second line is closed to go under the third curative TATL (250 MW)
    Given network file is "epic91/12Nodes3ParallelLines.uct"
    Given crac file is "epic91/crac_91_12_2.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_secure.json"
    When I launch search_tree_rao
    # Basecase / After PRA
    Then 0 remedial actions are used in preventive
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be 500.0 MW
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - Contingency DE2 DE3 1 - curative1" after "preventive" instant remedial actions should be 583.0 MW
    # After first curative
    Then 1 remedial actions are used after "Contingency DE2 DE3 1" at "curative1"
    Then the tap of PstRangeAction "CRA_PST_BE" should be -11 after "Contingency DE2 DE3 1" at "curative1"
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - Contingency DE2 DE3 1 - curative1" after "curative1" instant remedial actions should be 500.0 MW
    # After second curative
    Then 1 remedial actions are used after "Contingency DE2 DE3 1" at "curative2"
    Then the remedial action "CRA_CLOSE_NL2_BE3_2" is used after "Contingency DE2 DE3 1" at "curative2"
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - Contingency DE2 DE3 1 - curative2" after "curative2" instant remedial actions should be 263.0 MW
    # After third curative
    Then 1 remedial actions are used after "Contingency DE2 DE3 1" at "curative3"
    Then the remedial action "CRA_CLOSE_NL2_BE3_3" is used after "Contingency DE2 DE3 1" at "curative3"
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - Contingency DE2 DE3 1 - curative3" after "curative3" instant remedial actions should be 179.0 MW

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.2: Multi-curative with AUTO + curative instant 1 without CRAs
    Given network file is "epic91/TestCase16Nodes_multi_curative.uct"
    Given crac file is "epic91/crac_91_12_auto_curative1_noCRA.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_secure.json"
    When I launch search_tree_rao
    # Basecase / After PRA (PATL 500 MW)
    Then 0 remedial actions are used in preventive
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after PRA should be -326.0 MW
    # Outage (TATL 1000 MW)
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after PRA should be -1000.0 MW
    # After auto (TATL 850 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "auto"
    Then the remedial action "ARA_CLOSE_BE1_BE2_1" is used after "Contingency DE2 NL3 1" at "auto"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "auto" instant remedial actions should be -667.0 MW
    # After first curative (TATL 700 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative1"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative1" instant remedial actions should be -667.0 MW
    # After second curative (TATL 590 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "curative2"
    Then the tap of PstRangeAction "CRA_PST_BE" should be 3 after "Contingency DE2 NL3 1" at "curative2"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative2" instant remedial actions should be -568.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative2" instant remedial actions should be -568.0 MW
    # After third curative (PATL 500 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "curative3"
    Then the remedial action "CRA_CLOSE_BE3_BE4_1" is used after "Contingency DE2 NL3 1" at "curative3"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative3" instant remedial actions should be -283.0 MW

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.2: Same case as previous one with ra limitations : 0 curative1 RAs, 0 curative2 RAs
    Given network file is "epic91/12Nodes3ParallelLines.uct"
    Given crac file is "epic91/crac_91_12_2_with_ra_limits.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_secure.json"
    When I launch search_tree_rao
    Then 0 remedial actions are used after "Contingency DE2 DE3 1" at "curative1"
    Then 0 remedial actions are used after "Contingency DE2 DE3 1" at "curative2"
    Then 1 remedial actions are used after "Contingency DE2 DE3 1" at "curative3"

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.3: Multi-curative with AUTO + curative instant 2 without CRAs
    Given network file is "epic91/TestCase16Nodes_multi_curative.uct"
    Given crac file is "epic91/crac_91_12_auto_curative2_noCRA.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_secure.json"
    When I launch search_tree_rao
    # Basecase / After PRA (PATL 500 MW)
    Then 0 remedial actions are used in preventive
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after PRA should be -326.0 MW
    # Outage (TATL 1000 MW)
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after PRA should be -1000.0 MW
    # After auto (TATL 700 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "auto"
    Then the remedial action "ARA_CLOSE_BE1_BE2_1" is used after "Contingency DE2 NL3 1" at "auto"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "auto" instant remedial actions should be -667.0 MW
    # After first curative (TATL 630 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "curative1"
    Then the tap of PstRangeAction "CRA_PST_BE" should be 3 after "Contingency DE2 NL3 1" at "curative1"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "curative1" instant remedial actions should be -568.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative1" instant remedial actions should be -568.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative1" instant remedial actions should be -568.0 MW
    # After second curative (TATL 590 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative2"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative2" instant remedial actions should be -568.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative2" instant remedial actions should be -568.0 MW
    # After third curative (PATL 500 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "curative3"
    Then the remedial action "CRA_CLOSE_BE3_BE4_1" is used after "Contingency DE2 NL3 1" at "curative3"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative3" instant remedial actions should be -283.0 MW

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.4: Multi-curative with AUTO + curative instant 3 without CRAs
    Given network file is "epic91/TestCase16Nodes_multi_curative.uct"
    Given crac file is "epic91/crac_91_12_auto_curative3_noCRA.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_secure.json"
    When I launch search_tree_rao
    # Basecase / After PRA (PATL 500 MW)
    Then 0 remedial actions are used in preventive
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after PRA should be -326.0 MW
    # Outage (TATL 1000 MW)
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after PRA should be -1000.0 MW
    # After auto (TATL 700 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "auto"
    Then the remedial action "ARA_CLOSE_BE1_BE2_1" is used after "Contingency DE2 NL3 1" at "auto"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "auto" instant remedial actions should be -667.0 MW
    # After first curative (TATL 630 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "curative1"
    Then the tap of PstRangeAction "CRA_PST_BE" should be 3 after "Contingency DE2 NL3 1" at "curative1"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "curative1" instant remedial actions should be -568.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative1" instant remedial actions should be -568.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative1" instant remedial actions should be -568.0 MW
    # After second curative (TATL 590 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "curative2"
    Then the remedial action "CRA_CLOSE_BE3_BE4_1" is used after "Contingency DE2 NL3 1" at "curative2"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative2" instant remedial actions should be -283.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative2" instant remedial actions should be -283.0 MW
    # After third curative (PATL 500 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative3"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative3" instant remedial actions should be -283.0 MW

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.5: Multi-curative with curative instant 1 without CRAs
    Given network file is "epic91/TestCase16Nodes_multi_curative.uct"
    Given crac file is "epic91/crac_91_12_curative1_noCRA.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_secure.json"
    When I launch search_tree_rao
    # Basecase / After PRA (PATL 500 MW)
    Then the initial flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" should be -326.0 MW
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_CLOSE_BE1_BE2_1" is used after "Contingency DE2 NL3 1" at "preventive"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after PRA should be -228.0 MW
    # Outage (TATL 1000 MW)
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after PRA should be -667.0 MW
    # After first curative (TATL 700 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative1"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative1" instant remedial actions should be -667.0 MW
    # After second curative (TATL 590 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "curative2"
    Then the tap of PstRangeAction "CRA_PST_BE" should be 3 after "Contingency DE2 NL3 1" at "curative2"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative2" instant remedial actions should be -568.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative2" instant remedial actions should be -568.0 MW
    # After third curative (PATL 500 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "curative3"
    Then the remedial action "CRA_CLOSE_BE3_BE4_1" is used after "Contingency DE2 NL3 1" at "curative3"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative3" instant remedial actions should be -283.0 MW

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.6: Multi-curative with curative instant 2 without CRAs
    Given network file is "epic91/TestCase16Nodes_multi_curative.uct"
    Given crac file is "epic91/crac_91_12_curative2_noCRA.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_search_tree.json"
    When I launch search_tree_rao
    # Basecase / After PRA (PATL 300 MW)
    Then the initial flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" should be -326.0 MW
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_CLOSE_BE1_BE2_1" is used after "Contingency DE2 NL3 1" at "preventive"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after PRA should be -228.0 MW
    # Outage (TATL 1000 MW)
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after PRA should be -667.0 MW
    # After first curative (TATL 700 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "curative1"
    Then the tap of PstRangeAction "CRA_PST_BE" should be 3 after "Contingency DE2 NL3 1" at "curative1"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "curative1" instant remedial actions should be -568.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative1" instant remedial actions should be -568.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative1" instant remedial actions should be -568.0 MW
    # After second curative (TATL 590 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative2"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative2" instant remedial actions should be -568.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative2" instant remedial actions should be -568.0 MW
    # After third curative (PATL 300 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "curative3"
    Then the remedial action "CRA_CLOSE_BE3_BE4_1" is used after "Contingency DE2 NL3 1" at "curative3"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative3" instant remedial actions should be -283.0 MW

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.7: Multi-curative with curative instant 3 without CRAs
    # CNECs are already secure when entering curative2 perimeter
    # C2RAs must be activated anyway in anticipation of curative3
    Given network file is "epic91/TestCase16Nodes_multi_curative.uct"
    Given crac file is "epic91/crac_91_12_curative3_noCRA.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_secure.json"
    When I launch search_tree_rao
    # Basecase / After PRA (PATL 300 MW)
    Then the initial flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" should be -326.0 MW
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_CLOSE_BE1_BE2_1" is used after "Contingency DE2 NL3 1" at "preventive"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after PRA should be -228.0 MW
    # Outage (TATL 1000 MW)
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after PRA should be -667.0 MW
    # After first curative (TATL 630 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "curative1"
    Then the tap of PstRangeAction "CRA_PST_BE" should be 3 after "Contingency DE2 NL3 1" at "curative1"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "curative1" instant remedial actions should be -568.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative1" instant remedial actions should be -568.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative1" instant remedial actions should be -568.0 MW
    # After second curative (TATL 590 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "curative2"
    Then the remedial action "CRA_CLOSE_BE3_BE4_1" is used after "Contingency DE2 NL3 1" at "curative2"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative2" instant remedial actions should be -283.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative2" instant remedial actions should be -283.0 MW
    # After third curative (PATL 300 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative3"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative3" instant remedial actions should be -283.0 MW

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.8: Multi-curative with ARA and no CRAs
    Given network file is "epic91/TestCase16Nodes_multi_curative.uct"
    Given crac file is "epic91/crac_91_12_ARA_noCRA.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_secure.json"
    When I launch search_tree_rao
    # Basecase / After PRA (PATL 300 MW)
    Then the initial flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" should be -326.0 MW
    Then 0 remedial actions are used in preventive
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after PRA should be -326.0 MW
    # Outage (TATL 1000 MW)
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after PRA should be -1000.0 MW
    # After auto (TATL 850 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "auto"
    Then the remedial action "ARA_CLOSE_BE1_BE2_1" is used after "Contingency DE2 NL3 1" at "auto"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "auto" instant remedial actions should be -667.0 MW
    # After first curative (TATL 800 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative1"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative1" instant remedial actions should be -667.0 MW
    # After second curative (TATL 750 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative2"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative2" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative2" instant remedial actions should be -667.0 MW
    # After third curative (TATL 700 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative3"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative3" instant remedial actions should be -667.0 MW

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.9: Multi-curative without CRAs
    Given network file is "epic91/TestCase16Nodes_multi_curative.uct"
    Given crac file is "epic91/crac_91_12_noCRA.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_secure.json"
    When I launch search_tree_rao
    # Basecase / After PRA (PATL 300 MW)
    Then the initial flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" should be -326.0 MW
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_CLOSE_BE1_BE2_1" is used after "Contingency DE2 NL3 1" at "preventive"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after PRA should be -228.0 MW
    # Outage (TATL 1000 MW)
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after PRA should be -667.0 MW
    # After first curative (TATL 800 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative1"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative1" instant remedial actions should be -667.0 MW
    # After second curative (TATL 750 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative2"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative2" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative2" instant remedial actions should be -667.0 MW
    # After third curative (TATL 700 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative3"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative3" instant remedial actions should be -667.0 MW

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.10: Multi-curative with ARA and only C1RA
    Given network file is "epic91/TestCase16Nodes_multi_curative.uct"
    Given crac file is "epic91/crac_91_12_auto_C1RA.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_search_tree.json"
    When I launch search_tree_rao
    # Basecase / After PRA (PATL 500 MW)
    Then 0 remedial actions are used in preventive
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after PRA should be -326.0 MW
    # Outage (TATL 1000 MW)
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after PRA should be -1000.0 MW
    # After auto (TATL 850 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "auto"
    Then the remedial action "ARA_CLOSE_BE1_BE2_1" is used after "Contingency DE2 NL3 1" at "auto"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "auto" instant remedial actions should be -667.0 MW
    # After first curative (TATL 700 MW)
    Then 2 remedial actions are used after "Contingency DE2 NL3 1" at "curative1"
    Then the remedial action "CRA_CLOSE_BE3_BE4_1" is used after "Contingency DE2 NL3 1" at "curative1"
    Then the tap of PstRangeAction "CRA_PST_BE" should be 3 after "Contingency DE2 NL3 1" at "curative1"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "curative1" instant remedial actions should be -283.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative1" instant remedial actions should be -283.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative1" instant remedial actions should be -283.0 MW
    # After second curative (TATL 590 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative2"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative2" instant remedial actions should be -283.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative2" instant remedial actions should be -283.0 MW
    # After third curative (PATL 500 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative3"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative3" instant remedial actions should be -283.0 MW

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.11: Multi-curative with ARA and only C2RA
    Given network file is "epic91/TestCase16Nodes_multi_curative.uct"
    Given crac file is "epic91/crac_91_12_auto_C2RA.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_search_tree.json"
    When I launch search_tree_rao
    # Basecase / After PRA (PATL 500 MW)
    Then 0 remedial actions are used in preventive
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after PRA should be -326.0 MW
    # Outage (TATL 1000 MW)
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after PRA should be -1000.0 MW
    # After auto (TATL 850 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "auto"
    Then the remedial action "ARA_CLOSE_BE1_BE2_1" is used after "Contingency DE2 NL3 1" at "auto"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "auto" instant remedial actions should be -667.0 MW
    # After first curative (TATL 700 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative1"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative1" instant remedial actions should be -667.0 MW
    # After second curative (TATL 590 MW)
    Then 2 remedial actions are used after "Contingency DE2 NL3 1" at "curative2"
    Then the remedial action "CRA_CLOSE_BE3_BE4_1" is used after "Contingency DE2 NL3 1" at "curative2"
    Then the tap of PstRangeAction "CRA_PST_BE" should be 3 after "Contingency DE2 NL3 1" at "curative2"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative2" instant remedial actions should be -283.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative2" instant remedial actions should be -283.0 MW
    # After third curative (PATL 500 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative3"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative3" instant remedial actions should be -283.0 MW

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.12: Multi-curative with ARA and only C3RA
    Given network file is "epic91/TestCase16Nodes_multi_curative.uct"
    Given crac file is "epic91/crac_91_12_auto_C3RA.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_search_tree.json"
    When I launch search_tree_rao
    # Basecase / After PRA (PATL 400 MW)
    Then 0 remedial actions are used in preventive
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after PRA should be -326.0 MW
    # Outage (TATL 1000 MW)
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after PRA should be -1000.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after PRA should be -1000.0 MW
    # After auto (TATL 850 MW)
    Then 1 remedial actions are used after "Contingency DE2 NL3 1" at "auto"
    Then the remedial action "ARA_CLOSE_BE1_BE2_1" is used after "Contingency DE2 NL3 1" at "auto"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "auto" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "auto" instant remedial actions should be -667.0 MW
    # After first curative (TATL 750 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative1"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative1" instant remedial actions should be -667.0 MW
    # After second curative (TATL 690 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative2"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative2" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative2" instant remedial actions should be -667.0 MW
    # After third curative (PATL 400 MW)
    Then 2 remedial actions are used after "Contingency DE2 NL3 1" at "curative3"
    Then the remedial action "CRA_CLOSE_BE3_BE4_1" is used after "Contingency DE2 NL3 1" at "curative3"
    Then the tap of PstRangeAction "CRA_PST_BE" should be 3 after "Contingency DE2 NL3 1" at "curative3"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative3" instant remedial actions should be -283.0 MW

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.13: Multi-curative with only C1RA
    Given network file is "epic91/TestCase16Nodes_multi_curative.uct"
    Given crac file is "epic91/crac_91_12_C1RA.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_search_tree.json"
    When I launch search_tree_rao
    # Basecase / After PRA (PATL 500 MW)
    Then the initial flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" should be -326.0 MW
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_CLOSE_BE1_BE2_1" is used after "Contingency DE2 NL3 1" at "preventive"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after PRA should be -228.0 MW
    # Outage (TATL 1000 MW)
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after PRA should be -667.0 MW
    # After first curative (TATL 700 MW)
    Then 2 remedial actions are used after "Contingency DE2 NL3 1" at "curative1"
    Then the remedial action "CRA_CLOSE_BE3_BE4_1" is used after "Contingency DE2 NL3 1" at "curative1"
    Then the tap of PstRangeAction "CRA_PST_BE" should be 3 after "Contingency DE2 NL3 1" at "curative1"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "curative1" instant remedial actions should be -283.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative1" instant remedial actions should be -283.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative1" instant remedial actions should be -283.0 MW
    # After second curative (TATL 590 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative2"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative2" instant remedial actions should be -283.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative2" instant remedial actions should be -283.0 MW
    # After third curative (PATL 500 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative3"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative3" instant remedial actions should be -283.0 MW

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.14: Multi-curative with only C2RA
    Given network file is "epic91/TestCase16Nodes_multi_curative.uct"
    Given crac file is "epic91/crac_91_12_C2RA.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_search_tree.json"
    When I launch search_tree_rao
    # Basecase / After PRA (PATL 500 MW)
    Then the initial flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" should be -326.0 MW
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_CLOSE_BE1_BE2_1" is used after "Contingency DE2 NL3 1" at "preventive"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after PRA should be -228.0 MW
    # Outage (TATL 1000 MW)
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after PRA should be -667.0 MW
    # After first curative (TATL 700 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative1"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative1" instant remedial actions should be -667.0 MW
    # After second curative (TATL 590 MW)
    Then 2 remedial actions are used after "Contingency DE2 NL3 1" at "curative2"
    Then the remedial action "CRA_CLOSE_BE3_BE4_1" is used after "Contingency DE2 NL3 1" at "curative2"
    Then the tap of PstRangeAction "CRA_PST_BE" should be 3 after "Contingency DE2 NL3 1" at "curative2"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative2" instant remedial actions should be -283.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative2" instant remedial actions should be -283.0 MW
    # After third curative (PATL 500 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative3"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative3" instant remedial actions should be -283.0 MW

  @fast @rao @ac @multi-curative
  Scenario: US 91.12.15: Multi-curative with only C3RA
    Given network file is "epic91/TestCase16Nodes_multi_curative.uct"
    Given crac file is "epic91/crac_91_12_C3RA.json"
    Given configuration file is "epic91/RaoParameters_case_91_12_search_tree.json"
    When I launch search_tree_rao
    # Basecase / After PRA (PATL 500 MW)
    Then the initial flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" should be -326.0 MW
    Then 1 remedial actions are used in preventive
    Then the remedial action "PRA_CLOSE_BE1_BE2_1" is used after "Contingency DE2 NL3 1" at "preventive"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - preventive" after PRA should be -228.0 MW
    # Outage (TATL 1000 MW)
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after PRA should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after PRA should be -667.0 MW
    # After first curative (TATL 750 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative1"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative1" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative1" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative1" instant remedial actions should be -667.0 MW
    # After second curative (TATL 690 MW)
    Then 0 remedial actions are used after "Contingency DE2 NL3 1" at "curative2"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative2" after "curative2" instant remedial actions should be -667.0 MW
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative2" instant remedial actions should be -667.0 MW
    # After third curative (PATL 500 MW)
    Then 2 remedial actions are used after "Contingency DE2 NL3 1" at "curative3"
    Then the remedial action "CRA_CLOSE_BE3_BE4_1" is used after "Contingency DE2 NL3 1" at "curative3"
    Then the tap of PstRangeAction "CRA_PST_BE" should be 3 after "Contingency DE2 NL3 1" at "curative3"
    Then the flow on cnec "BBE1AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative3" after "curative3" instant remedial actions should be -283.0 MW
