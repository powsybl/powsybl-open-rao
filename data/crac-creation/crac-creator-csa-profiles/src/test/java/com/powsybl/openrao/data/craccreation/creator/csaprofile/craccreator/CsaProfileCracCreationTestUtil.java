package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.ElementaryAction;
import com.powsybl.openrao.data.cracapi.networkaction.InjectionSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.threshold.BranchThreshold;
import com.powsybl.openrao.data.cracapi.threshold.Threshold;
import com.powsybl.openrao.data.cracapi.triggercondition.UsageMethod;
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

    public static final String PREVENTIVE_INSTANT_ID = "preventive";
    public static final String OUTAGE_INSTANT_ID = "outage";
    public static final String AUTO_INSTANT_ID = "auto";
    public static final String CURATIVE_1_INSTANT_ID = "curative 1";
    public static final String CURATIVE_2_INSTANT_ID = "curative 2";
    public static final String CURATIVE_3_INSTANT_ID = "curative 3";
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
        assertEquals(Optional.of(expectedContingencyName), actualContingency.getName());
        assertEquals(expectedNetworkElementsIds, actualContingency.getElements().stream().map(ContingencyElement::getId).collect(Collectors.toSet()));
    }

    public static void assertContingencyNotImported(CsaProfileCracCreationContext cracCreationContext, String contingencyId, ImportStatus importStatus, String importStatusDetail) {
        assertTrue(cracCreationContext.getContingencyCreationContexts().stream().anyMatch(context -> !context.isImported() && contingencyId.equals(context.getNativeId()) && importStatus.equals(context.getImportStatus()) && importStatusDetail.equals(context.getImportStatusDetail())));
    }

    public static void assertCnecNotImported(CsaProfileCracCreationContext cracCreationContext, String assessedElementId, ImportStatus importStatus, String importStatusDetail) {
        assertTrue(cracCreationContext.getCnecCreationContexts().stream().anyMatch(context -> !context.isImported() && assessedElementId.equals(context.getNativeId()) && importStatus.equals(context.getImportStatus()) && importStatusDetail.equals(context.getImportStatusDetail())));
    }

    public static void assertFlowCnecEquality(FlowCnec flowCnec, String expectedFlowCnecIdAndName, String expectedNetworkElementId, String expectedInstant, String expectedContingencyId, Double expectedThresholdMaxLeft, Double expectedThresholdMinLeft, Double expectedThresholdMaxRight, Double expectedThresholdMinRight, Set<Side> expectedThresholdSides, String expectedOperator) {
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
        assertEquals(expectedOperator, flowCnec.getOperator());
    }

    public static void assertAngleCnecEquality(AngleCnec angleCnec, String expectedFlowCnecIdAndName, String expectedImportingNetworkElementId, String expectedExportingNetworkElementId, String expectedInstant, String expectedContingencyId, Double expectedThresholdMax, Double expectedThresholdMin, String expectedOperator) {
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
        assertEquals(expectedOperator, angleCnec.getOperator());
    }

    public static void assertVoltageCnecEquality(VoltageCnec voltageCnec, String expectedVoltageCnecIdAndName, String expectedNetworkElementId, String expectedInstant, String expectedContingencyId, Double expectedThresholdMax, Double expectedThresholdMin, String expectedOperator) {
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
        assertEquals(expectedOperator, voltageCnec.getOperator());
    }

    public static void assertPstRangeActionImported(CsaProfileCracCreationContext cracCreationContext, String id, String networkElement, boolean isAltered, int numberOfTriggerConditions, String expectedOperator) {
        CsaProfileElementaryCreationContext csaProfileElementaryCreationContext = cracCreationContext.getRemedialActionCreationContext(id);
        assertNotNull(csaProfileElementaryCreationContext);
        assertTrue(csaProfileElementaryCreationContext.isImported());
        assertEquals(isAltered, csaProfileElementaryCreationContext.isAltered());
        assertNotNull(cracCreationContext.getCrac().getPstRangeAction(id));
        String actualNetworkElement = cracCreationContext.getCrac().getPstRangeAction(id).getNetworkElement().toString();
        assertEquals(networkElement, actualNetworkElement);
        assertEquals(numberOfTriggerConditions, cracCreationContext.getCrac().getPstRangeAction(id).getTriggerConditions().size());
        assertEquals(expectedOperator, cracCreationContext.getCrac().getPstRangeAction(id).getOperator());
    }

    public static void assertNetworkActionImported(CsaProfileCracCreationContext cracCreationContext, String id, Set<String> networkElements, boolean isAltered, int numberOfTriggerConditions, String expectedOperator) {
        CsaProfileElementaryCreationContext csaProfileElementaryCreationContext = cracCreationContext.getRemedialActionCreationContext(id);
        assertNotNull(csaProfileElementaryCreationContext);
        assertTrue(csaProfileElementaryCreationContext.isImported());
        assertEquals(isAltered, csaProfileElementaryCreationContext.isAltered());
        assertNotNull(cracCreationContext.getCrac().getNetworkAction(id));
        Set<String> actualNetworkElements = cracCreationContext.getCrac().getNetworkAction(id).getNetworkElements().stream().map(NetworkElement::getId).collect(Collectors.toSet());
        assertEquals(networkElements, actualNetworkElements);
        assertEquals(numberOfTriggerConditions, cracCreationContext.getCrac().getNetworkAction(id).getTriggerConditions().size());
        assertEquals(expectedOperator, cracCreationContext.getCrac().getNetworkAction(id).getOperator());
    }

    // TODO: mutualize methods
    public static void assertHasOnInstantTriggerCondition(CsaProfileCracCreationContext cracCreationContext, String raId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
            cracCreationContext.getCrac().getRemedialAction(raId).getTriggerConditions().stream().filter(tc -> tc.getContingency().isEmpty() && tc.getCnec().isEmpty() && tc.getCountry().isEmpty())
                .anyMatch(tc -> tc.getInstant().equals(instant) && tc.getUsageMethod().equals(usageMethod))
        );
    }

    public static void assertHasOnInstantTriggerCondition(CsaProfileCracCreationContext cracCreationContext, String raId, String instant, UsageMethod usageMethod) {
        assertHasOnInstantTriggerCondition(cracCreationContext, raId, cracCreationContext.getCrac().getInstant(instant), usageMethod);
    }

    public static void assertHasOnContingencyStateTriggerCondition(CsaProfileCracCreationContext cracCreationContext, String raId, String contingencyId, Instant instant, UsageMethod usageMethod) {
        assertTrue(
            cracCreationContext.getCrac().getRemedialAction(raId).getTriggerConditions().stream().filter(tc -> tc.getContingency().isPresent() && tc.getCnec().isEmpty() && tc.getCountry().isEmpty())
                .anyMatch(tc -> tc.getContingency().get().getId().equals(contingencyId) && tc.getInstant().equals(instant) && tc.getUsageMethod().equals(usageMethod))
        );
    }

    public static void assertHasOnContingencyStateTriggerCondition(CsaProfileCracCreationContext cracCreationContext, String raId, String contingencyId, String instant, UsageMethod usageMethod) {
        assertHasOnContingencyStateTriggerCondition(cracCreationContext, raId, contingencyId, cracCreationContext.getCrac().getInstant(instant), usageMethod);
    }

    public static void assertHasOnConstraintTriggerCondition(CsaProfileCracCreationContext cracCreationContext, String raId, String cnecId, Instant instant, UsageMethod usageMethod, Class<? extends Cnec<?>> cnecType) {
        assertTrue(
            cracCreationContext.getCrac().getRemedialAction(raId).getTriggerConditions().stream().filter(tc -> tc.getContingency().isEmpty() && tc.getCnec().isPresent() && tc.getCountry().isEmpty())
                .anyMatch(tc -> tc.getCnec().get().getId().equals(cnecId) && cnecType.isInstance(tc.getCnec().get()) && tc.getInstant().equals(instant) && tc.getUsageMethod().equals(usageMethod))
        );
    }

    public static void assertHasOnConstraintTriggerCondition(CsaProfileCracCreationContext cracCreationContext, String raId, String flowCnecId, String instant, UsageMethod usageMethod, Class<? extends Cnec<?>> cnecType) {
        assertHasOnConstraintTriggerCondition(cracCreationContext, raId, flowCnecId, cracCreationContext.getCrac().getInstant(instant), usageMethod, cnecType);
    }

    public static void assertRaNotImported(CsaProfileCracCreationContext cracCreationContext, String raId, ImportStatus importStatus, String importStatusDetail) {
        CsaProfileElementaryCreationContext context = cracCreationContext.getRemedialActionCreationContext(raId);
        assertNotNull(context);
        assertFalse(context.isImported());
        assertEquals(importStatusDetail, context.getImportStatusDetail());
        assertEquals(importStatus, context.getImportStatus());
    }

    public static void assertSimpleTopologicalActionImported(NetworkAction networkAction, String raId, String raName, String switchId, ActionType actionType, String expectedOperator) {
        assertEquals(raId, networkAction.getId());
        assertEquals(raName, networkAction.getName());
        assertEquals(1, networkAction.getElementaryActions().size());
        ElementaryAction elementaryAction = networkAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof TopologicalAction);
        assertEquals(switchId, ((TopologicalAction) elementaryAction).getNetworkElement().getId());
        assertEquals(actionType, ((TopologicalAction) elementaryAction).getActionType());
        assertEquals(expectedOperator, networkAction.getOperator());
    }

    public static void assertSimpleInjectionSetpointActionImported(NetworkAction networkAction, String raId, String raName, String networkElementId, double setpoint, Unit unit, String expectedOperator) {
        assertEquals(raId, networkAction.getId());
        assertEquals(raName, networkAction.getName());
        assertEquals(1, networkAction.getElementaryActions().size());
        ElementaryAction elementaryAction = networkAction.getElementaryActions().iterator().next();
        assertTrue(elementaryAction instanceof InjectionSetpoint);
        assertEquals(networkElementId, ((InjectionSetpoint) elementaryAction).getNetworkElement().getId());
        assertEquals(setpoint, ((InjectionSetpoint) elementaryAction).getSetpoint());
        assertEquals(unit, ((InjectionSetpoint) elementaryAction).getUnit());
        assertEquals(expectedOperator, networkAction.getOperator());
    }

    public static void assertPstRangeActionImported(PstRangeAction pstRangeAction, String expectedId, String expectedName, String expectedPstId, Integer expectedMinTap, Integer expectedMaxTap, String expectedOperator) {
        assertEquals(expectedId, pstRangeAction.getId());
        assertEquals(expectedName, pstRangeAction.getName());
        assertEquals(expectedPstId, pstRangeAction.getNetworkElement().getId());
        assertEquals(expectedOperator, pstRangeAction.getOperator());
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
        return getCsaCracCreationContext(csaProfilesArchive, cracCreationDefaultParametersWithCsaExtension());
    }

    public static CracCreationParameters cracCreationDefaultParametersWithCsaExtension() {
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.addExtension(CsaCracCreationParameters.class, new CsaCracCreationParameters());
        cracCreationParameters.getExtension(CsaCracCreationParameters.class).setCapacityCalculationRegionEicCode("10Y1001C--00095L");
        cracCreationParameters.getExtension(CsaCracCreationParameters.class).setSpsMaxTimeToImplementThresholdInSeconds(0);
        return cracCreationParameters;
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(String csaProfilesArchive, Network network) {
        return getCsaCracCreationContext(csaProfilesArchive, network, OffsetDateTime.parse("2023-03-29T12:00Z"), cracCreationDefaultParametersWithCsaExtension());
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(String csaProfilesArchive, Network network, CracCreationParameters cracCreationParameters) {
        return getCsaCracCreationContext(csaProfilesArchive, network, OffsetDateTime.parse("2023-03-29T12:00Z"), cracCreationParameters);
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(String csaProfilesArchive, Network network, String timestamp) {
        return getCsaCracCreationContext(csaProfilesArchive, network, OffsetDateTime.parse(timestamp), cracCreationDefaultParametersWithCsaExtension());
    }

    public static CsaProfileCracCreationContext getCsaCracCreationContext(String csaProfilesArchive, Network network, OffsetDateTime offsetDateTime) {
        return getCsaCracCreationContext(csaProfilesArchive, network, offsetDateTime, cracCreationDefaultParametersWithCsaExtension());
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
        importParams.put(CgmesImport.IMPORT_CGM_WITH_SUBNETWORKS, false);
        return Network.read(Paths.get(new File(CsaProfileCracCreationTestUtil.class.getResource(filename).getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
    }
}
