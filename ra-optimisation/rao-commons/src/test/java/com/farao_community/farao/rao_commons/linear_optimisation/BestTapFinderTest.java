/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class BestTapFinderTest {

    private Network network;
    private Crac crac;
    private PstRangeAction pstRangeAction;
    private SystematicSensitivityResult systematicSensitivityResult;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.createWithPreventivePstRange();

        pstRangeAction = (PstRangeAction) crac.getRangeAction("pst");

        MPVariable mockVariable = Mockito.mock(MPVariable.class);
        LinearProblem mockLp = Mockito.mock(LinearProblem.class);
        Mockito.when(mockLp.getRangeActionSetPointVariable(pstRangeAction)).thenReturn(mockVariable);

        systematicSensitivityResult = getMockSensiResult(crac);

        Mockito.when(systematicSensitivityResult.getReferenceFlow(crac.getBranchCnec("cnec1basecase"))).thenReturn(3000.);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(pstRangeAction, crac.getBranchCnec("cnec1basecase"))).thenReturn(-250.);
        Mockito.when(mockVariable.solutionValue()).thenReturn(6.);
    }

    @Test
    public void testComputeBestTapPerPstGroup() {
        PstRangeAction pst1 = crac.newPstRangeAction().withId("pst1").withNetworkElement("ne1")
                .withInitialTap(1).withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
                .add();
        PstRangeAction pst2 = crac.newPstRangeAction().withId("pst2").withNetworkElement("ne2").withGroupId("group1")
                .withInitialTap(1).withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
                .add();
        PstRangeAction pst3 = crac.newPstRangeAction().withId("pst3").withNetworkElement("ne3").withGroupId("group1")
                .withInitialTap(1).withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
                .add();
        PstRangeAction pst4 = crac.newPstRangeAction().withId("pst4").withNetworkElement("ne4").withGroupId("group2")
                .withInitialTap(1).withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
                .add();
        PstRangeAction pst5 = crac.newPstRangeAction().withId("pst5").withNetworkElement("ne5").withGroupId("group2")
                .withInitialTap(1).withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
                .add();
        PstRangeAction pst6 = crac.newPstRangeAction().withId("pst6").withNetworkElement("ne6").withGroupId("group2")
                .withInitialTap(1).withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
                .add();
        PstRangeAction pst7 = crac.newPstRangeAction().withId("pst7").withNetworkElement("ne7").withGroupId("group2")
                .withInitialTap(1).withTapToAngleConversionMap(Map.of(1, 1., 2, 2.))
                .add();

        Map<PstRangeAction, Map<Integer, Double>> minMarginPerTap = new HashMap<>();
        minMarginPerTap.put(pst1, Map.of(3, 100., 4, 500.));

        minMarginPerTap.put(pst2, Map.of(3, 100., 4, 500.));
        minMarginPerTap.put(pst3, Map.of(3, 110., 4, 50.));

        minMarginPerTap.put(pst4, Map.of(-10, -30., -11, -80.));
        minMarginPerTap.put(pst5, Map.of(-10, -40., -11, -20.));
        minMarginPerTap.put(pst6, Map.of(-10, -70., -11, 200.));
        minMarginPerTap.put(pst7, Map.of(-11, Double.MAX_VALUE));

        Map<String, Integer> bestTapPerPstGroup = BestTapFinder.computeBestTapPerPstGroup(minMarginPerTap);
        assertEquals(2, bestTapPerPstGroup.size());
        assertEquals(3, bestTapPerPstGroup.get("group1").intValue());
        assertEquals(-10, bestTapPerPstGroup.get("group2").intValue());
    }

    private SystematicSensitivityResult getMockSensiResult(Crac crac) {
        SystematicSensitivityResult sensisResults = Mockito.mock(SystematicSensitivityResult.class);

        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec1basecase"))).thenReturn(100.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec1stateCurativeContingency1"))).thenReturn(200.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec1stateCurativeContingency2"))).thenReturn(300.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec2basecase"))).thenReturn(-400.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec2stateCurativeContingency1"))).thenReturn(-500.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec2stateCurativeContingency2"))).thenReturn(-600.);

        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec1basecase"))).thenReturn(10.);
        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec1stateCurativeContingency1"))).thenReturn(-20.);
        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec1stateCurativeContingency2"))).thenReturn(30.);
        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec2basecase"))).thenReturn(-40.);
        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec2stateCurativeContingency1"))).thenReturn(50.);
        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec2stateCurativeContingency2"))).thenReturn(-60.);

        return sensisResults;
    }

    private void assertTaps(double setPoint, int expectedTap) {
        Map<PstRangeAction, Integer> bestTaps = BestTapFinder.find(network,
                List.of(crac.getBranchCnec("cnec1basecase"), crac.getBranchCnec("cnec2basecase")),
                Map.of(pstRangeAction, setPoint),
                systematicSensitivityResult);
        assertEquals(1, bestTaps.size());
        assertEquals(expectedTap, bestTaps.get(pstRangeAction).intValue());
    }

    @Test
    public void testComputeBestTapsInTheMiddleOfTheRange() {
        assertTaps(4., 10);
    }

    @Test
    public void testComputeBestTapsHittingHighRange() {
        assertTaps(6.2, 16);
    }

    @Test
    public void testComputeBestTapsHittingLowRange() {
        assertTaps(-6.2, -16);
    }

    @Test
    public void testComputeBestTapsWithGroup() {
        assertTaps(6., 16);
    }

}
