# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 4.2.1: Time-coupled curative range actions synchronization

  Presentation of the US
  ----------------------
  This US presents cases where the curative synchronization is enabled on range actions only which forces a range action
  that is shared by several timestamps to take the same setpoint.
  ----------------------

  @fast @rao @marmot @curative-synchronization
  Scenario: 4.2.1.1: A shared PST takes the same tap on both timestamps
  In this scenario, two PSTs exist on the network. One of them is available to use in both timestamps
  (pst_be) while the other one is only usable at 10:30 (pst_de).
  when optimized independently, the two timestamps drive the shared PST pst_be at opposite signs : timestamp 10:30 needs
  it to be positive to relieve the CNEC on BE1-BE3 while timestamp 11:30 needs it to be negative to relieve the CNEC on BE1-BE2.
  The synchronization forces a single tap on both timestamps. Therefore, the two opposite optimums are not both
  reachable at the same time and the MIP settles on a compromise on pst_be and pst_de is optimized freely.
    Given configuration file is "4_time_coupled/4_2_curative_synchronization/RaoParameters_default_curativeRangeActionsSynchronization.json"
    Given time-coupled constraints are in file "epic93/empty-time-coupled-constraints.json" and rao inputs are:
      | Timestamp        | Network                         | CRAC                                                                         |
      | 2026-03-25 10:30 | common/TestCase12Nodes2PSTs.uct | 4_time_coupled/4_2_curative_synchronization/crac_20260325_1030_pst_only.json |
      | 2026-03-25 11:30 | common/TestCase12Nodes2PSTs.uct | 4_time_coupled/4_2_curative_synchronization/crac_20260325_1130_pst_only.json |
    When I launch marmot
    # the shared PST is synchronized
    Then the remedial action "pst_be" is used at timestamp "2026-03-25 10:30" after "common_contingency" at "curative"
    Then the remedial action "pst_be" is used at timestamp "2026-03-25 11:30" after "common_contingency" at "curative"
    Then the tap of PstRangeAction "pst_be" at timestamp "2026-03-25 10:30" after "common_contingency" at "curative" should be -16
    Then the tap of PstRangeAction "pst_be" at timestamp "2026-03-25 11:30" after "common_contingency" at "curative" should be -16
    # pst_de only exists at 10:30 and is not synchronized but still set at tap -16
    Then the remedial action "pst_de" is used at timestamp "2026-03-25 10:30" after "common_contingency" at "curative"
    Then the tap of PstRangeAction "pst_de" at timestamp "2026-03-25 10:30" after "common_contingency" at "curative" should be -16
    Then the total cost for timestamp "2026-03-25 10:30" is 474.32
    Then the total cost for timestamp "2026-03-25 11:30" is -14.14
    # 460.17 ~= 474.32 - 14.14
    Then the total cost for all timestamps is 460.17