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
        Situation initialSituation = new Situation(network, crac);

        assertNotNull(crac.getExtension(CracResultExtension.class));

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        assertEquals(1, resultVariantManager.getVariants().size());

        initialSituation.clear(Collections.emptyList());

        assertEquals(0, resultVariantManager.getVariants().size());
    }

    @Test
    public void optimizedSituationTest() {
        Situation optimizedSituation = new Situation(network, crac);

        assertNotNull(crac.getExtension(CracResultExtension.class));

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        assertEquals(1, resultVariantManager.getVariants().size());

        optimizedSituation.clear(Collections.emptyList());
        assertEquals(0, resultVariantManager.getVariants().size());
    }

    @Test
    public void sameRasTest() {
        Situation sameSituation1 = new Situation(network, crac);
        Situation sameSituation2 = new Situation(network, crac);
        Situation differentSituation = new Situation(network, crac);

        String variant1 = sameSituation1.getWorkingVariantId();
        String variant2 = sameSituation2.getWorkingVariantId();
        String variant3 = differentSituation.getWorkingVariantId();

        RangeActionResultExtension rangeActionResultExtension = crac.getRangeActions().iterator().next().getExtension(RangeActionResultExtension.class);
        RangeActionResult pstResult1 = rangeActionResultExtension.getVariant(variant1);
        RangeActionResult pstResult2 = rangeActionResultExtension.getVariant(variant2);
        RangeActionResult pstResult3 = rangeActionResultExtension.getVariant(variant3);

        String prevStateId = crac.getPreventiveState().getId();
        pstResult1.setSetPoint(prevStateId, 3);
        pstResult2.setSetPoint(prevStateId, 3);
        pstResult3.setSetPoint(prevStateId, 2);
    }
}
