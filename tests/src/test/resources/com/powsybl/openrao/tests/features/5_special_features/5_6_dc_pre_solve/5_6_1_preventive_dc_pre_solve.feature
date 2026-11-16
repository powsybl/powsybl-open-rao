# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 5.6.1: DC pre-solve (HINT/FORCED)
  This feature covers the max-preventive-search-tree-depth-dc and preventive-dc-solution-usage parameters: a
  faster DC search runs ahead of the AC preventive search tree and proposes a network action combination, which
  is then injected into the AC search either as a HINT (competes with single actions at depth 1) or as a FORCED
  choice (replaces depth 1 entirely). It reuses the dataset from 2.5.2.7 (combination better than individuals),
  but with the preventive AC search tree capped at depth 1 and no "predefined-combinations" declared: the AC
  search alone can only ever pick one single action, while the deeper DC pre-solve dynamically discovers a
  jointly-optimal pair (open_fr1_fr3 + close_fr1_fr5) and recovers it in the AC result.

  @fast @rao @ac @preventive-only @secure-flow
  Scenario: 5.6.1.1: shallow AC search depth alone misses the jointly-optimal combination
    Given network file is "common/network_16nodes.uct"
    Given crac file is "2_remedial_actions/2_5_combinations/crac_2_5_2_7.xml"
    Given configuration file is "5_special_features/5_6_dc_pre_solve/rao_parameters_shallow_depth_no_dc_pre_solve.json"
    When I launch rao at "2021-04-30 22:30"
    Then its security status should be "UNSECURED"
    Then 1 remedial actions are used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the worst margin is -90.82 A
    Then the margin on cnec "fr1_fr4_co1 - FFR1AA1 ->FFR4AA1   - co1_fr2_fr3_1 - curative" after CRA should be -90.82 A

  @fast @rao @ac @preventive-only @secure-flow @dc-pre-solve
  Scenario: 5.6.1.2: DC pre-solve in HINT mode recovers the jointly-optimal combination
    Given network file is "common/network_16nodes.uct"
    Given crac file is "2_remedial_actions/2_5_combinations/crac_2_5_2_7.xml"
    Given configuration file is "5_special_features/5_6_dc_pre_solve/rao_parameters_shallow_depth_dc_pre_solve_hint.json"
    When I launch rao at "2021-04-30 22:30"
    Then its security status should be "SECURED"
    Then 2 remedial actions are used in preventive
    Then the remedial action "open_fr1_fr3" is used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the worst margin is 45.17 A
    Then the margin on cnec "be2_fr3_n - BBE2AA1 ->FFR3AA1  - preventive" after PRA should be 45.17 A
    Then the margin on cnec "fr1_fr4_co1 - FFR1AA1 ->FFR4AA1   - co1_fr2_fr3_1 - curative" after CRA should be 108.45 A

  @fast @rao @ac @preventive-only @secure-flow @dc-pre-solve
  Scenario: 5.6.1.3: DC pre-solve in FORCED mode recovers the jointly-optimal combination
    Given network file is "common/network_16nodes.uct"
    Given crac file is "2_remedial_actions/2_5_combinations/crac_2_5_2_7.xml"
    Given configuration file is "5_special_features/5_6_dc_pre_solve/rao_parameters_shallow_depth_dc_pre_solve_forced.json"
    When I launch rao at "2021-04-30 22:30"
    Then its security status should be "SECURED"
    Then 2 remedial actions are used in preventive
    Then the remedial action "open_fr1_fr3" is used in preventive
    Then the remedial action "close_fr1_fr5" is used in preventive
    Then the worst margin is 45.17 A
    Then the margin on cnec "be2_fr3_n - BBE2AA1 ->FFR3AA1  - preventive" after PRA should be 45.17 A
    Then the margin on cnec "fr1_fr4_co1 - FFR1AA1 ->FFR4AA1   - co1_fr2_fr3_1 - curative" after CRA should be 108.45 A
