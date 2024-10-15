# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: Tests pst

  #Interesting case, 2P degrade la solution
  #Limiting element #01: margin = -55.36 A, element _1d9c658e-1a01-c0ee-d127-a22e1270a242 + _2e81de07-4c22-5aa1-9683-5e51b054f7f8 at state REE-RTE-Co-1 - curative, CNEC ID = "ARKALE_XAR_AR21_1_220 - ONE - REE-RTE-Co-1 - curative"
  Scenario: 0230 - ES-FR 2750
    Given network file is "pstSeries/network_0230_ESFR_2750.xiidm"
    Given crac file is "pstSeries/cracFrEs_0230.json"
    Given configuration file is "pstSeries/raoParametersES_FR_0230.json"
    Given RaoResult file is "pstSeries/raoResult_0230_ESFR_2750.json"
    #When I launch search_tree_rao at "2024-09-15 02:30"
    When I compute loadFlow with PST in regulation mode at "2024-09-15 02:30"

  Scenario: 0230 - FR-ES 2550
    Given network file is "pstSeries/network_0230_FRES_2550.xiidm"
    Given crac file is "pstSeries/cracFrEs_0230.json"
    Given configuration file is "pstSeries/raoParametersES_FR_0230.json"
    Given RaoResult file is "pstSeries/raoResult_0230_FRES_2550.json"
    When I compute loadFlow with PST in regulation mode at "2024-09-15 02:30"

  Scenario: 0530 - ES-FR 2700
    Given network file is "pstSeries/network_0530_ESFR_2700.xiidm"
    Given crac file is "pstSeries/cracFrEs_0530.json"
    Given configuration file is "pstSeries/raoParametersES_FR_0530.json"
    Given RaoResult file is "pstSeries/raoResult_0530_ESFR_2700.json"
    When I compute loadFlow with PST in regulation mode at "2024-09-15 05:30"

  Scenario: 0530 - FR-ES 2300
    Given network file is "pstSeries/network_0530_FRES_2300.xiidm"
    Given crac file is "pstSeries/cracFrEs_0530.json"
    Given configuration file is "pstSeries/raoParametersES_FR_0530.json"
    Given RaoResult file is "pstSeries/raoResult_0530_FRES_2300.json"
    When I compute loadFlow with PST in regulation mode at "2024-09-15 05:30"

  Scenario: 0930 - ES-FR 2700
    Given network file is "pstSeries/network_0930_ESFR_2700.xiidm"
    Given crac file is "pstSeries/cracFrEs_0930.json"
    Given configuration file is "pstSeries/raoParametersES_FR_0930.json"
    Given RaoResult file is "pstSeries/raoResult_0930_ESFR_2700.json"
    When I compute loadFlow with PST in regulation mode at "2024-09-15 09:30"

  Scenario: 1930 - ES-FR 2700
    Given network file is "pstSeries/network_1930_ESFR_2700.xiidm"
    Given crac file is "pstSeries/cracFrEs_1930.json"
    Given configuration file is "pstSeries/raoParametersES_FR_1930.json"
    Given RaoResult file is "pstSeries/raoResult_1930_ESFR_2700.json"
    When I compute loadFlow with PST in regulation mode at "2024-09-15 19:30"

  Scenario: 12 nodes
    Given network file is "common/TestCase12Nodes.uct"
    Given crac file is "epic2/SL_ep2us2case1.json"
    Given configuration file is "common/RaoParameters_posMargin_ampere.json"
    Given RaoResult file is "pstSeries/raoResult_12Nodes.json"
    When I compute loadFlow with PST in regulation mode at "2024-09-15 19:30"

