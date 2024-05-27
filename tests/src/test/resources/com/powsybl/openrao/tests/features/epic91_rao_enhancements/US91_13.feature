Feature: US 91.12: Auto search-tree

  @fast @rao @ac
  Scenario: US 91.13.1: Auto search tree with two available topological ARAs
    # One FORCED topological ARA is simulated
    # Two AVAILABLE topological ARA are present in the CRAC but one is enough to secure the network
    # One FORCED PST ARA will not be used because the network is already secure after the search tree
    Given network file is "epic91/12Nodes_2_twin_lines.uct"
    Given crac file is "epic91/crac_91_13_1.json"
    Given configuration file is "epic91/RaoParameters_case_91_13_1.json"
    When I launch search_tree_rao
    Then the initial flow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -382.0 MW
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -382.0 MW
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -1000.0 MW
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after "auto" instant remedial actions should be -207.0 MW
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative" after "curative" instant remedial actions should be -207.0 MW
    Then 2 remedial actions are used after "Contingency DE2 NL3 1" at "auto"
    Then the remedial action "ARA_CLOSE_NL2_BE3_2" is used after "Contingency DE2 NL3 1" at "auto"
    Then the remedial action "ARA_CLOSE_DE2_NL3_2" is used after "Contingency DE2 NL3 1" at "auto"

  @fast @rao @ac
  Scenario: US 91.13.2: RAO goes through all three Automaton Simulator's steps
    Given network file is "epic91/12Nodes_2_twin_lines.uct"
    Given crac file is "epic91/crac_91_13_2.json"
    Given configuration file is "epic91/RaoParameters_case_91_13_1.json"
    When I launch search_tree_rao
    Then the initial flow on cnec "NNL2AA1  BBE3AA1  1 - preventive" should be -382.0 MW
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - preventive" after PRA should be -382.0 MW
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage" after PRA should be -1000.0 MW
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto" after "auto" instant remedial actions should be -131.0 MW
    Then the flow on cnec "NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative" after "curative" instant remedial actions should be -131.0 MW
    Then 4 remedial actions are used after "Contingency DE2 NL3 1" at "auto"
    Then the remedial action "ARA_CLOSE_NL2_BE3_2" is used after "Contingency DE2 NL3 1" at "auto"
    Then the remedial action "ARA_CLOSE_DE2_NL3_2" is used after "Contingency DE2 NL3 1" at "auto"
    Then the remedial action "ARA_INJECTION_SETPOINT_800MW" is used after "Contingency DE2 NL3 1" at "auto"
    Then the tap of PstRangeAction "ARA_PST_BE" should be 16 after "Contingency DE2 NL3 1" at "auto"