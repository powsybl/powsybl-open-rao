# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 4.2: Time-coupled generator constraints with MARMOT based on JSON time-coupled constraints - Part 1

  Presentation of the US
  ----------------------

  All the following scenarios rely on similar network situation. The time-coupled computations simulate a full
  24-timestamp day of one hour each, illustrating the following generator constraints :
  - upward and downward power gradients
  - lead time
  - lag time

  In an ideal situation, no redispatching is required between 0:00 and 3:59, nor between 20:00 and 23:59. However,
  between 4:00 and 19:59, the redispatched power must be maximal to secure an interconnection line. Depending on the
  generator constraints that hold, the actual volumes of redispatching will drift further away from this ideal case.

  ----------------------

  Scenario: 4.2.1: No generator constraints
  No generator constraints hold so the situation corresponds to the ideal case described in the introduction.
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift.json"
    Given time-coupled constraints are in file "epic93/empty-time-coupled-constraints.json" and rao inputs are:
      | Timestamp        | Network             | CRAC                                 |
      | 2025-11-04 00:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040030.json |
      | 2025-11-04 01:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040130.json |
      | 2025-11-04 02:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040230.json |
      | 2025-11-04 03:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040330.json |
      | 2025-11-04 04:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040430.json |
      | 2025-11-04 05:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040530.json |
      | 2025-11-04 06:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040630.json |
      | 2025-11-04 07:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040730.json |
      | 2025-11-04 08:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040830.json |
      | 2025-11-04 09:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040930.json |
      | 2025-11-04 10:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041030.json |
      | 2025-11-04 11:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041130.json |
      | 2025-11-04 12:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041230.json |
      | 2025-11-04 13:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041330.json |
      | 2025-11-04 14:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041430.json |
      | 2025-11-04 15:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041530.json |
      | 2025-11-04 16:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041630.json |
      | 2025-11-04 17:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041730.json |
      | 2025-11-04 18:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041830.json |
      | 2025-11-04 19:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041930.json |
      | 2025-11-04 20:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511042030.json |
      | 2025-11-04 21:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511042130.json |
      | 2025-11-04 22:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511042230.json |
      | 2025-11-04 23:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511042330.json |
    When I launch marmot
    # Timestamp 00:30
    Then the total cost for timestamp "2025-11-04 00:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 00:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 00:30" is 0.0 MW
    # Timestamp 01:30
    Then the total cost for timestamp "2025-11-04 01:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 01:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 01:30" is 0.0 MW
    # Timestamp 02:30
    Then the total cost for timestamp "2025-11-04 02:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 02:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 02:30" is 0.0 MW
    # Timestamp 03:30
    Then the total cost for timestamp "2025-11-04 03:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 03:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 03:30" is 0.0 MW
    # Timestamp 04:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 04:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 04:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 04:30" is 3000.0 MW
    # Timestamp 05:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 05:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 05:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 05:30" is 3000.0 MW
    # Timestamp 06:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 06:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 06:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 06:30" is 3000.0 MW
    # Timestamp 07:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 07:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 07:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 07:30" is 3000.0 MW
    # Timestamp 08:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 08:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 08:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 08:30" is 3000.0 MW
    # Timestamp 09:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 09:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 09:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 09:30" is 3000.0 MW
    # Timestamp 10:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 10:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 10:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 10:30" is 3000.0 MW
    # Timestamp 11:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 11:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 11:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 11:30" is 3000.0 MW
    # Timestamp 12:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 12:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 12:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 12:30" is 3000.0 MW
    # Timestamp 13:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 13:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 13:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 13:30" is 3000.0 MW
    # Timestamp 14:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 14:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 14:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 14:30" is 3000.0 MW
    # Timestamp 15:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 15:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 15:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 15:30" is 3000.0 MW
    # Timestamp 16:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 16:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 16:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 16:30" is 3000.0 MW
    # Timestamp 17:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 17:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 17:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 17:30" is 3000.0 MW
    # Timestamp 18:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 18:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 18:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 18:30" is 3000.0 MW
    # Timestamp 19:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 19:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 19:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 19:30" is 3000.0 MW
    # Timestamp 20:30
    Then the total cost for timestamp "2025-11-04 20:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 20:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 20:30" is 0.0 MW
    # Timestamp 21:30
    Then the total cost for timestamp "2025-11-04 21:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 21:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 21:30" is 0.0 MW
    # Timestamp 22:30
    Then the total cost for timestamp "2025-11-04 22:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 22:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 22:30" is 0.0 MW
    # Timestamp 23:30
    Then the total cost for timestamp "2025-11-04 23:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 23:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 23:30" is 0.0 MW

  Scenario: 4.2.2: Generator constrained by its pMax
  The power of the generator is bounded by a maximal value called "pMax" that makes the generator unable to deliver the
  maximum expected power to fully secure the network. The RAO still applied as much redispatching as it can but gets
  overload penalty costs.
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift.json"
    Given time-coupled constraints are in file "epic93/time-coupled-constraints-without-constraints.json" and rao inputs are:
      | Timestamp        | Network                      | CRAC                                 |
      | 2025-11-04 00:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040030.json |
      | 2025-11-04 01:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040130.json |
      | 2025-11-04 02:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040230.json |
      | 2025-11-04 03:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040330.json |
      | 2025-11-04 04:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040430.json |
      | 2025-11-04 05:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040530.json |
      | 2025-11-04 06:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040630.json |
      | 2025-11-04 07:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040730.json |
      | 2025-11-04 10:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511041030.json |
      | 2025-11-04 09:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040930.json |
      | 2025-11-04 11:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511041130.json |
      | 2025-11-04 08:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040830.json |
      | 2025-11-04 12:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511041230.json |
      | 2025-11-04 13:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511041330.json |
      | 2025-11-04 14:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511041430.json |
      | 2025-11-04 15:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511041530.json |
      | 2025-11-04 16:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511041630.json |
      | 2025-11-04 17:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511041730.json |
      | 2025-11-04 18:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511041830.json |
      | 2025-11-04 19:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511041930.json |
      | 2025-11-04 20:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511042030.json |
      | 2025-11-04 21:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511042130.json |
      | 2025-11-04 22:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511042230.json |
      | 2025-11-04 23:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511042330.json |
    When I launch marmot
    # Timestamp 00:30
    Then the total cost for timestamp "2025-11-04 00:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 00:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 00:30" is 0.0 MW
    # Timestamp 01:30
    Then the total cost for timestamp "2025-11-04 01:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 01:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 01:30" is 0.0 MW
    # Timestamp 02:30
    Then the total cost for timestamp "2025-11-04 02:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 02:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 02:30" is 0.0 MW
    # Timestamp 03:30
    Then the total cost for timestamp "2025-11-04 03:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 03:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 03:30" is 0.0 MW
    # Timestamp 04:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 04:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 04:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 04:30" is 2500.0 MW
    # Timestamp 05:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000)= 625010
    Then the total cost for timestamp "2025-11-04 05:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 05:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 05:30" is 2500.0 MW
    # Timestamp 06:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000)= 625010
    Then the total cost for timestamp "2025-11-04 06:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 06:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 06:30" is 2500.0 MW
    # Timestamp 07:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 07:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 07:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 07:30" is 2500.0 MW
    # Timestamp 08:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 08:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 08:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 08:30" is 2500.0 MW
    # Timestamp 09:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 09:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 09:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 09:30" is 2500.0 MW
    # Timestamp 10:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 10:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 10:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 10:30" is 2500.0 MW
    # Timestamp 11:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 11:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 11:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 11:30" is 2500.0 MW
    # Timestamp 12:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 12:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 12:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 12:30" is 2500.0 MW
    # Timestamp 13:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 13:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 13:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 13:30" is 2500.0 MW
    # Timestamp 14:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 14:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 14:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 14:30" is 2500.0 MW
    # Timestamp 15:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 15:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 15:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 15:30" is 2500.0 MW
    # Timestamp 16:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 16:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 16:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 16:30" is 2500.0 MW
    # Timestamp 17:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 17:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 17:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 17:30" is 2500.0 MW
    # Timestamp 18:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 18:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 18:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 18:30" is 2500.0 MW
    # Timestamp 19:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 19:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 19:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 19:30" is 2500.0 MW
    # Timestamp 20:30
    Then the total cost for timestamp "2025-11-04 20:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 20:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 20:30" is 0.0 MW
    # Timestamp 21:30
    Then the total cost for timestamp "2025-11-04 21:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 21:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 21:30" is 0.0 MW
    # Timestamp 22:30
    Then the total cost for timestamp "2025-11-04 22:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 22:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 22:30" is 0.0 MW
    # Timestamp 23:30
    Then the total cost for timestamp "2025-11-04 23:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 23:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 23:30" is 0.0 MW

  Scenario: 4.2.3: Upward and downward power gradients
  The generator is restricted by upward and downward power gradients. When the generator is up, its power variations are
  more restricted than in the previous scenarios. As the RAO seeks to apply the maximal redispatching value between
  4:00 and 19:59, it has no choice but to start the generator earlier and stop it later so the power gradients are
  respected. This results in no overload penalties but in higher activation and variation costs because the generator is
  up for a longer time, thus delivering more power than previously. In this specific case, the upward power gradient is
  1000 MW/h so the generator must be switched on at 02:00. Similarly, the downward power gradient is -2000 MW/h so the
  generator will be completely shut down at 21:00.
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift.json"
    Given time-coupled constraints are in file "epic93/time-coupled-constraints-with-gradients.json" and rao inputs are:
      | Timestamp        | Network             | CRAC                                 |
      | 2025-11-04 00:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040030.json |
      | 2025-11-04 01:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040130.json |
      | 2025-11-04 02:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040230.json |
      | 2025-11-04 03:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040330.json |
      | 2025-11-04 04:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040430.json |
      | 2025-11-04 05:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040530.json |
      | 2025-11-04 06:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040630.json |
      | 2025-11-04 07:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040730.json |
      | 2025-11-04 08:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040830.json |
      | 2025-11-04 09:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511040930.json |
      | 2025-11-04 10:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041030.json |
      | 2025-11-04 11:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041130.json |
      | 2025-11-04 12:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041230.json |
      | 2025-11-04 13:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041330.json |
      | 2025-11-04 14:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041430.json |
      | 2025-11-04 15:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041530.json |
      | 2025-11-04 16:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041630.json |
      | 2025-11-04 17:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041730.json |
      | 2025-11-04 18:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041830.json |
      | 2025-11-04 19:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511041930.json |
      | 2025-11-04 20:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511042030.json |
      | 2025-11-04 21:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511042130.json |
      | 2025-11-04 22:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511042230.json |
      | 2025-11-04 23:30 | epic93/6Nodes.xiidm | epic93/us93_3/crac_202511042330.json |
    When I launch marmot
    # Timestamp 00:30
    Then the total cost for timestamp "2025-11-04 00:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 00:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 00:30" is 0.0 MW
    # Timestamp 01:30
    Then the total cost for timestamp "2025-11-04 01:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 01:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 01:30" is 0.0 MW
    # Timestamp 02:30: 10 (activation) + 50 * 1000 MW (variation) = 50010 -> because of the gradient
    Then the total cost for timestamp "2025-11-04 02:30" is 50010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 02:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 02:30" is 1000.0 MW
    # Timestamp 03:30: 10 (activation) + 50 * 2000 MW (variation) = 100010 -> because of the gradient
    Then the total cost for timestamp "2025-11-04 03:30" is 100010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 03:30" is 2000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 03:30" is 2000.0 MW
    # Timestamp 04:30: 10 (activation) + 50 * 3000 MW (variation) = 150010 -> finally reached the max
    Then the total cost for timestamp "2025-11-04 04:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 04:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 04:30" is 3000.0 MW
    # Timestamp 05:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 05:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 05:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 05:30" is 3000.0 MW
    # Timestamp 06:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 06:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 06:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 06:30" is 3000.0 MW
    # Timestamp 07:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 07:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 07:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 07:30" is 3000.0 MW
    # Timestamp 08:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 08:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 08:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 08:30" is 3000.0 MW
    # Timestamp 09:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 09:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 09:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 09:30" is 3000.0 MW
    # Timestamp 10:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 10:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 10:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 10:30" is 3000.0 MW
    # Timestamp 11:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 11:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 11:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 11:30" is 3000.0 MW
    # Timestamp 12:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 12:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 12:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 12:30" is 3000.0 MW
    # Timestamp 13:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 13:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 13:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 13:30" is 3000.0 MW
    # Timestamp 14:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 14:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 14:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 14:30" is 3000.0 MW
    # Timestamp 15:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 15:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 15:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 15:30" is 3000.0 MW
    # Timestamp 16:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 16:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 16:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 16:30" is 3000.0 MW
    # Timestamp 17:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 17:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 17:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 17:30" is 3000.0 MW
    # Timestamp 18:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 18:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 18:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 18:30" is 3000.0 MW
    # Timestamp 19:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 19:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 19:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 19:30" is 3000.0 MW
    # Timestamp 20:30: 10 (activation) + 50 * 1000 MW (variation) = 50010 -> because of the downward gradient (-2000MW/h)
    Then the total cost for timestamp "2025-11-04 20:30" is 50010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 20:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 20:30" is 1000.0 MW
    # Timestamp 21:30
    Then the total cost for timestamp "2025-11-04 21:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 21:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 21:30" is 0.0 MW
    # Timestamp 22:30
    Then the total cost for timestamp "2025-11-04 22:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 22:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 22:30" is 0.0 MW
    # Timestamp 23:30
    Then the total cost for timestamp "2025-11-04 23:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 23:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 23:30" is 0.0 MW


    # TODO: to modify: no longer needs go to Pmin after waiting lead time and before going to off
  Scenario: 4.2.4: Long lead and lag times and no gradient
  The generator now takes more time to warm up and cool down so it cannot be operated immediately at its full power just
  after being switched on. The lead and lag times both last longer than one hour - the duration of an optimization
  timestamp - so the generator must be activated sooner than 4:00 and deactivated later than 19:59 in order to produce
  its maximal power in the interval 4:00 to 19:59.
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift.json"
    Given time-coupled constraints are in file "epic93/time-coupled-constraints-with-lead-and-lag-times.json" and rao inputs are:
      | Timestamp        | Network                               | CRAC                                 |
      | 2025-11-04 00:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040030.json |
      | 2025-11-04 01:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040130.json |
      | 2025-11-04 02:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040230.json |
      | 2025-11-04 03:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040330.json |
      | 2025-11-04 04:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040430.json |
      | 2025-11-04 05:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040530.json |
      | 2025-11-04 06:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040630.json |
      | 2025-11-04 07:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040730.json |
      | 2025-11-04 08:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040830.json |
      | 2025-11-04 09:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040930.json |
      | 2025-11-04 10:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041030.json |
      | 2025-11-04 11:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041130.json |
      | 2025-11-04 12:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041230.json |
      | 2025-11-04 13:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041330.json |
      | 2025-11-04 14:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041430.json |
      | 2025-11-04 15:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041530.json |
      | 2025-11-04 16:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041630.json |
      | 2025-11-04 17:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041730.json |
      | 2025-11-04 18:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041830.json |
      | 2025-11-04 19:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041930.json |
      | 2025-11-04 20:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511042030.json |
      | 2025-11-04 21:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511042130.json |
      | 2025-11-04 22:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511042230.json |
      | 2025-11-04 23:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511042330.json |
    When I launch marmot
    # Timestamp 00:30
    Then the total cost for timestamp "2025-11-04 00:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 00:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 00:30" is 0.0 MW
    # Timestamp 01:30
    Then the total cost for timestamp "2025-11-04 01:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 01:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 01:30" is 0.0 MW
    # Timestamp 02:30 (start signal ?)
    Then the total cost for timestamp "2025-11-04 02:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 02:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 02:30" is 0.0 MW
    # Timestamp 03:30
    Then the total cost for timestamp "2025-11-04 03:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 03:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 03:30" is 0.0 MW
    # Timestamp 04:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 04:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 04:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 04:30" is 3000.0 MW
    # Timestamp 05:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 05:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 05:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 05:30" is 3000.0 MW
    # Timestamp 06:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 06:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 06:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 06:30" is 3000.0 MW
    # Timestamp 07:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 07:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 07:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 07:30" is 3000.0 MW
    # Timestamp 08:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 08:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 08:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 08:30" is 3000.0 MW
    # Timestamp 09:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 09:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 09:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 09:30" is 3000.0 MW
    # Timestamp 10:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 10:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 10:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 10:30" is 3000.0 MW
    # Timestamp 11:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 11:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 11:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 11:30" is 3000.0 MW
    # Timestamp 12:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 12:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 12:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 12:30" is 3000.0 MW
    # Timestamp 13:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 13:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 13:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 13:30" is 3000.0 MW
    # Timestamp 14:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 14:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 14:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 14:30" is 3000.0 MW
    # Timestamp 15:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 15:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 15:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 15:30" is 3000.0 MW
    # Timestamp 16:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 16:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 16:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 16:30" is 3000.0 MW
    # Timestamp 17:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 17:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 17:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 17:30" is 3000.0 MW
    # Timestamp 18:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 18:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 18:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 18:30" is 3000.0 MW
    # Timestamp 19:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 19:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 19:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 19:30" is 3000.0 MW
    # Timestamp 20:30:
    Then the total cost for timestamp "2025-11-04 20:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 20:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 20:30" is 0.0 MW
    # Timestamp 21:30
    Then the total cost for timestamp "2025-11-04 21:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 21:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 21:30" is 0.0 MW
    # Timestamp 22:30
    Then the total cost for timestamp "2025-11-04 22:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 22:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 22:30" is 0.0 MW
    # Timestamp 23:30
    Then the total cost for timestamp "2025-11-04 23:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 23:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 23:30" is 0.0 MW

    #TODO: Not that interesting because lead and lag time not that limiting
  Scenario: 4.2.5: Long lead time, short lag time and power gradients
  This situation is a combination of the two previous cases but with lead time lasting less than one hour and lag time
  lasting less than one hour. This means than the generator can be switched on and operated over its pMin in the same
  timestamp. However, the maximal power cannot be reached immediately because once the generator is up, it is still
  restricted by power gradients.
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift.json"
    Given time-coupled constraints are in file "epic93/time-coupled-constraints-with-lead-and-lag-times-and-gradients.json" and rao inputs are:
      | Timestamp        | Network                               | CRAC                                 |
      | 2025-11-04 00:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040030.json |
      | 2025-11-04 01:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040130.json |
      | 2025-11-04 02:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040230.json |
      | 2025-11-04 03:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040330.json |
      | 2025-11-04 04:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040430.json |
      | 2025-11-04 05:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040530.json |
      | 2025-11-04 06:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040630.json |
      | 2025-11-04 07:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040730.json |
      | 2025-11-04 08:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040830.json |
      | 2025-11-04 09:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511040930.json |
      | 2025-11-04 10:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041030.json |
      | 2025-11-04 11:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041130.json |
      | 2025-11-04 12:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041230.json |
      | 2025-11-04 13:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041330.json |
      | 2025-11-04 14:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041430.json |
      | 2025-11-04 15:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041530.json |
      | 2025-11-04 16:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041630.json |
      | 2025-11-04 17:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041730.json |
      | 2025-11-04 18:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041830.json |
      | 2025-11-04 19:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511041930.json |
      | 2025-11-04 20:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511042030.json |
      | 2025-11-04 21:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511042130.json |
      | 2025-11-04 22:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511042230.json |
      | 2025-11-04 23:30 | epic93/6Nodes_Pmin1000_Pmax3000.xiidm | epic93/us93_3/crac_202511042330.json |
    When I launch marmot
    # Timestamp 00:30
    Then the total cost for timestamp "2025-11-04 00:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 00:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 00:30" is 0.0 MW
    # Timestamp 01:30
    Then the total cost for timestamp "2025-11-04 01:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 01:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 01:30" is 0.0 MW
    # Timestamp 02:30
    Then the total cost for timestamp "2025-11-04 02:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 02:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 02:30" is 0.0 MW
    # Timestamp 03:30: 10 (activation) + 50 * 1500 MW (variation) = 50010 -> power increased at 1500 MW to ensure a step
    # up to 3000 MW at next timestamp without violating the power gradient
    Then the total cost for timestamp "2025-11-04 03:30" is 75010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 03:30" is 1500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 03:30" is 1500.0 MW
    # Timestamp 04:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 04:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 04:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 04:30" is 3000.0 MW
    # Timestamp 05:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 05:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 05:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 05:30" is 3000.0 MW
    # Timestamp 06:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 06:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 06:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 06:30" is 3000.0 MW
    # Timestamp 07:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 07:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 07:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 07:30" is 3000.0 MW
    # Timestamp 08:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 08:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 08:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 08:30" is 3000.0 MW
    # Timestamp 09:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 09:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 09:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 09:30" is 3000.0 MW
    # Timestamp 10:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 10:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 10:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 10:30" is 3000.0 MW
    # Timestamp 11:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 11:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 11:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 11:30" is 3000.0 MW
    # Timestamp 12:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 12:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 12:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 12:30" is 3000.0 MW
    # Timestamp 13:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 13:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 13:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 13:30" is 3000.0 MW
    # Timestamp 14:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 14:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 14:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 14:30" is 3000.0 MW
    # Timestamp 15:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 15:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 15:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 15:30" is 3000.0 MW
    # Timestamp 16:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 16:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 16:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 16:30" is 3000.0 MW
    # Timestamp 17:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 17:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 17:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 17:30" is 3000.0 MW
    # Timestamp 18:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 18:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 18:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 18:30" is 3000.0 MW
    # Timestamp 19:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the total cost for timestamp "2025-11-04 19:30" is 150010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 19:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 19:30" is 3000.0 MW
    # Timestamp 20:30: 10 (activation) + 50 * 1000 MW (variation) = 50010 because of gradient
    Then the total cost for timestamp "2025-11-04 20:30" is 50010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 20:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 20:30" is 1000.0 MW
    # Timestamp 21:30
    Then the total cost for timestamp "2025-11-04 21:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 21:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 21:30" is 0.0 MW
    # Timestamp 22:30
    Then the total cost for timestamp "2025-11-04 22:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 22:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 22:30" is 0.0 MW
    # Timestamp 23:30
    Then the total cost for timestamp "2025-11-04 23:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 23:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 23:30" is 0.0 MW

  Scenario: 4.2.6.1: No generator constraints
  From 00:30 to 03:30, 3000 MW of redispatching are necessary.
  From 08:30 to 15:30, 1100 MW of redispatching are necessary.
  From 20:30 to 23:30, 3000 MW or redispatching are necessary.
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift_penalty_100.json"
    Given time-coupled constraints are in file "epic93/empty-time-coupled-constraints.json" and rao inputs are:
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
    Then the total cost for all timestamps is 1640160.0

  Scenario: 4.2.6.2: Generator has a program that is not null but with lead and lag time
  The generator is already expected to be ON and OFF when we need it to be ON or OFF.
  From 00:30 to 03:30, P0 = 1500 MW -> 1500 MW of redispatching are necessary.
  From 08:30 to 15:30, P0 = 1100 MW -> 0 MW of redispatching are necessary.
  From 20:30 to 23:30, P0 = 1500 MW -> 1500 MW of redispatching are necessary.
  In particular, the generator does not go through PMin during its startup and shutdown -> we should not force it as it is not optimal.
  Since there is no gradient constraint, it should be able to reach optimal power after waiting for sufficient time (satisfying the lead and lag time constraints).
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift_penalty_100.json"
    Given time-coupled constraints are in file "epic93/time-coupled-constraints-with-lead-and-lag-times.json" and rao inputs are:
      | Timestamp        | Network                          | CRAC                                   |
      | 2025-11-04 00:30 | epic93/6Nodes_targetP_1500.xiidm | epic93/us93_3_6/crac_202511040030.json |
      | 2025-11-04 01:30 | epic93/6Nodes_targetP_1500.xiidm | epic93/us93_3_6/crac_202511040130.json |
      | 2025-11-04 02:30 | epic93/6Nodes_targetP_1500.xiidm | epic93/us93_3_6/crac_202511040230.json |
      | 2025-11-04 03:30 | epic93/6Nodes_targetP_1500.xiidm | epic93/us93_3_6/crac_202511040330.json |
      | 2025-11-04 04:30 | epic93/6Nodes_Pmin1000.xiidm     | epic93/us93_3_6/crac_202511040430.json |
      | 2025-11-04 06:30 | epic93/6Nodes_Pmin1000.xiidm     | epic93/us93_3_6/crac_202511040630.json |
      | 2025-11-04 07:30 | epic93/6Nodes_Pmin1000.xiidm     | epic93/us93_3_6/crac_202511040730.json |
      | 2025-11-04 05:30 | epic93/6Nodes_Pmin1000.xiidm     | epic93/us93_3_6/crac_202511040530.json |
      | 2025-11-04 08:30 | epic93/6Nodes_targetP_1100.xiidm | epic93/us93_3_6/crac_202511040830.json |
      | 2025-11-04 09:30 | epic93/6Nodes_targetP_1100.xiidm | epic93/us93_3_6/crac_202511040930.json |
      | 2025-11-04 10:30 | epic93/6Nodes_targetP_1100.xiidm | epic93/us93_3_6/crac_202511041030.json |
      | 2025-11-04 11:30 | epic93/6Nodes_targetP_1100.xiidm | epic93/us93_3_6/crac_202511041130.json |
      | 2025-11-04 12:30 | epic93/6Nodes_targetP_1100.xiidm | epic93/us93_3_6/crac_202511041230.json |
      | 2025-11-04 13:30 | epic93/6Nodes_targetP_1100.xiidm | epic93/us93_3_6/crac_202511041330.json |
      | 2025-11-04 14:30 | epic93/6Nodes_targetP_1100.xiidm | epic93/us93_3_6/crac_202511041430.json |
      | 2025-11-04 15:30 | epic93/6Nodes_targetP_1100.xiidm | epic93/us93_3_6/crac_202511041530.json |
      | 2025-11-04 16:30 | epic93/6Nodes_Pmin1000.xiidm     | epic93/us93_3_6/crac_202511041630.json |
      | 2025-11-04 17:30 | epic93/6Nodes_Pmin1000.xiidm     | epic93/us93_3_6/crac_202511041730.json |
      | 2025-11-04 18:30 | epic93/6Nodes_Pmin1000.xiidm     | epic93/us93_3_6/crac_202511041830.json |
      | 2025-11-04 19:30 | epic93/6Nodes_Pmin1000.xiidm     | epic93/us93_3_6/crac_202511041930.json |
      | 2025-11-04 20:30 | epic93/6Nodes_targetP_1500.xiidm | epic93/us93_3_6/crac_202511042030.json |
      | 2025-11-04 21:30 | epic93/6Nodes_targetP_1500.xiidm | epic93/us93_3_6/crac_202511042130.json |
      | 2025-11-04 22:30 | epic93/6Nodes_targetP_1500.xiidm | epic93/us93_3_6/crac_202511042230.json |
      | 2025-11-04 23:30 | epic93/6Nodes_targetP_1500.xiidm  | epic93/us93_3_6/crac_202511042330.json |
    When I launch marmot
    # Timestamp 00:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 00:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 00:30" is 3000.0 MW
    # Timestamp 01:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 01:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 01:30" is 3000.0 MW
    # Timestamp 02:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 02:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 02:30" is 3000.0 MW
    # Timestamp 03:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 03:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 03:30" is 1000.0 MW
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
    # Timestamp 08:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 08:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 08:30" is 1000.0 MW
    # Timestamp 09:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 09:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 09:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 09:30" is 1100.0 MW
    # Timestamp 10:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 10:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 10:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 10:30" is 1100.0 MW
    # Timestamp 11:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 11:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 11:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 11:30" is 1100.0 MW
    # Timestamp 12:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 12:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 12:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 12:30" is 1100.0 MW
    # Timestamp 13:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 13:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 13:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 13:30" is 1100.0 MW
    # Timestamp 14:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the total cost for timestamp "2025-11-04 14:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 14:30" is 1100.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 14:30" is 1100.0 MW
    # Timestamp 15:30: 10 (activation) + 50 * 1100 MW (variation) = 55010
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 15:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 15:30" is 1000.0 MW
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
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 20:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 20:30" is 1000.0 MW
    # Timestamp 21:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 21:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 21:30" is 3000.0 MW
    # Timestamp 22:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 22:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 22:30" is 3000.0 MW
    # Timestamp 23:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 23:30" is 3000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 23:30" is 3000.0 MW

  Scenario: 4.2.6.2: Same as 4.2.6.1, with lead and lag
  From 00:30 to 03:30, 3000 MW of redispatching are necessary.
  From 08:30 to 15:30, 1100 MW of redispatching are necessary.
  From 20:30 to 23:30, 3000 MW or redispatching are necessary.
  At 3:30, generator goes through Pmin early resulting in overload, before stopping for lead + lag = 4 timestamps.
  At 8:30, generator starts up by going through Pmin, resulting in light overload.
  At 15:30, generator starts shut down process (resulting in overloads) by going through Pmin.
  At 20:30, generator can start up again (it had stopped for 4 timestamps), but has to be at Pmin resulting in overloads.
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift_penalty_100.json"
    Given time-coupled constraints are in file "epic93/time-coupled-constraints-with-lead-and-lag-times.json" and rao inputs are:
      | Timestamp        | Network                      | CRAC                                   |
      | 2025-11-04 00:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040030.json |
      | 2025-11-04 01:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040130.json |
      | 2025-11-04 02:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040230.json |
      | 2025-11-04 03:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040330.json |
      | 2025-11-04 04:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040430.json |
      | 2025-11-04 05:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040530.json |
      | 2025-11-04 06:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040630.json |
      | 2025-11-04 07:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040730.json |
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
    # Timestamp 03:30: 10 (activation) + 50 * 1000 MW (variation) + 2000 MW * 100 (overload of 2000 MW, shifted violation penalty of 100) = 250010
    Then the total cost for timestamp "2025-11-04 03:30" is 250010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 03:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 03:30" is 1000.0 MW
    # Timestamp 04:30:
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
    # Timestamp 08:30: 10 (activation) + 50 * 1000 MW (variation) + 100 MW * 100 (overload of 100 MW, shifted violation penalty of 100) = 60010
    Then the total cost for timestamp "2025-11-04 08:30" is 60010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 08:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 08:30" is 1000.0 MW
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
    # Timestamp 15:30: 10 (activation) + 50 * 1100 MW (variation) + 100 MW * 100 (overload of 100 MW, shifted violation penalty of 100) = 60010
    Then the total cost for timestamp "2025-11-04 15:30" is 60010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 15:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 15:30" is 1000.0 MW
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
    # Timestamp 20:30: 10 (activation) + 50 * 1000 MW (variation) + 2000 MW * 100 (overload of 2000 MW, shifted violation penalty of 100) = 250010
    Then the total cost for timestamp "2025-11-04 20:30" is 250010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 20:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 20:30" is 1000.0 MW
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
    Then the total cost for all timestamps is 1850160.0


  Scenario: 4.2.6.3: In line with 4.2.6.1 and 4.2.6.2, with lead and lag and high shifted violation cost
    Generator never shuts down, because of its' high penalty cost on overloads, and because it's heavily constrained by its lead and lag
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift.json"
    Given time-coupled constraints are in file "epic93/time-coupled-constraints-with-lead-and-lag-times.json" and rao inputs are:
      | Timestamp        | Network                      | CRAC                                   |
      | 2025-11-04 00:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040030.json |
      | 2025-11-04 01:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040130.json |
      | 2025-11-04 02:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040230.json |
      | 2025-11-04 03:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040330.json |
      | 2025-11-04 04:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040430.json |
      | 2025-11-04 05:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040530.json |
      | 2025-11-04 06:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040630.json |
      | 2025-11-04 07:30 | epic93/6Nodes_Pmin1000.xiidm | epic93/us93_3_6/crac_202511040730.json |
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
    # Timestamp 16:30: 10 (activation) + 50 * 1000 MW (variation) = 50010
    Then the total cost for timestamp "2025-11-04 16:30" is 50010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 16:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 16:30" is 1000.0 MW
    # Timestamp 17:30: 10 (activation) + 50 * 1000 MW (variation) = 50010
    Then the total cost for timestamp "2025-11-04 17:30" is 50010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 17:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 17:30" is 1000.0 MW
    # Timestamp 18:30: 10 (activation) + 50 * 1000 MW (variation) = 50010
    Then the total cost for timestamp "2025-11-04 18:30" is 50010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 18:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 18:30" is 1000.0 MW
    # Timestamp 19:30: 10 (activation) + 50 * 1000 MW (variation) = 50010
    Then the total cost for timestamp "2025-11-04 19:30" is 50010.0
    Then the preventive power of generator "BBE1AA1 _generator" at timestamp "2025-11-04 19:30" is 1000.0 MW
    Then the preventive power of load "FFR1AA1 _load" at timestamp "2025-11-04 19:30" is 1000.0 MW
    # Timestamp 20:30: 10 (activation) + 50 * 3000 MW (variation) = 150010
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
    Then the total cost for all timestamps is 2040240.0