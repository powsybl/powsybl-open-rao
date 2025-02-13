/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.json;

import com.powsybl.action.*;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.networkaction.SwitchPair;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.range.TapRange;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.threshold.BranchThreshold;
import com.powsybl.openrao.data.crac.api.threshold.Threshold;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.crac.api.usagerule.UsageMethod.AVAILABLE;
import static com.powsybl.openrao.data.crac.api.usagerule.UsageMethod.FORCED;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class JsonRetrocompatibilityTest {

    /*
    The goal of this test class is to ensure that former JSON CRAC files are still
    importable, even when modifications are brought to the JSON importer.
     */

    /*
    CARE: the existing json file used in this test case SHOULD NOT BE MODIFIED. If
    the current tests do not pass, it means that formerly generated JSON CRAC will
    not be compatible anymore with the next version of open-rao -> This is NOT
    desirable.

    Instead, we need to ensure that the JSON CRAC files used in this class can still
    be imported as is. Using versioning of the importer if needed.
     */

    private Network network;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.createNetworkForJsonRetrocompatibilityTest();
    }

    @Test
    void testFormat() {
        assertEquals("JSON", new JsonImport().getFormat());
    }

    @Test
    void testNoNetworkProvided() {
        JsonImport jsonImport = new JsonImport();
        OpenRaoException exception;
        try (InputStream inputStream = getClass().getResourceAsStream("/retrocompatibility/v2/crac-v2.5.json")) {
            CracCreationParameters cracCreationParameters = CracCreationParameters.load();
            exception = assertThrows(OpenRaoException.class, () -> jsonImport.importData(inputStream, cracCreationParameters, null, null));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertEquals("Network object is null but it is needed to map contingency's elements", exception.getMessage());
    }

    @Test
    void importV1Point0Test() {

        // JSON file of open-rao v3.4.3
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.0.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(3, crac.getNetworkActions().size());
        assertEquals(2, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        testContentOfV1Point0Crac(crac);
    }

    @Test
    void importV1Point1Test() {

        // JSON file of open-rao v3.5
        // addition of switch pairs
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.1.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(2, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        testContentOfV1Point1Crac(crac);
    }

    @Test
    void importV1Point2Test() {

        // JSON file of open-rao v3.6
        // addition of injection range action
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.2.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(2, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        testContentOfV1Point2Crac(crac);
    }

    @Test
    void importV1Point3Test() {

        // JSON file of open-rao v3.9
        // addition of initial setpoints for InjectionRangeActions and HvdcRangeActions
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.3.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(2, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        testContentOfV1Point3Crac(crac);
    }

    @Test
    void importV1Point4Test() {

        // JSON file of open-rao v4.0
        // addition of angle cnecs
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.4.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(1, crac.getAngleCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(3, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        testContentOfV1Point4Crac(crac);
    }

    @Test
    void importV1Point5Test() {

        // JSON file of open-rao v4.1
        // addition of voltage cnecs
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.5.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(1, crac.getAngleCnecs().size());
        assertEquals(1, crac.getVoltageCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(3, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        testContentOfV1Point5Crac(crac);
    }

    @Test
    void importV1Point6Test() {

        // renaming usage rules
        // Branch threshold rule no longer handled

        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.6.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(1, crac.getAngleCnecs().size());
        assertEquals(1, crac.getVoltageCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(3, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        testContentOfV1Point6Crac(crac);
    }

    @Test
    void importV1Point7Test() {

        // renaming usage rules
        // Branch threshold rule no longer handled

        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.7.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(1, crac.getAngleCnecs().size());
        assertEquals(1, crac.getVoltageCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(4, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        testContentOfV1Point7Crac(crac);
    }

    @Test
    void importV1Point8Test() {

        // renaming usage rules
        // Branch threshold rule no longer handled

        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.8.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(1, crac.getAngleCnecs().size());
        assertEquals(1, crac.getVoltageCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(4, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        testContentOfV1Point8Crac(crac);
    }

    @Test
    void importV1Point9Test() {
        // Add support for CounterTrade remedial actions
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.9.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(1, crac.getAngleCnecs().size());
        assertEquals(1, crac.getVoltageCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(4, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        assertEquals(1, crac.getCounterTradeRangeActions().size());
        testContentOfV1Point9Crac(crac);
    }

    @Test
    void importV2Point0Test() {
        // Add support for user-defined Instants
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v2/crac-v2.0.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();
        assertEquals(4, crac.getNetworkActions().size());
        testContentOfV2Point0Crac(crac);
    }

    @Test
    void importV2Point1Test() {
        // Add support for CNECs' borders and relative-to-previous-time-step ranges for PSTs
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v2/crac-v2.1.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();
        testContentOfV2Point1Crac(crac);
    }

    @Test
    void importV2Point2Test() {
        // Add support for CNECs' borders and relative-to-previous-time-step ranges for PSTs
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v2/crac-v2.2.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();
        assertEquals(6, crac.getNetworkActions().size());
        testContentOfV2Point2Crac(crac);
    }

    @Test
    void importV2Point3Test() {
        // Add support for unified onConstraint usage rules
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v2/crac-v2.3.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();
        assertEquals(6, crac.getNetworkActions().size());
        testContentOfV2Point3Crac(crac);
    }

    @Test
    void importV2Point4Test() {
        // Add support for contingency in OnFlowConstraintInCountry
        // Side left/right replaced by one/two (from powsybl-core)
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v2/crac-v2.4.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();
        assertEquals(7, crac.getNetworkActions().size());
        testContentOfV2Point4Crac(crac);
    }

    @Test
    void importV2Point5Test() {
        // ElementaryAction are now Action from powsybl-core (more different types and fields name changes)
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v2/crac-v2.5.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();
        assertEquals(7, crac.getNetworkActions().size());
        testContentOfV2Point5Crac(crac);
    }

    @Test
    void importV2Point6Test() {
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v2/crac-v2.6.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();
        assertEquals(7, crac.getNetworkActions().size());
        assertTrue(crac.getTimestamp().isEmpty());
        testContentOfV2Point6Crac(crac);
    }

    @Test
    void importV2Point7Test() {
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v2/crac-v2.7.json");

        Crac crac = new JsonImport().importData(cracFile, CracCreationParameters.load(), network, null).getCrac();
        assertEquals(7, crac.getNetworkActions().size());
        testContentOfV2Point7Crac(crac);
    }

    private void testContentOfV1Point0Crac(Crac crac) {
        Instant preventiveInstant = crac.getInstant("preventive");
        Instant autoInstant = crac.getInstant("auto");
        Instant curativeInstant = crac.getInstant("curative");

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
        assertEquals(3, crac.getFlowCnec("cnec2prevId").getThresholds().size());
        assertTrue(crac.getFlowCnec("cnec2prevId").getThresholds().stream()
            .anyMatch(thr -> thr.getSide().equals(TwoSides.ONE) && thr.getUnit().equals(Unit.AMPERE) && thr.min().orElse(-999.).equals(-800.)));
        assertTrue(crac.getFlowCnec("cnec2prevId").getThresholds().stream()
            .anyMatch(thr -> thr.getSide().equals(TwoSides.ONE) && thr.getUnit().equals(Unit.PERCENT_IMAX) && thr.min().orElse(-999.).equals(-0.3)));
        assertTrue(crac.getFlowCnec("cnec2prevId").getThresholds().stream()
            .anyMatch(thr -> thr.getSide().equals(TwoSides.TWO) && thr.getUnit().equals(Unit.AMPERE) && thr.max().orElse(-999.).equals(1200.)));

        // ---------------------------
        // --- test NetworkActions ---
        // ---------------------------

        // check that NetworkAction are present
        assertNotNull(crac.getNetworkAction("pstSetpointRaId"));
        assertNotNull(crac.getNetworkAction("injectionSetpointRaId"));
        assertNotNull(crac.getNetworkAction("complexNetworkActionId"));

        // check elementaryActions
        assertEquals(1, crac.getNetworkAction("pstSetpointRaId").getElementaryActions().size());
        assertTrue(crac.getNetworkAction("pstSetpointRaId").getElementaryActions().iterator().next() instanceof PhaseTapChangerTapPositionAction);
        assertEquals(1, crac.getNetworkAction("injectionSetpointRaId").getElementaryActions().size());
        assertTrue(crac.getNetworkAction("injectionSetpointRaId").getElementaryActions().iterator().next() instanceof GeneratorAction);
        assertEquals(2, crac.getNetworkAction("complexNetworkActionId").getElementaryActions().size());

        // check onInstant usage rule
        assertEquals(1, crac.getNetworkAction("complexNetworkActionId").getUsageRules().size());
        UsageRule complexNetworkActionUsageRule = crac.getNetworkAction("complexNetworkActionId").getUsageRules().iterator().next();

        assertTrue(complexNetworkActionUsageRule instanceof OnInstant);
        OnInstant onInstant = (OnInstant) complexNetworkActionUsageRule;
        assertEquals(preventiveInstant, onInstant.getInstant());
        assertEquals(AVAILABLE, onInstant.getUsageMethod());

        // check several usage rules
        assertEquals(2, crac.getNetworkAction("pstSetpointRaId").getUsageRules().size());

        // check onContingencyState usage Rule
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

        // ----------------------------
        // --- test PstRangeActions ---
        // ----------------------------

        // check that RangeActions are present
        assertNotNull(crac.getRangeAction("pstRange1Id"));
        assertNotNull(crac.getRangeAction("pstRange2Id"));

        // check groupId
        assertTrue(crac.getRangeAction("pstRange1Id").getGroupId().isEmpty());
        assertEquals("group-1-pst", crac.getRangeAction("pstRange2Id").getGroupId().orElseThrow());

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

        // -----------------------------
        // --- test HvdcRangeActions ---
        // -----------------------------

        assertNotNull(crac.getRangeAction("hvdcRange1Id"));
        assertNotNull(crac.getRangeAction("hvdcRange2Id"));

        // check groupId
        assertTrue(crac.getRangeAction("hvdcRange1Id").getGroupId().isEmpty());
        assertEquals("group-1-hvdc", crac.getRangeAction("hvdcRange2Id").getGroupId().orElseThrow());

        // check preventive OnFlowConstraint usage rule
        assertEquals(1, crac.getHvdcRangeAction("hvdcRange2Id").getUsageRules().size());
        UsageRule hvdcRange2UsageRule = crac.getHvdcRangeAction("hvdcRange2Id").getUsageRules().iterator().next();

        assertTrue(hvdcRange2UsageRule instanceof OnConstraint<?>);
        OnConstraint<?> onFlowConstraint3 = (OnConstraint<?>) hvdcRange2UsageRule;
        assertEquals(preventiveInstant, onFlowConstraint3.getInstant());
        assertSame(crac.getCnec("cnec3curId"), onFlowConstraint3.getCnec());
        assertTrue(onFlowConstraint3.getCnec() instanceof FlowCnec);

        // check Hvdc range
        assertEquals(1, crac.getHvdcRangeAction("hvdcRange1Id").getRanges().size());
        StandardRange hvdcRange = crac.getHvdcRangeAction("hvdcRange1Id").getRanges().get(0);
        assertEquals(-1000, hvdcRange.getMin(), 1e-3);
        assertEquals(1000, hvdcRange.getMax(), 1e-3);
        assertEquals(Unit.MEGAWATT, hvdcRange.getUnit());

        // check usage rules
        assertEquals(4, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(Set::stream).filter(OnInstant.class::isInstance).count());
    }

    void testContentOfV1Point1Crac(Crac crac) {

        testContentOfV1Point0Crac(crac);

        // test SwitchPair
        assertNotNull(crac.getNetworkAction("switchPairRaId"));

        assertEquals(1, crac.getNetworkAction("switchPairRaId").getElementaryActions().size());
        assertTrue(crac.getNetworkAction("switchPairRaId").getElementaryActions().iterator().next() instanceof SwitchPair);

        SwitchPair switchPair = (SwitchPair) crac.getNetworkAction("switchPairRaId").getElementaryActions().iterator().next();
        assertEquals("to-open", switchPair.getSwitchToOpen().getId());
        assertEquals("to-close", switchPair.getSwitchToClose().getId());
    }

    void testContentOfV1Point2Crac(Crac crac) {

        testContentOfV1Point1Crac(crac);

        // test injection range action
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

        // check usage rules
        assertEquals(3, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(Set::stream).filter(OnContingencyState.class::isInstance).count());
        assertEquals(3, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(Set::stream).filter(OnConstraint.class::isInstance).filter(oc -> ((OnConstraint<?>) oc).getCnec() instanceof FlowCnec).count());
    }

    void testContentOfV1Point3Crac(Crac crac) {

        testContentOfV1Point2Crac(crac);

        assertEquals(100, crac.getHvdcRangeAction("hvdcRange1Id").getInitialSetpoint(), 1e-3);
        assertEquals(-100, crac.getHvdcRangeAction("hvdcRange2Id").getInitialSetpoint(), 1e-3);
        assertEquals(50, crac.getInjectionRangeAction("injectionRange1Id").getInitialSetpoint(), 1e-3);
    }

    void testContentOfV1Point4Crac(Crac crac) {
        Instant curativeInstant = crac.getInstant("curative");

        testContentOfV1Point3Crac(crac);

        // test angle cnec
        AngleCnec angleCnec = crac.getAngleCnec("angleCnecId");
        assertNotNull(angleCnec);

        assertEquals("eneId", angleCnec.getExportingNetworkElement().getId());
        assertEquals("ineId", angleCnec.getImportingNetworkElement().getId());
        assertEquals(curativeInstant, angleCnec.getState().getInstant());
        assertEquals("contingency1Id", angleCnec.getState().getContingency().get().getId());
        assertFalse(angleCnec.isOptimized());
        assertTrue(angleCnec.isMonitored());
        assertEquals("operator1", angleCnec.getOperator());
        assertEquals(1, angleCnec.getThresholds().size());
        Threshold threshold = angleCnec.getThresholds().iterator().next();
        assertEquals(Unit.DEGREE, threshold.getUnit());
        assertTrue(threshold.max().isEmpty());
        assertEquals(-100., threshold.min().orElse(0.0), 1e-3);

        //test onAngleCnec range action
        RangeAction rangeAction = crac.getRangeAction("pstRange3Id");
        assertEquals(1, rangeAction.getUsageRules().size());
        UsageRule pstRange3UsageRule = crac.getRangeAction("pstRange3Id").getUsageRules().iterator().next();

        assertTrue(pstRange3UsageRule instanceof OnConstraint<?>);
        OnConstraint<?> onAngleConstraint = (OnConstraint<?>) pstRange3UsageRule;
        assertEquals("angleCnecId", onAngleConstraint.getCnec().getId());
        assertTrue(onAngleConstraint.getCnec() instanceof AngleCnec);
        assertEquals(curativeInstant, onAngleConstraint.getInstant());

        // check usage rules
        assertEquals(1, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(Set::stream).filter(OnConstraint.class::isInstance).filter(oc -> ((OnConstraint<?>) oc).getCnec() instanceof AngleCnec).count());
    }

    void testContentOfV1Point5Crac(Crac crac) {
        Instant curativeInstant = crac.getInstant("curative");

        testContentOfV1Point4Crac(crac);

        // test voltage cnec
        VoltageCnec voltageCnec = crac.getVoltageCnec("voltageCnecId");
        assertNotNull(voltageCnec);

        assertEquals("voltageCnecNeId", voltageCnec.getNetworkElement().getId());
        assertEquals(curativeInstant, voltageCnec.getState().getInstant());
        assertEquals("contingency1Id", voltageCnec.getState().getContingency().get().getId());
        assertFalse(voltageCnec.isOptimized());
        assertTrue(voltageCnec.isMonitored());
        assertEquals("operator1", voltageCnec.getOperator());
        assertEquals(1, voltageCnec.getThresholds().size());
        Threshold threshold = voltageCnec.getThresholds().iterator().next();
        assertEquals(Unit.KILOVOLT, threshold.getUnit());
        assertTrue(threshold.max().isEmpty());
        assertEquals(380., threshold.min().orElse(0.0), 1e-3);
    }

    void testContentOfV1Point6Crac(Crac crac) {

        testContentOfV1Point5Crac(crac);
        // test usage rules
        assertEquals(4, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(Set::stream).filter(OnInstant.class::isInstance).count());
        assertEquals(3, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(Set::stream).filter(OnContingencyState.class::isInstance).count());
        assertEquals(3, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(Set::stream).filter(OnConstraint.class::isInstance).filter(oc -> ((OnConstraint<?>) oc).getCnec() instanceof FlowCnec).count());
        assertEquals(1, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(Set::stream).filter(OnConstraint.class::isInstance).filter(oc -> ((OnConstraint<?>) oc).getCnec() instanceof AngleCnec).count());
        // test speed
        assertEquals(10, crac.getPstRangeAction("pstRange1Id").getSpeed().get().intValue());
        assertEquals(20, crac.getHvdcRangeAction("hvdcRange1Id").getSpeed().get().intValue());
        assertEquals(30, crac.getInjectionRangeAction("injectionRange1Id").getSpeed().get().intValue());
        assertEquals(40, crac.getNetworkAction("complexNetworkActionId").getSpeed().get().intValue());
    }

    void testContentOfV1Point7Crac(Crac crac) {

        testContentOfV1Point6Crac(crac);
        // test new voltage constraint usage rules
        assertEquals(1, crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(Set::stream).filter(OnConstraint.class::isInstance).filter(oc -> ((OnConstraint<?>) oc).getCnec() instanceof VoltageCnec).count());
    }

    void testContentOfV1Point8Crac(Crac crac) {
        testContentOfV1Point7Crac(crac);
        // Unit no longer exist so nothing specific to test for this version
    }

    void testContentOfV1Point9Crac(Crac crac) {

        testContentOfV1Point8Crac(crac);
        // test counter trade range action
        assertNotNull(crac.getCounterTradeRangeAction("counterTradeRange1Id"));

        assertEquals("counterTradeRange1Name", crac.getCounterTradeRangeAction("counterTradeRange1Id").getName());
        assertNull(crac.getCounterTradeRangeAction("counterTradeRange1Id").getOperator());
        assertTrue(crac.getCounterTradeRangeAction("counterTradeRange1Id").getGroupId().isEmpty());
        assertEquals(2, crac.getCounterTradeRangeAction("counterTradeRange1Id").getRanges().size());
        assertEquals(Country.FR, crac.getCounterTradeRangeAction("counterTradeRange1Id").getExportingCountry());
        assertEquals(Country.DE, crac.getCounterTradeRangeAction("counterTradeRange1Id").getImportingCountry());

        // test usage methods for voltage/angle/onflow constraint usage rules
        assertEquals(1, crac.getRemedialActions().stream().filter(ra -> ra.getUsageRules().stream().anyMatch(usageRule -> usageRule instanceof OnConstraint<?> && ((OnConstraint<?>) usageRule).getCnec() instanceof VoltageCnec)).count());
        assertEquals(AVAILABLE, crac.getRangeAction("pstRange2Id").getUsageMethod(crac.getFlowCnec("cnec1prevId").getState()));
        assertEquals(FORCED, crac.getNetworkAction("injectionSetpointRaId").getUsageMethod(crac.getFlowCnec("cnec3autoId").getState()));
    }

    private void testContentOfV2Point0Crac(Crac crac) {
        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(1, crac.getAngleCnecs().size());
        assertEquals(1, crac.getVoltageCnecs().size());
        assertEquals(4, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        assertEquals(1, crac.getCounterTradeRangeActions().size());
        assertEquals(5, crac.getSortedInstants().size());
        testContentOfV1Point9Crac(crac);
        // test instants are well-defined
        List<Instant> instants = crac.getSortedInstants();
        assertEquals("preventive", instants.get(0).getId());
        assertEquals(InstantKind.PREVENTIVE, instants.get(0).getKind());
        assertEquals(0, instants.get(0).getOrder());
        assertEquals("outage", instants.get(1).getId());
        assertEquals(InstantKind.OUTAGE, instants.get(1).getKind());
        assertEquals(1, instants.get(1).getOrder());
        assertEquals("auto", instants.get(2).getId());
        assertEquals(InstantKind.AUTO, instants.get(2).getKind());
        assertEquals(2, instants.get(2).getOrder());
        assertEquals("toto", instants.get(3).getId());
        assertEquals(InstantKind.CURATIVE, instants.get(3).getKind());
        assertEquals(3, instants.get(3).getOrder());
        assertEquals("curative", instants.get(4).getId());
        assertEquals(InstantKind.CURATIVE, instants.get(4).getKind());
        assertEquals(4, instants.get(4).getOrder());
    }

    private void testContentOfV2Point1Crac(Crac crac) {
        testContentOfV2Point0Crac(crac);
        Map<Instant, RaUsageLimits> raUsageLimitsMap = crac.getRaUsageLimitsPerInstant();
        assertEquals(Set.of(crac.getInstant("curative")), raUsageLimitsMap.keySet());
        RaUsageLimits curativeRaUsageLimits = raUsageLimitsMap.get(crac.getInstant("curative"));
        assertEquals(4, curativeRaUsageLimits.getMaxRa());
        assertEquals(2, curativeRaUsageLimits.getMaxTso());
        assertEquals(Map.of("BE", 6, "FR", 5), curativeRaUsageLimits.getMaxTopoPerTso());
        assertEquals(Map.of("FR", 7), curativeRaUsageLimits.getMaxPstPerTso());
        assertEquals(Map.of("FR", 12), curativeRaUsageLimits.getMaxRaPerTso());
        assertEquals(Map.of("FR", 21), curativeRaUsageLimits.getMaxElementaryActionsPerTso());
    }

    private void testContentOfV2Point2Crac(Crac crac) {
        testContentOfV2Point1Crac(crac);

        Set<OnFlowConstraintInCountry> urs = crac.getRemedialAction("injectionSetpointRa2Id").getUsageRules()
            .stream().filter(OnFlowConstraintInCountry.class::isInstance)
            .map(OnFlowConstraintInCountry.class::cast)
            .collect(Collectors.toSet());
        assertEquals(1, urs.size());
        OnFlowConstraintInCountry ur = urs.iterator().next();
        assertEquals(crac.getInstant("curative"), ur.getInstant());
        assertTrue(ur.getContingency().isPresent());
        assertEquals("contingency2Id", ur.getContingency().get().getId());
        assertEquals(Country.FR, ur.getCountry());

        urs = crac.getRemedialAction("injectionSetpointRa3Id").getUsageRules()
            .stream().filter(OnFlowConstraintInCountry.class::isInstance)
            .map(OnFlowConstraintInCountry.class::cast)
            .collect(Collectors.toSet());
        assertEquals(1, urs.size());
        ur = urs.iterator().next();
        assertEquals(crac.getInstant("curative"), ur.getInstant());
        assertTrue(ur.getContingency().isEmpty());
        assertEquals(Country.FR, ur.getCountry());
    }

    private void testContentOfV2Point3Crac(Crac crac) {
        testContentOfV2Point2Crac(crac);

        // check that RangeAction4 is present with new range relative to previous instant
        assertNotNull(crac.getRangeAction("pstRange4Id"));
        assertEquals(2, crac.getPstRangeAction("pstRange4Id").getRanges().size());
        TapRange absRange = crac.getPstRangeAction("pstRange4Id").getRanges().stream()
            .filter(tapRange -> tapRange.getRangeType().equals(RangeType.ABSOLUTE))
            .findAny().orElse(null);
        TapRange relTimeStepRange = crac.getPstRangeAction("pstRange4Id").getRanges().stream()
            .filter(tapRange -> tapRange.getRangeType().equals(RangeType.RELATIVE_TO_PREVIOUS_TIME_STEP))
            .findAny().orElse(null);

        assertNotNull(absRange);
        assertEquals(-2, absRange.getMinTap());
        assertEquals(7, absRange.getMaxTap());
        assertNotNull(relTimeStepRange);
        assertEquals(-1, relTimeStepRange.getMinTap());
        assertEquals(4, relTimeStepRange.getMaxTap());
        assertEquals(Unit.TAP, relTimeStepRange.getUnit());

        // check new border attribute
        assertEquals("border1", crac.getCnec("cnec1outageId").getBorder());
        assertEquals("border1", crac.getCnec("cnec1prevId").getBorder());
        assertEquals("border2", crac.getCnec("cnec2prevId").getBorder());
        assertEquals("border3", crac.getCnec("cnec3autoId").getBorder());
        assertEquals("border3", crac.getCnec("cnec3curId").getBorder());
        assertEquals("border3", crac.getCnec("cnec3prevId").getBorder());
        assertEquals("border1", crac.getCnec("cnec4prevId").getBorder());
        assertEquals("border4", crac.getCnec("angleCnecId").getBorder());
        assertEquals("border5", crac.getCnec("voltageCnecId").getBorder());
    }

    private void testContentOfV2Point4Crac(Crac crac) {
        testContentOfV2Point3Crac(crac);

        // check (new) elementaryActions
        Iterator<Action> raPstIt = crac.getNetworkAction("pstSetpointRaId").getElementaryActions().iterator();
        assertEquals("pst", ((PhaseTapChangerTapPositionAction) raPstIt.next()).getTransformerId());

        Iterator<Action> ra1It = crac.getNetworkAction("injectionSetpointRaId").getElementaryActions().iterator();
        assertEquals("injection", ((GeneratorAction) ra1It.next()).getGeneratorId());

        assertEquals(4, crac.getNetworkAction("complexNetworkAction2Id").getElementaryActions().size());
        List<Action> ra2Actions = crac.getNetworkAction("complexNetworkAction2Id").getElementaryActions().stream().toList();
        assertTrue(ra2Actions.get(0) instanceof DanglingLineAction);
        assertEquals("DL1", ((DanglingLineAction) ra2Actions.get(0)).getDanglingLineId());
        assertTrue(ra2Actions.get(1) instanceof LoadAction);
        assertEquals("LD1", ((LoadAction) ra2Actions.get(1)).getLoadId());
        assertTrue(ra2Actions.get(2) instanceof SwitchAction);
        assertEquals("BR1", ((SwitchAction) ra2Actions.get(2)).getSwitchId());
        assertTrue(ra2Actions.get(3) instanceof ShuntCompensatorPositionAction);
        assertEquals("SC1", ((ShuntCompensatorPositionAction) ra2Actions.get(3)).getShuntCompensatorId());

        assertEquals(2, crac.getNetworkAction("complexNetworkActionId").getElementaryActions().size());
        List<Action> raCompleActions = crac.getNetworkAction("complexNetworkActionId").getElementaryActions().stream().toList();
        assertTrue(raCompleActions.get(0) instanceof PhaseTapChangerTapPositionAction);
        assertEquals("pst", ((PhaseTapChangerTapPositionAction) raCompleActions.get(0)).getTransformerId());
        assertTrue(raCompleActions.get(1) instanceof TerminalsConnectionAction);
        assertEquals("ne1Id", ((TerminalsConnectionAction) raCompleActions.get(1)).getElementId());
    }

    private void testContentOfV2Point5Crac(Crac crac) {
        testContentOfV2Point4Crac(crac);
    }

    private void testContentOfV2Point6Crac(Crac crac) {
        testContentOfV2Point5Crac(crac);

        // activation cost
        assertEquals(Optional.of(0.0), crac.getPstRangeAction("pstRange3Id").getActivationCost());
        assertTrue(crac.getPstRangeAction("pstRange1Id").getActivationCost().isEmpty());
        assertTrue(crac.getRangeAction("hvdcRange1Id").getActivationCost().isEmpty());
        assertEquals(Optional.of(100.0), crac.getRangeAction("hvdcRange2Id").getActivationCost());
        assertEquals(Optional.of(800.0), crac.getRangeAction("injectionRange1Id").getActivationCost());
        assertEquals(Optional.of(10000.0), crac.getRangeAction("counterTradeRange1Id").getActivationCost());
        assertEquals(Optional.of(500.0), crac.getNetworkAction("complexNetworkAction2Id").getActivationCost());
        assertTrue(crac.getNetworkAction("switchPairRaId").getActivationCost().isEmpty());

        // variation costs
        assertEquals(Optional.of(0.0), crac.getPstRangeAction("pstRange3Id").getVariationCost(VariationDirection.UP));
        assertEquals(Optional.of(0.0), crac.getPstRangeAction("pstRange3Id").getVariationCost(VariationDirection.DOWN));
        assertTrue(crac.getPstRangeAction("pstRange1Id").getVariationCost(VariationDirection.UP).isEmpty());
        assertTrue(crac.getPstRangeAction("pstRange1Id").getVariationCost(VariationDirection.DOWN).isEmpty());
        assertTrue(crac.getRangeAction("hvdcRange1Id").getVariationCost(VariationDirection.UP).isEmpty());
        assertTrue(crac.getRangeAction("hvdcRange1Id").getVariationCost(VariationDirection.DOWN).isEmpty());
        assertTrue(crac.getRangeAction("hvdcRange2Id").getVariationCost(VariationDirection.UP).isEmpty());
        assertEquals(Optional.of(500.0), crac.getRangeAction("hvdcRange2Id").getVariationCost(VariationDirection.DOWN));
        assertEquals(Optional.of(2000.0), crac.getRangeAction("injectionRange1Id").getVariationCost(VariationDirection.UP));
        assertTrue(crac.getRangeAction("injectionRange1Id").getVariationCost(VariationDirection.DOWN).isEmpty());
        assertEquals(Optional.of(15000.0), crac.getRangeAction("counterTradeRange1Id").getVariationCost(VariationDirection.UP));
        assertEquals(Optional.of(18000.0), crac.getRangeAction("counterTradeRange1Id").getVariationCost(VariationDirection.DOWN));
    }

    private void testContentOfV2Point7Crac(Crac crac) {
        testContentOfV2Point6Crac(crac);
        Optional<OffsetDateTime> timestamp = crac.getTimestamp();
        assertTrue(timestamp.isPresent());
        assertEquals(OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), timestamp.get());
    }
}
