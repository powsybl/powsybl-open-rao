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
public class SituationTest {

    private Network network;
    private String initialNetworkVariantId;
    private Crac crac;
    private Situation situation;
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
        situation = new Situation(network, crac);
        initialVariantId  = situation.getWorkingVariantId();
    }

    @Test
    public void variantIdsInitializationTest() {
        assertEquals(1, situation.getVariantIds().size());
        assertNotNull(crac.getExtension(CracResultExtension.class));
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        assertEquals(1, resultVariantManager.getVariants().size());
        assertEquals(initialVariantId, resultVariantManager.getVariants().iterator().next());
        assertEquals(2, network.getVariantManager().getVariantIds().size());
        assertTrue(network.getVariantManager().getVariantIds().contains(initialVariantId));
    }

    @Test
    public void rangeActionsInitializationTest() {
        RangeActionResult rangeActionResult = crac.getRangeAction("RA PST BE").getExtension(RangeActionResultExtension.class).getVariant(initialVariantId);
        situation.fillRangeActionResultsWithNetworkValues();
        assertEquals(0, rangeActionResult.getSetPoint("none-initial"), 0.1);
        assertEquals(0, ((PstRangeResult) rangeActionResult).getTap("none-initial"));
    }

    @Test
    public void differentIdsForTwoClonedVariants() {
        String clonedVariantId1 = situation.cloneVariant(initialVariantId);
        String clonedVariantId2 = situation.cloneVariant(initialVariantId);
        assertEquals(3, situation.getVariantIds().size());

        assertNotEquals(initialVariantId, clonedVariantId1);
        assertNotEquals(clonedVariantId1, clonedVariantId2);
    }

    @Test
    public void deleteVariantWithRemovingCracVariant() {
        String clonedVariantId = situation.cloneVariant(initialVariantId);
        assertEquals(initialVariantId, situation.getWorkingVariantId());
        assertEquals(2, situation.getVariantIds().size());
        assertEquals(3, situation.getNetwork().getVariantManager().getVariantIds().size());
        assertEquals(2, situation.getCrac().getExtension(ResultVariantManager.class).getVariants().size());

        situation.deleteVariant(clonedVariantId, false);

        assertEquals(2, situation.getNetwork().getVariantManager().getVariantIds().size());
        assertEquals(1, situation.getCrac().getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(1, situation.getVariantIds().size());
    }

    @Test
    public void deleteVariantWithKeepingCracVariant() {
        String clonedVariantId = situation.cloneVariant(initialVariantId);
        assertEquals(initialVariantId, situation.getWorkingVariantId());
        assertEquals(2, situation.getVariantIds().size());
        assertEquals(3, situation.getNetwork().getVariantManager().getVariantIds().size());
        assertEquals(2, situation.getCrac().getExtension(ResultVariantManager.class).getVariants().size());

        situation.deleteVariant(clonedVariantId, true);

        assertEquals(2, situation.getNetwork().getVariantManager().getVariantIds().size());
        assertEquals(2, situation.getCrac().getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(1, situation.getVariantIds().size());
    }

    @Test
    public void clearVariants() {
        situation.cloneVariant(initialVariantId);
        situation.cloneVariant(initialVariantId);
        assertEquals(3, situation.getVariantIds().size());
        situation.clear();
        assertEquals(initialNetworkVariantId, network.getVariantManager().getWorkingVariantId());
        assertEquals(0, crac.getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(0, situation.getVariantIds().size());
    }

    @Test
    public void clearVariantsWithKeepingCracVariant() {
        String keptVariantId = situation.cloneVariant(initialVariantId);
        situation.cloneVariant(initialVariantId);
        assertEquals(3, situation.getVariantIds().size());
        situation.clear(Collections.singletonList(keptVariantId));
        assertEquals(initialNetworkVariantId, network.getVariantManager().getWorkingVariantId());
        assertEquals(1, crac.getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(0, situation.getVariantIds().size());
    }

    @Test
    public void createNewVariantAfterClear() {
        situation.cloneVariant(initialVariantId);
        situation.cloneVariant(initialVariantId);
        situation.clear();
        String variantId = situation.createVariant();
        situation.setWorkingVariant(variantId);
        assertEquals(variantId, situation.getNetwork().getVariantManager().getWorkingVariantId());
        assertEquals(1, crac.getExtension(ResultVariantManager.class).getVariants().size());
        assertEquals(1, situation.getVariantIds().size());
    }

    @Test
    public void sameRasTest() {
        Situation situation = new Situation(network, crac);
        String initialVariantId = situation.getWorkingVariantId();
        String sameVariantId = situation.cloneVariant(initialVariantId);
        String differentVariantId = situation.cloneVariant(initialVariantId);

        RangeActionResultExtension rangeActionResultExtension = crac.getRangeActions().iterator().next().getExtension(RangeActionResultExtension.class);
        RangeActionResult pstResult1 = rangeActionResultExtension.getVariant(initialVariantId);
        RangeActionResult pstResult2 = rangeActionResultExtension.getVariant(sameVariantId);
        RangeActionResult pstResult3 = rangeActionResultExtension.getVariant(differentVariantId);

        String prevStateId = crac.getPreventiveState().getId();
        pstResult1.setSetPoint(prevStateId, 3);
        pstResult2.setSetPoint(prevStateId, 3);
        pstResult3.setSetPoint(prevStateId, 2);

        assertTrue(situation.sameRemedialActions(initialVariantId, sameVariantId));
        assertTrue(situation.sameRemedialActions(sameVariantId, initialVariantId));
        assertFalse(situation.sameRemedialActions(initialVariantId, differentVariantId));
    }
}
