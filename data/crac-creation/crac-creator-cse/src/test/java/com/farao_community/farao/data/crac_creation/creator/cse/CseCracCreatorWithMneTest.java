/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cse.critical_branch.CseCriticalBranchCreationContext;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import java.io.InputStream;
import java.time.OffsetDateTime;

import static org.junit.Assert.*;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
public class CseCracCreatorWithMneTest {
    private final OffsetDateTime offsetDateTime = null;
    private CracCreationParameters parameters = new CracCreationParameters();
    private Crac importedCrac;
    private CseCracCreationContext cracCreationContext;

    private void setUp(String cracFileName, String networkFileName) {
        InputStream is = getClass().getResourceAsStream(cracFileName);
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(is);
        Network network = Importers.loadNetwork(networkFileName, getClass().getResourceAsStream(networkFileName));
        CseCracCreator cseCracCreator = new CseCracCreator();
        cracCreationContext = cseCracCreator.createCrac(cseCrac, network, offsetDateTime, parameters);
        importedCrac = cracCreationContext.getCrac();
    }

    private void assertMneWithContingencyInCriticalBranchCreationContexts(String name, String contingencyId, String fromNode, String toNode, String suffix, boolean inverted) {
        String cnecOutageId = String.format("%s - %s->%s  - %s - %s", name, fromNode, toNode, contingencyId, Instant.OUTAGE);
        String cnecCurativeId = String.format("%s - %s->%s  - %s - %s", name, fromNode, toNode, contingencyId, Instant.CURATIVE);
        String cnecAutoId = String.format("%s - %s->%s  - %s - %s", name, fromNode, toNode, contingencyId, Instant.AUTO);
        String nameCnec = String.format("%s - %s - %s - %s", name, fromNode, toNode, contingencyId);
        CseCriticalBranchCreationContext cseCriticalBranchCreationContext = (CseCriticalBranchCreationContext) cracCreationContext.getBranchCnecCreationContext(nameCnec);
        assertNotNull(cseCriticalBranchCreationContext);
        assertFalse(cseCriticalBranchCreationContext.isSelected());
        assertTrue(cseCriticalBranchCreationContext.isImported());
        assertNotNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.OUTAGE));
        assertNotNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.CURATIVE));
        assertNotNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.AUTO));
        assertNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.PREVENTIVE));
        assertEquals(cseCriticalBranchCreationContext.getImportStatus(), ImportStatus.IMPORTED);
        assertFalse(cseCriticalBranchCreationContext.isBaseCase());
        assertEquals(contingencyId, cseCriticalBranchCreationContext.getContingencyId().orElseThrow());
        assertEquals(fromNode, cseCriticalBranchCreationContext.getNativeBranch().getFrom());
        assertEquals(toNode, cseCriticalBranchCreationContext.getNativeBranch().getTo());
        assertEquals(suffix, cseCriticalBranchCreationContext.getNativeBranch().getSuffix());
        assertEquals(cnecOutageId, cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.OUTAGE));
        assertEquals(cnecCurativeId, cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.CURATIVE));
        assertEquals(cnecAutoId, cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.AUTO));
        assertEquals(inverted, cseCriticalBranchCreationContext.isDirectionInvertedInNetwork());
    }

    private void assertMneWithContingencyNotInCriticalBranchCreationContexts(String name, ImportStatus importStatus) {
        CseCriticalBranchCreationContext cseCriticalBranchCreationContext = (CseCriticalBranchCreationContext) cracCreationContext.getBranchCnecCreationContext(name);
        assertNotNull(cseCriticalBranchCreationContext);
        assertFalse(cseCriticalBranchCreationContext.isSelected());
        assertFalse(cseCriticalBranchCreationContext.isImported());
        assertEquals(cseCriticalBranchCreationContext.getImportStatus(), importStatus);
        assertNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(name));
        assertNull(cracCreationContext.getCrac().getFlowCnec(name));
    }

    private void assertMneBaseCaseInCriticalBranchCreationContexts(String name, String fromNode, String toNode, String suffix, boolean inverted) {
        String cnecPreventiveId = String.format("%s - %s->%s - %s", name, fromNode, toNode, Instant.PREVENTIVE);
        String nameCnec = String.format("%s - %s - %s - %s", name, fromNode, toNode, "basecase");
        CseCriticalBranchCreationContext cseCriticalBranchCreationContext = (CseCriticalBranchCreationContext) cracCreationContext.getBranchCnecCreationContext(nameCnec);
        assertNotNull(cseCriticalBranchCreationContext);
        assertFalse(cseCriticalBranchCreationContext.isSelected());
        assertTrue(cseCriticalBranchCreationContext.isImported());
        assertNotNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.PREVENTIVE));
        assertNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.OUTAGE));
        assertNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.AUTO));
        assertNull(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.CURATIVE));
        assertTrue(cseCriticalBranchCreationContext.isBaseCase());
        assertTrue(cseCriticalBranchCreationContext.getContingencyId().isEmpty());
        assertEquals(fromNode, cseCriticalBranchCreationContext.getNativeBranch().getFrom());
        assertEquals(toNode, cseCriticalBranchCreationContext.getNativeBranch().getTo());
        assertEquals(suffix, cseCriticalBranchCreationContext.getNativeBranch().getSuffix());
        assertEquals(cnecPreventiveId, cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.PREVENTIVE));
        assertEquals(inverted, cseCriticalBranchCreationContext.isDirectionInvertedInNetwork());
    }

    public void assertMneWithContingencyInCrac(String name, String fromNode, String toNode, String suffix, String direction, String contingencyId, Instant instant, double expectedIMax, double expectedThreshold, Unit expectedThresholdUnit, double expectedNV) {
        String createdCnecId = String.format("%s - %s ->%s   - %s - %s", name, fromNode, toNode, contingencyId, instant);
        String nativeId = String.format("%s - %s  - %s  - %s", name, fromNode, toNode, contingencyId);
        Crac crac = cracCreationContext.getCrac();
        assertNotNull(crac.getFlowCnec(createdCnecId));
        assertEquals(name, crac.getFlowCnec(createdCnecId).getName());
        assertTrue(crac.getFlowCnec(createdCnecId).isMonitored());
        assertFalse(crac.getFlowCnec(createdCnecId).isOptimized());
        assertEquals(expectedIMax, crac.getFlowCnec(createdCnecId).getIMax(Side.LEFT), 0.00001);
        assertEquals(expectedIMax, crac.getFlowCnec(createdCnecId).getIMax(Side.RIGHT), 0.00001);
        FlowCnec flowCnec = crac.getFlowCnec(createdCnecId);
        assertTrue(hasThreshold(nativeId, expectedThreshold, expectedThresholdUnit, flowCnec, direction, Side.LEFT));
        assertTrue(hasThreshold(nativeId, expectedThreshold, expectedThresholdUnit, flowCnec, direction, Side.RIGHT));
        assertEquals(crac.getFlowCnec(createdCnecId).getState().getInstant(), instant);
        assertEquals(crac.getFlowCnec(createdCnecId).getNetworkElement().getId(), crac.getFlowCnec(createdCnecId).getNetworkElement().getName());
        boolean directionInvertedInNetwork = cracCreationContext.getBranchCnecCreationContext(nativeId).isDirectionInvertedInNetwork();
        if (directionInvertedInNetwork) {
            assertEquals(toNode + "  " + fromNode + "  " + suffix, crac.getFlowCnec(createdCnecId).getNetworkElement().getId());
        } else {
            assertEquals(fromNode + "  " + toNode + "  " + suffix, crac.getFlowCnec(createdCnecId).getNetworkElement().getId());

        }
        assertEquals(crac.getFlowCnec(createdCnecId).getNominalVoltage(Side.RIGHT), crac.getFlowCnec(createdCnecId).getNominalVoltage(Side.RIGHT));
        assertEquals(expectedNV, crac.getFlowCnec(createdCnecId).getNominalVoltage(Side.RIGHT), 0.00001);
    }

    public void assertMneBaseCaseInCrac(String name, String fromNode, String toNode, String suffix, String direction, Instant instant, double expectedIMax, double expectedThreshold, Unit expectedThresholdUnit, double expectedNV) {
        String createdCnecId = String.format("%s - %s ->%s  - %s", name, fromNode, toNode, instant);
        String nativeId = String.format("%s - %s  - %s  - %s", name, fromNode, toNode, "basecase");
        Crac crac = cracCreationContext.getCrac();
        assertNotNull(crac.getFlowCnec(createdCnecId));
        assertEquals(name, crac.getFlowCnec(createdCnecId).getName());
        assertTrue(crac.getFlowCnec(createdCnecId).isMonitored());
        assertFalse(crac.getFlowCnec(createdCnecId).isOptimized());
        assertEquals(expectedIMax, crac.getFlowCnec(createdCnecId).getIMax(Side.LEFT), 0.00001);
        assertEquals(expectedIMax, crac.getFlowCnec(createdCnecId).getIMax(Side.RIGHT), 0.00001);
        FlowCnec flowCnec = crac.getFlowCnec(createdCnecId);
        assertTrue(hasThreshold(nativeId, expectedThreshold, expectedThresholdUnit, flowCnec, direction, Side.LEFT));
        assertTrue(hasThreshold(nativeId, expectedThreshold, expectedThresholdUnit, flowCnec, direction, Side.RIGHT));
        assertTrue(crac.getFlowCnec(createdCnecId).getState().isPreventive());
        assertEquals(crac.getFlowCnec(createdCnecId).getNetworkElement().getId(), crac.getFlowCnec(createdCnecId).getNetworkElement().getName());
        boolean directionInvertedInNetwork = cracCreationContext.getBranchCnecCreationContext(nativeId).isDirectionInvertedInNetwork();
        if (directionInvertedInNetwork) {
            assertEquals(toNode + "  " + fromNode + "  " + suffix, crac.getFlowCnec(createdCnecId).getNetworkElement().getId());
        } else {
            assertEquals(fromNode + "  " + toNode + "  " + suffix, crac.getFlowCnec(createdCnecId).getNetworkElement().getId());
        }
        assertEquals(crac.getFlowCnec(createdCnecId).getNominalVoltage(Side.RIGHT), crac.getFlowCnec(createdCnecId).getNominalVoltage(Side.RIGHT));
        assertEquals(expectedNV, crac.getFlowCnec(createdCnecId).getNominalVoltage(Side.RIGHT), 0.00001);
    }

    private boolean hasThreshold(String nativeId, double expectedThreshold, Unit expectedThresholdUnit, FlowCnec flowCnec, String direction, Side side) {
        boolean directionInvertedInNetwork = cracCreationContext.getBranchCnecCreationContext(nativeId).isDirectionInvertedInNetwork();
        if (direction.equals("DIRECT")) {
            return flowCnec.getThresholds().stream().anyMatch(threshold ->
                    threshold.getSide().equals(side)
                            && ((!directionInvertedInNetwork && threshold.max().isPresent() && threshold.max().get().equals(expectedThreshold)) || (directionInvertedInNetwork && threshold.min().isPresent() && threshold.min().get().equals(-expectedThreshold)))
                            && (threshold.getUnit().equals(expectedThresholdUnit))
            );
        } else if (direction.equals("OPPOSITE")) {
            return flowCnec.getThresholds().stream().anyMatch(threshold ->
                    threshold.getSide().equals(side)
                            && ((directionInvertedInNetwork && threshold.max().isPresent() && threshold.max().get().equals(expectedThreshold)) || (!directionInvertedInNetwork && threshold.min().isPresent() && threshold.min().get().equals(-expectedThreshold)))
                            && (threshold.getUnit().equals(expectedThresholdUnit))
            );
        } else {
            return flowCnec.getThresholds().stream().anyMatch(threshold ->
                    threshold.getSide().equals(side)
                            && threshold.max().isPresent() && threshold.max().get().equals(expectedThreshold)
                            && threshold.min().isPresent() && threshold.min().get().equals(-expectedThreshold)
                            && threshold.getUnit().equals(expectedThresholdUnit)
            );
        }
    }

    public void assertAllMneCorrectlyImportedInCrac() {
        assertMneWithContingencyInCrac("mne_test", "NNL3AA1", "NNL2AA1", "1", "DIRECT", "outage_1", Instant.OUTAGE, 5000, 1.1, Unit.PERCENT_IMAX, 380);
        assertMneWithContingencyInCrac("mne_test", "NNL3AA1", "NNL2AA1", "1", "DIRECT", "outage_1", Instant.AUTO, 5000, 6000, Unit.AMPERE, 380);
        assertMneWithContingencyInCrac("mne_test", "NNL3AA1", "NNL2AA1", "1", "DIRECT", "outage_1", Instant.CURATIVE, 5000, 6500, Unit.AMPERE, 380);
        assertMneWithContingencyInCrac("mne_test", "NNL3AA1", "NNL2AA1", "1", "DIRECT", "outage_2", Instant.OUTAGE, 5000, 1.1, Unit.PERCENT_IMAX, 380);
        assertMneWithContingencyInCrac("mne_test", "NNL3AA1", "NNL2AA1", "1", "DIRECT", "outage_2", Instant.AUTO, 5000, 6000, Unit.AMPERE, 380);
        assertMneWithContingencyInCrac("mne_test", "NNL3AA1", "NNL2AA1", "1", "DIRECT", "outage_2", Instant.CURATIVE, 5000, 6500, Unit.AMPERE, 380);
        assertMneBaseCaseInCrac("mne_test", "NNL3AA1", "NNL2AA1", "1", "DIRECT", Instant.PREVENTIVE, 5000, 1, Unit.PERCENT_IMAX, 380);
    }

    public void assertAllMneCorrectlyImportedInCriticalBranchesCreationContext() {
        assertMneWithContingencyInCriticalBranchCreationContexts("mne_test", "outage_1", "NNL3AA1 ", "NNL2AA1 ", "1", true);
        assertMneWithContingencyInCriticalBranchCreationContexts("mne_test", "outage_2", "NNL3AA1 ", "NNL2AA1 ", "1", true);
        assertMneWithContingencyNotInCriticalBranchCreationContexts("mne_test - NNL3AA1  - NNL2AA1  - outage_3", ImportStatus.INCOMPLETE_DATA);
        assertMneWithContingencyNotInCriticalBranchCreationContexts("fake_mne_1 - NNL1AA1  - NNL3AA1  - mneHasTooManyBranches", ImportStatus.NOT_YET_HANDLED_BY_FARAO);
        assertMneBaseCaseInCriticalBranchCreationContexts("mne_test", "NNL3AA1 ", "NNL2AA1 ", "1", true);
    }

    @Test
    public void createCracWithMNELittleCase() {
        String cracName = "/cracs/cse_crac_with_MNE.xml";
        String networkName = "/networks/TestCase12Nodes_with_Xnodes.uct";
        setUp(cracName, networkName);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertAllMneCorrectlyImportedInCriticalBranchesCreationContext();
        assertAllMneCorrectlyImportedInCrac();
        assertEquals("[REMOVED] Critical branch \"mne_test - NNL3AA1  - NNL2AA1  - outage_3\" was not imported: INCOMPLETE_DATA. CNEC is defined on outage null which is not defined.", cracCreationContext.getCreationReport().getReport().get(3));
        assertEquals("[REMOVED] Critical branch \"fake_mne_1 - NNL1AA1  - NNL3AA1  - mneHasTooManyBranches\" was not imported: NOT_YET_HANDLED_BY_FARAO. MonitoredElement has more than 1 Branch.", cracCreationContext.getCreationReport().getReport().get(2));
    }
}
