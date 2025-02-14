/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.cim.craccreator;

import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.io.cim.parameters.CimCracCreationParameters;
import com.powsybl.openrao.data.crac.io.cim.parameters.VoltageCnecsCreationParameters;
import com.powsybl.openrao.data.crac.io.cim.parameters.VoltageMonitoredContingenciesAndThresholds;
import com.powsybl.openrao.data.crac.io.cim.parameters.VoltageThreshold;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class VoltageCnecsCreatorTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Crac crac;
    private CimCracCreationContext cracCreationContext;
    private Network network;
    private VoltageCnecsCreationParameters voltageCnecsCreationParameters;
    private Set<String> monitoredElements;
    private Map<String, VoltageMonitoredContingenciesAndThresholds> monitoredStatesAndThresholds;

    @BeforeEach
    public void setUp() throws IOException {
        Properties importParams = new Properties();
        importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        network = Network.read(Paths.get(new File(CimCracCreatorTest.class.getResource("/networks/MicroGrid.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        InputStream is = getClass().getResourceAsStream("/cracs/CIM_21_1_1.xml");
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.addExtension(CimCracCreationParameters.class, new CimCracCreationParameters());
        cracCreationParameters.getExtension(CimCracCreationParameters.class).setTimestamp(OffsetDateTime.parse("2021-04-01T23:00Z"));
        cracCreationContext = (CimCracCreationContext) Crac.readWithContext("CIM_21_1_1.xml", is, network, cracCreationParameters);
        crac = cracCreationContext.getCrac();

        // Imported contingencies (name -> id):
        // Co-1-name -> Co-1
        // Co-2-name -> Co-2
        // Co-3-name -> Co-3
        // Not imported contingencies (name):
        // Co-4-name
        // Co-5-name

        network.getIdentifiable("_d77b61ef-61aa-4b22-95f6-b56ca080788d").addAlias("VL2-alias");
        monitoredElements = Set.of("_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3", "VL2-alias");

        monitoredStatesAndThresholds = Map.of(
            PREVENTIVE_INSTANT_ID, new VoltageMonitoredContingenciesAndThresholds(null, Map.of(220., mockVoltageThreshold(215., 225.), 400., mockVoltageThreshold(390., 410.))),
            CURATIVE_INSTANT_ID, new VoltageMonitoredContingenciesAndThresholds(null, Map.of(220., mockVoltageThreshold(210., 240.), 400., mockVoltageThreshold(null, 450.))),
            OUTAGE_INSTANT_ID, new VoltageMonitoredContingenciesAndThresholds(Set.of("Co-3-name"), Map.of(220., mockVoltageThreshold(200., null), 400., mockVoltageThreshold(380., 420.))),
            AUTO_INSTANT_ID, new VoltageMonitoredContingenciesAndThresholds(Set.of("Co-1-name"), Map.of(220., mockVoltageThreshold(null, 250.), 400., mockVoltageThreshold(370., null)))
        );

        voltageCnecsCreationParameters = new VoltageCnecsCreationParameters(monitoredStatesAndThresholds, monitoredElements);
    }

    private VoltageThreshold mockVoltageThreshold(Double min, Double max) {
        VoltageThreshold threshold = Mockito.mock(VoltageThreshold.class);
        Mockito.when(threshold.getUnit()).thenReturn(Unit.KILOVOLT);
        Mockito.when(threshold.getMin()).thenReturn(min);
        Mockito.when(threshold.getMax()).thenReturn(max);
        return threshold;
    }

    @Test
    void testWrongNetworkElements() {
        // Test to monitor one element not in network, one element that is a generator, one that is a transformer (different nominalV on two ends)
        String missing = "missing";
        String generator = "_2844585c-0d35-488d-a449-685bcd57afbf";
        String transformer = "_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0";
        voltageCnecsCreationParameters = new VoltageCnecsCreationParameters(monitoredStatesAndThresholds, Set.of(missing, generator, transformer));
        new VoltageCnecsCreator(voltageCnecsCreationParameters, cracCreationContext, network).createAndAddCnecs();

        // Check that no voltage cnec was created
        assertEquals(0, crac.getVoltageCnecs().size());
        assertEquals(3, cracCreationContext.getVoltageCnecCreationContexts().size());

        // Check that info about missing element is saved in context
        assertEquals(1, cracCreationContext.getVoltageCnecCreationContextsForNetworkElement(missing).size());
        assertFalse(cracCreationContext.getVoltageCnecCreationContextsForNetworkElement(missing).iterator().next().isImported());
        assertEquals(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, cracCreationContext.getVoltageCnecCreationContextsForNetworkElement(missing).iterator().next().getImportStatus());

        // Check that info about generator is saved in context
        assertEquals(1, cracCreationContext.getVoltageCnecCreationContextsForNetworkElement(generator).size());
        assertFalse(cracCreationContext.getVoltageCnecCreationContextsForNetworkElement(generator).iterator().next().isImported());
        assertEquals(ImportStatus.INCONSISTENCY_IN_DATA, cracCreationContext.getVoltageCnecCreationContextsForNetworkElement(generator).iterator().next().getImportStatus());

        // Check that info about transformer is saved in context
        assertEquals(1, cracCreationContext.getVoltageCnecCreationContextsForNetworkElement(transformer).size());
        assertFalse(cracCreationContext.getVoltageCnecCreationContextsForNetworkElement(transformer).iterator().next().isImported());
        assertEquals(ImportStatus.INCONSISTENCY_IN_DATA, cracCreationContext.getVoltageCnecCreationContextsForNetworkElement(transformer).iterator().next().getImportStatus());
    }

    @Test
    void testWrongContingencies() {
        // One contingency was not imported, one contingency does not even exist in CRAC
        monitoredStatesAndThresholds = Map.of(
            CURATIVE_INSTANT_ID, new VoltageMonitoredContingenciesAndThresholds(Set.of("Co-4-name", "co-missing"), Map.of(225., mockVoltageThreshold(360., 440.)))
        );
        voltageCnecsCreationParameters = new VoltageCnecsCreationParameters(monitoredStatesAndThresholds, monitoredElements);
        new VoltageCnecsCreator(voltageCnecsCreationParameters, cracCreationContext, network).createAndAddCnecs();

        // Check that no voltage cnec was created
        assertEquals(0, crac.getVoltageCnecs().size());
        assertEquals(2, cracCreationContext.getVoltageCnecCreationContexts().size());

        // Check that info about missing contingencies
        assertEquals(1, cracCreationContext.getVoltageCnecCreationContextsForContingency("Co-4-name").size());
        assertFalse(cracCreationContext.getVoltageCnecCreationContextsForContingency("Co-4-name").iterator().next().isImported());
        assertEquals(ImportStatus.OTHER, cracCreationContext.getVoltageCnecCreationContextsForContingency("Co-4-name").iterator().next().getImportStatus());

        assertEquals(1, cracCreationContext.getVoltageCnecCreationContextsForContingency("co-missing").size());
        assertFalse(cracCreationContext.getVoltageCnecCreationContextsForContingency("co-missing").iterator().next().isImported());
        assertEquals(ImportStatus.OTHER, cracCreationContext.getVoltageCnecCreationContextsForContingency("co-missing").iterator().next().getImportStatus());
    }

    @Test
    void testNominalVNotDefinedInThreshold() {
        // Nominal V is 225, not defined in thresholds
        monitoredStatesAndThresholds = Map.of(
            CURATIVE_INSTANT_ID, new VoltageMonitoredContingenciesAndThresholds(Set.of("Co-3-name"), Map.of(100., mockVoltageThreshold(0., 1000.)))
        );
        voltageCnecsCreationParameters = new VoltageCnecsCreationParameters(monitoredStatesAndThresholds, monitoredElements);
        new VoltageCnecsCreator(voltageCnecsCreationParameters, cracCreationContext, network).createAndAddCnecs();

        // Check that no voltage cnec was created
        assertEquals(0, crac.getVoltageCnecs().size());
        assertEquals(2, cracCreationContext.getVoltageCnecCreationContexts().size());

        assertEquals(1, cracCreationContext.getVoltageCnecCreationContextsForNetworkElement("_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3").size());
        assertFalse(cracCreationContext.getVoltageCnecCreationContextsForNetworkElement("_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3").iterator().next().isImported());
        assertEquals(ImportStatus.INCOMPLETE_DATA, cracCreationContext.getVoltageCnecCreationContextsForNetworkElement("_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3").iterator().next().getImportStatus());

        assertEquals(1, cracCreationContext.getVoltageCnecCreationContextsForNetworkElement("VL2-alias").size());
        assertFalse(cracCreationContext.getVoltageCnecCreationContextsForNetworkElement("VL2-alias").iterator().next().isImported());
        assertEquals(ImportStatus.INCOMPLETE_DATA, cracCreationContext.getVoltageCnecCreationContextsForNetworkElement("VL2-alias").iterator().next().getImportStatus());
    }

    @Test
    void testWrongThreshold() {
        // unacceptable threshold
        monitoredStatesAndThresholds = Map.of(
            CURATIVE_INSTANT_ID, new VoltageMonitoredContingenciesAndThresholds(Set.of("Co-3-name"), Map.of(220., mockVoltageThreshold(null, null), 400., mockVoltageThreshold(null, null)))
        );
        voltageCnecsCreationParameters = new VoltageCnecsCreationParameters(monitoredStatesAndThresholds, monitoredElements);
        new VoltageCnecsCreator(voltageCnecsCreationParameters, cracCreationContext, network).createAndAddCnecs();

        // Check that no voltage cnec was created
        assertEquals(0, crac.getVoltageCnecs().size());
        assertEquals(2, cracCreationContext.getVoltageCnecCreationContexts().size());

        assertEquals(1, cracCreationContext.getVoltageCnecCreationContextsForNetworkElement("_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3").size());
        assertFalse(cracCreationContext.getVoltageCnecCreationContextsForNetworkElement("_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3").iterator().next().isImported());
        assertEquals(ImportStatus.INCONSISTENCY_IN_DATA, cracCreationContext.getVoltageCnecCreationContextsForNetworkElement("_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3").iterator().next().getImportStatus());

        assertEquals(1, cracCreationContext.getVoltageCnecCreationContextsForNetworkElement("VL2-alias").size());
        assertFalse(cracCreationContext.getVoltageCnecCreationContextsForNetworkElement("VL2-alias").iterator().next().isImported());
        assertEquals(ImportStatus.INCONSISTENCY_IN_DATA, cracCreationContext.getVoltageCnecCreationContextsForNetworkElement("VL2-alias").iterator().next().getImportStatus());
    }

    private void assertVoltageCnecImported(String nativeNetworkElementId, String networkElementId, String instantId, String nativeContingencyName, String contingencyId, Double thresholdMin, Double thresholdMax) {
        VoltageCnecCreationContext context = cracCreationContext.getVoltageCnecCreationContext(nativeNetworkElementId, instantId, nativeContingencyName);
        assertNotNull(context);

        String cnecId;
        if (contingencyId != null) {
            cnecId = String.format("[VC] %s - %s - %s", networkElementId, contingencyId, instantId);
        } else {
            cnecId = String.format("[VC] %s - %s", networkElementId, instantId);
        }

        assertEquals(cnecId, context.getCreatedCnecId());
        VoltageCnec cnec = crac.getVoltageCnec(cnecId);
        assertNotNull(cnec);
        assertEquals(instantId, cnec.getState().getInstant().getId());
        assertEquals(contingencyId, cnec.getState().getContingency().isPresent() ? cnec.getState().getContingency().get().getId() : null);
        assertEquals(1, cnec.getThresholds().size());
        assertEquals(Optional.ofNullable(thresholdMin), cnec.getThresholds().iterator().next().min());
        assertEquals(Optional.ofNullable(thresholdMax), cnec.getThresholds().iterator().next().max());
    }

    @Test
    void testCreateVoltageCnecsSuccessfully() {
        new VoltageCnecsCreator(voltageCnecsCreationParameters, cracCreationContext, network).createAndAddCnecs();

        assertEquals(12, crac.getVoltageCnecs().size());
        assertEquals(12, cracCreationContext.getVoltageCnecCreationContexts().size());
        assertTrue(cracCreationContext.getVoltageCnecCreationContexts().stream().allMatch(VoltageCnecCreationContext::isImported));

        assertVoltageCnecImported("_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3", "_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3", PREVENTIVE_INSTANT_ID, null, null, 390., 410.);
        assertVoltageCnecImported("_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3", "_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3", CURATIVE_INSTANT_ID, "Co-1-name", "Co-1", null, 450.);
        assertVoltageCnecImported("_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3", "_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3", CURATIVE_INSTANT_ID, "Co-2-name", "Co-2", null, 450.);
        assertVoltageCnecImported("_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3", "_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3", CURATIVE_INSTANT_ID, "Co-3-name", "Co-3", null, 450.);
        assertVoltageCnecImported("_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3", "_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3", OUTAGE_INSTANT_ID, "Co-3-name", "Co-3", 380., 420.);
        assertVoltageCnecImported("_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3", "_b7998ae6-0cc6-4dfe-8fec-0b549b07b6c3", AUTO_INSTANT_ID, "Co-1-name", "Co-1", 370., null);

        assertVoltageCnecImported("VL2-alias", "_d77b61ef-61aa-4b22-95f6-b56ca080788d", PREVENTIVE_INSTANT_ID, null, null, 215., 225.);
        assertVoltageCnecImported("VL2-alias", "_d77b61ef-61aa-4b22-95f6-b56ca080788d", CURATIVE_INSTANT_ID, "Co-1-name", "Co-1", 210., 240.);
        assertVoltageCnecImported("VL2-alias", "_d77b61ef-61aa-4b22-95f6-b56ca080788d", CURATIVE_INSTANT_ID, "Co-2-name", "Co-2", 210., 240.);
        assertVoltageCnecImported("VL2-alias", "_d77b61ef-61aa-4b22-95f6-b56ca080788d", CURATIVE_INSTANT_ID, "Co-3-name", "Co-3", 210., 240.);
        assertVoltageCnecImported("VL2-alias", "_d77b61ef-61aa-4b22-95f6-b56ca080788d", OUTAGE_INSTANT_ID, "Co-3-name", "Co-3", 200., null);
        assertVoltageCnecImported("VL2-alias", "_d77b61ef-61aa-4b22-95f6-b56ca080788d", AUTO_INSTANT_ID, "Co-1-name", "Co-1", null, 250.);
    }
}
