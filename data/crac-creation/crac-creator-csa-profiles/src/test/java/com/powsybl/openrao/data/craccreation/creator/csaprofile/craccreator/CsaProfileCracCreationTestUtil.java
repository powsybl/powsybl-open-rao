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
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.ElementaryAction;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
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
import org.eclipse.rdf4j.query.algebra.Str;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public final class CsaProfileCracCreationTestUtil {

    public static final String PREVENTIVE_INSTANT_ID = "preventive";
    public static final String OUTAGE_INSTANT_ID = "outage";
    public static final String AUTO_INSTANT_ID = "auto";
    public static final String CURATIVE_INSTANT_ID = "curative";
    public static final Network NETWORK = getNetworkFromResource("/networks/16Nodes.zip");

    private CsaProfileCracCreationTestUtil() {
    }

    public static ListAppender<ILoggingEvent> getLogs(Class<?> logsClass) {
        Logger logger = (Logger) LoggerFactory.getLogger(logsClass);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    public static void assertContingencyEquality(Contingency actualContingency, String expectedContingencyId, String expectedContingencyName, Set<String> expectedNetworkElementsIds) {
        assertEquals(expectedContingencyId, actualContingency.getId());
        assertEquals(expectedContingencyName, actualContingency.getName());
        assertEquals(expectedNetworkElementsIds.size(), actualContingency.getNetworkElements().size());
        assertTrue(expectedNetworkElementsIds.containsAll(actualContingency.getNetworkElements().stream().map(NetworkElement::getId).toList()));
    }

    public static void assertContingencyNotImported(CsaProfileCracCreationContext cracCreationContext, String contingencyId, ImportStatus importStatus, String importStatusDetail) {
        assertTrue(cracCreationContext.getContingencyCreationContexts().stream().anyMatch(context -> !context.isImported() && contingencyId.equals(context.getNativeId()) && importStatus.equals(context.getImportStatus()) && importStatusDetail.equals(context.getImportStatusDetail())));
    }

    public static void assertCnecNotImported(CsaProfileCracCreationContext cracCreationContext, String assessedElementId, ImportStatus importStatus, String importStatusDetail) {
        assertTrue(cracCreationContext.getCnecCreationContexts().stream().anyMatch(context -> !context.isImported() && assessedElementId.equals(context.getNativeId()) && importStatus.equals(context.getImportStatus()) && importStatusDetail.equals(context.getImportStatusDetail())));
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

    public static void assertFlowCnecEquality(FlowCnec flowCnec, String expectedFlowCnecIdAndName, String expectedNetworkElementId, String expectedInstant, String expectedContingencyId, Double expectedThresholdMaxLeft, Double expectedThresholdMinLeft, Double expectedThresholdMaxRight, Double expectedThresholdMinRight, Set<Side> expectedThresholdSides) {
        assertEquals(expectedFlowCnecIdAndName, flowCnec.getId());
        assertEquals(expectedFlowCnecIdAndName, flowCnec.getName());
        assertEquals(expectedNetworkElementId, flowCnec.getNetworkElement().getId());
        assertEquals(expectedInstant, flowCnec.getState().getInstant().getId());
        if (expectedContingencyId == null) {
            assertFalse(flowCnec.getState().getContingency().isPresent());
        } else {
            assertEquals(expectedContingencyId, flowCnec.getState().getContingency().get().getId());
        }

        List<BranchThreshold> thresholds = flowCnec.getThresholds().stream().sorted(Comparator.comparing(BranchThreshold::getSide)).toList();
        for (BranchThreshold threshold : thresholds) {
            Side side = threshold.getSide();
            assertEquals(side == Side.LEFT ? expectedThresholdMaxLeft : expectedThresholdMaxRight, threshold.max().orElse(null));
            assertEquals(side == Side.LEFT ? expectedThresholdMinLeft : expectedThresholdMinRight, threshold.min().orElse(null));
        }

        assertEquals(expectedThresholdSides, flowCnec.getMonitoredSides());
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

    public static void assertAngleCnecEquality(AngleCnec angleCnec, String expectedFlowCnecIdAndName, String expectedImportingNetworkElementId, String expectedExportingNetworkElementId, String expectedInstant, String expectedContingencyId, Double expectedThresholdMax, Double expectedThresholdMin) {
        assertEquals(expectedFlowCnecIdAndName, angleCnec.getId());
        assertEquals(expectedFlowCnecIdAndName, angleCnec.getName());
        assertEquals(expectedImportingNetworkElementId, angleCnec.getImportingNetworkElement().getId());
        assertEquals(expectedExportingNetworkElementId, angleCnec.getExportingNetworkElement().getId());
        assertEquals(expectedInstant, angleCnec.getState().getInstant().getId());
        if (expectedContingencyId == null) {
            assertFalse(angleCnec.getState().getContingency().isPresent());
        } else {
            assertEquals(expectedContingencyId, angleCnec.getState().getContingency().get().getId());
        }

        Threshold threshold = angleCnec.getThresholds().stream().toList().iterator().next();
        assertEquals(expectedThresholdMax, threshold.max().orElse(null));
        assertEquals(expectedThresholdMin, threshold.min().orElse(null));
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

    public static void assertVoltageCnecEquality(VoltageCnec voltageCnec, String expectedVoltageCnecIdAndName, String expectedNetworkElementId, String expectedInstant, String expectedContingencyId, Double expectedThresholdMax, Double expectedThresholdMin) {
        assertEquals(expectedVoltageCnecIdAndName, voltageCnec.getId());
        assertEquals(expectedVoltageCnecIdAndName, voltageCnec.getName());
        assertEquals(expectedNetworkElementId, voltageCnec.getNetworkElement().getId());
        assertEquals(expectedInstant, voltageCnec.getState().getInstant().getId());
        if (expectedContingencyId == null) {
            assertFalse(voltageCnec.getState().getContingency().isPresent());
        } else {
            assertEquals(expectedContingencyId, voltageCnec.getState().getContingency().get().getId());
        }
        Threshold threshold = voltageCnec.getThresholds().stream().toList().iterator().next();
        assertEquals(expectedThresholdMax, threshold.max().orElse(null));
        assertEquals(expectedThresholdMin, threshold.min().orElse(null));
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

    public static void assertHasOnInstantUsageRule(CsaProfileCracCreationContext cracCreationContext, String raId, String instant, UsageMethod usageMethod) {
        assertHasOnInstantUsageRule(cracCreationContext, raId, cracCreationContext.getCrac().getInstant(instant), usageMethod);
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

    public static void assertSimpleTopologicalActionImported(NetworkAction networkAction, String raId, String raName, String switchId, ActionType actionType) {
        assertEquals(raId, networkAction.getId());
        assertEquals(raName, networkAction.getName());
        assertEquals(1, networkAction.getElementaryActions().size());
        ElementaryAction elementaryAction = networkAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals(switchId, ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(actionType, ((TopologicalAction) elementaryAction).getActionType());
    }

    public static void assertTopologicalActionImported(CracCreationContext cracCreationContext, String raId, String raName, String switchId, int speed) {
        assertTopologicalActionImported(cracCreationContext, raId, raName, switchId);
        Optional<Integer> importedSpeed = cracCreationContext.getCrac().getNetworkAction(raId).getSpeed();
        assertNotNull(importedSpeed);
        assertEquals(speed, importedSpeed.get());
    }

    public static void assertPstRangeActionImported(PstRangeAction pstRangeAction, String expectedId, String expectedName, String expectedPstId, Integer expectedMinTap, Integer expectedMaxTap) {
        assertEquals(expectedId, pstRangeAction.getId());
        assertEquals(expectedName, pstRangeAction.getName());
        assertEquals(expectedPstId, pstRangeAction.getNetworkElement().getId());
        if (expectedMinTap == null && expectedMaxTap == null) {
            return;
        }
        assertEquals(expectedMinTap == null ? Integer.MIN_VALUE : expectedMinTap, pstRangeAction.getRanges().get(0).getMinTap());
        assertEquals(expectedMaxTap == null ? Integer.MAX_VALUE : expectedMaxTap, pstRangeAction.getRanges().get(0).getMaxTap());
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(String csaProfilesArchive, CracCreationParameters cracCreationParameters) {
        Network network = getNetworkFromResource(csaProfilesArchive);
        return getCsaCracCreationContext(csaProfilesArchive, network, cracCreationParameters);
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(String csaProfilesArchive) {
        return getCsaCracCreationContext(csaProfilesArchive, new CracCreationParameters());
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(String csaProfilesArchive, Network network) {
        return getCsaCracCreationContext(csaProfilesArchive, network, OffsetDateTime.parse("2023-03-29T12:00Z"), new CracCreationParameters());
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(String csaProfilesArchive, Network network, CracCreationParameters cracCreationParameters) {
        return getCsaCracCreationContext(csaProfilesArchive, network, OffsetDateTime.parse("2023-03-29T12:00Z"), cracCreationParameters);
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(String csaProfilesArchive, Network network, String timestamp) {
        return getCsaCracCreationContext(csaProfilesArchive, network, OffsetDateTime.parse(timestamp), new CracCreationParameters());
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(String csaProfilesArchive, Network network, OffsetDateTime offsetDateTime) {
        return getCsaCracCreationContext(csaProfilesArchive, network, offsetDateTime, new CracCreationParameters());
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(String csaProfilesArchive, Network network, OffsetDateTime offsetDateTime, CracCreationParameters cracCreationParameters) {
        CsaProfileCracImporter cracImporter = new CsaProfileCracImporter();
        InputStream inputStream = CsaProfileCracCreationTestUtil.class.getResourceAsStream(csaProfilesArchive);
        CsaProfileCrac nativeCrac = cracImporter.importNativeCrac(inputStream);
        CsaProfileCracCreator cracCreator = new CsaProfileCracCreator();
        return cracCreator.createCrac(nativeCrac, network, offsetDateTime, cracCreationParameters);
    }

    public static Network getNetworkFromResource(String filename) {
        Properties importParams = new Properties();
        importParams.put("iidm.import.cgmes.cgm-with-subnetworks", false);
        return Network.read(Paths.get(new File(CsaProfileCracCreatorTest.class.getResource(filename).getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
    }
}
