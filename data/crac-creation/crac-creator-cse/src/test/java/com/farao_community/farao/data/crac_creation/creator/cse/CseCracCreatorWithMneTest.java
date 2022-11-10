/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.logs.RaoBusinessLogs;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cse.critical_branch.CseCriticalBranchCreationContext;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import javax.print.attribute.standard.Sides;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
public class CseCracCreatorWithMneTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

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

    private void assertMneImportedInCracObject(String name) {
        assertNotNull(importedCrac.getFlowCnec(name));
        assertTrue(importedCrac.getFlowCnec(name).isMonitored());
        assertFalse(importedCrac.getFlowCnec(name).isOptimized());
    }

    private void assertMneWithContingencyInCriticalBranchCreationContexts(String name, String contingencyId) {
        CseCriticalBranchCreationContext cseCriticalBranchCreationContext = (CseCriticalBranchCreationContext) cracCreationContext.getBranchCnecCreationContext(name);
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
        // TODO getcreatedcnecsids name etc.
    }

    private void assertMneWithContingencyNotInCriticalBranchCreationContexts(String name) {
        CseCriticalBranchCreationContext cseCriticalBranchCreationContext = (CseCriticalBranchCreationContext) cracCreationContext.getBranchCnecCreationContext(name);
        assertNotNull(cseCriticalBranchCreationContext);
        assertFalse(cseCriticalBranchCreationContext.isSelected());
        assertFalse(cseCriticalBranchCreationContext.isImported());
        assertEquals(cseCriticalBranchCreationContext.getImportStatus(), ImportStatus.INCOMPLETE_DATA);
        // TODO getcreated ideas null
    }

    private void assertMneBaseCaseInCriticalBranchCreationContexts(String name) {
        CseCriticalBranchCreationContext cseCriticalBranchCreationContext = (CseCriticalBranchCreationContext) cracCreationContext.getBranchCnecCreationContext(name);
        assertNotNull(cseCriticalBranchCreationContext);
        assertFalse(cseCriticalBranchCreationContext.isSelected());
        assertTrue(cseCriticalBranchCreationContext.isImported());
        assertTrue(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.PREVENTIVE), true);
        assertFalse(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.OUTAGE), false);
        assertFalse(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.AUTO), false);
        assertFalse(cseCriticalBranchCreationContext.getCreatedCnecsIds().get(Instant.CURATIVE), false);
        assertTrue(cseCriticalBranchCreationContext.isBaseCase());
        assertTrue(cseCriticalBranchCreationContext.getContingencyId().isEmpty());
        // TODO getcreatedcnecsids name etc.
        // TODO assertnotnull null
    }

    private ListAppender<ILoggingEvent> getLogs(Class clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    public void assertMneInCrac(String createdCnecId, String nativeId, String expectedName, double expectedThreshold, Unit expectedThresholdUnit){
        Crac crac = cracCreationContext.getCrac();
        assertNotNull(crac.getFlowCnec(createdCnecId));
        assertEquals(expectedName, crac.getFlowCnec(createdCnecId).getName());
        assertTrue(crac.getFlowCnec(createdCnecId).isMonitored());
        assertFalse(crac.getFlowCnec(createdCnecId).isOptimized());
        assertEquals(expectedThreshold, crac.getFlowCnec(createdCnecId).getIMax(Side.LEFT), 0.00001);
        assertEquals(expectedThreshold, crac.getFlowCnec(createdCnecId).getIMax(Side.RIGHT), 0.00001);
        FlowCnec flowCnec = crac.getFlowCnec(createdCnecId);
        assertTrue(hasThreshold(nativeId, expectedThreshold, expectedThresholdUnit, flowCnec, Side.LEFT));
        assertTrue(hasThreshold(nativeId, expectedThreshold, expectedThresholdUnit, flowCnec, Side.RIGHT));
    }

    private boolean hasThreshold(String nativeId, double expectedThreshold, Unit expectedThresholdUnit, FlowCnec flowCnec, Side side) {
        boolean b = cracCreationContext.getBranchCnecCreationContext(nativeId).isDirectionInvertedInNetwork();
        return flowCnec.getThresholds().stream().anyMatch(threshold->
                threshold.getSide().equals(side)
                        && ((!b && threshold.max().isPresent() && threshold.max().get().equals(expectedThreshold)) || (b && threshold.min().isPresent() && threshold.min().get().equals(expectedThreshold)))
                        && (threshold.getUnit().equals(expectedThresholdUnit))
        );
    }

    @Test
    public void createCracWithMNELittleCase() {
        String cracName = "/cracs/cse_crac_with_MNE(cnec1become_mne).xml";
        String networkName = "/networks/TestCase12Nodes_with_Xnodes.uct";
        setUp(cracName, networkName);
        assertTrue(cracCreationContext.isCreationSuccessful());
        assertMneImportedInCracObject("fake_mne - NNL2AA1 ->NNL3AA1  - preventive");
        assertMneWithContingencyInCriticalBranchCreationContexts("fake_mne - NNL2AA1  - NNL3AA1  - outage_1", "outage_1");
        assertMneWithContingencyInCriticalBranchCreationContexts("fake_mne - NNL2AA1  - NNL3AA1  - outage_2", "outage_2");
        assertMneWithContingencyNotInCriticalBranchCreationContexts("fake_mne - NNL2AA1  - NNL3AA1  - outage_3");
        assertMneBaseCaseInCriticalBranchCreationContexts("fake_mne - NNL2AA1  - NNL3AA1  - basecase");
        ListAppender<ILoggingEvent> listAppender = getLogs(RaoBusinessLogs.class);
        cracCreationContext.getCreationReport().printCreationReport();
        // cracCreationContext.getCreationReport().getReport() replace les logs
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(logsList.get(2).getMessage(), "[REMOVED] Critical branch \"fake_mne - NNL2AA1  - NNL3AA1  - outage_3\" was not imported: INCOMPLETE_DATA. CNEC is defined on outage null which is not defined.");
        assertMneInCrac("fake_mne - NNL2AA1 ->NNL3AA1   - outage_1 - outage", "fake_mne - NNL2AA1  - NNL3AA1  - outage_1", "fake_mne", 5000);
        assertMneInCrac("fake_mne - NNL2AA1 ->NNL3AA1   - outage_1 - auto", "fake_mne - NNL2AA1  - NNL3AA1  - outage_1", "fake_mne", 5000);
        assertMneInCrac("fake_mne - NNL2AA1 ->NNL3AA1   - outage_1 - curative", "fake_mne - NNL2AA1  - NNL3AA1  - outage_1", "fake_mne", 5000);
        assertMneInCrac("fake_mne - NNL2AA1 ->NNL3AA1   - outage_2 - outage", "fake_mne - NNL2AA1  - NNL3AA1  - outage_2", "fake_mne", 5000);
        assertMneInCrac("fake_mne - NNL2AA1 ->NNL3AA1   - outage_2 - auto", "fake_mne - NNL2AA1  - NNL3AA1  - outage_2", "fake_mne", 5000);
        assertMneInCrac("fake_mne - NNL2AA1 ->NNL3AA1   - outage_2 - curative", "fake_mne - NNL2AA1  - NNL3AA1  - outage_2", "fake_mne", 5000);
        assertMneInCrac("fake_mne - NNL2AA1 ->NNL3AA1  - preventive", "fake_mne - NNL2AA1  - NNL3AA1  - basecase", "fake_mne", 5000);
        Crac crac = cracCreationContext.getCrac();
        assertEquals(crac.getFlowCnec("fake_mne - NNL2AA1 ->NNL3AA1   - outage_1 - outage").getState().getInstant(), Instant.OUTAGE);
        assertEquals(crac.getFlowCnec("fake_mne - NNL2AA1 ->NNL3AA1   - outage_1 - auto").getState().getInstant(), Instant.AUTO);
        assertEquals(crac.getFlowCnec("fake_mne - NNL2AA1 ->NNL3AA1   - outage_1 - curative").getState().getInstant(), Instant.CURATIVE);
        assertEquals(crac.getFlowCnec("fake_mne - NNL2AA1 ->NNL3AA1   - outage_1 - outage").getState().getInstant(), Instant.OUTAGE);
        assertEquals(crac.getFlowCnec("fake_mne - NNL2AA1 ->NNL3AA1   - outage_1 - auto").getState().getInstant(), Instant.AUTO);
        assertEquals(crac.getFlowCnec("fake_mne - NNL2AA1 ->NNL3AA1   - outage_1 - curative").getState().getInstant(), Instant.CURATIVE);
        assertTrue(crac.getFlowCnec("fake_mne - NNL2AA1 ->NNL3AA1  - preventive").getState().isPreventive());
        // TODO Refacto les Tests / Assert sûrement + Add test si 2 ou plus Branches par MonitoredElement mais dev pas encore fait
        // TODO add mne with diff imax
        // TODO Faire le test avec uct inversé // Mne inversé plutot
    }
}
