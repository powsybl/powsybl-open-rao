/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl.utils;

import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil.createNetworkForJsonRetrocompatibilityTest;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class ExhaustiveCracCreation {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    /*
    Small CRAC used in I/O unit tests of open-rao

    The idea of this CRAC is to be quite exhaustive regarding the diversity of the CRAC objects.
    It contains numerous variations of the CRAC objects, to ensure that they are all tested in
    the manipulations of the CRAC.
     */

    private ExhaustiveCracCreation() {
    }

    public static Crac create() {
        return create(CracFactory.findDefault());
    }

    private static ContingencyElementType randomContingencyElementType() {
        return ContingencyElementType.LINE;
    }

    public static Network createAssociatedNetwork() {
        // should be Line because of ContingencyElementType.LINE
        return createNetworkForJsonRetrocompatibilityTest();
    }

    public static Crac create(CracFactory cracFactory) {

        Crac crac = cracFactory.create("exhaustiveCracId", "exhaustiveCracName", OffsetDateTime.of(2025, 2, 3, 10, 12, 0, 0, ZoneOffset.UTC))
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);

        crac.newRaUsageLimits(CURATIVE_INSTANT_ID)
            .withMaxRa(4)
            .withMaxTso(2)
            .withMaxPstPerTso(new HashMap<>(Map.of("FR", 7)))
            .withMaxTopoPerTso(new HashMap<>(Map.of("FR", 5, "BE", 6)))
            .withMaxRaPerTso(new HashMap<>(Map.of("FR", 12)))
            .withMaxElementaryActionPerTso(new HashMap<>(Map.of("FR", 21)))
            .add();

        String contingency1Id = "contingency1Id";
        crac.newContingency().withId(contingency1Id).withContingencyElement("ne1Id", randomContingencyElementType()).add();

        String contingency2Id = "contingency2Id";
        crac.newContingency().withId(contingency2Id).withContingencyElement("ne2Id", randomContingencyElementType()).withContingencyElement("ne3Id", randomContingencyElementType()).add();

        crac.newFlowCnec().withId("cnec1prevId")
            .withNetworkElement("ne4Id")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withOperator("operator1")
            .withOptimized()
            .newThreshold().withSide(TwoSides.TWO).withUnit(Unit.AMPERE).withMin(-500.).add()
            .withIMax(1000., TwoSides.TWO)
            .withNominalVoltage(220.)
            .add();

        crac.newFlowCnec().withId("cnec1outageId")
            .withNetworkElement("ne4Id")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withOperator("operator1")
            .withOptimized()
            .newThreshold().withSide(TwoSides.TWO).withUnit(Unit.AMPERE).withMin(-800.).add()
            .withNominalVoltage(220.)
            .add();

        crac.newFlowCnec().withId("cnec2prevId")
            .withNetworkElement("ne5Id", "ne5Name")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withOperator("operator2")
            .withOptimized()
            .newThreshold().withSide(TwoSides.ONE).withUnit(Unit.PERCENT_IMAX).withMin(-0.3).add()
            .newThreshold().withSide(TwoSides.ONE).withUnit(Unit.AMPERE).withMin(-800.).add()
            .newThreshold().withSide(TwoSides.TWO).withUnit(Unit.AMPERE).withMin(-800.).add()
            .newThreshold().withSide(TwoSides.TWO).withUnit(Unit.AMPERE).withMax(1200.).add()
            .withNominalVoltage(220., TwoSides.TWO)
            .withNominalVoltage(380., TwoSides.ONE)
            .withIMax(2000.)
            .add();

        crac.newFlowCnec().withId("cnec3prevId")
            .withName("cnec3prevName")
            .withNetworkElement("ne2Id", "ne2Name")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withOperator("operator3")
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withSide(TwoSides.ONE).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withSide(TwoSides.TWO).add()
            .withReliabilityMargin(20.)
            .withMonitored()
            .add();

        crac.newFlowCnec().withId("cnec3autoId")
            .withName("cnec3autoName")
            .withNetworkElement("ne2Id", "ne2Name")
            .withInstant(AUTO_INSTANT_ID)
            .withContingency(contingency2Id)
            .withOperator("operator3")
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withSide(TwoSides.ONE).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withSide(TwoSides.TWO).add()
            .withReliabilityMargin(20.)
            .withMonitored()
            .add();

        crac.newFlowCnec().withId("cnec3curId")
            .withNetworkElement("ne2Id", "ne2Name")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency(contingency2Id)
            .withOperator("operator3")
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withSide(TwoSides.ONE).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withSide(TwoSides.TWO).add()
            .withReliabilityMargin(20.)
            .withMonitored()
            .add();

        crac.newFlowCnec().withId("cnec4prevId")
            .withName("cnec4prevName")
            .withNetworkElement("ne3Id")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withOperator("operator4")
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withSide(TwoSides.ONE).add()
            .withReliabilityMargin(0.)
            .withOptimized()
            .withMonitored()
            .add();

        crac.newAngleCnec().withId("angleCnecId")
            .withName("angleCnecName")
            .withExportingNetworkElement("eneId", "eneName")
            .withImportingNetworkElement("ineId", "ineName")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withOperator("operator1")
            .newThreshold().withUnit(Unit.DEGREE).withMin(-100.).withMax(100.).add()
            .withReliabilityMargin(10.)
            .withMonitored()
            .add();

        crac.newVoltageCnec().withId("voltageCnecId")
            .withName("voltageCnecName")
            .withNetworkElement("voltageCnecNeId", "voltageCnecNeName")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withOperator("operator1")
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(380.).add()
            .withReliabilityMargin(1.)
            .withMonitored()
            .add();

        // network action with one pst set point
        crac.newNetworkAction().withId("pstSetpointRaId")
            .withName("pstSetpointRaName")
            .withOperator("RTE")
            .newPhaseTapChangerTapPositionAction().withTapPosition(15).withNetworkElement("pst").add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT_ID).add()
            .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency(contingency1Id).withInstant(CURATIVE_INSTANT_ID).add()
            .add();

        // complex network action with one pst set point and one topology
        crac.newNetworkAction().withId("complexNetworkActionId")
            .withName("complexNetworkActionName")
            .withOperator("RTE")
            .newPhaseTapChangerTapPositionAction().withTapPosition(5).withNetworkElement("pst").add()
            .newTerminalsConnectionAction().withActionType(ActionType.CLOSE).withNetworkElement("ne1Id").add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.FORCED).withInstant(PREVENTIVE_INSTANT_ID).add()
            .add();

        // network action with one injection set point
        crac.newNetworkAction().withId("injectionSetpointRaId")
            .withName("injectionSetpointRaName")
            .withOperator("RTE")
            .withActivationCost(75d)
            .newGeneratorAction().withActivePowerValue(260.0).withNetworkElement("injection").add()
            .newOnConstraintUsageRule().withCnec("cnec3autoId").withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .add();

        // network action with multiple type elementary actions
        crac.newNetworkAction().withId("complexNetworkAction2Id")
            .withName("complexNetworkAction2Name")
            .withOperator("RTE")
            .newLoadAction().withActivePowerValue(260.0).withNetworkElement("LD1").add()
            .newDanglingLineAction().withActivePowerValue(-120.0).withNetworkElement("DL1").add()
            .newSwitchAction().withActionType(ActionType.OPEN).withNetworkElement("BR1").add()
            .newShuntCompensatorPositionAction().withSectionCount(13).withNetworkElement("SC1").add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(CURATIVE_INSTANT_ID).withContingency("contingency2Id").withCountry(Country.FR).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        // network action with one switch pair
        crac.newNetworkAction().withId("switchPairRaId")
            .withName("switchPairRaName")
            .withOperator("RTE")
            .newSwitchPair().withSwitchToOpen("to-open").withSwitchToClose("to-close", "to-close-name").add()
            .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency(contingency2Id).withInstant(CURATIVE_INSTANT_ID).add()
            .add();

        // range actions
        crac.newPstRangeAction().withId("pstRange1Id")
            .withName("pstRange1Name")
            .withOperator("RTE")
            .withNetworkElement("pst")
            .withInitialTap(2)
            .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
            .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(1).withMaxTap(7).add()
            .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT_ID).add()
            .add();

        crac.newPstRangeAction().withId("pstRange2Id")
            .withName("pstRange2Name")
            .withOperator("RTE")
            .withNetworkElement("pst2")
            .withGroupId("group-1-pst")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
            .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(-4).withMaxTap(3).add()
            .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-5).withMaxTap(1).add()
            .newTapRange().withRangeType(RangeType.RELATIVE_TO_PREVIOUS_TIME_STEP).withMinTap(-2).withMaxTap(5).add()
            .newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("cnec3prevId").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        crac.newPstRangeAction().withId("pstRange3Id")
            .withName("pstRange3Name")
            .withOperator("RTE")
            .withNetworkElement("pst3")
            .withGroupId("group-3-pst")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
            .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(1).withMaxTap(7).add()
            .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
            .newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec("angleCnecId").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        crac.newPstRangeAction().withId("pstRange4Id")
            .withName("pstRange4Name")
            .withOperator("RTE")
            .withNetworkElement("pst3")
            .withGroupId("group-3-pst")
            .withInitialTap(1)
            .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
            .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(1).withMaxTap(7).add()
            .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
            .newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec("voltageCnecId").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        crac.newPstRangeAction().withId("pstRange5Id").withName("pstRange5Name").withOperator("RTE").withNetworkElement("pst3")
            .withGroupId("group-3-pst")
            .withInitialTap(-3)
            .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5))
            .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(1).withMaxTap(7).add()
            .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.FORCED).withInstant(PREVENTIVE_INSTANT_ID).add()
            .newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("cnec3curId").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        crac.newHvdcRangeAction().withId("hvdcRange1Id")
            .withName("hvdcRange1Name")
            .withOperator("RTE")
            .withNetworkElement("hvdc")
            .newRange().withMin(-1000).withMax(1000).add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCountry(Country.FR).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        crac.newHvdcRangeAction().withId("hvdcRange2Id")
            .withName("hvdcRange2Name")
            .withOperator("RTE")
            .withNetworkElement("hvdc2")
            .withGroupId("group-1-hvdc")
            .newRange().withMin(-1000).withMax(1000).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1Id").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency2Id").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec("cnec3curId").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        crac.newInjectionRangeAction().withId("injectionRange1Id")
            .withName("injectionRange1Name")
            .withNetworkElementAndKey(1., "generator1Id")
            .withNetworkElementAndKey(-1., "generator2Id", "generator2Name")
            .withActivationCost(100d)
            .withVariationCost(750d, VariationDirection.UP)
            .withVariationCost(1000d, VariationDirection.DOWN)
            .newRange().withMin(-500).withMax(500).add()
            .newRange().withMin(-1000).withMax(1000).add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(CURATIVE_INSTANT_ID).withContingency("contingency2Id").withCountry(Country.ES).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1Id").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        crac.newCounterTradeRangeAction().withId("counterTradeRange1Id")
            .withName("counterTradeRange1Name")
            .withExportingCountry(Country.FR)
            .withImportingCountry(Country.DE)
            .withVariationCost(2000d, VariationDirection.UP)
            .withVariationCost(1000d, VariationDirection.DOWN)
            .newRange().withMin(-500).withMax(500).add()
            .newRange().withMin(-1000).withMax(1000).add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(CURATIVE_INSTANT_ID).withCountry(Country.ES).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1Id").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        return crac;
    }
}
