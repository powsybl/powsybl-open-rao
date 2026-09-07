# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 4.2.3: Time-coupled curative remedial actions synchronization

  Presentation of the US
  ----------------------
  This US presents cases where both synchronization parameters are enabled: the activated network action combination AND the
  setpoints of the common range actions must be the same for all timestamps.
  ----------------------

  @fast @rao @marmot @curative-synchronization
  Scenario: 4.2.3.1: Topological actions and range actions are synchronized together
  This is the same case as scenario 4.2.1.1 with PSTs available and different CNECs. One PST usable at both timestamps
  (pst_be) while the other is only available at 11:30 (pst_de).
  The search tree picks one combination of topological actions applied on both (here it is union of the two available
  network actions), and the MIP optimizes the range actions with pst_be forced to the same tap on both timestamps while pst_de
  is under no constraint.
    Given configuration file is "4_time_coupled/4_2_curative_synchronization/RaoParameters_default_curativeSynchronization.json"
    Given time-coupled constraints are in file "epic93/empty-time-coupled-constraints.json" and rao inputs are:
      | Timestamp        | Network                                                                               | CRAC                                                                |
      | 2026-03-25 10:30 | 4_time_coupled/4_2_curative_synchronization/TestCase12Nodes2PSTsWithParallelLines.uct | 4_time_coupled/4_2_curative_synchronization/crac_20260325_1030.json |
      | 2026-03-25 11:30 | 4_time_coupled/4_2_curative_synchronization/TestCase12Nodes2PSTsWithParallelLines.uct | 4_time_coupled/4_2_curative_synchronization/crac_20260325_1130.json |
    When I launch marmot
    # the same combination of topological actions is applied on both timestamps
    Then the remedial action "open_be2fr3" is used at timestamp "2026-03-25 10:30" after "common_contingency" at "curative"
    Then the remedial action "close_de1de3_2" is used at timestamp "2026-03-25 10:30" after "common_contingency" at "curative"
    Then the remedial action "open_be2fr3" is used at timestamp "2026-03-25 11:30" after "common_contingency" at "curative"
    Then the remedial action "close_de1de3_2" is used at timestamp "2026-03-25 11:30" after "common_contingency" at "curative"
    Then the remedial action "open_be1be3" is not used at timestamp "2026-03-25 10:30" after "common_contingency" at "curative"
    # the shared PST takes the same tap on both timestamps
    Then the tap of PstRangeAction "pst_be" at timestamp "2026-03-25 10:30" after "common_contingency" at "curative" should be -6
    Then the tap of PstRangeAction "pst_be" at timestamp "2026-03-25 11:30" after "common_contingency" at "curative" should be -6
    # pst_de only exists at 11:30 and is not synchronized
    Then the remedial action "pst_de" is used at timestamp "2026-03-25 11:30" after "common_contingency" at "curative"
    Then the tap of PstRangeAction "pst_de" at timestamp "2026-03-25 11:30" after "common_contingency" at "curative" should be 11
    # the objective function is global: every timestamp's result carries the global cost
    Then the total cost for timestamp "2026-03-25 10:30" is -96.36
    Then the total cost for timestamp "2026-03-25 11:30" is -96.36
    Then the total cost for all timestamps is -192.71
    Then the time-coupled security status should be "SECURED"