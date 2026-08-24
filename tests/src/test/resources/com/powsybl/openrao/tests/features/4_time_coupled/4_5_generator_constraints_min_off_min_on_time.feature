# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 4.5: Test to illustrate min-off and min-on time constraint

In the following test, ideally we would need:
From 00:30 to 03:30, 3000 MW of redispatching
From 08:30 to 15:30, 1100 MW of redispatching
From 20:30 to 23:30, 3000 MW of redispatching

  Scenario: 4.5.1: Min-off time is the only constraint
  Min-off time = 6h
  The generator starts at 10h30 instead of 8h30 -> 2200MW overload
  The generator stops at 13h30 instead of 15h30 -> 2200MW overload
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift_penalty_100.json"
    Given time-coupled constraints are in file "epic93/time-coupled-constraints-4_5_1.json" and rao inputs are:
      | Timestamp        | Network                      | CRAC                                   |
      | 2025-11-04 00:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040030.json |
      | 2025-11-04 01:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040130.json |
      | 2025-11-04 02:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040230.json |
      | 2025-11-04 03:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040330.json |
      | 2025-11-04 04:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040430.json |
      | 2025-11-04 06:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040630.json |
      | 2025-11-04 07:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040730.json |
      | 2025-11-04 05:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040530.json |
      | 2025-11-04 08:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040830.json |
      | 2025-11-04 09:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040930.json |
      | 2025-11-04 10:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041030.json |
      | 2025-11-04 11:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041130.json |
      | 2025-11-04 12:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041230.json |
      | 2025-11-04 13:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041330.json |
      | 2025-11-04 14:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041430.json |
      | 2025-11-04 15:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041530.json |
      | 2025-11-04 16:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041630.json |
      | 2025-11-04 17:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041730.json |
      | 2025-11-04 18:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041830.json |
      | 2025-11-04 19:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041930.json |
      | 2025-11-04 20:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042030.json |
      | 2025-11-04 21:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042130.json |
      | 2025-11-04 22:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042230.json |
      | 2025-11-04 23:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042330.json |
    When I launch marmot
    # Timestamp 00:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 00:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 00:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 00:30" is 3000.0 MW
    # Timestamp 01:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 01:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 01:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 01:30" is 3000.0 MW
    # Timestamp 02:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 02:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 02:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 02:30" is 3000.0 MW
    # Timestamp 03:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 03:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 03:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 03:30" is 3000.0 MW
    # Timestamp 04:30
    Then the total cost for timestamp "2025-11-04 04:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 04:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 04:30" is 0.0 MW
    # Timestamp 05:30
    Then the total cost for timestamp "2025-11-04 05:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 05:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 05:30" is 0.0 MW
    # Timestamp 06:30
    Then the total cost for timestamp "2025-11-04 06:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 06:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 06:30" is 0.0 MW
    # Timestamp 07:30
    Then the total cost for timestamp "2025-11-04 07:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 07:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 07:30" is 0.0 MW
    # Timestamp 08:30: 1100 MW * 100 (overload of 1100 MW, shifted violation penalty of 100) = 110000
    Then the total cost for timestamp "2025-11-04 08:30" is 110000.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 08:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 08:30" is 0.0 MW
    # Timestamp 09:30: 1100 MW * 100 (overload of 1100 MW, shifted violation penalty of 100) = 110000
    Then the total cost for timestamp "2025-11-04 09:30" is 110000.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 09:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 09:30" is 0.0 MW
    # Timestamp 10:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 10:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 10:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 10:30" is 1100.0 MW
    # Timestamp 11:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 11:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 11:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 11:30" is 1100.0 MW
    # Timestamp 12:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 12:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 12:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 12:30" is 1100.0 MW
    # Timestamp 13:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 13:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 13:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 13:30" is 1100.0 MW
    # Timestamp 14:30: 1100 MW * 100 (overload of 1100 MW, shifted violation penalty of 100) = 110000
    Then the total cost for timestamp "2025-11-04 14:30" is 110000.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 14:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 14:30" is 0.0 MW
    # Timestamp 15:30: 1100 MW * 100 (overload of 1100 MW, shifted violation penalty of 100) = 110000
    Then the total cost for timestamp "2025-11-04 15:30" is 110000.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 15:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 15:30" is 0.0 MW
    # Timestamp 16:30:
    Then the total cost for timestamp "2025-11-04 16:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 16:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 16:30" is 0.0 MW
    # Timestamp 17:30:
    Then the total cost for timestamp "2025-11-04 17:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 17:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 17:30" is 0.0 MW
    # Timestamp 18:30:
    Then the total cost for timestamp "2025-11-04 18:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 18:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 18:30" is 0.0 MW
    # Timestamp 19:30:
    Then the total cost for timestamp "2025-11-04 19:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 19:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 19:30" is 0.0 MW
    # Timestamp 20:30: 10 (activation) +  50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 20:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 20:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 20:30" is 3000.0 MW
    # Timestamp 21:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 21:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 21:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 21:30" is 3000.0 MW
    # Timestamp 22:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 22:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 22:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 22:30" is 3000.0 MW
    # Timestamp 23:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 23:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 23:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 23:30" is 3000.0 MW
    Then the total cost for all timestamps is 1860120.0

  Scenario: 4.5.2: Min-off time + lag time + lead time
    Min-off time = 1h, lagTime = 2.5 and leadTime = 2.25.
    Between a stop and a restart of the generator, we need to wait at least 5.75h -> 6h.
    We end up in the same situation as 4.5.1. The generator stays off for more than minOffTime because of the lead and lag time constraints.
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift_penalty_100.json"
    Given time-coupled constraints are in file "epic93/time-coupled-constraints-4_5_2.json" and rao inputs are:
      | Timestamp        | Network                      | CRAC                                   |
      | 2025-11-04 00:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040030.json |
      | 2025-11-04 01:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040130.json |
      | 2025-11-04 02:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040230.json |
      | 2025-11-04 03:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040330.json |
      | 2025-11-04 04:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040430.json |
      | 2025-11-04 06:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040630.json |
      | 2025-11-04 07:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040730.json |
      | 2025-11-04 05:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040530.json |
      | 2025-11-04 08:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040830.json |
      | 2025-11-04 09:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040930.json |
      | 2025-11-04 10:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041030.json |
      | 2025-11-04 11:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041130.json |
      | 2025-11-04 12:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041230.json |
      | 2025-11-04 13:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041330.json |
      | 2025-11-04 14:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041430.json |
      | 2025-11-04 15:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041530.json |
      | 2025-11-04 16:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041630.json |
      | 2025-11-04 17:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041730.json |
      | 2025-11-04 18:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041830.json |
      | 2025-11-04 19:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041930.json |
      | 2025-11-04 20:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042030.json |
      | 2025-11-04 21:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042130.json |
      | 2025-11-04 22:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042230.json |
      | 2025-11-04 23:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042330.json |
    When I launch marmot
    # Timestamp 00:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 00:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 00:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 00:30" is 3000.0 MW
    # Timestamp 01:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 01:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 01:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 01:30" is 3000.0 MW
    # Timestamp 02:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 02:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 02:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 02:30" is 3000.0 MW
    # Timestamp 03:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 03:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 03:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 03:30" is 3000.0 MW
    # Timestamp 04:30
    Then the total cost for timestamp "2025-11-04 04:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 04:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 04:30" is 0.0 MW
    # Timestamp 05:30
    Then the total cost for timestamp "2025-11-04 05:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 05:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 05:30" is 0.0 MW
    # Timestamp 06:30
    Then the total cost for timestamp "2025-11-04 06:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 06:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 06:30" is 0.0 MW
    # Timestamp 07:30
    Then the total cost for timestamp "2025-11-04 07:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 07:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 07:30" is 0.0 MW
    # Timestamp 08:30: 1100 MW * 100 (overload of 1100 MW, shifted violation penalty of 100) = 110000
    Then the total cost for timestamp "2025-11-04 08:30" is 110000.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 08:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 08:30" is 0.0 MW
    # Timestamp 09:30: 1100 MW * 100 (overload of 1100 MW, shifted violation penalty of 100) = 110000
    Then the total cost for timestamp "2025-11-04 09:30" is 110000.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 09:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 09:30" is 0.0 MW
    # Timestamp 10:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 10:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 10:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 10:30" is 1100.0 MW
    # Timestamp 11:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 11:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 11:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 11:30" is 1100.0 MW
    # Timestamp 12:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 12:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 12:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 12:30" is 1100.0 MW
    # Timestamp 13:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 13:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 13:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 13:30" is 1100.0 MW
    # Timestamp 14:30: 1100 MW * 100 (overload of 1100 MW, shifted violation penalty of 100) = 110000
    Then the total cost for timestamp "2025-11-04 14:30" is 110000.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 14:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 14:30" is 0.0 MW
    # Timestamp 15:30: 1100 MW * 100 (overload of 1100 MW, shifted violation penalty of 100) = 110000
    Then the total cost for timestamp "2025-11-04 15:30" is 110000.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 15:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 15:30" is 0.0 MW
    # Timestamp 16:30:
    Then the total cost for timestamp "2025-11-04 16:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 16:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 16:30" is 0.0 MW
    # Timestamp 17:30:
    Then the total cost for timestamp "2025-11-04 17:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 17:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 17:30" is 0.0 MW
    # Timestamp 18:30:
    Then the total cost for timestamp "2025-11-04 18:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 18:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 18:30" is 0.0 MW
    # Timestamp 19:30:
    Then the total cost for timestamp "2025-11-04 19:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 19:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 19:30" is 0.0 MW
    # Timestamp 20:30: 10 (activation) +  50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 20:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 20:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 20:30" is 3000.0 MW
    # Timestamp 21:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 21:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 21:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 21:30" is 3000.0 MW
    # Timestamp 22:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 22:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 22:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 22:30" is 3000.0 MW
    # Timestamp 23:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 23:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 23:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 23:30" is 3000.0 MW
    Then the total cost for all timestamps is 1860120.0

  Scenario: 4.5.3: Min-on time
    Min-on Time = 9h
    The generator must stay on for 9h after being turned on.
    -> the generator need to stay on one timestamp longer than necessary at Pmin. The generator stays on between 7h30 and 15h30 instead of 8h30 and 15h30.
    Since we don't know when the generator was first started up, the constraint does not apply for the first timestamps
    -> we don't need to force the generator to stay on from 00h30 to 9h30
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift_penalty_100.json"
    Given time-coupled constraints are in file "epic93/time-coupled-constraints-4_5_3.json" and rao inputs are:
      | Timestamp        | Network                      | CRAC                                   |
      | 2025-11-04 00:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040030.json |
      | 2025-11-04 01:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040130.json |
      | 2025-11-04 02:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040230.json |
      | 2025-11-04 03:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040330.json |
      | 2025-11-04 04:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040430.json |
      | 2025-11-04 06:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040630.json |
      | 2025-11-04 07:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040730.json |
      | 2025-11-04 05:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040530.json |
      | 2025-11-04 08:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040830.json |
      | 2025-11-04 09:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040930.json |
      | 2025-11-04 10:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041030.json |
      | 2025-11-04 11:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041130.json |
      | 2025-11-04 12:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041230.json |
      | 2025-11-04 13:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041330.json |
      | 2025-11-04 14:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041430.json |
      | 2025-11-04 15:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041530.json |
      | 2025-11-04 16:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041630.json |
      | 2025-11-04 17:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041730.json |
      | 2025-11-04 18:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041830.json |
      | 2025-11-04 19:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041930.json |
      | 2025-11-04 20:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042030.json |
      | 2025-11-04 21:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042130.json |
      | 2025-11-04 22:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042230.json |
      | 2025-11-04 23:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042330.json |
    When I launch marmot
    # Timestamp 00:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 00:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 00:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 00:30" is 3000.0 MW
    # Timestamp 01:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 01:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 01:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 01:30" is 3000.0 MW
    # Timestamp 02:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 02:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 02:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 02:30" is 3000.0 MW
    # Timestamp 03:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 03:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 03:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 03:30" is 3000.0 MW
    # Timestamp 04:30
    Then the total cost for timestamp "2025-11-04 04:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 04:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 04:30" is 0.0 MW
    # Timestamp 05:30
    Then the total cost for timestamp "2025-11-04 05:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 05:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 05:30" is 0.0 MW
    # Timestamp 06:30
    Then the total cost for timestamp "2025-11-04 06:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 06:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 06:30" is 0.0 MW
    # Timestamp 07:30: 10 (activation) + 50 * 1000 MW (variation) = 50010
    Then the total cost for timestamp "2025-11-04 07:30" is 50010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 07:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 07:30" is 1000.0 MW
    # Timestamp 08:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 08:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 08:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 08:30" is 1100.0 MW
    # Timestamp 09:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 09:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 09:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 09:30" is 1100.0 MW
    # Timestamp 10:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 10:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 10:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 10:30" is 1100.0 MW
    # Timestamp 11:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 11:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 11:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 11:30" is 1100.0 MW
    # Timestamp 12:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 12:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 12:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 12:30" is 1100.0 MW
    # Timestamp 13:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 13:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 13:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 13:30" is 1100.0 MW
    # Timestamp 14:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 14:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 14:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 14:30" is 1100.0 MW
    # Timestamp 15:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 15:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 15:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 15:30" is 1100.0 MW
    # Timestamp 16:30:
    Then the total cost for timestamp "2025-11-04 16:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 16:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 16:30" is 0.0 MW
    # Timestamp 17:30:
    Then the total cost for timestamp "2025-11-04 17:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 17:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 17:30" is 0.0 MW
    # Timestamp 18:30:
    Then the total cost for timestamp "2025-11-04 18:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 18:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 18:30" is 0.0 MW
    # Timestamp 19:30:
    Then the total cost for timestamp "2025-11-04 19:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 19:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 19:30" is 0.0 MW
    # Timestamp 20:30: 10 (activation) +  50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 20:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 20:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 20:30" is 3000.0 MW
    # Timestamp 21:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 21:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 21:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 21:30" is 3000.0 MW
    # Timestamp 22:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 22:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 22:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 22:30" is 3000.0 MW
    # Timestamp 23:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 23:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 23:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 23:30" is 3000.0 MW
    Then the total cost for all timestamps is 1690170.0

  Scenario: 4.5.4: Min-on time + Min-off time + leadTime + lagtime
    leadTime + lagTime + minOffTime = 6.2h -> the generator needs to stay off for at least 7h
    min-on time = 6h -> once started up, the generator need to stay on for at least 6h

    From 00:30 to 03:30, 3000 MW of redispatching

    solution 1:
    -> either it stays off from 4:30 to 10:30 -> from 8h30 to 10h30 we have an overload which would cost 110000 * 3 = 330000
    -> then to respect the min-on time constraint it would need to stay on until at least 16:30
    -> we won't have enough time to shut down the generator and start it up again so it would need to stay on until the end of the day
    -> this is too expensive !

    solution 2:
    -> it stays on all the way to 15:30 -> which would cost : 10 (activation) * 7 + 50 * Pmin * 4 + 50 * 1100 MW * 3 = 365070
    -> this way the min-on time is also respected
    -> however to respect the shutting down constraint -> we need to stop the generator at 13:30 -> 3 * 680070 = 330000
    -> from 20:30 to 23:30, 3000 MW of redispatching

    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift_penalty_100.json"
    Given time-coupled constraints are in file "epic93/time-coupled-constraints-4_5_4.json" and rao inputs are:
      | Timestamp        | Network                      | CRAC                                   |
      | 2025-11-04 00:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040030.json |
      | 2025-11-04 01:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040130.json |
      | 2025-11-04 02:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040230.json |
      | 2025-11-04 03:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040330.json |
      | 2025-11-04 04:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040430.json |
      | 2025-11-04 06:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040630.json |
      | 2025-11-04 07:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040730.json |
      | 2025-11-04 05:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040530.json |
      | 2025-11-04 08:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040830.json |
      | 2025-11-04 09:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040930.json |
      | 2025-11-04 10:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041030.json |
      | 2025-11-04 11:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041130.json |
      | 2025-11-04 12:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041230.json |
      | 2025-11-04 13:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041330.json |
      | 2025-11-04 14:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041430.json |
      | 2025-11-04 15:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041530.json |
      | 2025-11-04 16:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041630.json |
      | 2025-11-04 17:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041730.json |
      | 2025-11-04 18:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041830.json |
      | 2025-11-04 19:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511041930.json |
      | 2025-11-04 20:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042030.json |
      | 2025-11-04 21:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042130.json |
      | 2025-11-04 22:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042230.json |
      | 2025-11-04 23:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511042330.json |
    When I launch marmot
    # Timestamp 00:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 00:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 00:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 00:30" is 3000.0 MW
    # Timestamp 01:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 01:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 01:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 01:30" is 3000.0 MW
    # Timestamp 02:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 02:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 02:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 02:30" is 3000.0 MW
    # Timestamp 03:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 03:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 03:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 03:30" is 3000.0 MW
    # Timestamp 04:30: 10 (activation) + 50 * 1000 MW (variation) = 50010
    Then the total cost for timestamp "2025-11-04 04:30" is 50010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 04:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 04:30" is 1000.0 MW
    # Timestamp 05:30: 10 (activation) + 50 * 1000 MW (variation) = 50010
    Then the total cost for timestamp "2025-11-04 05:30" is 50010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 05:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 05:30" is 1000.0 MW
    # Timestamp 06:30: 10 (activation) + 50 * 1000 MW (variation) = 50010
    Then the total cost for timestamp "2025-11-04 06:30" is 50010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 06:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 06:30" is 1000.0 MW
    # Timestamp 07:30: 10 (activation) + 50 * 1000 MW (variation) = 50010
    Then the total cost for timestamp "2025-11-04 07:30" is 50010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 07:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 07:30" is 1000.0 MW
    # Timestamp 08:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 08:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 08:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 08:30" is 1100.0 MW
    # Timestamp 09:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 09:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 09:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 09:30" is 1100.0 MW
    # Timestamp 10:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 10:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 10:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 10:30" is 1100.0 MW
    # Timestamp 11:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 11:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 11:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 11:30" is 1100.0 MW
    # Timestamp 12:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 12:30" is 55010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 12:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 12:30" is 1100.0 MW
    # Timestamp 13:30: 1100 MW * 100 (overload of 1100 MW, shifted violation penalty of 100) = 110000
    Then the total cost for timestamp "2025-11-04 13:30" is 110000.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 13:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 13:30" is 0.0 MW
    # Timestamp 14:30: 1100 MW * 100 (overload of 1100 MW, shifted violation penalty of 100) = 110000
    Then the total cost for timestamp "2025-11-04 14:30" is 110000.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 14:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 14:30" is 0.0 MW
    # Timestamp 15:30: 1100 MW * 100 (overload of 1100 MW, shifted violation penalty of 100) = 110000
    Then the total cost for timestamp "2025-11-04 15:30" is 110000.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 15:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 15:30" is 0.0 MW
    # Timestamp 16:30:
    Then the total cost for timestamp "2025-11-04 16:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 16:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 16:30" is 0.0 MW
    # Timestamp 17:30:
    Then the total cost for timestamp "2025-11-04 17:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 17:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 17:30" is 0.0 MW
    # Timestamp 18:30:
    Then the total cost for timestamp "2025-11-04 18:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 18:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 18:30" is 0.0 MW
    # Timestamp 19:30:
    Then the total cost for timestamp "2025-11-04 19:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 19:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 19:30" is 0.0 MW
    # Timestamp 20:30: 10 (activation) +  50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 20:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 20:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 20:30" is 3000.0 MW
    # Timestamp 21:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 21:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 21:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 21:30" is 3000.0 MW
    # Timestamp 22:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 22:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 22:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 22:30" is 3000.0 MW
    # Timestamp 23:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 23:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 23:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 23:30" is 3000.0 MW
    Then the total cost for all timestamps is 2005169.0
