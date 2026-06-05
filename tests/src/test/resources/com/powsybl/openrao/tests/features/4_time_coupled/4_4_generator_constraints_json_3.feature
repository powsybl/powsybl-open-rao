# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 4.4 : Time-coupled generator constraints with MARMOT based on JSON time-coupled constraints - Part 3

  Presentation of the US
  ----------------------

  Simple time-coupled situations based on JSON time-coupled constraints illustrating :
    - taking into account start-up not allowed and power gradient generator constraints
    - re-optimization and application of preventive range actions across timestamps
    - re-optimization and application of curative range actions across timestamps

  ----------------------

  Scenario: 4.4.1 : Global MIP changes curative pst tap to adapt to a startup not allowed generator constraint
  This scenario shows how the global MIP respects the time coupled Start-Up not allowed constraint by adjusting curative
  and preventive remedial actions to accommodate the redispatching.

  It features 2 timestamps (19:30 and 20:30), 1 very cheap redispatching action on which the startup not allowed constraint
  is defined and 1 very expensive redispatching action on which no constraint is defined and 1 curative PST.
  2 CNECs are defined : a preventive one on line 2 and a curative one on line 3.
  - 19:30 : basecase, nothing happens, all generators are off.
  - 20:30 : the loss of the line 1 creates preventive and curative overloads

  At 20:30, the topological optimization run ignores the time-coupled constraint relying heavily on the cheap generator by
  using 1160MW of redispatching and setting the PST to 14. However, the global MIP strictly respects the constraint which
  forces the generator to stay OFF and relies heavily on the other generator pushing it to 580MW. This change of power
  leads to the re-optimization of the curative PST tap from 14 to 2 in order to keep the curative CNEC secure.
  Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
  Given time-coupled constraints are in file "epic93/time-coupled-constraints-startup-not-allowed.json" and rao inputs are:
    | Network                                    | Timestamp        | CRAC                                   |
    | epic93/TestCases_93_4_1/3NodesPST_1930.uct | 2025-11-04 19:30 | epic93/us93_4_1/crac_202511041930.json |
    | epic93/TestCases_93_4_1/3NodesPST_2030.uct | 2025-11-04 20:30 | epic93/us93_4_1/crac_202511042030.json |
  When I launch marmot
  # Timestamp 19:30 : basecase, no overloads, no actions needed
  Then the preventive power of generator "FFR2AA1 _generator" at timestamp "2025-11-04 19:30" is 0.0 MW
  Then the preventive power of generator "FFR3AA1 _generator" at timestamp "2025-11-04 19:30" is 0.0 MW
  Then the remedial action "CheapRDStartupNotAllowed" is not used at timestamp "2025-11-04 19:30" in preventive
  Then the remedial action "CostlyRDNoConstraint" is not used at timestamp "2025-11-04 19:30" in preventive
  # Timestamp 20:30 : MIP has no other choice than to use costly generator
  # start up not allowed is respected
  Then the preventive power of generator "FFR2AA1 _generator" at timestamp "2025-11-04 20:30" is 0.0 MW
  Then the remedial action "CheapRDStartupNotAllowed" is not used at timestamp "2025-11-04 19:30" in preventive
  Then the preventive power of generator "FFR3AA1 _generator" at timestamp "2025-11-04 20:30" is 580.0 MW
  Then the remedial action "CostlyRDNoConstraint" is used at timestamp "2025-11-04 20:30" in preventive
  Then the tap of PstRangeAction "pstCurativeAction" at timestamp "2025-11-04 20:30" after "Contingency L1" at "curative" should be 2
  # 10(activation) + 100(variation) * 580MW = 58010
  Then the total cost for all timestamps is 58010.0

  Scenario: 4.4.2 : Global MIP changes curative pst tap to adapt to power gradient constraint redispatching changes

  There are 3 timestamps separated by 1 hour (19:30, 20:30 and 21:30), 1 preventive costly redispatching
  action and 1 curative PST. The generator has a strict power gradient constraint of maximum 200MW per hour.
  3 CNECs are defined : 1 preventive CNEC on line 3, 2 curative CNECs (one on line 2 and the other on line 3)
  At 19:30, the network is secure, there is no overload. At 20:30 the loss of line 1 occurs which creates an overload
  on line 2 then at 21:30 an overload occurs on line 2 in preventive requiring 500MW of redispatching in order to be solved.

  What happens before the global MIP :
  - 19:30 : The network is secure, there is no overload no action is taken.
  - 20:30 : The loss of line 1 creates an overload on line 2 in curative (500MW flow while the threshold is 460MW),
  PST tap is set to -1 in curative no redispatching is necessary.
  - 21:30 : Severe preventive limit on line 2 requires 500MW of redispatching to be solved.

  After the global MIP :
  The global MIP can see the 200MW/h constraint meaning that jumping from 0 to 500MW at 21:30 is totally forbidden. It
  re-optimizes the range actions in curative and preventive in order to anticipate the constraint :
  - 19:30 : it pre-heats the generator to 196MW
  - 20:30 : it ramps up to 396MW respecting the 200MW/h constraint, this changes the flows and leads to the need of
  reoptimizing the pst tap to +1 in curative to avoid new overloads.
  - 21:30 : it reaches the 500MW needed to solve the overload on line 2 by adding 104MW < 200MW.
  Given configuration file is "epic93/RaoParameters_minCost_megawatt_dc.json"
  Given time-coupled constraints are in file "epic93/time-coupled-power-gradient-constraint.json" and rao inputs are:
    | Network                                    | Timestamp        | CRAC                                   |
    | epic93/TestCases_93_4_2/2NodesPST_1930.uct | 2025-11-04 19:30 | epic93/us93_4_2/crac_202511041930.json |
    | epic93/TestCases_93_4_2/2NodesPST_2030.uct | 2025-11-04 20:30 | epic93/us93_4_2/crac_202511042030.json |
    | epic93/TestCases_93_4_2/2NodesPST_2130.uct | 2025-11-04 21:30 | epic93/us93_4_2/crac_202511042130.json |
  When I launch marmot
  # Timestamp 19:30: pre-heating the generator to 196MW, cost = 10(activation) + 196MW * 10(variation) = 1970
  Then the total cost for timestamp "2025-11-04 19:30" is 1970.0
  Then the preventive power of generator "FFR2AA1 _generator" at timestamp "2025-11-04 19:30" is 196.0 MW
  Then the remedial action "redispatchingAction" is used at timestamp "2025-11-04 19:30" in preventive
  # Timestamp 20:30 : ramping up to 396MW, cost = 10(activation) + 396MW * 10(variation) = 3960
  Then the total cost for timestamp "2025-11-04 20:30" is 3970.0
  Then the preventive power of generator "FFR2AA1 _generator" at timestamp "2025-11-04 20:30" is 396.0 MW
  Then the remedial action "redispatchingAction" is used at timestamp "2025-11-04 20:30" in preventive
  # pst tap is changed from -1 to +1 by global MIP
  Then the tap of PstRangeAction "pstCurativeAction" at timestamp "2025-11-04 20:30" after "Contingency L1" at "curative" should be 1
  # Timestamp 21:30 : 500MW of redispatching  10(activation) + 500MW * 10(variation) = 5010
  Then the total cost for timestamp "2025-11-04 21:30" is 5010.0
  Then the preventive power of generator "FFR2AA1 _generator" at timestamp "2025-11-04 21:30" is 500 MW
  Then the remedial action "redispatchingAction" is used at timestamp "2025-11-04 21:30" in preventive
  # 10 + 10 + 10 + (196+396+500)*10 = 10950
  Then the total cost for all timestamps is 10950.0