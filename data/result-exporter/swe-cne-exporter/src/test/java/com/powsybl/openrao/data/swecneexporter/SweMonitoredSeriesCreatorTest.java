/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.swecneexporter;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracio.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.cracio.cim.craccreator.CnecCreationContext;
import com.powsybl.openrao.data.cracio.cim.craccreator.MeasurementCreationContext;
import com.powsybl.openrao.data.cracio.cim.craccreator.MonitoredSeriesCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.swecneexporter.xsd.MonitoredSeries;
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
        this.crac = Mockito.mock(Crac.class);
        this.raoResult = Mockito.mock(RaoResult.class);
        this.cracCreationContext = Mockito.mock(CimCracCreationContext.class);
        this.sweCneHelper = Mockito.mock(SweCneHelper.class);
        this.network = Mockito.mock(Network.class);

        Mockito.when(sweCneHelper.getCrac()).thenReturn(crac);
        Mockito.when(sweCneHelper.getRaoResult()).thenReturn(raoResult);
        Mockito.when(sweCneHelper.getNetwork()).thenReturn(network);
        Instant preventiveInstant = mockInstant(true, PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
        Instant outageInstant = mockInstant(false, OUTAGE_INSTANT_ID, InstantKind.OUTAGE);
        Instant autoInstant = mockInstant(false, AUTO_INSTANT_ID, InstantKind.AUTO);
        Instant curativeInstant = mockInstant(false, CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        Mockito.when(crac.getInstant(PREVENTIVE_INSTANT_ID)).thenReturn(preventiveInstant);
        Mockito.when(crac.getInstant(OUTAGE_INSTANT_ID)).thenReturn(outageInstant);
        Mockito.when(crac.getInstant(AUTO_INSTANT_ID)).thenReturn(autoInstant);
        Mockito.when(crac.getInstant(CURATIVE_INSTANT_ID)).thenReturn(curativeInstant);
    }

    private static Instant mockInstant(boolean isPreventive, String instantId, InstantKind instantKind) {
        Instant instant = Mockito.mock(Instant.class);
        Mockito.when(instant.isPreventive()).thenReturn(isPreventive);
        Mockito.when(instant.toString()).thenReturn(instantId);
        Mockito.when(instant.getKind()).thenReturn(instantKind);
        return instant;
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
            if (!instant.isPreventive()) {
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

        Mockito.when(cnec.getIMax(TwoSides.ONE)).thenReturn(1000.);

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
        if (!instant.isPreventive()) {
            cnecId = mscc.getNativeName() + " - " + contingency.getId() + " - " + instant;
        } else {
            cnecId = mscc.getNativeName() + " - " + instant;
        }
        FlowCnec flowCnec = crac.getFlowCnec(cnecId);
        Mockito.when(flowCnec.getMonitoredSides()).thenReturn(Set.of(TwoSides.ONE, TwoSides.TWO));
        Mockito.when(raoResult.getFlow(instant, flowCnec, TwoSides.ONE, Unit.AMPERE)).thenReturn(flow);
        Mockito.when(raoResult.getFlow(instant, flowCnec, TwoSides.TWO, Unit.AMPERE)).thenReturn(flow + 10);
        Mockito.when(flowCnec.computeMargin(flow, TwoSides.ONE, Unit.AMPERE)).thenReturn(1000 - flow);
        Mockito.when(flowCnec.computeMargin(flow, TwoSides.TWO, Unit.AMPERE)).thenReturn(1100 - flow);
    }
}
