/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.glsk.api.GlskProvider;
import com.farao_community.farao.data.glsk.ucte.UcteGlskDocument;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class AbsolutePtdfSumsComputationTest {
    private static final double DOUBLE_TOLERANCE = 0.001;

    Crac crac;
    Network network;
    GlskProvider glskProvider;
    List<Pair<Country, Country>> boundaries;
    SystematicSensitivityResult systematicSensitivityResult;

    @Before
    public void setUp() {
        crac = CommonCracCreation.create();
        network = NetworkImportsUtil.import12NodesNetwork();
        glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/glsk_proportional_12nodes.xml"))
            .getGlskProvider(network, Instant.parse("2016-07-28T22:30:00Z"));
        boundaries = Arrays.asList(new ImmutablePair<>(Country.FR, Country.BE),
                new ImmutablePair<>(Country.FR, Country.DE),
                new ImmutablePair<>(Country.NL, Country.BE),
                new ImmutablePair<>(Country.NL, Country.DE));
        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(Mockito.any(LinearGlsk.class), Mockito.any(Cnec.class))).thenReturn(0.1);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(Mockito.any(LinearGlsk.class), Mockito.eq(crac.getCnec("cnec1basecase")))).thenReturn(.1, .2, .3, .4);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(Mockito.any(LinearGlsk.class), Mockito.eq(crac.getCnec("cnec2basecase")))).thenReturn(.3, .3, .2, .1);

    }

    @Test
    public void testComputation() {
        Map<Cnec, Double> ptdfSums = AbsolutePtdfSumsComputation.computeAbsolutePtdfSums(crac.getCnecs(), glskProvider, boundaries, systematicSensitivityResult);
        assertEquals(0.8, ptdfSums.get(crac.getCnec("cnec1basecase")), DOUBLE_TOLERANCE);
        assertEquals(0.6, ptdfSums.get(crac.getCnec("cnec2basecase")), DOUBLE_TOLERANCE);
        assertEquals(0, ptdfSums.get(crac.getCnec("cnec1stateCurativeContingency1")), DOUBLE_TOLERANCE);
        assertEquals(0, ptdfSums.get(crac.getCnec("cnec1stateCurativeContingency2")), DOUBLE_TOLERANCE);
        assertEquals(0, ptdfSums.get(crac.getCnec("cnec2stateCurativeContingency1")), DOUBLE_TOLERANCE);
        assertEquals(0, ptdfSums.get(crac.getCnec("cnec2stateCurativeContingency2")), DOUBLE_TOLERANCE);

    }
}
