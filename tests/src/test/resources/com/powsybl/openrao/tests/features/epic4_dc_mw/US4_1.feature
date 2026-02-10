# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 4.1: Import of a CRAC with MW threshold

  @fast @crac
  Scenario: US 4.1.1: Import of a CRAC with MW threshold
  No threshold with a PERCENT_IMAX unit is defined in the CRAC so no iMax was imported.
    Given crac file is "epic4/SL_ep4us1_MW.json"
    Given network file is "common/TestCase12Nodes.uct"
    When I import crac
    Then it should have the following flow CNECs:
      | Name                    | NetworkElementId    | Instant    | Contingency         | Optimized | Monitored | ImaxLeft | ImaxRight | NominalVoltageLeft | NominalVoltageRight |
      | Tie-line FR BE or to ex | BBE2AA1  FFR3AA1  1 | preventive |                     | yes       | no        | NaN      | NaN       | 380.0              | 380.0               |
      | Tie-line FR BE or to ex | BBE2AA1  FFR3AA1  1 | curative   | Contingency FR1 FR3 | yes       | no        | NaN      | NaN       | 380.0              | 380.0               |
      | Tie-line FR BE or to ex | BBE2AA1  FFR3AA1  1 | outage     | Contingency FR1 FR3 | yes       | no        | NaN      | NaN       | 380.0              | 380.0               |
      | Tie-line FR DE          | FFR2AA1  DDE3AA1  1 | preventive |                     | yes       | no        | NaN      | NaN       | 380.0              | 380.0               |
      | Tie-line FR DE          | FFR2AA1  DDE3AA1  1 | curative   | Contingency FR1 FR3 | yes       | no        | NaN      | NaN       | 380.0              | 380.0               |
      | Tie-line FR DE          | FFR2AA1  DDE3AA1  1 | outage     | Contingency FR1 FR3 | yes       | no        | NaN      | NaN       | 380.0              | 380.0               |
    Then the flow cnecs should have the following thresholds:
      | CnecId                                               | Unit     | Min   | Max  | Side |
      | BBE2AA1  FFR3AA1  1 - preventive                     | MEGAWATT | None  | 1500 | ONE  |
      | BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - curative | MEGAWATT | None  | 1500 | ONE  |
      | BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - outage   | MEGAWATT | None  | 1500 | ONE  |
      | FFR2AA1  DDE3AA1  1 - preventive                     | AMPERE   | -1500 | 1500 | TWO  |
      | FFR2AA1  DDE3AA1  1 - Contingency FR1 FR3 - curative | AMPERE   | -1500 | 1500 | TWO  |
      | FFR2AA1  DDE3AA1  1 - Contingency FR1 FR3 - outage   | MEGAWATT | -1500 | 1500 | ONE  |