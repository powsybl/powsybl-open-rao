# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 94.2: Voltage Monitoring

  @fast @ac @rao @angle-monitoring
  Scenario: US 94.2.1: Basic Voltage Monitoring
    Given network file is "epic94/voltage_monitoring.xiidm"
    Given crac file is "epic94/voltage_monitoring_crac.json"
    Given configuration file is "epic94/monitoring_parameters.json"
    When I launch rao
    When I launch voltage monitoring on 1 threads
    Then the voltage monitoring result is "HIGH_CONSTRAINT"
    And the min voltage of CNEC "vc" should be 363.58 kV at "curative"
    And the max voltage of CNEC "vc" should be 363.58 kV at "curative"
    And the voltage margin of CNEC "vc" should be -13.58 kV at "curative"
    And the network action "Injection L1 - 2" is used after "coL1" at "curative" during monitoring

  @fast @ac @rao @angle-monitoring
  Scenario: US 94.2.2: Basic Voltage Monitoring with preventive and curative VoltageCNECs
    Given network file is "epic94/voltage_monitoring.xiidm"
    Given crac file is "epic94/voltage_monitoring_with_preventive_crac.json"
    Given configuration file is "epic94/monitoring_parameters.json"
    When I launch rao
    When I launch voltage monitoring on 1 threads
    Then the voltage monitoring result is "HIGH_CONSTRAINT"
    And the min voltage of CNEC "vc - preventive" should be 385.70 kV at "curative"
    And the max voltage of CNEC "vc - preventive" should be 385.70 kV at "curative"
    And the voltage margin of CNEC "vc - preventive" should be 4.30 kV at "curative"
    And the min voltage of CNEC "vc - curative" should be 363.58 kV at "curative"
    And the max voltage of CNEC "vc - curative" should be 363.58 kV at "curative"
    And the voltage margin of CNEC "vc - curative" should be -13.58 kV at "curative"
    And the network action "Injection L1 - 2" is used after "coL1" at "curative" during monitoring
