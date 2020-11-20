/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.*;
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
/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
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
        crac.getCnec("cnec1basecase").addExtension(CnecLoopFlowExtension.class, Mockito.mock(CnecLoopFlowExtension.class));
        crac.getCnec("cnec2basecase").addExtension(CnecLoopFlowExtension.class, Mockito.mock(CnecLoopFlowExtension.class));
        raoData = RaoData.createOnPreventiveState(network, crac);
        initialVariantId  = raoData.getWorkingVariantId();
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
        Assert.assertEquals(0, rangeActionResult.getSetPoint("none-initial"), 0.1);
        Assert.assertEquals(Integer.valueOf(0), ((PstRangeResult) rangeActionResult).getTap("none-initial"));
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
        Set<Country> loopflowCountries = raoData.getLoopflowCountries();
        assertEquals(0, loopflowCountries.size());

        Set<Cnec> loopflowCnecs = raoData.getLoopflowCnecs();
        assertEquals(2, loopflowCnecs.size());
    }

    @Test
    public void loopflowSingleCountry() {
        Set<Country> countrySet = new HashSet<>();
        countrySet.add(Country.DE);
        raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, countrySet);

        Set<Country> loopflowCountries = raoData.getLoopflowCountries();
        assertEquals(1, loopflowCountries.size());

        Set<Cnec> loopflowCnecs = raoData.getLoopflowCnecs();
        assertEquals(1, loopflowCnecs.size());
    }
}
