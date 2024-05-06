# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

@flowbased
Feature: Flowbased computation

  @fast @flowbased @mock
  Scenario: FB.1: critical branches only
    ## Basecase: (all lines have the same characteristics)
    ## 500 MW -> FR ====== BE <- 500 MW
    ##           \\       //
    ##            \\     //
    ##             \\   //
    ##              \\ //
    ##               DE -> 1000 MW
    Given network file is "flowbased_computation/3nodes.uct"
    Given crac file is "flowbased_computation/SL_CB.json"
    Given Belgian glsk file is "flowbased_computation/GlskBE.xml"
    Given German glsk file is "flowbased_computation/GlskDE.xml"
    Given French glsk file is "flowbased_computation/GlskFR.xml"
    When I launch flowbased computation with "OpenLoadFlow"
    # Flows
    Then the flow on branch "BBE1AA1  DDE1AA1  1 - preventive" before outage is 250.0
    Then the flow on branch "BBE1AA1  DDE1AA1  2 - preventive" before outage is 250.0
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -250.0
    Then the flow on branch "DDE1AA1  FFR1AA1  2 - preventive" before outage is -250.0
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is 0.0
    Then the flow on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is 0.0
    # Basecase zone-to-zone BE-DE
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - preventive" before outage is 0.33
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - preventive" before outage is 0.33
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -0.166
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - preventive" before outage is -0.166
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is 0.166
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is 0.166
    # Basecase zone-to-zone FR-DE
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - preventive" before outage is 0.166
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - preventive" before outage is 0.166
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -0.33
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - preventive" before outage is -0.33
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is -0.166
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is -0.166

  @fast @flowbased @mock
  Scenario: FB.2: critical branches and contingencies
    ## Basecase:                                Outage A:                                  Outage B:
    ## 500 MW -> FR ====== BE <- 500 MW      ->    500 MW -> FR ====== BE <- 500 MW   &&     500 MW -> FR ------ BE <- 500 MW
    ##           \\       //                 ->              \        //              &&               \\       //
    ##            \\     //                  ->               \      //               &&                \\     //
    ##             \\   //                   ->                \    //                &&                 \\   //
    ##              \\ //                    ->                 \  //                 &&                  \\ //
    ##               DE -> 1000 MW           ->                  DE -> 1000 MW        &&                   DE -> 1000 MW
    Given network file is "flowbased_computation/3nodes.uct"
    Given crac file is "flowbased_computation/SL_CBCo.json"
    Given Belgian glsk file is "flowbased_computation/GlskBE.xml"
    Given German glsk file is "flowbased_computation/GlskDE.xml"
    Given French glsk file is "flowbased_computation/GlskFR.xml"
    When I launch flowbased computation with "OpenLoadFlow"
    # Flows
    Then the flow on branch "BBE1AA1  DDE1AA1  1 - preventive" before outage is 250.0
    Then the flow on branch "BBE1AA1  DDE1AA1  2 - preventive" before outage is 250.0
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -250.0
    Then the flow on branch "DDE1AA1  FFR1AA1  2 - preventive" before outage is -250.0
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is 0.0
    Then the flow on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is 0.0
    Then the flow on branch "BBE1AA1  DDE1AA1  1 - A_open_DEFR2 - outage" at instant "outage", after outage "A_open_DEFR2" is 312.5
    Then the flow on branch "BBE1AA1  DDE1AA1  2 - A_open_DEFR2 - outage" at instant "outage", after outage "A_open_DEFR2" is 312.5
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - A_open_DEFR2 - outage" at instant "outage", after outage "A_open_DEFR2" is -375.0
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - A_open_DEFR2 - outage" at instant "outage", after outage "A_open_DEFR2" is -62.5
    Then the flow on branch "BBE1AA1  FFR1AA1  2 - A_open_DEFR2 - outage" at instant "outage", after outage "A_open_DEFR2" is -62.5
    Then the flow on branch "BBE1AA1  DDE1AA1  1 - B_open_BEFR2 - outage" at instant "outage", after outage "B_open_BEFR2" is 250.0
    Then the flow on branch "BBE1AA1  DDE1AA1  2 - B_open_BEFR2 - outage" at instant "outage", after outage "B_open_BEFR2" is 250.0
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - B_open_BEFR2 - outage" at instant "outage", after outage "B_open_BEFR2" is -250.0
    Then the flow on branch "DDE1AA1  FFR1AA1  2 - B_open_BEFR2 - outage" at instant "outage", after outage "B_open_BEFR2" is -250.0
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - B_open_BEFR2 - outage" at instant "outage", after outage "B_open_BEFR2" is 0.0
    # Basecase
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - preventive" before outage is 0.33
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - preventive" before outage is 0.33
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -0.166
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - preventive" before outage is -0.166
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is 0.166
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is 0.166
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - preventive" before outage is 0.166
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - preventive" before outage is 0.166
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -0.33
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - preventive" before outage is -0.33
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is -0.166
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is -0.166
    # Outage A
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - A_open_DEFR2 - outage" at instant "outage", after outage "A_open_DEFR2" is 0.375
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - A_open_DEFR2 - outage" at instant "outage", after outage "A_open_DEFR2" is 0.375
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - A_open_DEFR2 - outage" at instant "outage", after outage "A_open_DEFR2" is -0.25
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - A_open_DEFR2 - outage" at instant "outage", after outage "A_open_DEFR2" is 0.125
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - A_open_DEFR2 - outage" at instant "outage", after outage "A_open_DEFR2" is 0.125
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - A_open_DEFR2 - outage" at instant "outage", after outage "A_open_DEFR2" is 0.25
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - A_open_DEFR2 - outage" at instant "outage", after outage "A_open_DEFR2" is 0.25
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - A_open_DEFR2 - outage" at instant "outage", after outage "A_open_DEFR2" is -0.5
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - A_open_DEFR2 - outage" at instant "outage", after outage "A_open_DEFR2" is -0.25
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - A_open_DEFR2 - outage" at instant "outage", after outage "A_open_DEFR2" is -0.25
    # Outage B
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - B_open_BEFR2 - outage" at instant "outage", after outage "B_open_BEFR2" is 0.375
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - B_open_BEFR2 - outage" at instant "outage", after outage "B_open_BEFR2" is 0.375
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - B_open_BEFR2 - outage" at instant "outage", after outage "B_open_BEFR2" is -0.125
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - B_open_BEFR2 - outage" at instant "outage", after outage "B_open_BEFR2" is -0.125
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - B_open_BEFR2 - outage" at instant "outage", after outage "B_open_BEFR2" is 0.25
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - B_open_BEFR2 - outage" at instant "outage", after outage "B_open_BEFR2" is 0.125
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - B_open_BEFR2 - outage" at instant "outage", after outage "B_open_BEFR2" is 0.125
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - B_open_BEFR2 - outage" at instant "outage", after outage "B_open_BEFR2" is -0.375
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - B_open_BEFR2 - outage" at instant "outage", after outage "B_open_BEFR2" is -0.375
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - B_open_BEFR2 - outage" at instant "outage", after outage "B_open_BEFR2" is -0.25
    # Outage A (curative instant, without CRA)
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - A_open_DEFR2 - curative" at instant "curative", after outage "A_open_DEFR2" is 0.375
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - A_open_DEFR2 - curative" at instant "curative", after outage "A_open_DEFR2" is 0.375
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - A_open_DEFR2 - curative" at instant "curative", after outage "A_open_DEFR2" is -0.25
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - A_open_DEFR2 - curative" at instant "curative", after outage "A_open_DEFR2" is 0.125
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - A_open_DEFR2 - curative" at instant "curative", after outage "A_open_DEFR2" is 0.125
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - A_open_DEFR2 - curative" at instant "curative", after outage "A_open_DEFR2" is 0.25
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - A_open_DEFR2 - curative" at instant "curative", after outage "A_open_DEFR2" is 0.25
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - A_open_DEFR2 - curative" at instant "curative", after outage "A_open_DEFR2" is -0.5
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - A_open_DEFR2 - curative" at instant "curative", after outage "A_open_DEFR2" is -0.25
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - A_open_DEFR2 - curative" at instant "curative", after outage "A_open_DEFR2" is -0.25
        # Outage B (curative instant, without CRA)
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - B_open_BEFR2 - curative" at instant "curative", after outage "B_open_BEFR2" is 0.375
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - B_open_BEFR2 - curative" at instant "curative", after outage "B_open_BEFR2" is 0.375
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - B_open_BEFR2 - curative" at instant "curative", after outage "B_open_BEFR2" is -0.125
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - B_open_BEFR2 - curative" at instant "curative", after outage "B_open_BEFR2" is -0.125
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - B_open_BEFR2 - curative" at instant "curative", after outage "B_open_BEFR2" is 0.25
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - B_open_BEFR2 - curative" at instant "curative", after outage "B_open_BEFR2" is 0.125
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - B_open_BEFR2 - curative" at instant "curative", after outage "B_open_BEFR2" is 0.125
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - B_open_BEFR2 - curative" at instant "curative", after outage "B_open_BEFR2" is -0.375
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - B_open_BEFR2 - curative" at instant "curative", after outage "B_open_BEFR2" is -0.375
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - B_open_BEFR2 - curative" at instant "curative", after outage "B_open_BEFR2" is -0.25

  @fast @flowbased @mock
  Scenario: FB.3: preventive remedial action and critical branches
    ## Basecase: with the PRA, it becomes:    Basecase with PRA (same topology as Outage A above):
    ## 500 MW -> FR ====== BE <- 500 MW      ->    500 MW -> FR ====== BE <- 500 MW
    ##           \\       //                 ->              \        //
    ##            \\     //                  ->               \      //
    ##             \\   //                   ->                \    //
    ##              \\ //                    ->                 \  //
    ##               DE -> 1000 MW           ->                  DE -> 1000 MW
    Given network file is "flowbased_computation/3nodes.uct"
    Given crac file is "flowbased_computation/SL_CBPRA.json"
    Given Belgian glsk file is "flowbased_computation/GlskBE.xml"
    Given German glsk file is "flowbased_computation/GlskDE.xml"
    Given French glsk file is "flowbased_computation/GlskFR.xml"
    When I launch flowbased computation with "OpenLoadFlow"
    #Flows
    Then the flow on branch "BBE1AA1  DDE1AA1  1 - preventive" before outage is 312.5
    Then the flow on branch "BBE1AA1  DDE1AA1  2 - preventive" before outage is 312.5
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -375.0
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is -62.5
    Then the flow on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is -62.5
    # Basecase with PRA, zone-to-zone BE-DE
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - preventive" before outage is 0.375
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - preventive" before outage is 0.375
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -0.25
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is 0.125
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is 0.125
    # Basecase with PRA, zone-to-zone FR-DE
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - preventive" before outage is 0.25
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - preventive" before outage is 0.25
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -0.5
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is -0.25
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is -0.25

  @fast @flowbased @mock
  Scenario: FB.4: preventive remedial action, critical branches, outages and curative remedial actions
    ## Basecase:                             Basecase with PRA (same topology as Outage1 above):
    ## 500 MW -> FR ====== BE <- 500 MW      ->    500 MW -> FR ====== BE <- 500 MW
    ##           \\       //                 ->              \        //
    ##            \\     //                  ->               \      //
    ##             \\   //                   ->                \    //
    ##              \\ //                    ->                 \  //
    ##               DE -> 1000 MW           ->                  DE -> 1000 MW
    ##
    ##
    ## With PRA and Outage1:                 With PRA, Outage1 and CRA1:
    ## 500 MW -> FR ====== BE <- 500 MW      ->    500 MW -> FR ------ BE <- 500 MW
    ##            \       /                  ->              \        /
    ##             \     /                   ->               \      /
    ##              \   /                    ->                \    /
    ##               \ /                     ->                 \  /
    ##               DE -> 1000 MW           ->                  DE -> 1000 MW
    ##
    ##
    ## With PRA and Outage2:                 With PRA, Outage2 and CRA2:
    ## 500 MW -> FR ------ BE <- 500 MW      ->    500 MW -> FR ------ BE <- 500 MW
    ##            \       //                 ->              \\       //
    ##             \     //                  ->               \\     //
    ##              \   //                   ->                \\   //
    ##               \ //                    ->                 \\ //
    ##               DE -> 1000 MW           ->                  DE -> 1000 MW

    Given network file is "flowbased_computation/3nodes.uct"
    Given crac file is "flowbased_computation/SL_CBCoRA.json"
    Given Belgian glsk file is "flowbased_computation/GlskBE.xml"
    Given German glsk file is "flowbased_computation/GlskDE.xml"
    Given French glsk file is "flowbased_computation/GlskFR.xml"
    When I launch flowbased computation with "OpenLoadFlow"
    # Flows
    Then the flow on branch "BBE1AA1  DDE1AA1  1 - preventive" before outage is 312.5
    Then the flow on branch "BBE1AA1  DDE1AA1  2 - preventive" before outage is 312.5
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -375.0
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is -62.5
    Then the flow on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is -62.5
    Then the flow on branch "BBE1AA1  DDE1AA1  1 - OpenBEDE2 - outage" at instant "outage", after outage "OpenBEDE2" is 500.0
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - OpenBEDE2 - outage" at instant "outage", after outage "OpenBEDE2" is -500.0
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - OpenBEDE2 - outage" at instant "outage", after outage "OpenBEDE2" is 0.0
    Then the flow on branch "BBE1AA1  FFR1AA1  2 - OpenBEDE2 - outage" at instant "outage", after outage "OpenBEDE2" is 0.0
    Then the flow on branch "BBE1AA1  DDE1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 300.0
    Then the flow on branch "BBE1AA1  DDE1AA1  2 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 300.0
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -400.0
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -100.0
    Then the flow on branch "BBE1AA1  DDE1AA1  1 - OpenBEDE2 - curative" at instant "curative", after outage "OpenBEDE2" is 500.0
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - OpenBEDE2 - curative" at instant "curative", after outage "OpenBEDE2" is -500.0
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - OpenBEDE2 - curative" at instant "curative", after outage "OpenBEDE2" is 0.0
    Then the flow on branch "BBE1AA1  DDE1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 250.0
    Then the flow on branch "BBE1AA1  DDE1AA1  2 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 250.0
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -250.0
    Then the flow on branch "DDE1AA1  FFR1AA1  2 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -250.0
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 0.0
    # Basecase + PRA
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - preventive" before outage is 0.375
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - preventive" before outage is 0.375
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -0.25
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is 0.125
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is 0.125
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - preventive" before outage is 0.25
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - preventive" before outage is 0.25
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -0.5
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is -0.25
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is -0.25
    # With PRA and Outage1
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - OpenBEDE2 - outage" at instant "outage", after outage "OpenBEDE2" is 0.6
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEDE2 - outage" at instant "outage", after outage "OpenBEDE2" is -0.4
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEDE2 - outage" at instant "outage", after outage "OpenBEDE2" is 0.2
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - OpenBEDE2 - outage" at instant "outage", after outage "OpenBEDE2" is 0.2
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - OpenBEDE2 - outage" at instant "outage", after outage "OpenBEDE2" is 0.4
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEDE2 - outage" at instant "outage", after outage "OpenBEDE2" is -0.6
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEDE2 - outage" at instant "outage", after outage "OpenBEDE2" is -0.2
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - OpenBEDE2 - outage" at instant "outage", after outage "OpenBEDE2" is -0.2
    # With PRA and Outage2
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 0.4
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 0.4
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -0.2
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 0.2
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 0.2
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 0.2
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -0.6
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -0.4
    # With PRA, Outage1 and CRA1
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - OpenBEDE2 - curative" at instant "curative", after outage "OpenBEDE2" is 0.66
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEDE2 - curative" at instant "curative", after outage "OpenBEDE2" is -0.33
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEDE2 - curative" at instant "curative", after outage "OpenBEDE2" is 0.33
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - OpenBEDE2 - curative" at instant "curative", after outage "OpenBEDE2" is 0.33
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEDE2 - curative" at instant "curative", after outage "OpenBEDE2" is -0.66
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEDE2 - curative" at instant "curative", after outage "OpenBEDE2" is -0.33
    # With PRA, Outage2 and CRA2
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 0.375
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 0.375
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -0.125
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -0.125
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 0.25
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 0.125
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  DDE1AA1  2 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 0.125
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -0.375
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -0.375
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -0.25

  @fast @flowbased @mock
  Scenario: FB.5: Test with a PST
    ## Basecase:                               After PRA:
    ## 500 MW -> FR ====== BE <- 500 MW      ->    500 MW -> FR ====== BE <- 500 MW
    ##            \       /                  ->              \        /
    ##             \     PST (tap -8)        ->               \     PST (tap 0)
    ##              \   /                    ->                \    /
    ##               \ /                     ->                 \  /
    ##               DE -> 1000 MW           ->                  DE -> 1000 MW
    ##
    ##
    ## After PRA and outage:                     After PRA, outage and CRA:
    ## 500 MW -> FR ------ BE <- 500 MW      ->    500 MW -> FR ------ BE <- 500 MW
    ##            \       /                  ->              \        /
    ##             \     PST (tap 0)       ->                 \     PST (tap +14)
    ##              \   /                    ->                \    /
    ##               \ /                     ->                 \  /
    ##               DE -> 1000 MW           ->                  DE -> 1000 MW
    ##
    ##
    Given network file is "flowbased_computation/3nodes_pst.uct"
    Given crac file is "flowbased_computation/SL_pst.json"
    Given Belgian glsk file is "flowbased_computation/GlskBE.xml"
    Given German glsk file is "flowbased_computation/GlskDE.xml"
    Given French glsk file is "flowbased_computation/GlskFR.xml"
    When I launch flowbased computation with "OpenLoadFlow"
    # Flows
    Then the flow on branch "BBE2AA1  DDE1AA1  1 - preventive" before outage is 500.0
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -500.0
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is 0.0
    Then the flow on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is 0.0
    Then the flow on branch "BBE2AA1  DDE1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 500.0
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -500.0
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 0.0
    Then the flow on branch "BBE2AA1  DDE1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -7.0
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -1007.1
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 507.05
    # Basecase + PRA: see "Outage1" above
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE2AA1  DDE1AA1  1 - preventive" before outage is 0.6
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -0.4
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is 0.2
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE2AA1  DDE1AA1  1 - preventive" before outage is 0.4
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -0.6
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is -0.2
    # After outage: see "Outage1+CRA1" above
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE2AA1  DDE1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 0.66
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -0.33
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 0.33
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE2AA1  DDE1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 0.33
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -0.66
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -0.33
    # After CRA:
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE2AA1  DDE1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 0.66
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -0.33
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 0.33
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE2AA1  DDE1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 0.33
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -0.66
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -0.33

  Scenario: FB.6: Test 2 with a PST
    ## Expected results computed manually on Convergence
    ##
    Given network file is "flowbased_computation/3nodes_pst2.uct"
    Given crac file is "flowbased_computation/SL_pst2.json"
    Given Belgian glsk file is "flowbased_computation/GlskBE.xml"
    Given German glsk file is "flowbased_computation/GlskDE.xml"
    Given French glsk file is "flowbased_computation/GlskFR.xml"
    When I launch flowbased computation with "OpenLoadFlow"
    # Flows
    Then the flow on branch "BBE2AA1  DDE1AA1  1 - preventive" before outage is 125.0
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -437.0
    Then the flow on branch "DDE1AA1  FFR1AA1  2 - preventive" before outage is -437.0
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is 187.5
    Then the flow on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is 187.5
    Then the flow on branch "BBE2AA1  DDE1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 197.0
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -401.5
    Then the flow on branch "DDE1AA1  FFR1AA1  2 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -401.5
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 303.0
    Then the flow on branch "BBE2AA1  DDE1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 478.0
    Then the flow on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -261.5
    Then the flow on branch "DDE1AA1  FFR1AA1  2 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -261.5
    Then the flow on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 22.0
    # Basecase + PRA: see "Outage1" above
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE2AA1  DDE1AA1  1 - preventive" before outage is 0.48
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -0.26
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - preventive" before outage is -0.26
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is 0.26
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is 0.26
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE2AA1  DDE1AA1  1 - preventive" before outage is 0.24
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - preventive" before outage is -0.38
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - preventive" before outage is -0.38
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - preventive" before outage is -0.12
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  2 - preventive" before outage is -0.12
    # After outage: see "Outage1+CRA1" above
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE2AA1  DDE1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 0.58
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -0.21
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -0.21
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 0.42
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE2AA1  DDE1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is 0.20
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -0.40
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -0.40
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - outage" at instant "outage", after outage "OpenBEFR2" is -0.20
    # After CRA:
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE2AA1  DDE1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 0.58
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -0.21
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -0.21
    Then for an exchange from zone "BE" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 0.42
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE2AA1  DDE1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is 0.20
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -0.40
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "DDE1AA1  FFR1AA1  2 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -0.40
    Then for an exchange from zone "FR" to zone DE, the ptdf value on branch "BBE1AA1  FFR1AA1  1 - OpenBEFR2 - curative" at instant "curative", after outage "OpenBEFR2" is -0.20
