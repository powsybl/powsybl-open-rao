/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.glsk.import_.glsk_provider.GlskProvider;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowComputationTest {

    private static final double DOUBLE_TOLERANCE = 0.1;
    private Crac crac;
    private Network network;

    @Before
    public void setUp() {
        network = ExampleGenerator.network();
        crac = ExampleGenerator.crac();

        CnecLoopFlowExtension loopFlowExtensionMock = Mockito.mock(CnecLoopFlowExtension.class);
        crac.getCnec("FR-BE1").addExtension(CnecLoopFlowExtension.class, loopFlowExtensionMock);
        crac.getCnec("BE1-BE2").addExtension(CnecLoopFlowExtension.class, loopFlowExtensionMock);
        crac.getCnec("BE2-NL").addExtension(CnecLoopFlowExtension.class, loopFlowExtensionMock);
        crac.getCnec("FR-DE").addExtension(CnecLoopFlowExtension.class, loopFlowExtensionMock);
        crac.getCnec("DE-NL").addExtension(CnecLoopFlowExtension.class, loopFlowExtensionMock);
    }

    @Test
    public void calculateLoopFlowTest() {
        GlskProvider glskProvider = ExampleGenerator.glskProvider();
        ReferenceProgram referenceProgram = ExampleGenerator.referenceProgram();
        SystematicSensitivityResult ptdfsAndFlows = ExampleGenerator.systematicSensitivityResult(network, crac, glskProvider);

        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(crac, glskProvider, referenceProgram);
        LoopFlowResult loopFlowResult = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(ptdfsAndFlows, network, new HashSet<>());

        assertEquals(-50., loopFlowResult.getLoopFlow(crac.getCnec("FR-BE1")), DOUBLE_TOLERANCE);
        assertEquals(200., loopFlowResult.getLoopFlow(crac.getCnec("BE1-BE2")), DOUBLE_TOLERANCE);
        assertEquals(-50., loopFlowResult.getLoopFlow(crac.getCnec("BE2-NL")), DOUBLE_TOLERANCE);
        assertEquals(50., loopFlowResult.getLoopFlow(crac.getCnec("FR-DE")), DOUBLE_TOLERANCE);
        assertEquals(50., loopFlowResult.getLoopFlow(crac.getCnec("DE-NL")), DOUBLE_TOLERANCE);

        assertEquals(80, loopFlowResult.getCommercialFlow(crac.getCnec("FR-BE1")), DOUBLE_TOLERANCE);
        assertEquals(80., loopFlowResult.getCommercialFlow(crac.getCnec("BE1-BE2")), DOUBLE_TOLERANCE);
        assertEquals(80, loopFlowResult.getCommercialFlow(crac.getCnec("BE2-NL")), DOUBLE_TOLERANCE);
        assertEquals(120., loopFlowResult.getCommercialFlow(crac.getCnec("FR-DE")), DOUBLE_TOLERANCE);
        assertEquals(120., loopFlowResult.getCommercialFlow(crac.getCnec("DE-NL")), DOUBLE_TOLERANCE);

        assertEquals(30., loopFlowResult.getReferenceFlow(crac.getCnec("FR-BE1")), DOUBLE_TOLERANCE);
        assertEquals(280., loopFlowResult.getReferenceFlow(crac.getCnec("BE1-BE2")), DOUBLE_TOLERANCE);
        assertEquals(30., loopFlowResult.getReferenceFlow(crac.getCnec("BE2-NL")), DOUBLE_TOLERANCE);
        assertEquals(170., loopFlowResult.getReferenceFlow(crac.getCnec("FR-DE")), DOUBLE_TOLERANCE);
        assertEquals(170., loopFlowResult.getReferenceFlow(crac.getCnec("DE-NL")), DOUBLE_TOLERANCE);
    }

    @Test
    public void getLoopflowCnecsForCountriesTest() {
        Set<Country> countries = new HashSet<>();
        countries.add(Country.valueOf("BE"));
        countries.add(Country.valueOf("NL"));
        Set<Cnec> loopflowCnecs = LoopFlowComputation.getLoopflowCnecsForCountries(crac, network, countries);

        assertEquals(4, loopflowCnecs.size());
    }
}
