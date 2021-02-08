/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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

    private Crac crac;
    private ZonalData<LinearGlsk> glskProvider;
    private List<Pair<EICode, EICode>> boundaries;
    private SystematicSensitivityResult systematicSensitivityResult;

    @Before
    public void setUp() {
        crac = CommonCracCreation.create();
        Network network = NetworkImportsUtil.import12NodesNetwork();
        glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/glsk_proportional_12nodes_with_alegro.xml"))
            .getZonalGlsks(network, Instant.parse("2016-07-28T22:30:00Z"));
        boundaries = Arrays.asList(
            new ImmutablePair<>(new EICode(Country.FR), new EICode(Country.BE)),
            new ImmutablePair<>(new EICode(Country.FR), new EICode(Country.DE)),
            new ImmutablePair<>(new EICode(Country.DE), new EICode(Country.BE)),
            new ImmutablePair<>(new EICode(Country.BE), new EICode("22Y201903145---4")));

        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(Mockito.any(LinearGlsk.class), Mockito.any(BranchCnec.class)))
            .thenAnswer(
                new Answer<Double>() {
                    @Override public Double answer(InvocationOnMock invocation) {
                        LinearGlsk linearGlsk = (LinearGlsk) invocation.getArguments()[0];
                        BranchCnec branchCnec = (BranchCnec) invocation.getArguments()[1];
                        if (branchCnec.getId().equals("cnec1basecase")) {
                            switch (linearGlsk.getId().substring(0, EICode.EIC_LENGTH)) {
                                case "10YFR-RTE------C":
                                    return 0.1;
                                case "10YBE----------2":
                                    return 0.2;
                                case "10YCB-GERMANY--8":
                                    return 0.3;
                                case "22Y201903145---4":
                                    return 0.4;
                                default:
                                    return 0.;
                            }
                        } else if (branchCnec.getId().equals("cnec2basecase")) {
                            switch (linearGlsk.getId().substring(0, EICode.EIC_LENGTH)) {
                                case "10YFR-RTE------C":
                                    return 0.3;
                                case "10YBE----------2":
                                    return 0.3;
                                case "10YCB-GERMANY--8":
                                    return 0.2;
                                case "22Y201903145---4":
                                    return 0.1;
                                default:
                                    return 0.;
                            }
                        } else {
                            return 0.;
                        }
                    }
                });
    }

    @Test
    public void testComputation() {
        Map<BranchCnec, Double> ptdfSums = AbsolutePtdfSumsComputation.computeAbsolutePtdfSums(crac.getBranchCnecs(), glskProvider, boundaries, systematicSensitivityResult);
        assertEquals(0.6, ptdfSums.get(crac.getBranchCnec("cnec1basecase")), DOUBLE_TOLERANCE); // abs(0.1 - 0.2) + abs(0.1 - 0.3) + abs(0.3 - 0.2) + abs(0.2 - 0.4)
        assertEquals(0.4, ptdfSums.get(crac.getBranchCnec("cnec2basecase")), DOUBLE_TOLERANCE); // abs(0.3 - 0.3) + abs(0.3 - 0.2) + abs(0.2 - 0.3) + abs(0.3 - 0.1)
        assertEquals(0, ptdfSums.get(crac.getBranchCnec("cnec1stateCurativeContingency1")), DOUBLE_TOLERANCE);
        assertEquals(0, ptdfSums.get(crac.getBranchCnec("cnec1stateCurativeContingency2")), DOUBLE_TOLERANCE);
        assertEquals(0, ptdfSums.get(crac.getBranchCnec("cnec2stateCurativeContingency1")), DOUBLE_TOLERANCE);
        assertEquals(0, ptdfSums.get(crac.getBranchCnec("cnec2stateCurativeContingency2")), DOUBLE_TOLERANCE);
    }
}
