# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 3.1: Import of a cnec monitored in a single direction

  @fast @crac @mock
  Scenario: US 3.1.1
    Given crac file is "epic3/SL_ep3us1_all_dir.json"
    Given network file is "common/TestCase12Nodes.uct"
    When I import crac
    Then it should have the following flow CNECs:
      | Name                    | NetworkElementId    | Instant    | Contingency         | Optimized | Monitored | ImaxLeft | ImaxRight | NominalVoltageLeft | NominalVoltageRight |
      | Tie-line BE FR or to ex | BBE2AA1  FFR3AA1  1 | preventive |                     | yes       | no        | 5000.0   | 5000.0    | 400.0              | 400.0               |
      | Tie-line BE FR or to ex | BBE2AA1  FFR3AA1  1 | curative   | Contingency FR1 FR3 | yes       | no        | 5000.0   | 5000.0    | 400.0              | 400.0               |
      | Tie-line BE FR or to ex | BBE2AA1  FFR3AA1  1 | outage     | Contingency FR1 FR3 | yes       | no        | 5000.0   | 5000.0    | 400.0              | 400.0               |
      | Tie-line FR DE          | FFR2AA1  DDE3AA1  1 | preventive |                     | yes       | no        | 5000.0   | 5000.0    | 400.0              | 400.0               |
      | Tie-line FR DE          | FFR2AA1  DDE3AA1  1 | curative   | Contingency FR1 FR3 | yes       | no        | 5000.0   | 5000.0    | 400.0              | 400.0               |
      | Tie-line FR DE          | FFR2AA1  DDE3AA1  1 | outage     | Contingency FR1 FR3 | yes       | no        | 5000.0   | 5000.0    | 400.0              | 400.0               |
    Then the flow cnecs should have the following thresholds:
      | CnecId                                               | Unit   | Min   | Max  | Side  |
      | BBE2AA1  FFR3AA1  1 - preventive                     | AMPERE | None  | 1500 | RIGHT |
      | BBE2AA1  FFR3AA1  1 - preventive                     | AMPERE | -500  | None | RIGHT |
      | BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - outage   | AMPERE | None  | 1500 | RIGHT |
      | BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - outage   | AMPERE | -500  | None | RIGHT |
      | BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - curative | AMPERE | None  | 1500 | RIGHT |
      | BBE2AA1  FFR3AA1  1 - Contingency FR1 FR3 - curative | AMPERE | -500  | None | RIGHT |
      | FFR2AA1  DDE3AA1  1 - preventive                     | AMPERE | -1500 | 1500 | RIGHT |
      | FFR2AA1  DDE3AA1  1 - Contingency FR1 FR3 - outage   | AMPERE | -1500 | 1500 | RIGHT |
      | FFR2AA1  DDE3AA1  1 - Contingency FR1 FR3 - curative | AMPERE | -1500 | 1500 | RIGHT |