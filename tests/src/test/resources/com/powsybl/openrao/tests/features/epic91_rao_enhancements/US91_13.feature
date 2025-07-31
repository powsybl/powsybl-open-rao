# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 91.13: PST Regulation

  @ac @fast @rao
  Scenario: US 91.13.1.a: Unsecure case with 2 nodes and 1 PST
    Given network file is "epic91/2Nodes3ParallelLinesPST.uct"
    Given crac file is "epic91/crac-91-13-1.json"
    Given configuration file is "epic91/RaoParameters_ac.json"
    When I launch search_tree_rao
    Then the tap of PstRangeAction "pstBeFr2" should be -16 in preventive
    And the margin on cnec "cnecBeFr1Preventive" after PRA should be 579.65 A
    And the tap of PstRangeAction "pstBeFr2" should be -2 after "Contingency BE1 FR1 3" at "curative"
    And the worst margin is -690.23 A
    And the margin on cnec "cnecBeFr2Curative" after CRA should be -690.23 A
    And the margin on cnec "cnecBeFr1Curative" after CRA should be -676.38 A

  @ac @fast @rao @pst-regulation
  Scenario: US 91.13.1.b: Duplicate of US 91.13.1.a with PST regulation
  At the end of curative optimization, the PST is still unsecure but can be secured by moving the tap to position 7
  even if it worsens the minimum margin significantly.
    Given network file is "epic91/2Nodes3ParallelLinesPST.uct"
    Given crac file is "epic91/crac-91-13-1.json"
    Given configuration file is "epic91/RaoParameters_ac_pstRegulation.json"
    When I launch search_tree_rao
    Then the tap of PstRangeAction "pstBeFr2" should be -16 in preventive
    And the margin on cnec "cnecBeFr1Preventive" after PRA should be 579.65 A
    And the tap of PstRangeAction "pstBeFr2" should be 7 after "Contingency BE1 FR1 3" at "curative"
    And the worst margin is -1382.77 A
    And the margin on cnec "cnecBeFr1Curative" after CRA should be -1382.77 A
    And the margin on cnec "cnecBeFr2Curative" after CRA should be 15.49 A

  @ac @fast @rao
  Scenario: US 91.13.2.a: Margin is maximized by putting the PST in abutment
  By putting the PST on tap -16, the margin on the preventive and curative CNECs is maximized.
  The PST is overloaded but this is invisible for the RAO since no CNEC is defined for it.
    Given network file is "epic91/2Nodes3ParallelLinesPST.uct"
    Given crac file is "epic91/crac-91-13-2.json"
    Given configuration file is "epic91/RaoParameters_ac.json"
    When I launch search_tree_rao
    Then the tap of PstRangeAction "pstBeFr2" should be -16 in preventive
    And the margin on cnec "cnecBeFr1Preventive" after PRA should be 579.65 A
    And the tap of PstRangeAction "pstBeFr2" should be -16 after "Contingency BE1 FR1 3" at "curative"
    And the worst margin is 418.84 A
    And the margin on cnec "cnecBeFr1Curative" after CRA should be 418.84 A

  @ac @fast @rao @pst-regulation
  Scenario: US 91.13.2.b: Duplicate of US 91.13.2.a with PST regulation
  At the end of curative optimization, the PST is still unsecure but can be secured by moving the tap to position 7.
  However, the PST in is abutment at tap -16 so it cannot be regulated, thus leaving the situation as is.
    Given network file is "epic91/2Nodes3ParallelLinesPST.uct"
    Given crac file is "epic91/crac-91-13-2.json"
    Given configuration file is "epic91/RaoParameters_ac_pstRegulation.json"
    When I launch search_tree_rao
    Then the tap of PstRangeAction "pstBeFr2" should be -16 in preventive
    And the margin on cnec "cnecBeFr1Preventive" after PRA should be 579.65 A
    And the tap of PstRangeAction "pstBeFr2" should be -16 after "Contingency BE1 FR1 3" at "curative"
    And the worst margin is 418.84 A
    And the margin on cnec "cnecBeFr1Curative" after CRA should be 418.84 A

  @ac @fast @rao
  Scenario: US 91.13.3.a: 3 contingencies and 3 PSTs
    Given network file is "epic91/4NodesSeries.uct"
    Given crac file is "epic91/crac-91-13-3.json"
    Given configuration file is "epic91/RaoParameters_ac.json"
    When I launch search_tree_rao
    Then the worst margin is 7.13 A on cnec "cnecFr34PstAuto"
    And the margin on cnec "cnecFr34Curative" after CRA should be 984.32 A
    # Preventive taps
    And the tap of PstRangeAction "pstFr12" should be 16 in preventive
    And the tap of PstRangeAction "pstFr23" should be 9 in preventive
    And the tap of PstRangeAction "pstFr34" should be 16 in preventive
    # Auto taps
    And the tap of PstRangeAction "pstFr34" should be 6 after "Contingency FR 34" at "auto"
    # Curative taps
    And the tap of PstRangeAction "pstFr12" should be 16 after "Contingency FR 12" at "curative"
    And the tap of PstRangeAction "pstFr12" should be 16 after "Contingency FR 23" at "curative"
    And the tap of PstRangeAction "pstFr12" should be 16 after "Contingency FR 34" at "curative"
    And the tap of PstRangeAction "pstFr23" should be 9 after "Contingency FR 12" at "curative"
    And the tap of PstRangeAction "pstFr23" should be 9 after "Contingency FR 23" at "curative"
    And the tap of PstRangeAction "pstFr23" should be 9 after "Contingency FR 34" at "curative"
    And the tap of PstRangeAction "pstFr34" should be 16 after "Contingency FR 12" at "curative"
    And the tap of PstRangeAction "pstFr34" should be 16 after "Contingency FR 23" at "curative"
    And the tap of PstRangeAction "pstFr34" should be 9 after "Contingency FR 34" at "curative"

  @ac @fast @rao @pst-regulation
  Scenario: US 91.13.3.b: Duplicate of US 91.13.3.a with PST regulation
  PST regulation is performed on all three PSTs but the behavior varies from one to another:
  - pstFr12: the PST is in abutment so it cannot be moved
  - pstFr23: the PST is only available in preventive so it cannot be regulated
  - pstFr34: the PST is in abutment so it cannot be moved, except after "Contingency FR 34" thanks to the automaton
  The curative margin on FR3-FR4 is reduced because of regulation.
    Given network file is "epic91/4NodesSeries.uct"
    Given crac file is "epic91/crac-91-13-3.json"
    Given configuration file is "epic91/RaoParameters_ac_3pstsRegulation.json"
    When I launch search_tree_rao
    Then the worst margin is 7.13 A on cnec "cnecFr34PstAuto"
    And the margin on cnec "cnecFr34Curative" after CRA should be 513.72 A
    # Preventive taps
    And the tap of PstRangeAction "pstFr12" should be 16 in preventive
    And the tap of PstRangeAction "pstFr23" should be 9 in preventive
    And the tap of PstRangeAction "pstFr34" should be 16 in preventive
    # Auto taps
    And the tap of PstRangeAction "pstFr34" should be 6 after "Contingency FR 34" at "auto"
    # Curative taps
    And the tap of PstRangeAction "pstFr12" should be 16 after "Contingency FR 12" at "curative"
    And the tap of PstRangeAction "pstFr12" should be 16 after "Contingency FR 23" at "curative"
    And the tap of PstRangeAction "pstFr12" should be 16 after "Contingency FR 34" at "curative"
    And the tap of PstRangeAction "pstFr23" should be 9 after "Contingency FR 12" at "curative"
    And the tap of PstRangeAction "pstFr23" should be 9 after "Contingency FR 23" at "curative"
    And the tap of PstRangeAction "pstFr23" should be 9 after "Contingency FR 34" at "curative"
    And the tap of PstRangeAction "pstFr34" should be 16 after "Contingency FR 12" at "curative"
    And the tap of PstRangeAction "pstFr34" should be 16 after "Contingency FR 23" at "curative"
    And the tap of PstRangeAction "pstFr34" should be 3 after "Contingency FR 34" at "curative"

  @ac @fast @rao @pst-regulation
  Scenario: US 91.13.4: Regulation with two equivalent parallel PSTs
    Both PSTs are put in abutment because of their symmetric behaviors.
    Given network file is "epic91/2Nodes3ParallelLines2PSTs.uct"
    Given crac file is "epic91/crac-91-13-4.json"
    Given configuration file is "epic91/RaoParameters_ac_2pstsRegulation.json"
    When I launch search_tree_rao
    And the tap of PstRangeAction "pstBeFr2" should be 16 after "Contingency BE1 FR1 1" at "curative"
    And the tap of PstRangeAction "pstBeFr3" should be 16 after "Contingency BE1 FR1 1" at "curative"
