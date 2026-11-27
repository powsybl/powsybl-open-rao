# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 5.6.2: DC pre-solve (HINT/FORCED) - curative half
  This feature covers the pre-solve-parameters' max-curative-search-tree-depth and curative-solution-usage: a
  faster DC search runs ahead of the AC curative search tree and proposes a network action combination, which
  is then injected into the AC search either as a HINT (competes with single actions at depth 1) or as a FORCED
  choice (replaces depth 1 entirely). It reuses the dataset from 2.5.2.2 (optimal combination in curative), but
  with the curative AC search tree capped at depth 1 and no "predefined-combinations" declared: the AC search
  alone can only ever pick one single action, while the deeper DC pre-solve dynamically discovers the
  jointly-optimal triple (open_fr1_fr3 + open_be1_be4 + close_fr1_fr5) and recovers it in the AC result.

  @fast @rao @ac @contingency-scenarios @secure-flow
  Scenario: 5.6.2.1: shallow AC search depth alone misses the jointly-optimal combination
    Given network file is "2_remedial_actions/2_5_combinations/network_2_5_2_2.uct"
    Given crac file is "2_remedial_actions/2_5_combinations/crac_2_5_2_2.xml"
    Given configuration file is "5_special_features/5_6_dc_pre_solve/rao_parameters_shallow_depth_no_dc_pre_solve.json"
    When I launch rao at "2021-04-30 22:30"
    Then 1 remedial actions are used after "co1_fr2_fr3_1" at "curative"

  @fast @rao @ac @contingency-scenarios @secure-flow @dc-pre-solve
  Scenario: 5.6.2.2: DC pre-solve in HINT mode recovers the jointly-optimal combination
    Given network file is "2_remedial_actions/2_5_combinations/network_2_5_2_2.uct"
    Given crac file is "2_remedial_actions/2_5_combinations/crac_2_5_2_2.xml"
    Given configuration file is "5_special_features/5_6_dc_pre_solve/rao_parameters_shallow_depth_dc_pre_solve_hint_curative.json"
    When I launch rao at "2021-04-30 22:30"
    Then 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_be1_be4" is used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"

  @fast @rao @ac @contingency-scenarios @secure-flow @dc-pre-solve
  Scenario: 5.6.2.3: DC pre-solve in FORCED mode recovers the jointly-optimal combination
    Given network file is "2_remedial_actions/2_5_combinations/network_2_5_2_2.uct"
    Given crac file is "2_remedial_actions/2_5_combinations/crac_2_5_2_2.xml"
    Given configuration file is "5_special_features/5_6_dc_pre_solve/rao_parameters_shallow_depth_dc_pre_solve_forced_curative.json"
    When I launch rao at "2021-04-30 22:30"
    Then 3 remedial actions are used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_fr1_fr3" is used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "open_be1_be4" is used after "co1_fr2_fr3_1" at "curative"
    Then the remedial action "close_fr1_fr5" is used after "co1_fr2_fr3_1" at "curative"
