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
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static junit.framework.TestCase.assertFalse;
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
    private AngleCnec acPrev;
    private AngleCnec acCur1;
    private NetworkAction naOpenL1Prev;
    private NetworkAction naOpenL1Cur;
    private NetworkAction naInjectionCur;

    @Before
    public void setUp() {
        network = Importers.loadNetwork("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        //network = Importers.loadNetwork("testSimpleNetwork.xiidm", getClass().getResourceAsStream("/testSimpleNetwork.xiidm"));
        crac = CracFactory.findDefault().create("test-crac");

        crac.newContingency().withId("coL1").withNetworkElement("L1").add();
        crac.newContingency().withId("coL2").withNetworkElement("L2").add();
        crac.newContingency().withId("coL1L2").withNetworkElement("L1").withNetworkElement("L2").add();

        acPrev = addAngleCnec("acPrev", Instant.PREVENTIVE, null, "VL1", "VL2", -200., 500.);
        acCur1 = addAngleCnec("acCur1", Instant.CURATIVE, "coL1", "VL1", "VL2", -200., 500.);

        naOpenL1Prev = crac.newNetworkAction()
                .withId("Open L1 - 1")
                .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
                .newOnAngleConstraintUsageRule().withInstant(Instant.PREVENTIVE).withAngleCnec(acPrev.getId()).add()
                .add();
        naOpenL1Cur = crac.newNetworkAction()
                .withId("Open L1 - 2")
                .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
                .newOnAngleConstraintUsageRule().withInstant(Instant.CURATIVE).withAngleCnec(acCur1.getId()).add()
                .add();
        naInjectionCur = crac.newNetworkAction()
                .with("naInjectionCur")
                .newInjectionSetPoint().withNetworkElement("").


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
        //when(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState())).thenReturn(Set.of(naOpenL1Prev));
        when(raoResult.getActivatedNetworkActionsDuringState(crac.getState("coL1", Instant.CURATIVE))).thenReturn(Set.of(naOpenL1Cur));
        runAngleMonitoring();
        angleMonitoringResult.printConstraints().forEach(subConstraint -> System.out.println(subConstraint));
        assertFalse(angleMonitoringResult.isSecure());
    }
}
