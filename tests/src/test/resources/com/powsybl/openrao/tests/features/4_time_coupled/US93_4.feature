# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 93.4: Time-coupled generator constraints with MARMOT based on JSON time-coupled constraints - Part 2

  Presentation of the US
  ----------------------

  Simple 3-timestamp time-coupled situations illustrating the following generator constraints :
  - shutDown allowed
  - startUp allowed

  ----------------------
  Scenario: US 93.4.1: No constraints, specifically : StartUp allowed
  From 04:30 onwards, redispatching is required.
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift.json"
    Given time-coupled constraints are in file "epic93/time-coupled-constraints-startup-prohibited.json" and rao inputs are:
      | Timestamp        | Network                      | CRAC                                 |
      | 2025-11-04 03:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040330.json |
      | 2025-11-04 04:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040430.json |
      | 2025-11-04 05:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040530.json |
    When I launch marmot
    # Timestamp 03:30
    Then the total cost for timestamp "2025-11-04 03:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at state timestamp "2025-11-04 03:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at state timestamp "2025-11-04 03:30" is 0.0 MW
    Then the range action "redispatchingAction" at state timestamp "2025-11-04 03:30" is not used
    # Timestamp 04:30: 10 (activation) + 2500 * 1 (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 04:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at state timestamp "2025-11-04 04:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at state timestamp "2025-11-04 04:30" is 2500.0 MW
    Then the range action "redispatchingAction" at state timestamp "2025-11-04 04:30" is used
    # Timestamp 05:30: 10 (activation) + 2500 * 1 (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 05:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at state timestamp "2025-11-04 05:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at state timestamp "2025-11-04 05:30" is 2500.0 MW
    Then the range action "redispatchingAction" at state timestamp "2025-11-04 05:30" is used

  Scenario: US 93.4.2: Same as 93.4.1, but StartUp not allowed
    From 04:30 onwards, redispatching is required. Nevertheless, BBE1AA1_generator is not available
    since startup is prohibited.
    Results are suprising because MIP avoids starting up generator BBE1AA1_generator, but post-MIP rounding results in generator
    being started up. Here's a detailed explanation:
    ON state is defined for P above Pmin = max(OFF_THRESHOLD, defined Pmin), so here Pmin = OFF_THRESHOLD = 1.
    Here, MIP sets P just under Pmin (1 MW) to avoid setting state to ON and minimize overload. After MIP, result is rounded
    and injections greater than INJECTION_HVDC_ACTIVATION_THRESHOLD = 1 MW are considered as activated.
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift.json"
    Given time-coupled constraints are in file "epic93/time-coupled-constraints-startup-prohibited.json" and rao inputs are:
      | Timestamp        | Network                      | CRAC                                 |
      | 2025-11-04 03:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040330.json |
      | 2025-11-04 04:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040430.json |
      | 2025-11-04 05:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511040530.json |
    When I launch marmot
    # Timestamp 03:30
    Then the total cost for timestamp "2025-11-04 03:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at state timestamp "2025-11-04 03:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at state timestamp "2025-11-04 03:30" is 0.0 MW
    Then the range action "redispatchingAction" at state timestamp "2025-11-04 03:30" is not used
    # For the following timestamps, redispatchingAction is considered activated rao-wise even if MIP-wise generator was not set to ON.
    # Timestamp 04:30: 10 (activation) + 50 * 1 (variation) + 2999 MW * 1000 (overload of 2999 MW, shifted violation penalty of 1000) = 2999060
    Then the total cost for timestamp "2025-11-04 04:30" is 2999060.0
    Then the preventive power of generator "BBE1AA1 _generator" at state timestamp "2025-11-04 04:30" is 1.0 MW
    Then the preventive power of load "FFR1AA1 _load" at state timestamp "2025-11-04 04:30" is 1.0 MW
    Then the range action "redispatchingAction" at state timestamp "2025-11-04 04:30" is used
    # Timestamp 05:30: 10 (activation) + 50 * 1 (variation) + 2999 MW * 1000 (overload of 2999 MW, shifted violation penalty of 1000) = 2999060
    Then the total cost for timestamp "2025-11-04 05:30" is 2999060.0
    Then the preventive power of generator "BBE1AA1 _generator" at state timestamp "2025-11-04 05:30" is 1.0 MW
    Then the preventive power of load "FFR1AA1 _load" at state timestamp "2025-11-04 05:30" is 1.0 MW
    Then the range action "redispatchingAction" at state timestamp "2025-11-04 05:30" is used

  Scenario: US 93.4.3: No constraints, specifically : ShutDown allowed
  Redispatching action is necessary at 19:30 but not afterwards.
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift.json"
    Given time-coupled constraints are in file "epic93/time-coupled-constraints-shutdown-prohibited.json" and rao inputs are:
      | Timestamp        | Network                      | CRAC                                 |
      | 2025-11-04 19:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511041930.json |
      | 2025-11-04 20:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511042030.json |
      | 2025-11-04 21:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511042130.json |
    When I launch marmot
    # Timestamp 19:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 19:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at state timestamp "2025-11-04 19:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at state timestamp "2025-11-04 19:30" is 2500.0 MW
    Then the range action "redispatchingAction" at state timestamp "2025-11-04 19:30" is used
    # Timestamp 20:30
    Then the total cost for timestamp "2025-11-04 20:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at state timestamp "2025-11-04 20:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at state timestamp "2025-11-04 20:30" is 0.0 MW
    Then the range action "redispatchingAction" at state timestamp "2025-11-04 20:30" is not used
    # Timestamp 21:30
    Then the total cost for timestamp "2025-11-04 21:30" is 0.0
    Then the preventive power of generator "BBE1AA1 _generator" at state timestamp "2025-11-04 21:30" is 0.0 MW
    Then the preventive power of load "FFR1AA1 _load" at state timestamp "2025-11-04 21:30" is 0.0 MW
    Then the range action "redispatchingAction" at state timestamp "2025-11-04 21:30" is not used

  Scenario: US 93.4.4: ShutDown not allowed
  Redispatching action is necessary at 19:30 but not afterwards. Since shutdown isn't possible, redispatching action
  is set at its Pmin afterwards.
    Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc_0_shift.json"
    Given time-coupled constraints are in file "epic93/time-coupled-constraints-shutdown-prohibited.json" and rao inputs are:
      | Timestamp        | Network                      | CRAC                                 |
      | 2025-11-04 19:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511041930.json |
      | 2025-11-04 20:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511042030.json |
      | 2025-11-04 21:30 | epic93/6Nodes_Pmax2500.xiidm | epic93/us93_3/crac_202511042130.json |
    When I launch marmot
    # Timestamp 19:30: 10 (activation) + 50 * 2500 MW (variation) + 500 MW * 1000 (overload of 500 MW, shifted violation penalty of 1000) = 625010
    Then the total cost for timestamp "2025-11-04 19:30" is 625010.0
    Then the preventive power of generator "BBE1AA1 _generator" at state timestamp "2025-11-04 19:30" is 2500.0 MW
    Then the preventive power of load "FFR1AA1 _load" at state timestamp "2025-11-04 19:30" is 2500.0 MW
    Then the range action "redispatchingAction" at state timestamp "2025-11-04 19:30" is used
    # Timestamp 20:30: 10 (activation) + 50 * 1 MW (variation) = 60
    Then the total cost for timestamp "2025-11-04 20:30" is 60.0
    Then the preventive power of generator "BBE1AA1 _generator" at state timestamp "2025-11-04 20:30" is 1.0 MW
    Then the preventive power of load "FFR1AA1 _load" at state timestamp "2025-11-04 20:30" is 1.0 MW
    Then the range action "redispatchingAction" at state timestamp "2025-11-04 20:30" is used
    # Timestamp 21:30: 10 (activation) + 50 * 1 MW (variation) = 60
    Then the total cost for timestamp "2025-11-04 21:30" is 60.0
    Then the preventive power of generator "BBE1AA1 _generator" at state timestamp "2025-11-04 21:30" is 1.0 MW
    Then the preventive power of load "FFR1AA1 _load" at state timestamp "2025-11-04 21:30" is 1.0 MW
    Then the range action "redispatchingAction" at state timestamp "2025-11-04 21:30" is used
