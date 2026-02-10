# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 91.13: PST Regulation

  @ac @fast @rao @max-min-margin @ampere
  Scenario: US 91.13.1.a: Unsecure case with a globally optimized tap position - PST is the limiting element
  The curative scenario cannot be secured, the curative tap is chosen such that both curative CNECs are overloaded but
  with the lowest global overload reachable. The PST is the most limiting element at the end of curative.
    Given network file is "epic91/2Nodes3ParallelLinesPST.uct"
    Given crac file is "epic91/crac-91-13-1.json"
    Given configuration file is "epic91/RaoParameters_ac.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    And the worst margin is -690.23 A
    And the tap of PstRangeAction "pstBeFr2" should be -16 in preventive
    And the margin on cnec "cnecBeFr1Preventive" after PRA should be 579.65 A
    And the tap of PstRangeAction "pstBeFr2" should be -2 after "Contingency BE1 FR1 3" at "curative"
    And the value of the objective function after CRA should be 690.23
    And the worst margin is -690.23 A
    And the value of the objective function after CRA should be 690.23
    And the margin on cnec "cnecBeFr2Curative" after CRA should be -690.23 A
    And the margin on cnec "cnecBeFr1Curative" after CRA should be -676.38 A

  @ac @fast @rao @pst-regulation @max-min-margin @ampere
  Scenario: US 91.13.1.b: Duplicate of US 91.13.1.a with PST regulation
  At the end of curative optimization, the PST is still unsecure but can be secured by moving the tap to position 7
  even if it worsens the minimum margin significantly. Regulation can be performed because the PST is the most
  limiting element.
    Given network file is "epic91/2Nodes3ParallelLinesPST.uct"
    Given crac file is "epic91/crac-91-13-1.json"
    Given configuration file is "epic91/RaoParameters_ac_pstRegulation.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    And the worst margin is -1382.77 A
    And the tap of PstRangeAction "pstBeFr2" should be -16 in preventive
    And the margin on cnec "cnecBeFr1Preventive" after PRA should be 579.65 A
    And the tap of PstRangeAction "pstBeFr2" should be 7 after "Contingency BE1 FR1 3" at "curative"
    And the value of the objective function after CRA should be 1382.77
    And the worst margin is -1382.77 A
    And the value of the objective function after CRA should be 1382.77
    And the margin on cnec "cnecBeFr1Curative" after CRA should be -1382.77 A
    And the margin on cnec "cnecBeFr2Curative" after CRA should be 15.49 A

  @ac @fast @rao @max-min-margin @ampere
  Scenario: US 91.13.2.a: Unsecure case with a globally optimized tap position - PST not is the limiting element
  The curative scenario cannot be secured, the curative tap is chosen such that both curative CNECs are overloaded but
  with the lowest global overload reachable. The PST is not the most limiting element at the end of curative.
    Given network file is "epic91/2Nodes3ParallelLinesPST.uct"
    Given crac file is "epic91/crac-91-13-2.json"
    Given configuration file is "epic91/RaoParameters_ac.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    And the worst margin is -247.23 A
    And the tap of PstRangeAction "pstBeFr2" should be -16 in preventive
    And the margin on cnec "cnecBeFr1Preventive" after PRA should be 1479.65 A
    And the tap of PstRangeAction "pstBeFr2" should be 4 after "Contingency BE1 FR1 3" at "curative"
    And the value of the objective function after CRA should be 247.23
    And the worst margin is -247.23 A
    And the value of the objective function after CRA should be 247.23
    And the margin on cnec "cnecBeFr1Curative" after CRA should be -247.23 A
    And the margin on cnec "cnecBeFr2Curative" after CRA should be -219.56 A

  @ac @fast @rao @pst-regulation @max-min-margin @ampere
  Scenario: US 91.13.2.b: Duplicate of US 91.13.2.a with PST regulation
  At the end of curative optimization, the PST is still unsecure but can be secured by moving the tap to position 7
  even if it worsens the minimum margin significantly. Regulation cannot be performed because the PST is not the most
  limiting element.
    Given network file is "epic91/2Nodes3ParallelLinesPST.uct"
    Given crac file is "epic91/crac-91-13-2.json"
    Given configuration file is "epic91/RaoParameters_ac_pstRegulation.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    And the worst margin is -247.23 A
    And the tap of PstRangeAction "pstBeFr2" should be -16 in preventive
    And the margin on cnec "cnecBeFr1Preventive" after PRA should be 1479.65 A
    And the tap of PstRangeAction "pstBeFr2" should be 4 after "Contingency BE1 FR1 3" at "curative"
    And the value of the objective function after CRA should be 247.23
    And the worst margin is -247.23 A
    And the value of the objective function after CRA should be 247.23
    And the margin on cnec "cnecBeFr1Curative" after CRA should be -247.23 A
    And the margin on cnec "cnecBeFr2Curative" after CRA should be -219.56 A

  @ac @fast @rao @max-min-margin @ampere
  Scenario: US 91.13.3.a: 3 contingency scenarios and 3 PSTs with different use cases
  - Scenario 12: PST 34 has a negative margin of -444.08 A so even though PST 12 should go down to tap position -16 to
  maximize its own margin because the flow decreases with the tap position, the minimal margin does not improve below
  tap position -10 (margin at tap position -9 is about -462 A)
  - Scenario 23: globally optimized tap between preventive CNEC and curative PST leading the preventive tap of PST 23 to
  be -6
  - Scenario 34: automaton secures PST 34 straight from auto instant at tap -15
  Each curative scenario is unsecure with some PSTs overloaded.
    Given network file is "epic91/4NodesSeries.uct"
    Given crac file is "epic91/crac-91-13-3.json"
    Given configuration file is "epic91/RaoParameters_ac.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    And the worst margin is -758.18 A
    # Preventive taps
    And the tap of PstRangeAction "pstFr12" should be 0 in preventive
    And the tap of PstRangeAction "pstFr23" should be -6 in preventive
    And the tap of PstRangeAction "pstFr34" should be 0 in preventive
    # Auto taps
    And the tap of PstRangeAction "pstFr34" should be -15 after "Contingency FR 34" at "auto"
    # Curative taps
    And the tap of PstRangeAction "pstFr12" should be -10 after "Contingency FR 12" at "curative"
    And the tap of PstRangeAction "pstFr12" should be 0 after "Contingency FR 23" at "curative"
    And the tap of PstRangeAction "pstFr12" should be 0 after "Contingency FR 34" at "curative"
    And the tap of PstRangeAction "pstFr23" should be -6 after "Contingency FR 12" at "curative"
    And the tap of PstRangeAction "pstFr23" should be -6 after "Contingency FR 23" at "curative"
    And the tap of PstRangeAction "pstFr23" should be -6 after "Contingency FR 34" at "curative"
    And the tap of PstRangeAction "pstFr34" should be 0 after "Contingency FR 12" at "curative"
    And the tap of PstRangeAction "pstFr34" should be 0 after "Contingency FR 23" at "curative"
    And the tap of PstRangeAction "pstFr34" should be -15 after "Contingency FR 34" at "curative"
    # Curative margins
    And the margin on cnec "cnecFr12PstCurative - Co12" after CRA should be -384.40 A
    And the margin on cnec "cnecFr12PstCurative - Co23" after CRA should be -444.08 A
    And the margin on cnec "cnecFr12PstCurative - Co34" after CRA should be -444.08 A
    And the margin on cnec "cnecFr23PstCurative - Co12" after CRA should be 184.55 A
    And the margin on cnec "cnecFr23PstCurative - Co23" after CRA should be -697.26 A
    And the margin on cnec "cnecFr23PstCurative - Co34" after CRA should be 184.55 A
    And the margin on cnec "cnecFr34PstCurative - Co12" after CRA should be -444.08 A
    And the margin on cnec "cnecFr34PstCurative - Co23" after CRA should be -444.08 A
    And the margin on cnec "cnecFr34PstCurative - Co34" after CRA should be 5.78 A
    # Overall
    And the value of the objective function after CRA should be 758.18
    And the worst margin is -758.18 A

  @ac @fast @rao @pst-regulation @max-min-margin @ampere
  Scenario: US 91.13.3.b: Duplicate of US 91.13.3.a with PST regulation
  Regulation is performed on PSTs 12 and 34 because they can be moved in curative.
  The regulation tap is either -15 if the contingency was parallel to the PST or -5 otherwise.
    Given network file is "epic91/4NodesSeries.uct"
    Given crac file is "epic91/crac-91-13-3.json"
    Given configuration file is "epic91/RaoParameters_ac_3pstsRegulation.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    And the worst margin is -758.18 A
    # Preventive taps
    And the tap of PstRangeAction "pstFr12" should be 0 in preventive
    And the tap of PstRangeAction "pstFr23" should be -6 in preventive
    And the tap of PstRangeAction "pstFr34" should be 0 in preventive
    # Auto taps
    And the tap of PstRangeAction "pstFr34" should be -15 after "Contingency FR 34" at "auto"
    # Curative taps
    And the tap of PstRangeAction "pstFr12" should be -15 after "Contingency FR 12" at "curative"
    And the tap of PstRangeAction "pstFr12" should be -5 after "Contingency FR 23" at "curative"
    And the tap of PstRangeAction "pstFr12" should be -5 after "Contingency FR 34" at "curative"
    And the tap of PstRangeAction "pstFr23" should be -6 after "Contingency FR 12" at "curative"
    And the tap of PstRangeAction "pstFr23" should be -6 after "Contingency FR 23" at "curative"
    And the tap of PstRangeAction "pstFr23" should be -6 after "Contingency FR 34" at "curative"
    And the tap of PstRangeAction "pstFr34" should be -5 after "Contingency FR 12" at "curative"
    And the tap of PstRangeAction "pstFr34" should be -5 after "Contingency FR 23" at "curative"
    And the tap of PstRangeAction "pstFr34" should be -15 after "Contingency FR 34" at "curative"
    # Curative margins
    And the margin on cnec "cnecFr12PstCurative - Co12" after CRA should be 5.78 A
    And the margin on cnec "cnecFr12PstCurative - Co23" after CRA should be 78.98 A
    And the margin on cnec "cnecFr12PstCurative - Co34" after CRA should be 78.98 A
    And the margin on cnec "cnecFr23PstCurative - Co12" after CRA should be 184.55 A
    And the margin on cnec "cnecFr23PstCurative - Co23" after CRA should be -697.26 A
    And the margin on cnec "cnecFr23PstCurative - Co34" after CRA should be 184.55 A
    And the margin on cnec "cnecFr34PstCurative - Co12" after CRA should be 78.98 A
    And the margin on cnec "cnecFr34PstCurative - Co23" after CRA should be 78.98 A
    And the margin on cnec "cnecFr34PstCurative - Co34" after CRA should be 5.78 A
    # Overall
    And the value of the objective function after CRA should be 758.18
    And the worst margin is -758.18 A

  @ac @fast @rao @pst-regulation @max-min-margin @ampere
  Scenario: US 91.13.4: Regulation with two equivalent parallel PSTs
  The two PSTs are identical. Status quo is not to move any tap leaving both overloaded at the end of curative
  optimization. PST regulation will put both in abutment at tap 16 since the loadflow regulates tap by only focusing on
  one PST instead of the whole network.
    Given network file is "epic91/2Nodes3ParallelLines2PSTs.uct"
    Given crac file is "epic91/crac-91-13-4.json"
    Given configuration file is "epic91/RaoParameters_ac_2pstsRegulation.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    And the worst margin is -733.27 A
    And the tap of PstRangeAction "pstBeFr2" should be 16 after "Contingency BE1 FR1 1" at "curative"
    And the tap of PstRangeAction "pstBeFr3" should be 16 after "Contingency BE1 FR1 1" at "curative"

  @ac @fast @rao @max-min-margin @ampere
  Scenario: US 91.13.5.a: Unsecure case with a globally optimized tap position - Monitored line in series with PST
  The PST itself is not overloaded but the line connected in series is.
    Given network file is "epic91/3NodesPSTSeries.uct"
    Given crac file is "epic91/crac-91-13-5.json"
    Given configuration file is "epic91/RaoParameters_ac.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    And the worst margin is -579.06 A
    And the tap of PstRangeAction "pstFr1Fr2" should be -16 in preventive
    And the margin on cnec "cnecFr1Fr3Preventive" after PRA should be 346.79 A
    And the tap of PstRangeAction "pstFr1Fr2" should be -7 after "Contingency FR1 FR3 3" at "curative"
    And the value of the objective function after CRA should be 579.06
    And the worst margin is -579.06 A
    And the value of the objective function after CRA should be 579.06
    And the margin on cnec "cnecFr2Fr3Curative" after CRA should be -579.06 A
    And the margin on cnec "cnecFr1Fr3Curative" after CRA should be -559.13 A

  @ac @fast @rao @pst-regulation @max-min-margin @ampere
  Scenario: US 91.13.5.b: Duplicate of US 91.13.5.a with PST regulation
  The line in series with the PST is overloaded, triggering PST regulation.
    Given network file is "epic91/3NodesPSTSeries.uct"
    Given crac file is "epic91/crac-91-13-5.json"
    Given configuration file is "epic91/RaoParameters_ac_pstRegulationSeries.json"
    When I launch rao
    Then the execution details should be "The RAO only went through first preventive"
    And the worst margin is -1187.64 A
    And the tap of PstRangeAction "pstFr1Fr2" should be -16 in preventive
    And the margin on cnec "cnecFr1Fr3Preventive" after PRA should be 346.79 A
    And the tap of PstRangeAction "pstFr1Fr2" should be 5 after "Contingency FR1 FR3 3" at "curative"
    And the value of the objective function after CRA should be 1187.64
    And the worst margin is -1187.64 A
    And the value of the objective function after CRA should be 1187.64
    And the margin on cnec "cnecFr1Fr3Curative" after CRA should be -1187.64 A
    And the margin on cnec "cnecFr2Fr3Curative" after CRA should be 48.36 A
