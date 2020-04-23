/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import static org.junit.Assert.*;
/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SystematicSensitivityAnalysisService.class})
public class LinearRaoDataTest {

    private Network network;
    private String initialNetworkVariantId;
    private Crac crac;
    private LinearRaoData linearRaoData;
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
        linearRaoData = new LinearRaoData(network, crac);
        initialVariantId  = linearRaoData.getWorkingVariantId();
    }

    @Test
    public void variantIdsInitializationTest() {
        assertEquals(1, linearRaoData.getVariantIds().size());
        assertNotNull(crac.getExtension(CracResultExtension.class));
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        assertEquals(1, resultVariantManager.getVariants().size());
        assertEquals(initialVariantId, resultVariantManager.getVariants().iterator().next());
        assertEquals(1, network.getVariantManager().getVariantIds().size());
    }

    @Test
    public void rangeActionsInitializationTest() {
        RangeActionResult rangeActionResult = crac.getRangeAction("RA PST BE").getExtension(RangeActionResultExtension.class).getVariant(initialVariantId);
        linearRaoData.fillRangeActionResultsWithNetworkValues();
        assertEquals(0, rangeActionResult.getSetPoint("none-initial"), 0.1);
        assertEquals(0, ((PstRangeResult) rangeActionResult).getTap("none-initial"));
    }

    @Test
    public void differentIdsForTwoClonedVariants() {
        String clonedVariantId1 = linearRaoData.cloneWorkingVariant();
        String clonedVariantId2 = linearRaoData.cloneWorkingVariant();
        assertEquals(3, linearRaoData.getVariantIds().size());

        assertNotEquals(initialVariantId, clonedVariantId1);
        assertNotEquals(clonedVariantId1, clonedVariantId2);
    }

    @Test
    public void deleteVariantWithRemovingCracVariant() {
        String clonedVariantId = linearRaoData.cloneWorkingVariant();
        assertEquals(initialVariantId, linearRaoData.getWorkingVariantId());
        assertEquals(2, linearRaoData.getVariantIds().size());
        assertEquals(1, linearRaoData.getNetwork().getVariantManager().getVariantIds().size());
        assertEquals(2, linearRaoData.getCrac().getExtension(ResultVariantManager.class).getVariants().size());

        linearRaoData.deleteVariant(clonedVariantId, false);

        assertEquals(1, linearRaoData.getNetwork().getVariantManager().getVariantIds().size());
        assertEquals(1, linearRaoData.getCrac().getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(1, linearRaoData.getVariantIds().size());
    }

    @Test
    public void deleteVariantWithKeepingCracVariant() {
        String clonedVariantId = linearRaoData.cloneWorkingVariant();
        assertEquals(initialVariantId, linearRaoData.getWorkingVariantId());
        assertEquals(2, linearRaoData.getVariantIds().size());
        assertEquals(1, linearRaoData.getNetwork().getVariantManager().getVariantIds().size());
        assertEquals(2, linearRaoData.getCrac().getExtension(ResultVariantManager.class).getVariants().size());

        linearRaoData.deleteVariant(clonedVariantId, true);

        assertEquals(1, linearRaoData.getNetwork().getVariantManager().getVariantIds().size());
        assertEquals(2, linearRaoData.getCrac().getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(1, linearRaoData.getVariantIds().size());
    }

    @Test
    public void clearVariants() {
        linearRaoData.cloneWorkingVariant();
        linearRaoData.cloneWorkingVariant();
        assertEquals(3, linearRaoData.getVariantIds().size());
        linearRaoData.clear();
        assertEquals(initialNetworkVariantId, network.getVariantManager().getWorkingVariantId());
        assertEquals(0, crac.getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(0, linearRaoData.getVariantIds().size());
    }

    @Test
    public void clearVariantsWithKeepingCracVariant() {
        String keptVariantId = linearRaoData.cloneWorkingVariant();
        linearRaoData.cloneWorkingVariant();
        assertEquals(3, linearRaoData.getVariantIds().size());
        linearRaoData.clearWithKeepingCracResults(Collections.singletonList(keptVariantId));
        assertEquals(initialNetworkVariantId, network.getVariantManager().getWorkingVariantId());
        assertEquals(1, crac.getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(0, linearRaoData.getVariantIds().size());
    }

    @Test
    public void sameRasTest() {
        LinearRaoData linearRaoData = new LinearRaoData(network, crac);
        String initialVariantId = linearRaoData.getWorkingVariantId();
        String sameVariantId = linearRaoData.cloneWorkingVariant();
        String differentVariantId = linearRaoData.cloneWorkingVariant();

        RangeActionResultExtension rangeActionResultExtension = crac.getRangeActions().iterator().next().getExtension(RangeActionResultExtension.class);
        RangeActionResult pstResult1 = rangeActionResultExtension.getVariant(initialVariantId);
        RangeActionResult pstResult2 = rangeActionResultExtension.getVariant(sameVariantId);
        RangeActionResult pstResult3 = rangeActionResultExtension.getVariant(differentVariantId);

        String prevStateId = crac.getPreventiveState().getId();
        pstResult1.setSetPoint(prevStateId, 3);
        pstResult2.setSetPoint(prevStateId, 3);
        pstResult3.setSetPoint(prevStateId, 2);

        assertTrue(linearRaoData.sameRemedialActions(initialVariantId, sameVariantId));
        assertTrue(linearRaoData.sameRemedialActions(sameVariantId, initialVariantId));
        assertFalse(linearRaoData.sameRemedialActions(initialVariantId, differentVariantId));
    }
}
