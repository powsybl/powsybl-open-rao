/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.iidm.network.Network;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import static org.junit.Assert.*;
/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SystematicSensitivityAnalysisService.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
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
        crac = CommonCracCreation.create();
        NetworkElement pstElement = new NetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name");
        PstRange pstRange = new PstWithRange("RA PST BE", pstElement);
        ((SimpleCrac) crac).addRangeAction(pstRange);
        crac.synchronize(network);
        raoData = new RaoData(network, crac);
        initialVariantId  = raoData.getWorkingVariantId();
    }

    @Test
    public void variantIdsInitializationTest() {
        assertEquals(1, raoData.getVariantIds().size());
        assertNotNull(crac.getExtension(CracResultExtension.class));
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        Assert.assertEquals(1, resultVariantManager.getVariants().size());
        Assert.assertEquals(initialVariantId, resultVariantManager.getVariants().iterator().next());
        assertEquals(1, network.getVariantManager().getVariantIds().size());
    }

    @Test
    public void rangeActionsInitializationTest() {
        RangeActionResult rangeActionResult = crac.getRangeAction("RA PST BE").getExtension(RangeActionResultExtension.class).getVariant(initialVariantId);
        raoData.getRaoDataManager().fillRangeActionResultsWithNetworkValues();
        Assert.assertEquals(0, rangeActionResult.getSetPoint("none-initial"), 0.1);
        Assert.assertEquals(0, ((PstRangeResult) rangeActionResult).getTap("none-initial"));
    }

    @Test
    public void differentIdsForTwoClonedVariants() {
        String clonedVariantId1 = raoData.cloneWorkingVariant();
        String clonedVariantId2 = raoData.cloneWorkingVariant();
        assertEquals(3, raoData.getVariantIds().size());

        assertNotEquals(initialVariantId, clonedVariantId1);
        assertNotEquals(clonedVariantId1, clonedVariantId2);
    }

    @Test
    public void deleteVariantWithRemovingCracVariant() {
        String clonedVariantId = raoData.cloneWorkingVariant();
        assertEquals(initialVariantId, raoData.getWorkingVariantId());
        assertEquals(2, raoData.getVariantIds().size());
        assertEquals(1, raoData.getNetwork().getVariantManager().getVariantIds().size());
        Assert.assertEquals(2, raoData.getCrac().getExtension(ResultVariantManager.class).getVariants().size());

        raoData.deleteVariant(clonedVariantId, false);

        assertEquals(1, raoData.getNetwork().getVariantManager().getVariantIds().size());
        Assert.assertEquals(1, raoData.getCrac().getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(1, raoData.getVariantIds().size());
    }

    @Test
    public void deleteVariantWithKeepingCracVariant() {
        String clonedVariantId = raoData.cloneWorkingVariant();
        assertEquals(initialVariantId, raoData.getWorkingVariantId());
        assertEquals(2, raoData.getVariantIds().size());
        assertEquals(1, raoData.getNetwork().getVariantManager().getVariantIds().size());
        Assert.assertEquals(2, raoData.getCrac().getExtension(ResultVariantManager.class).getVariants().size());

        raoData.deleteVariant(clonedVariantId, true);

        assertEquals(1, raoData.getNetwork().getVariantManager().getVariantIds().size());
        Assert.assertEquals(2, raoData.getCrac().getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(1, raoData.getVariantIds().size());
    }

    @Test
    public void clearVariants() {
        raoData.cloneWorkingVariant();
        raoData.cloneWorkingVariant();
        assertEquals(3, raoData.getVariantIds().size());
        raoData.clear();
        assertEquals(initialNetworkVariantId, network.getVariantManager().getWorkingVariantId());
        Assert.assertEquals(0, crac.getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(0, raoData.getVariantIds().size());
    }

    @Test
    public void clearVariantsWithKeepingCracVariant() {
        String keptVariantId = raoData.cloneWorkingVariant();
        raoData.cloneWorkingVariant();
        assertEquals(3, raoData.getVariantIds().size());
        raoData.clearWithKeepingCracResults(Collections.singletonList(keptVariantId));
        assertEquals(initialNetworkVariantId, network.getVariantManager().getWorkingVariantId());
        Assert.assertEquals(1, crac.getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(0, raoData.getVariantIds().size());
    }

    @Test
    public void sameRasTest() {
        RaoData raoData = new RaoData(network, crac);
        String initialVariantId = raoData.getWorkingVariantId();
        String sameVariantId = raoData.cloneWorkingVariant();
        String differentVariantId = raoData.cloneWorkingVariant();

        RangeActionResultExtension rangeActionResultExtension = crac.getRangeActions().iterator().next().getExtension(RangeActionResultExtension.class);
        RangeActionResult pstResult1 = rangeActionResultExtension.getVariant(initialVariantId);
        RangeActionResult pstResult2 = rangeActionResultExtension.getVariant(sameVariantId);
        RangeActionResult pstResult3 = rangeActionResultExtension.getVariant(differentVariantId);

        String prevStateId = crac.getPreventiveState().getId();
        pstResult1.setSetPoint(prevStateId, 3);
        pstResult2.setSetPoint(prevStateId, 3);
        pstResult3.setSetPoint(prevStateId, 2);

        assertTrue(raoData.getRaoDataManager().sameRemedialActions(initialVariantId, sameVariantId));
        assertTrue(raoData.getRaoDataManager().sameRemedialActions(sameVariantId, initialVariantId));
        assertFalse(raoData.getRaoDataManager().sameRemedialActions(initialVariantId, differentVariantId));
    }
}
