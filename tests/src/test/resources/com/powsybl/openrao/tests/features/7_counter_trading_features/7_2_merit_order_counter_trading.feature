Feature: 7.2: Merit order CT Actions
  This feature covers CT actions whose area GLSK is a merit order: a tranche moves only once the previous ones are saturated.
  Only the directions used by the scenario are given, an area without merit order keeps its proportional GLSK from the glsk file.
  All scenarios use 2NodesFR1NodeBE:
        - FR1 and FR2 produce 500 MW each and BE consumes 1000 MW
        - all lines have the same reactance and a 600 MW limit.
  ========================
  WARNINGS : The merit order glsk are currently hard coded in the test as the data are currently not accessible in powsybl core
             This hard coded glsk will be replace before the merge of the corresponding pull request
  ========================
  @rao @counter-trading
  Scenario: 7.2.1: Merit order, first tranche sufficient
    Given network file is "counter-trading/2NodesFR1NodeBE.uct"
    Given crac file is "counter-trading/crac-2-nodes-fr-1-node-be.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes_fr_1node_be.xml"
    Given merit order glsk for area "FR" is:
      | direction | generator          | minP | maxP |
      | down      | FFR1AA1 _generator | 0    | 500  |
      | down      | FFR2AA1 _generator | 0    | 500  |
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the remedial action "counterTradingActionFR" is used in preventive
    Then the remedial action "counterTradingActionBE" is used in preventive
    Then the generator "FFR1AA1 _generator" should have a targetP of 100 MW after PRA
    Then the generator "FFR2AA1 _generator" should have a targetP of 500 MW after PRA
    Then the value of the objective function after PRA should be 20010.0

  @rao @counter-trading
  Scenario: 7.2.2: Merit order, first tranche saturated
    Given network file is "counter-trading/2NodesFR1NodeBE.uct"
    Given crac file is "counter-trading/crac-2-nodes-fr-1-node-be.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes_fr_1node_be.xml"
    Given merit order glsk for area "FR" is:
      | direction | generator          | minP | maxP |
      | down      | FFR2AA1 _generator | 300  | 500  |
      | down      | FFR1AA1 _generator | 0    | 500  |
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the remedial action "counterTradingActionFR" is used in preventive
    Then the remedial action "counterTradingActionBE" is used in preventive
    Then the generator "FFR1AA1 _generator" should have a targetP of 300 MW after PRA
    Then the generator "FFR2AA1 _generator" should have a targetP of 300 MW after PRA
    Then the value of the objective function after PRA should be 20010.0

  @rao @counter-trading
  Scenario: 7.2.3: Merit order, same generator in two tranches
    Given network file is "counter-trading/2NodesFR1NodeBE.uct"
    Given crac file is "counter-trading/crac-2-nodes-fr-1-node-be.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes_fr_1node_be.xml"
    Given merit order glsk for area "FR" is:
      | direction | generator          | minP | maxP |
      | down      | FFR1AA1 _generator | 400  | 500  |
      | down      | FFR2AA1 _generator | 400  | 500  |
      | down      | FFR1AA1 _generator | 0    | 400  |
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the remedial action "counterTradingActionFR" is used in preventive
    Then the remedial action "counterTradingActionBE" is used in preventive
    Then the generator "FFR1AA1 _generator" should have a targetP of 200 MW after PRA
    Then the generator "FFR2AA1 _generator" should have a targetP of 400 MW after PRA
    Then the value of the objective function after PRA should be 20010.0

  @rao @counter-trading
  Scenario: 7.2.4: Merit order, upward direction only, same generator in two tranches
    Given network file is "counter-trading/2NodesFR1NodeBE.uct"
    Given crac file is "counter-trading/crac-2-nodes-fr-1-node-be.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes_fr_1node_be.xml"
    Given merit order glsk for area "BE" is:
      | direction | generator          | minP  | maxP |
      | up        | BBE1AA1 _generator | -1000 | -700 |
      | up        | BBE1AA1 _generator | -700  | -500 |
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the remedial action "counterTradingActionFR" is used in preventive
    Then the remedial action "counterTradingActionBE" is used in preventive
    Then the generator "FFR1AA1 _generator" should have a targetP of 220 MW after PRA
    Then the generator "FFR2AA1 _generator" should have a targetP of 380 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of -600 MW after PRA
    Then the value of the objective function after PRA should be 20010.0

  @rao @counter-trading
  Scenario: 7.2.5: Merit order, downward direction in FR and upward direction in BE
    Given network file is "counter-trading/2NodesFR1NodeBE.uct"
    Given crac file is "counter-trading/crac-2-nodes-fr-1-node-be.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes_fr_1node_be.xml"
    Given merit order glsk for area "FR" is:
      | direction | generator          | minP | maxP |
      | down      | FFR2AA1 _generator | 300  | 500  |
      | down      | FFR1AA1 _generator | 0    | 500  |
    Given merit order glsk for area "BE" is:
      | direction | generator          | minP  | maxP |
      | up        | BBE1AA1 _generator | -1000 | -700 |
      | up        | BBE1AA1 _generator | -700  | -500 |
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the remedial action "counterTradingActionFR" is used in preventive
    Then the remedial action "counterTradingActionBE" is used in preventive
    Then the generator "FFR1AA1 _generator" should have a targetP of 300 MW after PRA
    Then the generator "FFR2AA1 _generator" should have a targetP of 300 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of -600 MW after PRA
    Then the value of the objective function after PRA should be 20010.0

  @rao @counter-trading
  Scenario: 7.2.6: Merit order, upward and downward lists in both areas, only the needed direction is used
    Given network file is "counter-trading/2NodesFR1NodeBE.uct"
    Given crac file is "counter-trading/crac-2-nodes-fr-1-node-be.json"
    Given configuration file is "counter-trading/rao-parameters-ct.json"
    Given ucte glsk file is "counter-trading/glsk_2nodes_fr_1node_be.xml"
    Given merit order glsk for area "FR" is:
      | direction | generator          | minP | maxP |
      | up        | FFR2AA1 _generator | 500  | 1000 |
      | up        | FFR1AA1 _generator | 500  | 1000 |
      | down      | FFR1AA1 _generator | 0    | 500  |
      | down      | FFR2AA1 _generator | 0    | 500  |
    Given merit order glsk for area "BE" is:
      | direction | generator          | minP  | maxP  |
      | up        | BBE1AA1 _generator | -1000 | -500  |
      | down      | BBE1AA1 _generator | -2000 | -1000 |
    When I launch rao at "2021-04-30 22:30"
    Then the execution details should be "The RAO only went through first preventive"
    Then its security status should be "SECURED"
    Then the remedial action "counterTradingActionFR" is used in preventive
    Then the remedial action "counterTradingActionBE" is used in preventive
    Then the generator "FFR1AA1 _generator" should have a targetP of 100 MW after PRA
    Then the generator "FFR2AA1 _generator" should have a targetP of 500 MW after PRA
    Then the generator "BBE1AA1 _generator" should have a targetP of -600 MW after PRA
    Then the value of the objective function after PRA should be 20010.0
