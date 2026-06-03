# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: 6.2.2: Voltage Monitoring
  This feature covers voltage monitoring, which is parametrised in the CRAC file.

  @fast @ac @rao @voltage-monitoring @secure-flow
  Scenario: 6.2.2.1: Basic Voltage Monitoring
  Simple voltage monitoring case with one VoltageCnec in curative and one remedial action.
  The CRAC file does not contain any FlowCNEC but the computation is still performed.
    Given network file is "epic94/voltage_monitoring.xiidm"
    Given crac file is "epic94/voltage_monitoring_crac.json"
    Given configuration file is "epic94/monitoring_parameters.json"
    When I launch rao
    When I launch voltage monitoring on 1 threads
    Then the execution details should be "The RAO only went through first preventive and went through voltage monitoring"
    Then its security status should be "UNSECURED"
    Then the voltage monitoring result is "HIGH_CONSTRAINT"
    Then the min voltage of CNEC "vc" should be 363.58 kV at "curative"
    Then the max voltage of CNEC "vc" should be 363.58 kV at "curative"
    Then the voltage margin of CNEC "vc" should be -13.58 kV at "curative"
    Then the network action "Injection L1 - 2" is used after "coL1" at "curative" during monitoring

  @fast @ac @rao @voltage-monitoring @secure-flow
  Scenario: 6.2.2.2: Basic Voltage Monitoring with preventive and curative VoltageCNECs
  Simple voltage monitoring case with two VoltageCnecs (one in preventive and the other in curative) and one remedial action.
  The CRAC file does not contain any FlowCNEC but the computation is still performed.
    Given network file is "epic94/voltage_monitoring.xiidm"
    Given crac file is "epic94/voltage_monitoring_with_preventive_crac.json"
    Given configuration file is "epic94/monitoring_parameters.json"
    When I launch rao
    When I launch voltage monitoring on 1 threads
    Then the execution details should be "The RAO only went through first preventive and went through voltage monitoring"
    Then its security status should be "UNSECURED"
    Then the voltage monitoring result is "HIGH_CONSTRAINT"
    Then the min voltage of CNEC "vc - preventive" should be 385.70 kV at "preventive"
    Then the max voltage of CNEC "vc - preventive" should be 385.70 kV at "preventive"
    Then the voltage margin of CNEC "vc - preventive" should be 4.30 kV at "preventive"
    Then the min voltage of CNEC "vc - curative" should be 363.58 kV at "curative"
    Then the max voltage of CNEC "vc - curative" should be 363.58 kV at "curative"
    Then the voltage margin of CNEC "vc - curative" should be -13.58 kV at "curative"
    Then the network action "Injection L1 - 2" is used after "coL1" at "curative" during monitoring
