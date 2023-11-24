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

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class SweMonitoredSeriesCreatorTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private SweCneHelper sweCneHelper;
    private Crac crac;
    private RaoResult raoResult;
    private CimCracCreationContext cracCreationContext;
    private Network network;

    @BeforeEach
    public void setup() {
        this.crac = mock(Crac.class);
        this.raoResult = mock(RaoResult.class);
        this.cracCreationContext = mock(CimCracCreationContext.class);
        this.sweCneHelper = mock(SweCneHelper.class);
        this.network = mock(Network.class);

        when(sweCneHelper.getCrac()).thenReturn(crac);
        when(sweCneHelper.getRaoResult()).thenReturn(raoResult);
        when(sweCneHelper.getNetwork()).thenReturn(network);
        Instant preventiveInstant = mockInstant(true, PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
        Instant outageInstant = mockInstant(false, OUTAGE_INSTANT_ID, InstantKind.OUTAGE);
        Instant autoInstant = mockInstant(false, AUTO_INSTANT_ID, InstantKind.AUTO);
        Instant curativeInstant = mockInstant(false, CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        when(crac.getInstant(PREVENTIVE_INSTANT_ID)).thenReturn(preventiveInstant);
        when(crac.getInstant(OUTAGE_INSTANT_ID)).thenReturn(outageInstant);
        when(crac.getInstant(AUTO_INSTANT_ID)).thenReturn(autoInstant);
        when(crac.getInstant(CURATIVE_INSTANT_ID)).thenReturn(curativeInstant);
    }

    private static Instant mockInstant(boolean isPreventive, String instantId, InstantKind instantKind) {
        Instant instant = mock(Instant.class);
        when(instant.isPreventive()).thenReturn(isPreventive);
        when(instant.toString()).thenReturn(instantId);
        when(instant.getInstantKind()).thenReturn(instantKind);
        return instant;
    }

    @Test
    void generateMonitoredSeriesTest() {
        Contingency contingency = mock(Contingency.class);
        when(contingency.getId()).thenReturn("contingency");

        MonitoredSeriesCreationContext mscc1 = createMSCC("ms1Id", "ms1Name", "ms1ResourceId", "ms1ResourceName");
        MonitoredSeriesCreationContext mscc2 = createMSCC("ms2Id", "ms2Name", "ms2ResourceId", "ms2ResourceName");
        MonitoredSeriesCreationContext mscc3 = createMSCC("ms3Id", "ms3Name", "ms3ResourceId", "ms3ResourceName");
        when(cracCreationContext.getMonitoredSeriesCreationContexts()).thenReturn(Map.of(
            "ms1Id", mscc1,
            "ms2ID", mscc2,
            "ms3ID", mscc3
        ));

        addCnecsToMscc(mscc1, Set.of(PREVENTIVE_INSTANT_ID), new HashSet<>());
        addCnecsToMscc(mscc2, Set.of(PREVENTIVE_INSTANT_ID, OUTAGE_INSTANT_ID, AUTO_INSTANT_ID, CURATIVE_INSTANT_ID), Set.of(contingency));
        addCnecsToMscc(mscc3, Set.of(OUTAGE_INSTANT_ID, AUTO_INSTANT_ID, CURATIVE_INSTANT_ID), Set.of(contingency));

        setCnecResult(mscc1, crac.getInstant(PREVENTIVE_INSTANT_ID), null, 100);
        setCnecResult(mscc2, crac.getInstant(PREVENTIVE_INSTANT_ID), contingency, -80);
        setCnecResult(mscc2, crac.getInstant(OUTAGE_INSTANT_ID), contingency, -120);
        setCnecResult(mscc2, crac.getInstant(AUTO_INSTANT_ID), contingency, 120);
        setCnecResult(mscc2, crac.getInstant(CURATIVE_INSTANT_ID), contingency, -90);
        setCnecResult(mscc3, crac.getInstant(OUTAGE_INSTANT_ID), contingency, -105);
        setCnecResult(mscc3, crac.getInstant(AUTO_INSTANT_ID), contingency, -105);
        setCnecResult(mscc3, crac.getInstant(CURATIVE_INSTANT_ID), contingency, 95);

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
        MonitoredSeriesCreationContext mscc = mock(MonitoredSeriesCreationContext.class);
        when(mscc.getNativeId()).thenReturn(mrId);
        when(mscc.getNativeName()).thenReturn(name);
        when(mscc.getNativeResourceId()).thenReturn(resourceId);
        when(mscc.getNativeResourceName()).thenReturn(resourceName);
        when(mscc.isImported()).thenReturn(true);
        when(mscc.getMeasurementCreationContexts()).thenReturn(new HashSet<>());
        return mscc;
    }

    private void addCnecsToMscc(MonitoredSeriesCreationContext mscc, Set<String> instantIds, Set<Contingency> contingencies) {
        for (String instantId : instantIds) {
            MeasurementCreationContext measurementCC = mock(MeasurementCreationContext.class);
            MultiKeyMap<Object, CnecCreationContext> cnecCCs = new MultiKeyMap<>();
            Instant instant = crac.getInstant(instantId);
            if (!instant.isPreventive()) {
                for (Contingency contingency : contingencies) {
                    CnecCreationContext cnecCC = mock(CnecCreationContext.class);
                    when(cnecCC.isImported()).thenReturn(true);
                    String cnecId = mscc.getNativeName() + " - " + contingency.getId() + " - " + instantId;
                    addCnecToCracAndNetwork(cnecId, instant, contingency, mscc.getNativeResourceId());
                    when(cnecCC.getCreatedCnecId()).thenReturn(cnecId);
                    MultiKey<Object> key = new MultiKey<>(contingency.getId(), instantId);
                    cnecCCs.put(key, cnecCC);
                }
            } else {
                CnecCreationContext cnecCC = mock(CnecCreationContext.class);
                when(cnecCC.isImported()).thenReturn(true);
                String cnecId = mscc.getNativeName() + " - " + instantId;
                addCnecToCracAndNetwork(cnecId, instant, null, mscc.getNativeResourceId());
                when(cnecCC.getCreatedCnecId()).thenReturn(cnecId);
                MultiKey<Object> key = new MultiKey<>("", instantId);
                cnecCCs.put(key, cnecCC);
            }
            when(measurementCC.getCnecCreationContexts()).thenReturn(cnecCCs);
            when(measurementCC.isImported()).thenReturn(true);

            Set<MeasurementCreationContext> measurementCCs = mscc.getMeasurementCreationContexts();
            measurementCCs.add(measurementCC);
            when(mscc.getMeasurementCreationContexts()).thenReturn(measurementCCs);
        }
    }

    private void addCnecToCracAndNetwork(String cnecId, Instant instant, Contingency contingency, String resourceId) {
        FlowCnec cnec = mock(FlowCnec.class);

        State state = mock(State.class);
        when(state.getInstant()).thenReturn(instant);
        when(state.getContingency()).thenReturn(Objects.isNull(contingency) ? Optional.empty() : Optional.of(contingency));
        when(cnec.getState()).thenReturn(state);

        when(cnec.getIMax(Side.LEFT)).thenReturn(1000.);

        NetworkElement networkElement = mock(NetworkElement.class);
        when(networkElement.getId()).thenReturn(resourceId);
        when(cnec.getNetworkElement()).thenReturn(networkElement);

        Branch<?> branch = mock(Branch.class);
        Terminal terminal1 = mock(Terminal.class);
        VoltageLevel voltageLevel1 = mock(VoltageLevel.class);
        when(voltageLevel1.getId()).thenReturn(resourceId + " - vl1");
        when(terminal1.getVoltageLevel()).thenReturn(voltageLevel1);
        when(branch.getTerminal1()).thenReturn(terminal1);
        Terminal terminal2 = mock(Terminal.class);
        VoltageLevel voltageLevel2 = mock(VoltageLevel.class);
        when(voltageLevel2.getId()).thenReturn(resourceId + " - vl2");
        when(terminal2.getVoltageLevel()).thenReturn(voltageLevel2);
        when(branch.getTerminal2()).thenReturn(terminal2);
        when(network.getBranch(resourceId)).thenReturn(branch);

        when(crac.getFlowCnec(cnecId)).thenReturn(cnec);
    }

    private void setCnecResult(MonitoredSeriesCreationContext mscc, Instant instant, Contingency contingency, double flow) {
        String cnecId;
        if (!instant.isPreventive()) {
            cnecId = mscc.getNativeName() + " - " + contingency.getId() + " - " + instant;
        } else {
            cnecId = mscc.getNativeName() + " - " + instant;
        }
        FlowCnec flowCnec = crac.getFlowCnec(cnecId);
        when(flowCnec.getMonitoredSides()).thenReturn(Set.of(Side.LEFT, Side.RIGHT));
        when(raoResult.getFlow(instant, flowCnec, Side.LEFT, Unit.AMPERE)).thenReturn(flow);
        when(raoResult.getFlow(instant, flowCnec, Side.RIGHT, Unit.AMPERE)).thenReturn(flow + 10);
        when(flowCnec.computeMargin(flow, Side.LEFT, Unit.AMPERE)).thenReturn(1000 - flow);
        when(flowCnec.computeMargin(flow, Side.RIGHT, Unit.AMPERE)).thenReturn(1100 - flow);
    }
}
