/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.CnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MeasurementCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MonitoredSeriesCreationContext;
import com.farao_community.farao.data.crac_impl.InstantImpl;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.swe_cne_exporter.xsd.MonitoredSeries;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageLevel;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class SweMonitoredSeriesCreatorTest {

    private SweCneHelper sweCneHelper;
    private Crac crac;
    private RaoResult raoResult;
    private CimCracCreationContext cracCreationContext;
    private Network network;

    @BeforeEach
    public void setup() {
        this.crac = Mockito.mock(Crac.class);
        this.raoResult = Mockito.mock(RaoResult.class);
        this.cracCreationContext = Mockito.mock(CimCracCreationContext.class);
        this.sweCneHelper = Mockito.mock(SweCneHelper.class);
        this.network = Mockito.mock(Network.class);

        Mockito.when(sweCneHelper.getCrac()).thenReturn(crac);
        Mockito.when(sweCneHelper.getRaoResult()).thenReturn(raoResult);
        Mockito.when(sweCneHelper.getNetwork()).thenReturn(network);
        Instant instantPrev = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
        Instant instantOutage = new InstantImpl("outage", InstantKind.OUTAGE, instantPrev);
        Instant instantAuto = new InstantImpl("auto", InstantKind.AUTO, instantOutage);
        Instant instantCurative = new InstantImpl("curative", InstantKind.CURATIVE, instantAuto);
        Mockito.when(crac.getInstant("preventive")).thenReturn(instantPrev);
        Mockito.when(crac.getInstant("outage")).thenReturn(instantOutage);
        Mockito.when(crac.getInstant("auto")).thenReturn(instantAuto);
        Mockito.when(crac.getInstant("curative")).thenReturn(instantCurative);
    }

    @Test
    void generateMonitoredSeriesTest() {
        Contingency contingency = Mockito.mock(Contingency.class);
        Mockito.when(contingency.getId()).thenReturn("contingency");

        MonitoredSeriesCreationContext mscc1 = createMSCC("ms1Id", "ms1Name", "ms1ResourceId", "ms1ResourceName");
        MonitoredSeriesCreationContext mscc2 = createMSCC("ms2Id", "ms2Name", "ms2ResourceId", "ms2ResourceName");
        MonitoredSeriesCreationContext mscc3 = createMSCC("ms3Id", "ms3Name", "ms3ResourceId", "ms3ResourceName");
        Mockito.when(cracCreationContext.getMonitoredSeriesCreationContexts()).thenReturn(Map.of(
            "ms1Id", mscc1,
            "ms2ID", mscc2,
            "ms3ID", mscc3
        ));

        addCnecsToMscc(mscc1, Set.of("preventive"), new HashSet<>());
        addCnecsToMscc(mscc2, Set.of("preventive", "outage", "auto", "curative"), Set.of(contingency));
        addCnecsToMscc(mscc3, Set.of("outage", "auto", "curative"), Set.of(contingency));

        setCnecResult(mscc1, crac.getInstant("preventive"), null, 100);
        setCnecResult(mscc2, crac.getInstant("preventive"), contingency, -80);
        setCnecResult(mscc2, crac.getInstant("outage"), contingency, -120);
        setCnecResult(mscc2, crac.getInstant("auto"), contingency, 120);
        setCnecResult(mscc2, crac.getInstant("curative"), contingency, -90);
        setCnecResult(mscc3, crac.getInstant("outage"), contingency, -105);
        setCnecResult(mscc3, crac.getInstant("auto"), contingency, -105);
        setCnecResult(mscc3, crac.getInstant("curative"), contingency, 95);

        SweMonitoredSeriesCreator monitoredSeriesCreator = new SweMonitoredSeriesCreator(sweCneHelper, cracCreationContext);

        List<MonitoredSeries> basecaseMonitoredSeries = monitoredSeriesCreator.generateMonitoredSeries(null);
        List<MonitoredSeries> contingencyMonitoredSeries = monitoredSeriesCreator.generateMonitoredSeries(contingency);

        //monitored series 1 and 2 each have a preventive cnec
        assertEquals(2, basecaseMonitoredSeries.size());
        assertEquals("ms1Id", basecaseMonitoredSeries.get(0).getMRID());
        assertEquals("ms2Id", basecaseMonitoredSeries.get(1).getMRID());
        //monitored series 2 and 3 each have three post contingency cnecs, but the OUTAGE and AUTO flows for the third series are equal so are merged
        assertEquals(5, contingencyMonitoredSeries.size());
        assertEquals("ms2Id", contingencyMonitoredSeries.get(0).getMRID());
        assertEquals("ms2Id", contingencyMonitoredSeries.get(1).getMRID());
        assertEquals("ms2Id", contingencyMonitoredSeries.get(2).getMRID());
        assertEquals("ms3Id", contingencyMonitoredSeries.get(3).getMRID());
        assertEquals("ms3Id", contingencyMonitoredSeries.get(4).getMRID());
    }

    private MonitoredSeriesCreationContext createMSCC(String mrId, String name, String resourceId, String resourceName) {
        MonitoredSeriesCreationContext mscc = Mockito.mock(MonitoredSeriesCreationContext.class);
        Mockito.when(mscc.getNativeId()).thenReturn(mrId);
        Mockito.when(mscc.getNativeName()).thenReturn(name);
        Mockito.when(mscc.getNativeResourceId()).thenReturn(resourceId);
        Mockito.when(mscc.getNativeResourceName()).thenReturn(resourceName);
        Mockito.when(mscc.isImported()).thenReturn(true);
        Mockito.when(mscc.getMeasurementCreationContexts()).thenReturn(new HashSet<>());
        return mscc;
    }

    private void addCnecsToMscc(MonitoredSeriesCreationContext mscc, Set<String> instantIds, Set<Contingency> contingencies) {
        for (String instantId : instantIds) {
            MeasurementCreationContext measurementCC = Mockito.mock(MeasurementCreationContext.class);
            MultiKeyMap<Object, CnecCreationContext> cnecCCs = new MultiKeyMap<>();
            Instant instant = crac.getInstant(instantId);
            if (instant.getInstantKind() != InstantKind.PREVENTIVE) {
                for (Contingency contingency : contingencies) {
                    CnecCreationContext cnecCC = Mockito.mock(CnecCreationContext.class);
                    Mockito.when(cnecCC.isImported()).thenReturn(true);
                    String cnecId = mscc.getNativeName() + " - " + contingency.getId() + " - " + instantId;
                    addCnecToCracAndNetwork(cnecId, instant, contingency, mscc.getNativeResourceId());
                    Mockito.when(cnecCC.getCreatedCnecId()).thenReturn(cnecId);
                    MultiKey<Object> key = new MultiKey<>(contingency.getId(), instantId);
                    cnecCCs.put(key, cnecCC);
                }
            } else {
                CnecCreationContext cnecCC = Mockito.mock(CnecCreationContext.class);
                Mockito.when(cnecCC.isImported()).thenReturn(true);
                String cnecId = mscc.getNativeName() + " - " + instantId;
                addCnecToCracAndNetwork(cnecId, instant, null, mscc.getNativeResourceId());
                Mockito.when(cnecCC.getCreatedCnecId()).thenReturn(cnecId);
                MultiKey<Object> key = new MultiKey<>("", instantId);
                cnecCCs.put(key, cnecCC);
            }
            Mockito.when(measurementCC.getCnecCreationContexts()).thenReturn(cnecCCs);
            Mockito.when(measurementCC.isImported()).thenReturn(true);

            Set<MeasurementCreationContext> measurementCCs = mscc.getMeasurementCreationContexts();
            measurementCCs.add(measurementCC);
            Mockito.when(mscc.getMeasurementCreationContexts()).thenReturn(measurementCCs);
        }
    }

    private void addCnecToCracAndNetwork(String cnecId, Instant instant, Contingency contingency, String resourceId) {
        FlowCnec cnec = Mockito.mock(FlowCnec.class);

        State state = Mockito.mock(State.class);
        Mockito.when(state.getInstant()).thenReturn(instant);
        Mockito.when(state.getContingency()).thenReturn(Objects.isNull(contingency) ? Optional.empty() : Optional.of(contingency));
        Mockito.when(cnec.getState()).thenReturn(state);

        Mockito.when(cnec.getIMax(Side.LEFT)).thenReturn(1000.);

        NetworkElement networkElement = Mockito.mock(NetworkElement.class);
        Mockito.when(networkElement.getId()).thenReturn(resourceId);
        Mockito.when(cnec.getNetworkElement()).thenReturn(networkElement);

        Branch<?> branch = Mockito.mock(Branch.class);
        Terminal terminal1 = Mockito.mock(Terminal.class);
        VoltageLevel voltageLevel1 = Mockito.mock(VoltageLevel.class);
        Mockito.when(voltageLevel1.getId()).thenReturn(resourceId + " - vl1");
        Mockito.when(terminal1.getVoltageLevel()).thenReturn(voltageLevel1);
        Mockito.when(branch.getTerminal1()).thenReturn(terminal1);
        Terminal terminal2 = Mockito.mock(Terminal.class);
        VoltageLevel voltageLevel2 = Mockito.mock(VoltageLevel.class);
        Mockito.when(voltageLevel2.getId()).thenReturn(resourceId + " - vl2");
        Mockito.when(terminal2.getVoltageLevel()).thenReturn(voltageLevel2);
        Mockito.when(branch.getTerminal2()).thenReturn(terminal2);
        Mockito.when(network.getBranch(resourceId)).thenReturn(branch);

        Mockito.when(crac.getFlowCnec(cnecId)).thenReturn(cnec);
    }

    private void setCnecResult(MonitoredSeriesCreationContext mscc, Instant instant, Contingency contingency, double flow) {
        String cnecId;
        if (instant.getInstantKind() != InstantKind.PREVENTIVE) {
            cnecId = mscc.getNativeName() + " - " + contingency.getId() + " - " + instant;
        } else {
            cnecId = mscc.getNativeName() + " - " + instant;
        }
        FlowCnec flowCnec = crac.getFlowCnec(cnecId);
        Mockito.when(flowCnec.getMonitoredSides()).thenReturn(Set.of(Side.LEFT, Side.RIGHT));
        Mockito.when(raoResult.getFlow(instant, flowCnec, Side.LEFT, Unit.AMPERE)).thenReturn(flow);
        Mockito.when(raoResult.getFlow(instant, flowCnec, Side.RIGHT, Unit.AMPERE)).thenReturn(flow + 10);
        Mockito.when(flowCnec.computeMargin(flow, Side.LEFT, Unit.AMPERE)).thenReturn(1000 - flow);
        Mockito.when(flowCnec.computeMargin(flow, Side.RIGHT, Unit.AMPERE)).thenReturn(1100 - flow);
    }
}
