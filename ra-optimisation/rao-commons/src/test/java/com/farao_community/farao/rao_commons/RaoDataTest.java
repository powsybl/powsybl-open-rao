/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *//*


package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
*/
/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 *//*

public class RaoDataTest {

    private Network network;
    private String initialNetworkVariantId;
    private Crac crac;
    private RaoData raoData;
    private String initialVariantId;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        initialNetworkVariantId = network.getVariantManager().getWorkingVariantId();
        crac = CommonCracCreation.createWithPstRange();
        crac.synchronize(network);
        crac.getBranchCnec("cnec1basecase").addExtension(CnecLoopFlowExtension.class, Mockito.mock(CnecLoopFlowExtension.class));
        crac.getBranchCnec("cnec2basecase").addExtension(CnecLoopFlowExtension.class, Mockito.mock(CnecLoopFlowExtension.class));
        raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, new RaoParameters());
        initialVariantId = raoData.getWorkingVariantId();
    }

    @Test
    public void testNoPerimeter() {
        RaoData raoData = new RaoData(network, crac, crac.getPreventiveState(), null, null, null, null, new RaoParameters());
        assertEquals(crac.getBranchCnecs().size(), raoData.getCnecs().size());
    }

    @Test
    public void variantIdsInitializationTest() {
        assertEquals(1, raoData.getCracVariantManager().getVariantIds().size());
        assertNotNull(crac.getExtension(CracResultExtension.class));
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        Assert.assertEquals(1, resultVariantManager.getVariants().size());
        Assert.assertEquals(initialVariantId, resultVariantManager.getVariants().iterator().next());
        assertEquals(1, network.getVariantManager().getVariantIds().size());
    }

    @Test
    public void rangeActionsInitializationTest() {
        RangeActionResult rangeActionResult = crac.getRangeAction("pst").getExtension(RangeActionResultExtension.class).getVariant(initialVariantId);
        raoData.getCracResultManager().fillRangeActionResultsWithNetworkValues();
        Assert.assertEquals(0, rangeActionResult.getSetPoint("preventive"), 0.1);
        Assert.assertEquals(Integer.valueOf(0), ((PstRangeResult) rangeActionResult).getTap("preventive"));
    }

    @Test
    public void curativeRangeActionsInitializationTest() {
        Crac crac1 = CommonCracCreation.createWithCurativePstRange();
        crac1.synchronize(network);
        RaoData raoData1 = new RaoData(network, crac1, crac1.getPreventiveState(), Collections.singleton(crac1.getPreventiveState()), null, null, null, new RaoParameters());
        raoData1.getCracResultManager().fillRangeActionResultsWithNetworkValues();
        RangeActionResult rangeActionResult = crac1.getRangeAction("pst").getExtension(RangeActionResultExtension.class).getVariant(raoData1.getWorkingVariantId());
        assertNotNull(rangeActionResult);
        Assert.assertEquals(0, rangeActionResult.getSetPoint("preventive"), 0.1);
        Assert.assertEquals(Integer.valueOf(0), ((PstRangeResult) rangeActionResult).getTap("preventive"));
    }

    @Test
    public void differentIdsForTwoClonedVariants() {
        String clonedVariantId1 = raoData.getCracVariantManager().cloneWorkingVariant();
        String clonedVariantId2 = raoData.getCracVariantManager().cloneWorkingVariant();
        assertEquals(3, raoData.getCracVariantManager().getVariantIds().size());

        assertNotEquals(initialVariantId, clonedVariantId1);
        assertNotEquals(clonedVariantId1, clonedVariantId2);
    }

    @Test
    public void deleteVariantWithRemovingCracVariant() {
        String clonedVariantId = raoData.getCracVariantManager().cloneWorkingVariant();
        assertEquals(initialVariantId, raoData.getWorkingVariantId());
        assertEquals(2, raoData.getCracVariantManager().getVariantIds().size());
        assertEquals(1, raoData.getNetwork().getVariantManager().getVariantIds().size());
        Assert.assertEquals(2, raoData.getCrac().getExtension(ResultVariantManager.class).getVariants().size());

        raoData.getCracVariantManager().deleteVariant(clonedVariantId, false);

        assertEquals(1, raoData.getNetwork().getVariantManager().getVariantIds().size());
        Assert.assertEquals(1, raoData.getCrac().getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(1, raoData.getCracVariantManager().getVariantIds().size());
    }

    @Test
    public void deleteVariantWithKeepingCracVariant() {
        String clonedVariantId = raoData.getCracVariantManager().cloneWorkingVariant();
        assertEquals(initialVariantId, raoData.getWorkingVariantId());
        assertEquals(2, raoData.getCracVariantManager().getVariantIds().size());
        assertEquals(1, raoData.getNetwork().getVariantManager().getVariantIds().size());
        Assert.assertEquals(2, raoData.getCrac().getExtension(ResultVariantManager.class).getVariants().size());

        raoData.getCracVariantManager().deleteVariant(clonedVariantId, true);

        assertEquals(1, raoData.getNetwork().getVariantManager().getVariantIds().size());
        Assert.assertEquals(2, raoData.getCrac().getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(1, raoData.getCracVariantManager().getVariantIds().size());
    }

    @Test
    public void clearVariants() {
        raoData.getCracVariantManager().cloneWorkingVariant();
        raoData.getCracVariantManager().cloneWorkingVariant();
        assertEquals(3, raoData.getCracVariantManager().getVariantIds().size());
        raoData.getCracVariantManager().clear();
        assertEquals(initialNetworkVariantId, network.getVariantManager().getWorkingVariantId());
        Assert.assertEquals(0, crac.getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(0, raoData.getCracVariantManager().getVariantIds().size());
    }

    @Test
    public void clearVariantsWithKeepingCracVariant() {
        String keptVariantId = raoData.getCracVariantManager().cloneWorkingVariant();
        raoData.getCracVariantManager().cloneWorkingVariant();
        assertEquals(3, raoData.getCracVariantManager().getVariantIds().size());
        raoData.getCracVariantManager().clearWithKeepingCracResults(Collections.singletonList(keptVariantId));
        assertEquals(initialNetworkVariantId, network.getVariantManager().getWorkingVariantId());
        Assert.assertEquals(1, crac.getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(0, raoData.getCracVariantManager().getVariantIds().size());
    }

    @Test
    public void sameRasTest() {
        String initialVariantId = raoData.getWorkingVariantId();
        String sameVariantId = raoData.getCracVariantManager().cloneWorkingVariant();
        String differentVariantId = raoData.getCracVariantManager().cloneWorkingVariant();

        RangeActionResultExtension rangeActionResultExtension = crac.getRangeActions().iterator().next().getExtension(RangeActionResultExtension.class);
        RangeActionResult pstResult1 = rangeActionResultExtension.getVariant(initialVariantId);
        RangeActionResult pstResult2 = rangeActionResultExtension.getVariant(sameVariantId);
        RangeActionResult pstResult3 = rangeActionResultExtension.getVariant(differentVariantId);

        String prevStateId = crac.getPreventiveState().getId();
        pstResult1.setSetPoint(prevStateId, 3);
        pstResult2.setSetPoint(prevStateId, 3);
        pstResult3.setSetPoint(prevStateId, 2);

        assertTrue(raoData.getCracResultManager().sameRemedialActions(initialVariantId, sameVariantId));
        assertTrue(raoData.getCracResultManager().sameRemedialActions(sameVariantId, initialVariantId));
        assertFalse(raoData.getCracResultManager().sameRemedialActions(initialVariantId, differentVariantId));
    }

    @Test
    public void loopflowCountries() {
        Set<Country> loopflowCountries = raoData.getRaoParameters().getLoopflowCountries();
        assertEquals(0, loopflowCountries.size());

        Set<BranchCnec> loopflowCnecs = raoData.getLoopflowCnecs();
        assertEquals(2, loopflowCnecs.size());
    }

    @Test
    public void loopflowSingleCountry() {
        RaoParameters raoParameters = new RaoParameters();
        Set<Country> countrySet = new HashSet<>();
        countrySet.add(Country.DE);
        raoParameters.setLoopflowCountries(countrySet);

        raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, raoParameters);

        Set<Country> loopflowCountries = raoData.getRaoParameters().getLoopflowCountries();
        assertEquals(1, loopflowCountries.size());

        Set<BranchCnec> loopflowCnecs = raoData.getLoopflowCnecs();
        assertEquals(1, loopflowCnecs.size());
    }

    @Test
    public void testCreateFromExistingVariant() {
        Crac crac1 = CommonCracCreation.createWithCurativePstRange();
        crac1.synchronize(network);
        RaoData preventiveRaoData = new RaoData(network, crac1, crac1.getPreventiveState(), Collections.singleton(crac1.getPreventiveState()), null, null, null, new RaoParameters());
        String variantId = preventiveRaoData.getPreOptimVariantId();
        Contingency contingency = crac.getContingency("Contingency FR1 FR3");
        RaoData curativeRaoData = new RaoData(
                network,
                crac1,
                crac1.getState(contingency, Instant.CURATIVE),
                Collections.singleton(crac1.getState(contingency, Instant.CURATIVE)),
                null,
                null,
                variantId,
                new RaoParameters());
        RangeActionResult rangeActionResult = curativeRaoData.getCrac().getRangeAction("pst").getExtension(RangeActionResultExtension.class).getVariant(curativeRaoData.getWorkingVariantId());
        assertNotNull(rangeActionResult);
        Assert.assertEquals(0, rangeActionResult.getSetPoint(crac.getPreventiveState().getId()), 0.1);
        Assert.assertEquals(0, rangeActionResult.getSetPoint(crac.getState(contingency, Instant.CURATIVE).getId()), 0.1);
        Assert.assertEquals(Integer.valueOf(0), ((PstRangeResult) rangeActionResult).getTap(crac.getPreventiveState().getId()));
        Assert.assertEquals(Integer.valueOf(0), ((PstRangeResult) rangeActionResult).getTap(crac.getState(contingency, Instant.CURATIVE).getId()));
    }
}
*/
