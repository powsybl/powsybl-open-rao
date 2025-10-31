# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 93.3: Intertemporal generator constraints

  Presentation of the US
  ----------------------

  All the following scenarios rely on the same network situation. The intertemporal computations simulate a full
  24-timestamp day of one hour each.

  In an ideal situation, no redispatching is required between 0:00 and 3:59, nor between 20:00 and 23:59. However,
  between 4:00 and 19:59, the redispatched power must be maximal. Depending on the generator constraints that hold, the
  actual volumes of redispatching will drift further away from this ideal case.

  ----------------------

  Scenario: US 93.3.1: No generator constraints
  No generator constraints hold so the situation corresponds to the ideal case described in the introduction.

  Scenario: US 93.3.2: Generator constrained by its pMax
  The power of the generator is bounded by a maximal value called "pMax" that makes the generator unable to deliver the
  maximum expected power to fully secure the network. The RAO still applied as much redispatching as it can but gets
  overload penalty costs.

  Scenario: US 93.3.3: Upward and downward power gradients
  The generator is restricted by upward and downward power gradients. When the generator is up, its power variations are
  more restricted than in the previous scenarios. As the RAO seeks to apply the maximal redispatching value between
  4:00 and 19:59, it has no choice but to start the generator earlier and stop it later so the power gradients are
  respected. This results in no overload penalties but in higher activation and variation costs because the generator is
  up for a longer time, thus delivering more power than previously.

  Scenario: US 93.3.4: Long lead and lag times
  The generator now takes more time to warm up and cool down so it cannot be operated immediately at its full power just
  after being switched on. The lead and lag times both last longer than one hour - the duration of an optimization
  timestamp - so the generator must be activated sooner than 4:00 and deactivated later than 19:59 in order to produce
  its maximal power in the interval 4:00 to 19:59.

  Scenario: US 93.3.5: Long lead time, short lag time and power gradients
  This situation is a combination of the two previous cases but with lead and lag time lasting less than one hour. This
  means than the generator can be switched on and operated over its pMin in the same timestamp. However, the maximal
  power cannot be reached immediately because once the generator is up, it is still restricted by power gradients.
