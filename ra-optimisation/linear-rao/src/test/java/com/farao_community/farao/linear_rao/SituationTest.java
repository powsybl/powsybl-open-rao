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

import static org.junit.Assert.*;
/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SystematicSensitivityAnalysisService.class})
public class SituationTest {

    private Network network;
    private Crac crac;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        NetworkElement pstElement = new NetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name");
        PstRange pstRange = new PstWithRange("RA PST BE", pstElement);
        ((SimpleCrac) crac).addRangeAction(pstRange);
        crac.synchronize(network);
    }

    @Test
    public void initialSituationTest() {
        InitialSituation initialSituation = new InitialSituation(network, network.getVariantManager().getWorkingVariantId(), crac);

        assertNotNull(crac.getExtension(CracResultExtension.class));

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        assertEquals(1, resultVariantManager.getVariants().size());
        assertEquals(2, network.getVariantManager().getVariantIds().size());

        initialSituation.deleteCracResultVariant();
        assertEquals(0, resultVariantManager.getVariants().size());

        initialSituation.deleteNetworkVariant();
        assertEquals(1, network.getVariantManager().getVariantIds().size());

    }

    @Test
    public void optimizedSituationTest() {
        OptimizedSituation optimizedSituation = new OptimizedSituation(network, network.getVariantManager().getWorkingVariantId(), crac);

        assertNotNull(crac.getExtension(CracResultExtension.class));

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        assertEquals(1, resultVariantManager.getVariants().size());
        assertEquals(2, network.getVariantManager().getVariantIds().size());

        optimizedSituation.deleteCracResultVariant();
        assertEquals(0, resultVariantManager.getVariants().size());

        optimizedSituation.deleteNetworkVariant();
        assertEquals(1, network.getVariantManager().getVariantIds().size());
    }

    @Test
    public void sameRasTest() {
        OptimizedSituation sameSituation1 = new OptimizedSituation(network, network.getVariantManager().getWorkingVariantId(), crac);
        OptimizedSituation sameSituation2 = new OptimizedSituation(network, network.getVariantManager().getWorkingVariantId(), crac);
        OptimizedSituation differentSituation = new OptimizedSituation(network, network.getVariantManager().getWorkingVariantId(), crac);

        String variant1 = sameSituation1.getCracResultVariant();
        String variant2 = sameSituation2.getCracResultVariant();
        String variant3 = differentSituation.getCracResultVariant();

        RangeActionResultExtension rangeActionResultExtension = crac.getRangeActions().iterator().next().getExtension(RangeActionResultExtension.class);
        RangeActionResult pstResult1 = rangeActionResultExtension.getVariant(variant1);
        RangeActionResult pstResult2 = rangeActionResultExtension.getVariant(variant2);
        RangeActionResult pstResult3 = rangeActionResultExtension.getVariant(variant3);

        String prevStateId = crac.getPreventiveState().getId();
        pstResult1.setSetPoint(prevStateId, 3);
        pstResult2.setSetPoint(prevStateId, 3);
        pstResult3.setSetPoint(prevStateId, 2);

        assertTrue(sameSituation1.sameRaResults(sameSituation2));
        assertTrue(sameSituation2.sameRaResults(sameSituation1));
        assertFalse(sameSituation1.sameRaResults(differentSituation));
    }
}
