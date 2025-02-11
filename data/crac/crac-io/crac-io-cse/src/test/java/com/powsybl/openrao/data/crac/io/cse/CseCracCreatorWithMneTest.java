/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.cse;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.cse.criticalbranch.CseCriticalBranchCreationContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
class CseCracCreatorWithMneTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private final CracCreationParameters parameters = new CracCreationParameters();
    private CseCracCreationContext cracCreationContext;
    private Instant preventiveInstant;
    private Instant outageInstant;
    private Instant autoInstant;
    private Instant curativeInstant;

    private void setUp(String cracFileName, String networkFileName) throws IOException {
        InputStream is = getClass().getResourceAsStream(cracFileName);
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        cracCreationContext = (CseCracCreationContext) Crac.readWithContext(cracFileName, is, network, parameters);
        preventiveInstant = cracCreationContext.getCrac().getInstant(PREVENTIVE_INSTANT_ID);
        outageInstant = cracCreationContext.getCrac().getInstant(OUTAGE_INSTANT_ID);
        autoInstant = cracCreationContext.getCrac().getInstant(AUTO_INSTANT_ID);
        curativeInstant = cracCreationContext.getCrac().getInstant(CURATIVE_INSTANT_ID);
    }

    private void assertMneWithContingencyInCriticalBranchCreationContexts(String name, String contingencyId, String fromNode, String toNode, String suffix, boolean inverted) {
        String cnecOutageId = String.format("%s - %s->%s  - %s - %s", name, fromNode, toNode, contingencyId, outageInstant);
        String cnecCurativeId = String.format("%s - %s->%s  - %s - %s", name, fromNode, toNode, contingencyId, CURATIVE_INSTANT_ID);
        String cnecAutoId = String.format("%s - %s->%s  - %s - %s", name, fromNode, toNode, contingencyId, autoInstant);
        String nameCnec = String.format("%s - %s - %s - %s", name, fromNode, toNode, contingencyId);
        CseCriticalBranchCreationContext cseCriticalBranchCreationContext = (CseCriticalBranchCreationContext) cracCreationContext.getBranchCnecCreationContext(nameCnec);
        assertNotNull(cseCriticalBranchCreationContext);
        assertFalse(cseCriticalBranchCreationContext.isSelected());
        assertTrue(cseCriticalBranchCreationContext.isImported());
        assertNotNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(OUTAGE_INSTANT_ID));
        assertNotNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(CURATIVE_INSTANT_ID));
        assertNotNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(AUTO_INSTANT_ID));
        assertNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(PREVENTIVE_INSTANT_ID));
        assertEquals(ImportStatus.IMPORTED, cseCriticalBranchCreationContext.getImportStatus());
        assertFalse(cseCriticalBranchCreationContext.isBaseCase());
        assertEquals(contingencyId, cseCriticalBranchCreationContext.getContingencyId().orElseThrow());
        assertEquals(fromNode, cseCriticalBranchCreationContext.getNativeBranch().getFrom());
        assertEquals(toNode, cseCriticalBranchCreationContext.getNativeBranch().getTo());
        assertEquals(suffix, cseCriticalBranchCreationContext.getNativeBranch().getSuffix());
        assertEquals(cnecOutageId, cseCriticalBranchCreationContext.getCreatedCnecsIds().get(OUTAGE_INSTANT_ID));
        assertEquals(cnecCurativeId, cseCriticalBranchCreationContext.getCreatedCnecsIds().get(CURATIVE_INSTANT_ID));
        assertEquals(cnecAutoId, cseCriticalBranchCreationContext.getCreatedCnecsIds().get(AUTO_INSTANT_ID));
        assertEquals(inverted, cseCriticalBranchCreationContext.isDirectionInvertedInNetwork());
    }

    private void assertMneNotImported(String name, ImportStatus importStatus) {
        CseCriticalBranchCreationContext cseCriticalBranchCreationContext = (CseCriticalBranchCreationContext) cracCreationContext.getBranchCnecCreationContext(name);
        assertNotNull(cseCriticalBranchCreationContext);
        assertFalse(cseCriticalBranchCreationContext.isSelected());
        assertFalse(cseCriticalBranchCreationContext.isImported());
        assertEquals(cseCriticalBranchCreationContext.getImportStatus(), importStatus);
        assertNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(name));
    }

    private void assertMneBaseCaseInCriticalBranchCreationContexts(String name, String fromNode, String toNode, String suffix, boolean inverted) {
        String cnecPreventiveId = String.format("%s - %s->%s - %s", name, fromNode, toNode, preventiveInstant);
        String nameCnec = String.format("%s - %s - %s - %s", name, fromNode, toNode, "basecase");
        CseCriticalBranchCreationContext cseCriticalBranchCreationContext = (CseCriticalBranchCreationContext) cracCreationContext.getBranchCnecCreationContext(nameCnec);
        assertNotNull(cseCriticalBranchCreationContext);
        assertFalse(cseCriticalBranchCreationContext.isSelected());
        assertTrue(cseCriticalBranchCreationContext.isImported());
        assertNotNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(PREVENTIVE_INSTANT_ID));
        assertNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(OUTAGE_INSTANT_ID));
        assertNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(AUTO_INSTANT_ID));
        assertNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(CURATIVE_INSTANT_ID));
        assertTrue(cseCriticalBranchCreationContext.isBaseCase());
        assertTrue(cseCriticalBranchCreationContext.getContingencyId().isEmpty());
        assertEquals(fromNode, cseCriticalBranchCreationContext.getNativeBranch().getFrom());
        assertEquals(toNode, cseCriticalBranchCreationContext.getNativeBranch().getTo());
        assertEquals(suffix, cseCriticalBranchCreationContext.getNativeBranch().getSuffix());
        assertEquals(cnecPreventiveId, cseCriticalBranchCreationContext.getCreatedCnecsIds().get(PREVENTIVE_INSTANT_ID));
        assertEquals(inverted, cseCriticalBranchCreationContext.isDirectionInvertedInNetwork());
    }

    public void assertMneWithContingencyInCrac(String name, String fromNode, String toNode, String suffix, String direction, String contingencyId, Instant instant, double expectedIMax, double expectedThreshold, Unit expectedThresholdUnit, double expectedNV) {
        String createdCnecId = String.format("%s - %s ->%s   - %s - %s", name, fromNode, toNode, contingencyId, instant);
        String nativeId = String.format("%s - %s  - %s  - %s", name, fromNode, toNode, contingencyId);
        Crac crac = cracCreationContext.getCrac();
        FlowCnec flowCnec = crac.getFlowCnec(createdCnecId);
        assertNotNull(flowCnec);
        assertEquals(name, flowCnec.getName());
        assertTrue(flowCnec.isMonitored());
        assertFalse(flowCnec.isOptimized());
        assertEquals(expectedIMax, flowCnec.getIMax(TwoSides.ONE), 0.00001);
        assertEquals(expectedIMax, flowCnec.getIMax(TwoSides.TWO), 0.00001);
        assertTrue(hasThreshold(nativeId, expectedThreshold, expectedThresholdUnit, flowCnec, direction, TwoSides.ONE));
        assertTrue(hasThreshold(nativeId, expectedThreshold, expectedThresholdUnit, flowCnec, direction, TwoSides.TWO));
        assertEquals(contingencyId, flowCnec.getState().getContingency().get().getId());
        assertEquals(instant, flowCnec.getState().getInstant());
        assertEquals(flowCnec.getNetworkElement().getId(), crac.getFlowCnec(createdCnecId).getNetworkElement().getName());
        boolean directionInvertedInNetwork = cracCreationContext.getBranchCnecCreationContext(nativeId).isDirectionInvertedInNetwork();
        if (directionInvertedInNetwork) {
            assertEquals(toNode + "  " + fromNode + "  " + suffix, flowCnec.getNetworkElement().getId());
        } else {
            assertEquals(fromNode + "  " + toNode + "  " + suffix, flowCnec.getNetworkElement().getId());
        }
        assertEquals(expectedNV, flowCnec.getNominalVoltage(TwoSides.TWO), 0.00001);
        assertEquals(expectedNV, flowCnec.getNominalVoltage(TwoSides.ONE), 0.00001);
    }

    public void assertMneBaseCaseInCrac(String name, String fromNode, String toNode, String suffix, String direction, Instant instant, double expectedIMax, double expectedThreshold, Unit expectedThresholdUnit, double expectedNV) {
        String createdCnecId = String.format("%s - %s ->%s  - %s", name, fromNode, toNode, instant);
        String nativeId = String.format("%s - %s  - %s  - %s", name, fromNode, toNode, "basecase");
        Crac crac = cracCreationContext.getCrac();
        FlowCnec flowCnec = crac.getFlowCnec(createdCnecId);
        assertNotNull(flowCnec);
        assertEquals(name, flowCnec.getName());
        assertTrue(flowCnec.isMonitored());
        assertFalse(flowCnec.isOptimized());
        assertEquals(expectedIMax, flowCnec.getIMax(TwoSides.ONE), 0.00001);
        assertEquals(expectedIMax, flowCnec.getIMax(TwoSides.TWO), 0.00001);

        assertTrue(hasThreshold(nativeId, expectedThreshold, expectedThresholdUnit, flowCnec, direction, TwoSides.ONE));
        assertTrue(hasThreshold(nativeId, expectedThreshold, expectedThresholdUnit, flowCnec, direction, TwoSides.TWO));
        assertTrue(flowCnec.getState().isPreventive());
        boolean directionInvertedInNetwork = cracCreationContext.getBranchCnecCreationContext(nativeId).isDirectionInvertedInNetwork();
        if (directionInvertedInNetwork) {
            assertEquals(toNode + "  " + fromNode + "  " + suffix, flowCnec.getNetworkElement().getId());
        } else {
            assertEquals(fromNode + "  " + toNode + "  " + suffix, flowCnec.getNetworkElement().getId());
        }
        assertEquals(expectedNV, flowCnec.getNominalVoltage(TwoSides.TWO), 0.00001);
        assertEquals(expectedNV, flowCnec.getNominalVoltage(TwoSides.ONE), 0.00001);
    }

    private boolean hasThreshold(String nativeId, double expectedThreshold, Unit expectedThresholdUnit, FlowCnec flowCnec, String direction, TwoSides side) {
        boolean directionInvertedInNetwork = cracCreationContext.getBranchCnecCreationContext(nativeId).isDirectionInvertedInNetwork();
        if (!directionInvertedInNetwork && direction.equals("DIRECT")
            || directionInvertedInNetwork && direction.equals("OPPOSITE")) {
            // should have max
            return flowCnec.getThresholds().stream().anyMatch(threshold -> threshold.getSide().equals(side)
                    && threshold.max().isPresent() && threshold.max().get().equals(expectedThreshold)
                    && threshold.getUnit().equals(expectedThresholdUnit)
            );
        }
        if (directionInvertedInNetwork && direction.equals("DIRECT")
            || !directionInvertedInNetwork && direction.equals("OPPOSITE")) {
            // should have min
            return flowCnec.getThresholds().stream().anyMatch(threshold -> threshold.getSide().equals(side)
                    && threshold.min().isPresent() && threshold.min().get().equals(-expectedThreshold)
                    && threshold.getUnit().equals(expectedThresholdUnit)
            );
        }
        // should have min and max
        return flowCnec.getThresholds().stream().anyMatch(threshold ->
                threshold.getSide().equals(side)
                        && threshold.max().isPresent() && threshold.max().get().equals(expectedThreshold)
                        && threshold.min().isPresent() && threshold.min().get().equals(-expectedThreshold)
                        && threshold.getUnit().equals(expectedThresholdUnit)
        );
    }

    public void assertAllMneCorrectlyImportedInCrac() {
        assertMneWithContingencyInCrac("mne_test", "NNL2AA1", "NNL3AA1", "1", "DIRECT", "outage_1", outageInstant, 5000, 1.1, Unit.PERCENT_IMAX, 380);
        assertMneWithContingencyInCrac("mne_test", "NNL2AA1", "NNL3AA1", "1", "DIRECT", "outage_1", autoInstant, 5000, 6000, Unit.AMPERE, 380);
        assertMneWithContingencyInCrac("mne_test", "NNL2AA1", "NNL3AA1", "1", "DIRECT", "outage_1", curativeInstant, 5000, 6500, Unit.AMPERE, 380);
        assertMneWithContingencyInCrac("mne_test", "NNL2AA1", "NNL3AA1", "1", "DIRECT", "outage_2", outageInstant, 5000, 1.1, Unit.PERCENT_IMAX, 380);
        assertMneWithContingencyInCrac("mne_test", "NNL2AA1", "NNL3AA1", "1", "DIRECT", "outage_2", autoInstant, 5000, 6000, Unit.AMPERE, 380);
        assertMneWithContingencyInCrac("mne_test", "NNL2AA1", "NNL3AA1", "1", "DIRECT", "outage_2", curativeInstant, 5000, 6500, Unit.AMPERE, 380);
        assertMneBaseCaseInCrac("mne_test", "NNL2AA1", "NNL3AA1", "1", "DIRECT", preventiveInstant, 5000, 1, Unit.PERCENT_IMAX, 380);
    }

    public void assertAllMneCorrectlyImportedInCriticalBranchesCreationContext() {
        assertMneWithContingencyInCriticalBranchCreationContexts("mne_test", "outage_1", "NNL2AA1 ", "NNL3AA1 ", "1", false);
        assertMneWithContingencyInCriticalBranchCreationContexts("mne_test", "outage_2", "NNL2AA1 ", "NNL3AA1 ", "1", false);
        assertMneNotImported("mne_test - NNL2AA1  - NNL3AA1  - outage_3", ImportStatus.INCOMPLETE_DATA);
        assertMneNotImported("fake_mne_1 ; fake_mne_2", ImportStatus.INCONSISTENCY_IN_DATA);
        assertMneBaseCaseInCriticalBranchCreationContexts("mne_test", "NNL2AA1 ", "NNL3AA1 ", "1", false);
    }

    @Test
    void createCracWithMNELittleCase() throws IOException {
        setUp("/cracs/cse_crac_with_MNE.xml", "/networks/TestCase12Nodes_with_Xnodes.uct");
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertAllMneCorrectlyImportedInCriticalBranchesCreationContext();
        assertAllMneCorrectlyImportedInCrac();
        assertEquals("[REMOVED] Monitored element \"mne_test - NNL2AA1  - NNL3AA1  - outage_3\" was not imported: INCOMPLETE_DATA. CNEC is defined on outage outage_3 which is not defined.", cracCreationContext.getCreationReport().getReport().get(3));
        assertEquals("[REMOVED] Monitored element \"fake_mne_1 ; fake_mne_2\" was not imported: INCONSISTENCY_IN_DATA. MonitoredElement has more than 1 Branch.", cracCreationContext.getCreationReport().getReport().get(2));
    }
}
