# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 4.2.2: Time-coupled curative network actions synchronization

  Presentation of the US
  ----------------------
  This US presents cases where the curative synchronization is enabled on network actions only and one single combination of network actions
  is activated on every timestamp at once.
  ----------------------

  @fast @rao @marmot @curative-synchronization
  Scenario: 4.2.2.1: The union of each timestamps' preferred actions is activated
  In this scenario, 2 PSTs (pst_de and pst_be) and 3 curative topological actions (close_de1de3_2, open_be2fr3, open_be1be3)
  exist and the only occurring contingency is the loss of the line FFR1AA1-FFR3AA1 on both timestamps.
  Both timestamps share the same two curative topological actions and the same two curative CNECs, only the CNEC
  thresholds differ.
  when optimized independently, timestamp 10:30 is secured by close_de1de3_2 alone and timestamp 11:30 by
  open_be2fr3 alone. In the time-coupled search tree, both actions are applied simultaneously on both timestamps.
    Given configuration file is "4_time_coupled/4_2_curative_synchronization/RaoParameters_default_curativeTopologicalActionsSynchronization.json"
    Given time-coupled constraints are in file "epic93/empty-time-coupled-constraints.json" and rao inputs are:
      | Timestamp        | Network                                                                               | CRAC                                                                                       |
      | 2026-03-25 10:30 | 4_time_coupled/4_2_curative_synchronization/TestCase12Nodes2PSTsWithParallelLines.uct | 4_time_coupled/4_2_curative_synchronization/crac_20260325_1030_topological_only_union.json |
      | 2026-03-25 11:30 | 4_time_coupled/4_2_curative_synchronization/TestCase12Nodes2PSTsWithParallelLines.uct | 4_time_coupled/4_2_curative_synchronization/crac_20260325_1130_topological_only_union.json |
    When I launch marmot
    # the same combination is applied on both timestamps
    Then the remedial action "close_de1de3_2" is used at timestamp "2026-03-25 10:30" after "common_contingency" at "curative"
    Then the remedial action "open_be2fr3" is used at timestamp "2026-03-25 10:30" after "common_contingency" at "curative"
    Then the remedial action "close_de1de3_2" is used at timestamp "2026-03-25 11:30" after "common_contingency" at "curative"
    Then the remedial action "open_be2fr3" is used at timestamp "2026-03-25 11:30" after "common_contingency" at "curative"
    Then the total cost for timestamp "2026-03-25 10:30" is -100
    Then the total cost for timestamp "2026-03-25 11:30" is -100
    Then the total cost for all timestamps is -200
    Then the time-coupled security status should be "SECURED"

  @fast @rao @marmot @curative-synchronization
  Scenario: 4.2.2.2: The action securing both timestamps is preferred over each timestamp's own choice
  Same actions and CNECs as the previous scenario, only timestamp 10:30's DE1-DE3 threshold is relaxed to 550 MW.
  when optimized independently, timestamp 10:30 still prefers close_de1de3_2 and timestamp 11:30 needs open_be2fr3.
  Since close_de1de3_2 cannot secure timestamp 11:30, the time-coupled search tree applies open_be2fr3 alone on both
  timestamps, even though it is not timestamp 10:30's own preference.
    Given configuration file is "4_time_coupled/4_2_curative_synchronization/RaoParameters_default_curativeTopologicalActionsSynchronization.json"
    Given time-coupled constraints are in file "epic93/empty-time-coupled-constraints.json" and rao inputs are:
      | Timestamp        | Network                                                                               | CRAC                                                                                 |
      | 2026-03-25 10:30 | 4_time_coupled/4_2_curative_synchronization/TestCase12Nodes2PSTsWithParallelLines.uct | 4_time_coupled/4_2_curative_synchronization/crac_20260325_1030_topological_only.json |
      | 2026-03-25 11:30 | 4_time_coupled/4_2_curative_synchronization/TestCase12Nodes2PSTsWithParallelLines.uct | 4_time_coupled/4_2_curative_synchronization/crac_20260325_1130_topological_only.json |
    When I launch marmot
    # only "open_be2fr2" is activated on both timestamps
    Then the remedial action "open_be2fr3" is used at timestamp "2026-03-25 10:30" after "common_contingency" at "curative"
    Then the remedial action "open_be2fr3" is used at timestamp "2026-03-25 11:30" after "common_contingency" at "curative"
    Then the remedial action "close_de1de3_2" is not used at timestamp "2026-03-25 10:30" after "common_contingency" at "curative"
    Then the remedial action "close_de1de3_2" is not used at timestamp "2026-03-25 11:30" after "common_contingency" at "curative"
    Then the total cost for timestamp "2026-03-25 10:30" is -50
    Then the total cost for timestamp "2026-03-25 11:30" is -100
    Then the total cost for all timestamps is -150
    Then the time-coupled security status should be "SECURED"