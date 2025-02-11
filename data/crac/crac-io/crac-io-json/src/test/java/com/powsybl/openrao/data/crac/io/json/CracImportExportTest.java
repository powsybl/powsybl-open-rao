/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.json;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.action.*;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.TechnicalLogs;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.networkaction.SwitchPair;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.range.TapRange;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.threshold.BranchThreshold;
import com.powsybl.openrao.data.crac.impl.utils.ExhaustiveCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.data.crac.api.usagerule.UsageMethod.AVAILABLE;
import static com.powsybl.openrao.data.crac.api.usagerule.UsageMethod.FORCED;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class CracImportExportTest {

    @Test
    void testExists() {
        assertTrue(new JsonImport().exists("crac-v2.5.json", getClass().getResourceAsStream("/retrocompatibility/v2/crac-v2.5.json")));
        assertTrue(new JsonImport().exists("cracHeader.json", getClass().getResourceAsStream("/cracHeader.json")));
        assertFalse(new JsonImport().exists("invalidCrac.json", getClass().getResourceAsStream("/invalidCrac.json")));
        assertFalse(new JsonImport().exists("invalidCrac.txt", getClass().getResourceAsStream("/invalidCrac.txt")));
    }

    @Test
    void testImportCracWithUnknownVersion() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new JsonImport().exists("crac-v100.0.json", getClass().getResourceAsStream("/crac-v100.0.json")));
        assertEquals("v100.0 is not a valid JSON CRAC version.", exception.getMessage());
    }

    @Test
    void testNonNullOffsetDateTime() {
        Network network = NetworkImportsUtil.createNetworkForJsonRetrocompatibilityTest();
        CracCreationContext context = new JsonImport().importData(getClass().getResourceAsStream("/retrocompatibility/v2/crac-v2.5.json"), new CracCreationParameters(), network);
        assertTrue(context.isCreationSuccessful());
        assertNull(context.getTimeStamp());
        assertEquals("test", context.getNetworkName());
    }

    @Test
    void testImportFailure() {
        CracCreationContext context = new JsonImport().importData(getClass().getResourceAsStream("/retrocompatibility/v2/crac-v2.5.json"), new CracCreationParameters(), Mockito.mock(Network.class));
        assertNotNull(context);
        assertFalse(context.isCreationSuccessful());
        assertNull(context.getCrac());
        assertEquals(List.of("[ERROR] In Contingency, network element with id ne1Id does not exist in network null, so it does not have type information and can not be converted to a contingency element."), context.getCreationReport().getReport());
    }

    @Test
    void explicitJsonRoundTripTest() {
        Crac crac = ExhaustiveCracCreation.create();
        Crac importedCrac = RoundTripUtil.explicitJsonRoundTrip(crac, ExhaustiveCracCreation.createAssociatedNetwork());
        checkContent(importedCrac);
    }

    @Test
    void implicitJsonRoundTripTest() {
        Crac crac = ExhaustiveCracCreation.create();
        Crac importedCrac = RoundTripUtil.implicitJsonRoundTrip(crac, ExhaustiveCracCreation.createAssociatedNetwork());
        checkContent(importedCrac);
    }

    private void checkContent(Crac crac) {
        Instant preventiveInstant = crac.getInstant("preventive");
        Instant autoInstant = crac.getInstant("auto");
        Instant curativeInstant = crac.getInstant("curative");

        // check overall content
        assertNotNull(crac);
        assertEquals(5, crac.getStates().size());
        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(1, crac.getAngleCnecs().size());
        assertEquals(1, crac.getVoltageCnecs().size());
        assertEquals(9, crac.getRangeActions().size());
        assertEquals(5, crac.getNetworkActions().size());

        // --------------------------
        // --- test Ra Usage Limits ---
        // --------------------------

        RaUsageLimits expectedUsageLimits = crac.getRaUsageLimits(curativeInstant);
        assertEquals(4, expectedUsageLimits.getMaxRa());
        assertEquals(2, expectedUsageLimits.getMaxTso());
        assertEquals(Map.of("FR", 12), expectedUsageLimits.getMaxRaPerTso());
        assertEquals(Map.of("FR", 7), expectedUsageLimits.getMaxPstPerTso());
        assertEquals(Map.of("FR", 5, "BE", 6), expectedUsageLimits.getMaxTopoPerTso());
        assertEquals(Map.of("FR", 21), expectedUsageLimits.getMaxElementaryActionsPerTso());
        // check instant with no limits
        assertEquals(new RaUsageLimits(), crac.getRaUsageLimits(preventiveInstant));

        // --------------------------
        // --- test Contingencies ---
        // --------------------------

        // check that Contingencies are present
        assertNotNull(crac.getContingency("contingency1Id"));
        assertNotNull(crac.getContingency("contingency2Id"));

        // check network elements
        assertEquals(1, crac.getContingency("contingency1Id").getElements().size());
        assertEquals("ne1Id", crac.getContingency("contingency1Id").getElements().iterator().next().getId());
        assertEquals(2, crac.getContingency("contingency2Id").getElements().size());

        // ----------------------
        // --- test FlowCnecs ---
        // ----------------------

        // check that Cnecs are present
        assertNotNull(crac.getFlowCnec("cnec1prevId"));
        assertNotNull(crac.getFlowCnec("cnec1outageId"));
        assertNotNull(crac.getFlowCnec("cnec2prevId"));
        assertNotNull(crac.getFlowCnec("cnec3prevId"));
        assertNotNull(crac.getFlowCnec("cnec3autoId"));
        assertNotNull(crac.getFlowCnec("cnec3curId"));
        assertNotNull(crac.getFlowCnec("cnec4prevId"));

        // check network element
        assertEquals("ne2Id", crac.getFlowCnec("cnec3prevId").getNetworkElement().getId());
        assertEquals("ne2Name", crac.getFlowCnec("cnec3prevId").getNetworkElement().getName());
        assertEquals("ne4Id", crac.getFlowCnec("cnec1outageId").getNetworkElement().getId());
        assertEquals("ne4Id", crac.getFlowCnec("cnec1outageId").getNetworkElement().getName());

        // check instants and contingencies
        assertEquals(preventiveInstant, crac.getFlowCnec("cnec1prevId").getState().getInstant());
        assertTrue(crac.getFlowCnec("cnec1prevId").getState().getContingency().isEmpty());
        assertEquals(curativeInstant, crac.getFlowCnec("cnec3curId").getState().getInstant());
        assertEquals("contingency2Id", crac.getFlowCnec("cnec3curId").getState().getContingency().get().getId());
        assertEquals(autoInstant, crac.getFlowCnec("cnec3autoId").getState().getInstant());
        assertEquals("contingency2Id", crac.getFlowCnec("cnec3autoId").getState().getContingency().get().getId());

        // check monitored and optimized
        assertFalse(crac.getFlowCnec("cnec3prevId").isOptimized());
        assertTrue(crac.getFlowCnec("cnec3prevId").isMonitored());
        assertTrue(crac.getFlowCnec("cnec4prevId").isOptimized());
        assertTrue(crac.getFlowCnec("cnec4prevId").isMonitored());

        // check operators
        assertEquals("operator1", crac.getFlowCnec("cnec1prevId").getOperator());
        assertEquals("operator1", crac.getFlowCnec("cnec1outageId").getOperator());
        assertEquals("operator2", crac.getFlowCnec("cnec2prevId").getOperator());
        assertEquals("operator3", crac.getFlowCnec("cnec3prevId").getOperator());
        assertEquals("operator4", crac.getFlowCnec("cnec4prevId").getOperator());

        // check iMax and nominal voltage
        assertEquals(2000., crac.getFlowCnec("cnec2prevId").getIMax(TwoSides.ONE), 1e-3);
        assertEquals(2000., crac.getFlowCnec("cnec2prevId").getIMax(TwoSides.TWO), 1e-3);
        assertEquals(380., crac.getFlowCnec("cnec2prevId").getNominalVoltage(TwoSides.ONE), 1e-3);
        assertEquals(220., crac.getFlowCnec("cnec2prevId").getNominalVoltage(TwoSides.TWO), 1e-3);
        assertEquals(Double.NaN, crac.getFlowCnec("cnec1prevId").getIMax(TwoSides.ONE), 1e-3);
        assertEquals(1000., crac.getFlowCnec("cnec1prevId").getIMax(TwoSides.TWO), 1e-3);
        assertEquals(220., crac.getFlowCnec("cnec1prevId").getNominalVoltage(TwoSides.ONE), 1e-3);
        assertEquals(220., crac.getFlowCnec("cnec1prevId").getNominalVoltage(TwoSides.TWO), 1e-3);

        // check threshold
        assertEquals(1, crac.getFlowCnec("cnec4prevId").getThresholds().size());
        BranchThreshold threshold = crac.getFlowCnec("cnec4prevId").getThresholds().iterator().next();
        assertEquals(Unit.MEGAWATT, threshold.getUnit());
        assertEquals(TwoSides.ONE, threshold.getSide());
        assertTrue(threshold.min().isEmpty());
        assertEquals(500., threshold.max().orElse(0.0), 1e-3);
        assertEquals(4, crac.getFlowCnec("cnec2prevId").getThresholds().size());

        // ----------------------
        // --- test AngleCnec ---
        // ----------------------
        AngleCnec angleCnec = crac.getAngleCnec("angleCnecId");
        assertNotNull(angleCnec);
        assertEquals("eneId", angleCnec.getExportingNetworkElement().getId());
        assertEquals("ineId", angleCnec.getImportingNetworkElement().getId());
        assertEquals(curativeInstant, angleCnec.getState().getInstant());
        assertEquals("contingency1Id", angleCnec.getState().getContingency().get().getId());
        assertFalse(angleCnec.isOptimized());
        assertTrue(angleCnec.isMonitored());
        assertEquals("operator1", angleCnec.getOperator());
        assertEquals(-90., angleCnec.getLowerBound(Unit.DEGREE).orElseThrow(), 1e-3);
        assertEquals(90., angleCnec.getUpperBound(Unit.DEGREE).orElseThrow(), 1e-3);

        // ----------------------
        // --- test VoltageCnec ---
        // ----------------------
        VoltageCnec voltageCnec = crac.getVoltageCnec("voltageCnecId");
        assertNotNull(voltageCnec);
        assertEquals("voltageCnecNeId", voltageCnec.getNetworkElement().getId());
        assertEquals(curativeInstant, voltageCnec.getState().getInstant());
        assertEquals("contingency1Id", voltageCnec.getState().getContingency().get().getId());
        assertFalse(voltageCnec.isOptimized());
        assertTrue(voltageCnec.isMonitored());
        assertEquals("operator1", voltageCnec.getOperator());
        assertEquals(381, voltageCnec.getLowerBound(Unit.KILOVOLT).orElseThrow(), 1e-3);

        // ---------------------------
        // --- test NetworkActions ---
        // ---------------------------

        // check that NetworkAction are present
        assertNotNull(crac.getNetworkAction("pstSetpointRaId"));
        assertNotNull(crac.getNetworkAction("injectionSetpointRaId"));
        assertNotNull(crac.getNetworkAction("complexNetworkActionId"));
        assertNotNull(crac.getNetworkAction("switchPairRaId"));
        assertNotNull(crac.getNetworkAction("complexNetworkAction2Id"));

        // check elementaryActions
        assertEquals(1, crac.getNetworkAction("pstSetpointRaId").getElementaryActions().size());
        Action pstAction = crac.getNetworkAction("pstSetpointRaId").getElementaryActions().iterator().next();
        assertTrue(pstAction instanceof PhaseTapChangerTapPositionAction);
        assertEquals("pst", ((PhaseTapChangerTapPositionAction) pstAction).getTransformerId());

        assertEquals(1, crac.getNetworkAction("injectionSetpointRaId").getElementaryActions().size());
        Action ra1Action = crac.getNetworkAction("injectionSetpointRaId").getElementaryActions().iterator().next();
        assertTrue(ra1Action instanceof GeneratorAction);
        assertEquals("injection", ((GeneratorAction) ra1Action).getGeneratorId());

        assertEquals(2, crac.getNetworkAction("complexNetworkActionId").getElementaryActions().size());
        List<Action> raComplexActions = crac.getNetworkAction("complexNetworkActionId").getElementaryActions().stream().toList();
        assertTrue(raComplexActions.get(0) instanceof PhaseTapChangerTapPositionAction);
        assertEquals("pst", ((PhaseTapChangerTapPositionAction) raComplexActions.get(0)).getTransformerId());
        assertTrue(raComplexActions.get(1) instanceof TerminalsConnectionAction);
        assertEquals("ne1Id", ((TerminalsConnectionAction) raComplexActions.get(1)).getElementId());

        assertEquals(4, crac.getNetworkAction("complexNetworkAction2Id").getElementaryActions().size());
        List<Action> raComplex2Actions = crac.getNetworkAction("complexNetworkAction2Id").getElementaryActions().stream().toList();
        assertTrue(raComplex2Actions.get(0) instanceof DanglingLineAction);
        assertEquals("DL1", ((DanglingLineAction) raComplex2Actions.get(0)).getDanglingLineId());
        assertTrue(raComplex2Actions.get(1) instanceof LoadAction);
        assertEquals("LD1", ((LoadAction) raComplex2Actions.get(1)).getLoadId());
        assertTrue(raComplex2Actions.get(2) instanceof SwitchAction);
        assertEquals("BR1", ((SwitchAction) raComplex2Actions.get(2)).getSwitchId());
        assertTrue(raComplex2Actions.get(3) instanceof ShuntCompensatorPositionAction);
        assertEquals("SC1", ((ShuntCompensatorPositionAction) raComplex2Actions.get(3)).getShuntCompensatorId());

        // check onInstant usage rule
        assertEquals(1, crac.getNetworkAction("complexNetworkActionId").getUsageRules().size());
        OnInstant onInstant = crac.getNetworkAction("complexNetworkActionId").getUsageRules().stream()
            .filter(ur -> ur instanceof OnInstant)
            .map(ur -> (OnInstant) ur)
            .findAny().orElse(null);
        assertNotNull(onInstant);
        assertEquals(preventiveInstant, onInstant.getInstant());
        assertEquals(FORCED, onInstant.getUsageMethod());

        // check several usage rules
        assertEquals(2, crac.getNetworkAction("pstSetpointRaId").getUsageRules().size());

        // check onContingencyState usage Rule (curative)
        OnContingencyState onContingencyState = crac.getNetworkAction("pstSetpointRaId").getUsageRules().stream()
            .filter(ur -> ur instanceof OnContingencyState)
            .map(ur -> (OnContingencyState) ur)
            .findAny().orElse(null);
        assertNotNull(onContingencyState);
        assertEquals("contingency1Id", onContingencyState.getContingency().getId());
        assertEquals(curativeInstant, onContingencyState.getInstant());
        assertEquals(FORCED, onContingencyState.getUsageMethod());

        // check automaton OnFlowConstraint usage rule
        assertEquals(1, crac.getNetworkAction("injectionSetpointRaId").getUsageRules().size());
        UsageRule injectionSetpointRaUsageRule = crac.getNetworkAction("injectionSetpointRaId").getUsageRules().iterator().next();

        assertTrue(injectionSetpointRaUsageRule instanceof OnConstraint<?>);
        OnConstraint<?> onFlowConstraint1 = (OnConstraint<?>) injectionSetpointRaUsageRule;
        assertEquals("cnec3autoId", onFlowConstraint1.getCnec().getId());
        assertTrue(onFlowConstraint1.getCnec() instanceof FlowCnec);
        assertEquals(autoInstant, onFlowConstraint1.getInstant());
        assertEquals(FORCED, onFlowConstraint1.getUsageMethod());

        // test SwitchPair

        assertEquals(1, crac.getNetworkAction("switchPairRaId").getElementaryActions().size());
        assertTrue(crac.getNetworkAction("switchPairRaId").getElementaryActions().iterator().next() instanceof SwitchPair);

        SwitchPair switchPair = (SwitchPair) crac.getNetworkAction("switchPairRaId").getElementaryActions().iterator().next();
        assertEquals("to-open", switchPair.getSwitchToOpen().getId());
        assertEquals("to-close", switchPair.getSwitchToClose().getId());

        // ----------------------------
        // --- test PstRangeActions ---
        // ----------------------------

        // check that RangeActions are present
        assertNotNull(crac.getRangeAction("pstRange1Id"));
        assertNotNull(crac.getRangeAction("pstRange2Id"));
        assertNotNull(crac.getRangeAction("pstRange3Id"));
        assertNotNull(crac.getRangeAction("pstRange5Id"));

        // check groupId
        assertTrue(crac.getRangeAction("pstRange1Id").getGroupId().isEmpty());
        assertEquals("group-1-pst", crac.getRangeAction("pstRange2Id").getGroupId().orElseThrow());
        assertEquals("group-3-pst", crac.getRangeAction("pstRange3Id").getGroupId().orElseThrow());

        // check taps
        assertEquals(2, crac.getPstRangeAction("pstRange1Id").getInitialTap());
        assertEquals(0.5, crac.getPstRangeAction("pstRange1Id").convertTapToAngle(-2));
        assertEquals(2.5, crac.getPstRangeAction("pstRange1Id").convertTapToAngle(2));
        assertEquals(2, crac.getPstRangeAction("pstRange1Id").convertAngleToTap(2.5));

        // check Tap Range
        assertEquals(2, crac.getPstRangeAction("pstRange1Id").getRanges().size());

        TapRange absRange = crac.getPstRangeAction("pstRange1Id").getRanges().stream()
            .filter(tapRange -> tapRange.getRangeType().equals(RangeType.ABSOLUTE))
            .findAny().orElse(null);
        TapRange relRange = crac.getPstRangeAction("pstRange1Id").getRanges().stream()
            .filter(tapRange -> tapRange.getRangeType().equals(RangeType.RELATIVE_TO_INITIAL_NETWORK))
            .findAny().orElse(null);

        assertNotNull(absRange);
        assertEquals(1, absRange.getMinTap());
        assertEquals(7, absRange.getMaxTap());
        assertNotNull(relRange);
        assertEquals(-3, relRange.getMinTap());
        assertEquals(3, relRange.getMaxTap());
        assertEquals(Unit.TAP, relRange.getUnit());

        // check OnFlowConstraint usage rule
        assertEquals(1, crac.getPstRangeAction("pstRange2Id").getUsageRules().size());
        UsageRule pstRange2UsageRule = crac.getPstRangeAction("pstRange2Id").getUsageRules().iterator().next();

        assertTrue(pstRange2UsageRule instanceof OnConstraint<?>);
        OnConstraint<?> onFlowConstraint2 = (OnConstraint<?>) pstRange2UsageRule;
        assertEquals(preventiveInstant, onFlowConstraint2.getInstant());
        assertSame(crac.getCnec("cnec3prevId"), onFlowConstraint2.getCnec());
        assertTrue(onFlowConstraint2.getCnec() instanceof FlowCnec);
        assertEquals(AVAILABLE, onFlowConstraint2.getUsageMethod());

        // check Tap Range
        assertEquals(3, crac.getPstRangeAction("pstRange2Id").getRanges().size());

        absRange = crac.getPstRangeAction("pstRange2Id").getRanges().stream()
            .filter(tapRange -> tapRange.getRangeType().equals(RangeType.ABSOLUTE))
            .findAny().orElse(null);
        relRange = crac.getPstRangeAction("pstRange2Id").getRanges().stream()
            .filter(tapRange -> tapRange.getRangeType().equals(RangeType.RELATIVE_TO_INITIAL_NETWORK))
            .findAny().orElse(null);
        TapRange relTimestampRange = crac.getPstRangeAction("pstRange2Id").getRanges().stream()
            .filter(tapRange -> tapRange.getRangeType().equals(RangeType.RELATIVE_TO_PREVIOUS_TIME_STEP))
            .findAny().orElse(null);

        assertNotNull(absRange);
        assertEquals(-4, absRange.getMinTap());
        assertEquals(3, absRange.getMaxTap());
        assertNotNull(relRange);
        assertEquals(-5, relRange.getMinTap());
        assertEquals(1, relRange.getMaxTap());
        assertNotNull(relTimestampRange);
        assertEquals(-2, relTimestampRange.getMinTap());
        assertEquals(5, relTimestampRange.getMaxTap());
        assertEquals(Unit.TAP, relRange.getUnit());
        assertEquals(Unit.TAP, relTimestampRange.getUnit());

        // check OnAngleConstraint usage rule
        assertEquals(1, crac.getPstRangeAction("pstRange3Id").getUsageRules().size());
        UsageRule pstRange3UsageRule = crac.getPstRangeAction("pstRange3Id").getUsageRules().iterator().next();

        assertTrue(pstRange3UsageRule instanceof OnConstraint<?>);
        OnConstraint<?> onConstraint = (OnConstraint<?>) pstRange3UsageRule;
        assertEquals(curativeInstant, onConstraint.getInstant());
        assertSame(crac.getCnec("angleCnecId"), onConstraint.getCnec());
        assertTrue(onConstraint.getCnec() instanceof AngleCnec);
        assertEquals(AVAILABLE, onConstraint.getUsageMethod());

        // check OnVoltageConstraint usage rule
        Set<UsageRule> pstRange4IdUsageRules = crac.getPstRangeAction("pstRange4Id").getUsageRules();
        assertEquals(1, pstRange4IdUsageRules.size());
        UsageRule pstRange4IdFirstUsageRules = pstRange4IdUsageRules.iterator().next();
        assertTrue(pstRange4IdFirstUsageRules instanceof OnConstraint<?>);
        OnConstraint<?> onVoltageConstraint = (OnConstraint<?>) pstRange4IdFirstUsageRules;
        assertEquals(curativeInstant, onVoltageConstraint.getInstant());
        assertSame(crac.getCnec("voltageCnecId"), onVoltageConstraint.getCnec());
        assertTrue(onVoltageConstraint.getCnec() instanceof VoltageCnec);
        assertEquals(AVAILABLE, onVoltageConstraint.getUsageMethod());

        // check Usage Method for pst5
        PstRangeAction pst5 = crac.getPstRangeAction("pstRange5Id");
        assertEquals(2, pst5.getUsageRules().size());

        List<UsageRule> onFlowConstrainRule = pst5.getUsageRules().stream().filter(usageRule -> usageRule instanceof OnConstraint<?>).filter(oc -> ((OnConstraint<?>) oc).getCnec() instanceof FlowCnec).toList();
        assertEquals(1, onFlowConstrainRule.size());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstrainRule.get(0).getUsageMethod(crac.getPreventiveState()));

        List<UsageRule> onInstantRule = pst5.getUsageRules().stream().filter(usageRule -> usageRule instanceof OnInstant).toList();
        assertEquals(1, onInstantRule.size());
        assertEquals(UsageMethod.FORCED, onInstantRule.get(0).getUsageMethod(crac.getPreventiveState()));

        // asserts that FORCED UsageMethod prevails over AVAILABLE
        assertEquals(UsageMethod.FORCED, pst5.getUsageMethod(crac.getPreventiveState()));

        // -----------------------------
        // --- test HvdcRangeActions ---
        // -----------------------------

        assertNotNull(crac.getRangeAction("hvdcRange1Id"));
        assertNotNull(crac.getRangeAction("hvdcRange2Id"));

        // check groupId
        assertTrue(crac.getRangeAction("hvdcRange1Id").getGroupId().isEmpty());
        assertEquals("group-1-hvdc", crac.getRangeAction("hvdcRange2Id").getGroupId().orElseThrow());

        // check preventive OnFlowConstraint usage rule
        assertEquals(3, crac.getHvdcRangeAction("hvdcRange2Id").getUsageRules().size());
        OnConstraint<?> onFlowConstraint3 = (OnConstraint<?>) crac.getHvdcRangeAction("hvdcRange2Id").getUsageRules().stream().filter(OnConstraint.class::isInstance).filter(oc -> ((OnConstraint<?>) oc).getCnec() instanceof FlowCnec).findAny().orElseThrow();
        assertEquals(preventiveInstant, onFlowConstraint3.getInstant());
        assertEquals(AVAILABLE, onFlowConstraint3.getUsageMethod());
        assertSame(crac.getCnec("cnec3curId"), onFlowConstraint3.getCnec());
        assertTrue(onFlowConstraint3.getCnec() instanceof FlowCnec);

        // check Hvdc range
        assertEquals(1, crac.getHvdcRangeAction("hvdcRange1Id").getRanges().size());
        StandardRange hvdcRange = crac.getHvdcRangeAction("hvdcRange1Id").getRanges().get(0);
        assertEquals(-1000, hvdcRange.getMin(), 1e-3);
        assertEquals(1000, hvdcRange.getMax(), 1e-3);
        assertEquals(Unit.MEGAWATT, hvdcRange.getUnit());

        // Check OnFlowConstraintInCountry usage rules
        Set<UsageRule> usageRules = crac.getHvdcRangeAction("hvdcRange1Id").getUsageRules();
        assertEquals(1, usageRules.size());
        UsageRule hvdcRange1UsageRule = usageRules.iterator().next();

        assertTrue(hvdcRange1UsageRule instanceof OnFlowConstraintInCountry);
        OnFlowConstraintInCountry ur = (OnFlowConstraintInCountry) hvdcRange1UsageRule;
        assertEquals(preventiveInstant, ur.getInstant());
        assertEquals(Country.FR, ur.getCountry());
        assertEquals(AVAILABLE, ur.getUsageMethod());

        // ---------------------------------
        // --- test InjectionRangeAction ---
        // ---------------------------------

        assertNotNull(crac.getInjectionRangeAction("injectionRange1Id"));

        assertEquals("injectionRange1Name", crac.getInjectionRangeAction("injectionRange1Id").getName());
        assertNull(crac.getInjectionRangeAction("injectionRange1Id").getOperator());
        assertTrue(crac.getInjectionRangeAction("injectionRange1Id").getGroupId().isEmpty());
        Map<NetworkElement, Double> networkElementAndKeys = crac.getInjectionRangeAction("injectionRange1Id").getInjectionDistributionKeys();
        assertEquals(2, networkElementAndKeys.size());
        assertEquals(1., networkElementAndKeys.entrySet().stream().filter(e -> e.getKey().getId().equals("generator1Id")).findAny().orElseThrow().getValue(), 1e-3);
        assertEquals(-1., networkElementAndKeys.entrySet().stream().filter(e -> e.getKey().getId().equals("generator2Id")).findAny().orElseThrow().getValue(), 1e-3);
        assertEquals("generator2Name", networkElementAndKeys.entrySet().stream().filter(e -> e.getKey().getId().equals("generator2Id")).findAny().orElseThrow().getKey().getName());
        assertEquals(2, crac.getInjectionRangeAction("injectionRange1Id").getRanges().size());

        // Check OnFlowConstraintInCountry usage rules
        usageRules = crac.getInjectionRangeAction("injectionRange1Id").getUsageRules();
        assertEquals(2, usageRules.size());
        ur = (OnFlowConstraintInCountry) usageRules.stream().filter(OnFlowConstraintInCountry.class::isInstance).findAny().orElseThrow();
        assertEquals(curativeInstant, ur.getInstant());
        assertEquals(Country.ES, ur.getCountry());
        assertTrue(ur.getContingency().isPresent());
        assertEquals("contingency2Id", ur.getContingency().get().getId());

        // ---------------------------------
        // --- test CounterTradeRangeAction ---
        // ---------------------------------

        assertNotNull(crac.getCounterTradeRangeAction("counterTradeRange1Id"));

        assertEquals("counterTradeRange1Name", crac.getCounterTradeRangeAction("counterTradeRange1Id").getName());
        assertNull(crac.getCounterTradeRangeAction("counterTradeRange1Id").getOperator());
        assertTrue(crac.getCounterTradeRangeAction("counterTradeRange1Id").getGroupId().isEmpty());
        assertEquals(2, crac.getCounterTradeRangeAction("counterTradeRange1Id").getRanges().size());
        assertEquals(Country.FR, crac.getCounterTradeRangeAction("counterTradeRange1Id").getExportingCountry());
        assertEquals(Country.DE, crac.getCounterTradeRangeAction("counterTradeRange1Id").getImportingCountry());

        // Check OnFlowConstraintInCountry usage rules
        usageRules = crac.getRemedialAction("counterTradeRange1Id").getUsageRules();
        assertEquals(2, usageRules.size());
        ur = (OnFlowConstraintInCountry) usageRules.stream().filter(OnFlowConstraintInCountry.class::isInstance).findAny().orElseThrow();
        assertEquals(curativeInstant, ur.getInstant());
        assertEquals(Country.ES, ur.getCountry());
        assertEquals(AVAILABLE, ur.getUsageMethod());
    }

    @Test
    void testImportNotJsonFile() {
        InputStream inputStream = Mockito.mock(InputStream.class);
        assertFalse(new JsonImport().exists("file.xml", inputStream));
    }

    @Test
    void testImportEmptyCrac() throws IOException {
        Network network = Mockito.mock(Network.class);
        Crac crac = Crac.read("emptyCrac.json", CracImportExportTest.class.getResourceAsStream("/emptyCrac.json"), network);
        assertNotNull(crac);
    }

    @Test
    void testImportCracWithErrors() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new JsonImport().exists("cracWithErrors.json", CracImportExportTest.class.getResourceAsStream("/cracWithErrors.json")));
        assertEquals("JSON file is not a valid CRAC v2.5. Reasons: /instants/3/kind: does not have a value in the enumeration [\"PREVENTIVE\", \"OUTAGE\", \"AUTO\", \"CURATIVE\"]; /contingencies/1/networkElementsIds/0: integer found, string expected; /contingencies/1/networkElementsIds/1: integer found, string expected; /contingencies/2: required property 'networkElementsIds' not found", exception.getMessage());
    }

    private static ListAppender<ILoggingEvent> initLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(TechnicalLogs.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }
}
