package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.threshold.BranchThreshold;
import com.powsybl.openrao.data.cracapi.threshold.Threshold;
import com.powsybl.openrao.data.cracapi.usagerule.*;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.CsaProfileCrac;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.importer.CsaProfileCracImporter;
import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.parameters.CsaCracCreationParameters;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public final class CsaProfileCracCreationTestUtil {

    private CsaProfileCracCreationTestUtil() {
    }

    public static ListAppender<ILoggingEvent> getLogs(Class<?> logsClass) {
        Logger logger = (Logger) LoggerFactory.getLogger(logsClass);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    public static void assertContingencyEquality(Contingency c, String expectedContingencyId, String expectedContingencyName, int expectedNetworkElementsSize, List<String> expectedNetworkElementsIds) {
        assertEquals(expectedContingencyId, c.getId());
        assertEquals(expectedContingencyName, c.getName());
        List<NetworkElement> networkElements = c.getNetworkElements().stream()
            .sorted(Comparator.comparing(NetworkElement::getId)).toList();
        assertEquals(expectedNetworkElementsSize, networkElements.size());
        for (int i = 0; i < expectedNetworkElementsSize; i++) {
            assertEquals(expectedNetworkElementsIds.get(i), networkElements.get(i).getId());
        }
    }

    public static void assertFlowCnecEquality(FlowCnec fc, String expectedFlowCnecId, String expectedFlowCnecName, String expectedNetworkElementId,
                                              Instant expectedInstant, String expectedContingencyId, Double expectedThresholdMax, Double expectedThresholdMin, Side expectedThresholdSide) {
        assertFlowCnecEquality(fc, expectedFlowCnecId, expectedFlowCnecName, expectedNetworkElementId, expectedInstant, expectedContingencyId, expectedThresholdMax, expectedThresholdMin, expectedThresholdMax, expectedThresholdMin, Set.of(expectedThresholdSide));
    }

    public static void assertFlowCnecEquality(FlowCnec fc, String expectedFlowCnecId, String expectedFlowCnecName, String expectedNetworkElementId,
                                              Instant expectedInstant, String expectedContingencyId, Double expectedThresholdMaxLeft, Double expectedThresholdMinLeft, Double expectedThresholdMaxRight, Double expectedThresholdMinRight, Set<Side> expectedThresholdSides) {
        assertEquals(expectedFlowCnecId, fc.getId());
        assertEquals(expectedFlowCnecName, fc.getName());
        assertEquals(expectedNetworkElementId, fc.getNetworkElement().getId());
        assertEquals(expectedInstant, fc.getState().getInstant());
        if (expectedContingencyId == null) {
            assertFalse(fc.getState().getContingency().isPresent());
        } else {
            assertEquals(expectedContingencyId, fc.getState().getContingency().get().getId());
        }

        List<BranchThreshold> thresholds = fc.getThresholds().stream().sorted(Comparator.comparing(BranchThreshold::getSide)).toList();
        for (BranchThreshold threshold : thresholds) {
            Side side = threshold.getSide();
            assertEquals(side == Side.LEFT ? expectedThresholdMaxLeft : expectedThresholdMaxRight, threshold.max().orElse(null));
            assertEquals(side == Side.LEFT ? expectedThresholdMinLeft : expectedThresholdMinRight, threshold.min().orElse(null));
        }

        assertEquals(expectedThresholdSides, fc.getMonitoredSides());
    }

    public static void assertAngleCnecEquality(AngleCnec angleCnec, String expectedFlowCnecId, String expectedFlowCnecName, String expectedImportingNetworkElementId, String expectedExportingNetworkElementId,
                                               Instant expectedInstant, String expectedContingencyId, Double expectedThresholdMax, Double expectedThresholdMin, boolean isMonitored) {
        assertEquals(expectedFlowCnecId, angleCnec.getId());
        assertEquals(expectedFlowCnecName, angleCnec.getName());
        assertEquals(expectedImportingNetworkElementId, angleCnec.getImportingNetworkElement().getId());
        assertEquals(expectedExportingNetworkElementId, angleCnec.getExportingNetworkElement().getId());
        assertEquals(expectedInstant, angleCnec.getState().getInstant());
        if (expectedContingencyId == null) {
            assertFalse(angleCnec.getState().getContingency().isPresent());
        } else {
            assertEquals(expectedContingencyId, angleCnec.getState().getContingency().get().getId());
        }

        Threshold threshold = angleCnec.getThresholds().stream().toList().iterator().next();
        assertEquals(expectedThresholdMax, threshold.max().orElse(null));
        assertEquals(expectedThresholdMin, threshold.min().orElse(null));
        assertEquals(isMonitored, angleCnec.isMonitored());
    }

    public static void assertVoltageCnecEquality(VoltageCnec voltageCnec, String expectedVoltageCnecId, String expectedFlowCnecName, String expectedNetworkElementId,
                                                 Instant expectedInstant, String expectedContingencyId, Double expectedThresholdMax, Double expectedThresholdMin, boolean isMonitored) {
        assertEquals(expectedVoltageCnecId, voltageCnec.getId());
        assertEquals(expectedFlowCnecName, voltageCnec.getName());
        assertEquals(expectedNetworkElementId, voltageCnec.getNetworkElement().getId());
        assertEquals(expectedInstant, voltageCnec.getState().getInstant());
        if (expectedContingencyId == null) {
            assertFalse(voltageCnec.getState().getContingency().isPresent());
        } else {
            assertEquals(expectedContingencyId, voltageCnec.getState().getContingency().get().getId());
        }

        Threshold threshold = voltageCnec.getThresholds().stream().toList().iterator().next();
        assertEquals(expectedThresholdMax, threshold.max().orElse(null));
        assertEquals(expectedThresholdMin, threshold.min().orElse(null));
        assertEquals(isMonitored, voltageCnec.isMonitored());
    }

    public static void assertPstRangeActionImported(CsaProfileCracCreationContext cracCreationContext, String id, String networkElement, boolean isAltered, int numberOfUsageRules) {
        CsaProfileElementaryCreationContext csaProfileElementaryCreationContext = cracCreationContext.getRemedialActionCreationContext(id);
        assertNotNull(csaProfileElementaryCreationContext);
        assertTrue(csaProfileElementaryCreationContext.isImported());
        assertEquals(isAltered, csaProfileElementaryCreationContext.isAltered());
        assertNotNull(cracCreationContext.getCrac().getPstRangeAction(id));
        String actualNetworkElement = cracCreationContext.getCrac().getPstRangeAction(id).getNetworkElement().toString();
        assertEquals(networkElement, actualNetworkElement);
        assertEquals(numberOfUsageRules, cracCreationContext.getCrac().getPstRangeAction(id).getUsageRules().size());
    }

    public static void assertNetworkActionImported(CsaProfileCracCreationContext cracCreationContext, String id, Set<String> networkElements, boolean isAltered, int numberOfUsageRules) {
        CsaProfileElementaryCreationContext csaProfileElementaryCreationContext = cracCreationContext.getRemedialActionCreationContext(id);
        assertNotNull(csaProfileElementaryCreationContext);
        assertTrue(csaProfileElementaryCreationContext.isImported());
        assertEquals(isAltered, csaProfileElementaryCreationContext.isAltered());
        assertNotNull(cracCreationContext.getCrac().getNetworkAction(id));
        Set<String> actualNetworkElements = cracCreationContext.getCrac().getNetworkAction(id).getNetworkElements().stream().map(NetworkElement::getId).collect(Collectors.toSet());
        assertEquals(networkElements, actualNetworkElements);
        assertEquals(numberOfUsageRules, cracCreationContext.getCrac().getNetworkAction(id).getUsageRules().size());
    }

    public static void assertHasOnInstantUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
            cracCreationContext.getCrac().getRemedialAction(raId).getUsageRules().stream().filter(OnInstant.class::isInstance)
                .map(OnInstant.class::cast)
                .anyMatch(ur -> ur.getInstant().equals(instant) && ur.getUsageMethod().equals(usageMethod))
        );
    }

    public static void assertHasOnContingencyStateUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, String contingencyId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
            cracCreationContext.getCrac().getRemedialAction(raId).getUsageRules().stream().filter(OnContingencyState.class::isInstance)
                .map(OnContingencyState.class::cast)
                .anyMatch(ur -> ur.getContingency().getId().equals(contingencyId) && ur.getInstant().equals(instant) && ur.getUsageMethod().equals(usageMethod))
        );
    }

    public static void assertHasOnFlowConstraintUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, String flowCnecId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
            cracCreationContext.getCrac().getRemedialAction(raId).getUsageRules().stream().filter(OnFlowConstraint.class::isInstance)
                .map(OnFlowConstraint.class::cast)
                .anyMatch(ur -> ur.getFlowCnec().getId().equals(flowCnecId) && ur.getInstant().equals(instant) && ur.getUsageMethod().equals(usageMethod))
        );
    }

    public static void assertHasOnAngleConstraintUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, String angleCnecId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
            cracCreationContext.getCrac().getRemedialAction(raId).getUsageRules().stream().filter(OnAngleConstraint.class::isInstance)
                .map(OnAngleConstraint.class::cast)
                .anyMatch(ur -> ur.getAngleCnec().getId().equals(angleCnecId) && ur.getInstant().equals(instant) && ur.getUsageMethod().equals(usageMethod))
        );
    }

    public static void assertHasOnVoltageConstraintUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, String voltageCnecId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
            cracCreationContext.getCrac().getRemedialAction(raId).getUsageRules().stream().filter(OnVoltageConstraint.class::isInstance)
                .map(OnVoltageConstraint.class::cast)
                .anyMatch(ur -> ur.getVoltageCnec().getId().equals(voltageCnecId) && ur.getInstant().equals(instant) && ur.getUsageMethod().equals(usageMethod))
        );
    }

    public static void assertRaNotImported(CsaProfileCracCreationContext cracCreationContext, String raId, ImportStatus importStatus, String importStatusDetail) {
        CsaProfileElementaryCreationContext context = cracCreationContext.getRemedialActionCreationContext(raId);
        assertNotNull(context);
        assertFalse(context.isImported());
        assertEquals(importStatusDetail, context.getImportStatusDetail());
        assertEquals(importStatus, context.getImportStatus());
    }

    public static void assertTopologicalActionImported(CracCreationContext cracCreationContext, String raId, String raName, String switchId) {
        NetworkAction ra = cracCreationContext.getCrac().getNetworkAction(raId);
        assertEquals(raName, ra.getName());
        assertEquals(switchId, ra.getNetworkElements().stream().toList().get(0).getId());
    }

    public static void assertTopologicalActionImported(CracCreationContext cracCreationContext, String raId, String raName, String switchId, int speed) {
        assertTopologicalActionImported(cracCreationContext, raId, raName, switchId);
        Optional<Integer> importedSpeed = cracCreationContext.getCrac().getNetworkAction(raId).getSpeed();
        assertNotNull(importedSpeed);
        assertEquals(speed, importedSpeed.get());
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(String csaProfilesArchive) {
        Network network = getNetworkFromResource(csaProfilesArchive);
        return getCsaCracCreationContext(csaProfilesArchive, network, false);
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(String csaProfilesArchive, boolean useGeographicalFilter) {
        Network network = getNetworkFromResource(csaProfilesArchive);
        return getCsaCracCreationContext(csaProfilesArchive, network, useGeographicalFilter);
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(String csaProfilesArchive, Network network, boolean useGeographicalFilter) {
        return getCsaCracCreationContext(csaProfilesArchive, network, OffsetDateTime.parse("2023-03-29T12:00Z"), useGeographicalFilter);
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(String csaProfilesArchive, Network network, OffsetDateTime offsetDateTime, boolean useGeographicalFilter) {
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = CsaProfileCracCreationTestUtil.class.getResourceAsStream(csaProfilesArchive);
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);
        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        CracCreationParameters parameters = new CracCreationParameters();
        CsaCracCreationParameters csaParameters = new CsaCracCreationParameters();
        csaParameters.setUseCnecGeographicalFilter(useGeographicalFilter);
        parameters.addExtension(CsaCracCreationParameters.class, csaParameters);
        return cracCreator.createCrac(nativeCrac, network, offsetDateTime, parameters);
    }

    public static Network getNetworkFromResource(String filename) {
        Properties importParams = new Properties();
        importParams.put("iidm.import.cgmes.cgm-with-subnetworks", false);
        return Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource(filename).getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
    }
}
