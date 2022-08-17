/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.cne_exporter_commons.CneHelper;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.CnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MeasurementCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MonitoredSeriesCreationContext;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.swe_cne_exporter.xsd.MonitoredSeries;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageLevel;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SweMonitoredSeriesCreatorTest {

    private CneHelper cneHelper;
    private Crac crac;
    private RaoResult raoResult;
    private CimCracCreationContext cracCreationContext;
    private Network network;

    @Before
    public void setup() {
        this.crac = Mockito.mock(Crac.class);
        this.raoResult = Mockito.mock(RaoResult.class);
        this.cracCreationContext = Mockito.mock(CimCracCreationContext.class);
        this.cneHelper = Mockito.mock(CneHelper.class);
        this.network = Mockito.mock(Network.class);

        Mockito.when(cneHelper.getCrac()).thenReturn(crac);
        Mockito.when(cneHelper.getRaoResult()).thenReturn(raoResult);
        Mockito.when(cneHelper.getNetwork()).thenReturn(network);
    }

    @Test
    public void generateMonitoredSeriesTest() {
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

        addCnecsToMscc(mscc1, Set.of(Instant.PREVENTIVE), new HashSet<>());
        addCnecsToMscc(mscc2, Set.of(Instant.PREVENTIVE, Instant.OUTAGE, Instant.AUTO, Instant.CURATIVE), Set.of(contingency));
        addCnecsToMscc(mscc3, Set.of(Instant.OUTAGE, Instant.AUTO, Instant.CURATIVE), Set.of(contingency));

        setCnecResult(mscc1, Instant.PREVENTIVE, null, 100);
        setCnecResult(mscc2, Instant.PREVENTIVE, contingency, -80);
        setCnecResult(mscc2, Instant.OUTAGE, contingency, -120);
        setCnecResult(mscc2, Instant.AUTO, contingency, 120);
        setCnecResult(mscc2, Instant.CURATIVE, contingency, -90);
        setCnecResult(mscc3, Instant.OUTAGE, contingency, -105);
        setCnecResult(mscc3, Instant.AUTO, contingency, -105);
        setCnecResult(mscc3, Instant.CURATIVE, contingency, 95);

        SweMonitoredSeriesCreator monitoredSeriesCreator = new SweMonitoredSeriesCreator(cneHelper, cracCreationContext);

        List<MonitoredSeries> basecaseMonitoredSeries = monitoredSeriesCreator.generateMonitoredSeries(null);
        List<MonitoredSeries> contingencyMonitoredSeries = monitoredSeriesCreator.generateMonitoredSeries(contingency);

        //monitored series 1 and 2 each have a preventive cnec
        assertEquals(2, basecaseMonitoredSeries.size());
        assertEquals("ms1Id", basecaseMonitoredSeries.get(0).getMRID());
        assertEquals("ms2Id", basecaseMonitoredSeries.get(1).getMRID());
        //monitored series 2 and 3 each have three post contingency cnecs, but the OUTAGE and AUTO flows for the third series are equal so are merged
        assertEquals(5, contingencyMonitoredSeries.size());
        assertEquals("ms2Id", basecaseMonitoredSeries.get(0).getMRID());
        assertEquals("ms2Id", basecaseMonitoredSeries.get(1).getMRID());
        assertEquals("ms2Id", basecaseMonitoredSeries.get(2).getMRID());
        assertEquals("ms3Id", basecaseMonitoredSeries.get(3).getMRID());
        assertEquals("ms3Id", basecaseMonitoredSeries.get(4).getMRID());
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

    private void addCnecsToMscc(MonitoredSeriesCreationContext mscc, Set<Instant> instants, Set<Contingency> contingencies) {
        for (Instant instant : instants) {
            MeasurementCreationContext measurementCC = Mockito.mock(MeasurementCreationContext.class);
            MultiKeyMap<Object, CnecCreationContext> cnecCCs = new MultiKeyMap<>();
            if (instant != Instant.PREVENTIVE) {
                for (Contingency contingency : contingencies) {
                    CnecCreationContext cnecCC = Mockito.mock(CnecCreationContext.class);
                    Mockito.when(cnecCC.isImported()).thenReturn(true);
                    String cnecId = mscc.getNativeName() + " - " + contingency.getId() + " - " + instant;
                    addCnecToCracAndNetwork(cnecId, instant, contingency, mscc.getNativeResourceId());
                    Mockito.when(cnecCC.getCreatedCnecId()).thenReturn(cnecId);
                    MultiKey<Object> key = new MultiKey<>(contingency.getId(), instant);
                    cnecCCs.put(key, cnecCC);
                }
            } else {
                CnecCreationContext cnecCC = Mockito.mock(CnecCreationContext.class);
                Mockito.when(cnecCC.isImported()).thenReturn(true);
                String cnecId = mscc.getNativeName() + " - " + instant;
                addCnecToCracAndNetwork(cnecId, instant, null, mscc.getNativeResourceId());
                Mockito.when(cnecCC.getCreatedCnecId()).thenReturn(cnecId);
                MultiKey<Object> key = new MultiKey<>("", instant);
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
        if (instant != Instant.PREVENTIVE) {
            cnecId = mscc.getNativeName() + " - " + contingency.getId() + " - " + instant;
        } else {
            cnecId = mscc.getNativeName() + " - " + instant;
        }
        FlowCnec flowCnec = crac.getFlowCnec(cnecId);
        Mockito.when(raoResult.getFlow(OptimizationState.AFTER_CRA, flowCnec, Unit.AMPERE)).thenReturn(flow);
    }
}
