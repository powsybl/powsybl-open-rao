# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 4.5 : Time-coupled PST tap gradient constraints

  Presentation of the US
  ----------------------
  This US presents cases where a PST is constrained by tap gradients that limit the upward and
  downward number of taps it can move between two consecutive timestamps.
  ----------------------

  @fast @rao @dc @marmot
  Scenario: 4.5.1: No constraint
    Reference scenario, with no time-coupled constraints. Each timestamp moves the PST as much as they need.
    The network is a 2 Node network with one PST available. The timestamps only differ by the CNEC thresholds.
    Given configuration file is "4_time_coupled/4_5_pst_constraints/RaoParameters_minCost_megawatt_dc.json"
    Given time-coupled constraints are in file "epic93/empty-time-coupled-constraints.json" and rao inputs are:
      | Timestamp        | Network                                          | CRAC                                   |
      | 2026-03-25 19:30 | 4_time_coupled/4_5_pst_constraints/2NodesPST.uct | 4_time_coupled/crac-20260325-1930.json |
      | 2026-03-25 20:30 | 4_time_coupled/4_5_pst_constraints/2NodesPST.uct | 4_time_coupled/crac-20260325-2030.json |
      | 2026-03-25 21:30 | 4_time_coupled/4_5_pst_constraints/2NodesPST.uct | 4_time_coupled/crac-20260325-2130.json |
    When I launch marmot
    Then the time-coupled security status should be "SECURED"
    # 19:30 is secured without any action
    Then the remedial action "pst_be_fr" is not used at timestamp "2026-03-25 19:30" in preventive
    Then the preventive tap of PstRangeAction "pst_be_fr" at timestamp "2026-03-25 19:30" should be 0
    # 20:30 overloaded by 33MW needs the pst at tap 1
    Then the remedial action "pst_be_fr" is used at timestamp "2026-03-25 20:30" in preventive
    Then the preventive tap of PstRangeAction "pst_be_fr" at timestamp "2026-03-25 20:30" should be 1
    # 21:30 overload by 133MW and needs the pst at tap 3
    Then the remedial action "pst_be_fr" is used at timestamp "2026-03-25 21:30" in preventive
    Then the preventive tap of PstRangeAction "pst_be_fr" at timestamp "2026-03-25 21:30" should be 3

  @fast @rao @dc @marmot
  Scenario: 4.5.2: Tap gradient over three timestamps
  Same scenario as previous one. The PSTs are constrained with a -/+ 1 tap/h gradient
  constraints. The network is secure at 19:30 and 20:30, then overloaded at 21:30 where the PST must be at 3
  in order to solve it. The MIP starts moving the PST one tap at a time for it to reach 3 by 21:30.
    Given configuration file is "4_time_coupled/4_5_pst_constraints/RaoParameters_minCost_megawatt_dc.json"
    Given time-coupled constraints are in file "epic93/time-coupled-pst-constraints-with-one-tap-per-hour.json" and rao inputs are:
      | Timestamp        | Network                                          | CRAC                                   |
      | 2026-03-25 19:30 | 4_time_coupled/4_5_pst_constraints/2NodesPST.uct | 4_time_coupled/crac-20260325-1930.json |
      | 2026-03-25 20:30 | 4_time_coupled/4_5_pst_constraints/2NodesPST.uct | 4_time_coupled/crac-20260325-2030.json |
      | 2026-03-25 21:30 | 4_time_coupled/4_5_pst_constraints/2NodesPST.uct | 4_time_coupled/crac-20260325-2130.json |
    When I launch marmot
    Then the time-coupled security status should be "SECURED"
    # 19:30 is secure but the PST is prepared at tap 1, it will solve 21:30
    Then the remedial action "pst_be_fr" is used at timestamp "2026-03-25 19:30" in preventive
    Then the preventive tap of PstRangeAction "pst_be_fr" at timestamp "2026-03-25 19:30" should be 1
    # one more upward tap variation at 20:30 to reach 3 at 21:30
    Then the remedial action "pst_be_fr" is used at timestamp "2026-03-25 20:30" in preventive
    Then the preventive tap of PstRangeAction "pst_be_fr" at timestamp "2026-03-25 20:30" should be 2
    # 21:30, the PST tap reaches 3 in order to solve the overload
    Then the remedial action "pst_be_fr" is used at timestamp "2026-03-25 21:30" in preventive
    Then the preventive tap of PstRangeAction "pst_be_fr" at timestamp "2026-03-25 21:30" should be 3

  @fast @rao @dc @marmot
  Scenario: 4.5.3: Tap gradient with 30 minute gaps between timestamps
  Same case as before with a constraint of +/-2 tap/h with the timestamps at 30 minutes apart instead of 1 hour.
    Given configuration file is "4_time_coupled/4_5_pst_constraints/RaoParameters_minCost_megawatt_dc.json"
    Given time-coupled constraints are in file "epic93/time-coupled-pst-constraints-with-two-taps-per-hour.json" and rao inputs are:
      | Timestamp        | Network                                          | CRAC                                         |
      | 2026-03-25 19:30 | 4_time_coupled/4_5_pst_constraints/2NodesPST.uct | 4_time_coupled/4_5_3/crac-20260325-1930.json |
      | 2026-03-25 20:00 | 4_time_coupled/4_5_pst_constraints/2NodesPST.uct | 4_time_coupled/4_5_3/crac-20260325-2000.json |
    When I launch marmot
    Then the time-coupled security status should be "SECURED"
    Then the remedial action "pst_be_fr" is used at timestamp "2026-03-25 19:30" in preventive
    Then the preventive tap of PstRangeAction "pst_be_fr" at timestamp "2026-03-25 19:30" should be 2
    Then the remedial action "pst_be_fr" is used at timestamp "2026-03-25 20:00" in preventive
    Then the preventive tap of PstRangeAction "pst_be_fr" at timestamp "2026-03-25 20:00" should be 3
