/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Angle monitoring test class
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class AngleMonitoringTest {

    private Network network;
    private Crac crac;
    private RaoResult raoResult;
    private LoadFlowParameters loadFlowParameters;
    private Map<Country, Set<ScalableNetworkElement>> glsks;
    private AngleMonitoringResult angleMonitoringResult;

    @Before
    public void setUp() {
        //network = Importers.loadNetwork("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        network = Importers.loadNetwork("testSimpleNetwork.xiidm", getClass().getResourceAsStream("/testSimpleNetwork.xiidm"));
        crac = CracFactory.findDefault().create("test-crac");

        crac.newContingency().withId("coL1").withNetworkElement("L1").add();

        loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(false);

        // GLSKs
        Set<ScalableNetworkElement> frScalable = Set.of(new ScalableNetworkElement("FFR1AA1 _generator", 60f, ScalableNetworkElement.ScalableType.GENERATOR),
                new ScalableNetworkElement("FFR2AA1 _generator", 30f, ScalableNetworkElement.ScalableType.GENERATOR),
                new ScalableNetworkElement("FFR3AA1 _generator", 10f, ScalableNetworkElement.ScalableType.GENERATOR));
        Set<ScalableNetworkElement> beScalable = Set.of(new ScalableNetworkElement("BBE1AA1 _generator", 60f, ScalableNetworkElement.ScalableType.GENERATOR),
                new ScalableNetworkElement("BBE3AA1 _generator", 30f, ScalableNetworkElement.ScalableType.GENERATOR),
                new ScalableNetworkElement("BBE2AA1 _generator", 10f, ScalableNetworkElement.ScalableType.GENERATOR));
        glsks = new HashMap<>();
        glsks.put(Country.FR, frScalable);
        glsks.put(Country.BE, beScalable);

        raoResult = Mockito.mock(RaoResult.class);
        when(raoResult.getActivatedNetworkActionsDuringState(any())).thenReturn(Collections.emptySet());
        when(raoResult.getActivatedRangeActionsDuringState(any())).thenReturn(Collections.emptySet());
    }

    private AngleCnec addAngleCnec(String id, Instant instant, String contingency, String importingNetworkElement, String exportingNetworkElement, Double min, Double max) {
        return crac.newAngleCnec()
                .withId(id)
                .withInstant(instant)
                .withContingency(contingency)
                .withImportingNetworkElement(importingNetworkElement)
                .withExportingNetworkElement(exportingNetworkElement)
                .withMonitored()
                .newThreshold().withUnit(Unit.DEGREE).withMin(min).withMax(max).add()
                .add();
    }

    private void runAngleMonitoring() {
        angleMonitoringResult = new AngleMonitoring(crac, network, raoResult, glsks, "OpenLoadFlow", loadFlowParameters).run(1);
    }

    @Test
    public void test1() {
        addAngleCnec("angleCnec1", Instant.PREVENTIVE, null, "VL1", "VL2", -200., 500.);
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isSecure());
    }
}
