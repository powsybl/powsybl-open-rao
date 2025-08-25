# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 20.2: Handle loopflows in second preventive optimization

  @fast @rao @mock @dc @second-preventive @loopflow
  Scenario: US 20.2.1: LF constraint in curative is solved by 2P
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic20/CBCORA_ep20us2case1.xml"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    Given configuration file is "epic20/RaoParameters_maxMargin_MW_DC_withLF_with2P.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 0 remedial actions are used in preventive
    And 0 remedial actions are used after "CO1" at "curative"
    And the worst margin is 500 MW
    And the value of the objective function after CRA should be -500
    And the loopflow threshold on cnec "003_FR-DE - curative" should be 250 MW
    And the initial loopflow on cnec "003_FR-DE - curative" should be -341 MW
    Then the execution details should be "Second preventive improved first preventive results"

  @fast @rao @mock @dc @second-preventive @loopflow
  Scenario: US 20.2.2: LF constraint in curative is solved by curative network action + 2P
    # curative network action restores the network to US20.2.1 situation, that's why we
    # finally fall back to same loopflow after CRA as US20.2.1's initial loopflow
    Given network file is "common/TestCase12NodesDoubledPstLine.uct" for CORE CC
    Given crac file is "epic20/CBCORA_ep20us2case2.xml"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    Given configuration file is "epic20/RaoParameters_maxMargin_MW_DC_withLF_with2P.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 0 remedial actions are used in preventive
    Then 1 remedial actions are used after "CO1" at "curative"
    And the worst margin is 543 MW
    And the value of the objective function after CRA should be -543
    And the loopflow threshold on cnec "003_FR-DE - curative" should be 250 MW
    And the initial loopflow on cnec "003_FR-DE - curative" should be -368 MW
    And the loopflow on cnec "003_FR-DE - curative" after CRA should be -341 MW
    Then the execution details should be "Second preventive improved first preventive results"

  @fast @rao @mock @dc @second-preventive @loopflow
  Scenario: US 20.2.3: LF constraint avoided on preventive CNEC in 2P
    Given network file is "common/TestCase12Nodes.uct" for CORE CC
    Given crac file is "epic20/CBCORA_ep20us2case3.xml"
    Given loopflow glsk file is "common/glsk_proportional_12nodes.xml"
    Given configuration file is "epic20/RaoParameters_maxMargin_MW_DC_withLF_with2P.json"
    When I launch search_tree_rao at "2019-01-08 12:00"
    Then 1 remedial actions are used in preventive
    And the remedial action "PRA_PST_BE" is used in preventive
    And 0 remedial actions are used after "CO1" at "curative"
    And the worst margin is 370 MW
    And the value of the objective function after CRA should be -370
    And the margin on cnec "003_FR-DE - curative" after CRA should be 370 MW
    And the loopflow threshold on cnec "001_FR-DE - preventive" should be 400 MW
    And the initial loopflow on cnec "001_FR-DE - preventive" should be -124 MW
    And the loopflow on cnec "001_FR-DE - preventive" after PRA should be -297 MW
    # Doesn't run 2P because LF constraint is already seen in 1P as the CNEC is preventive
    Then the execution details should be "The RAO only went through first preventive"