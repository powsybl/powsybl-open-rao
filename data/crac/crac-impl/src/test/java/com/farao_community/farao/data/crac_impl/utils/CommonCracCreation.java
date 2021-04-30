/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.utils;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_creation_util.PstHelper;
import com.powsybl.iidm.network.Network;

import static com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil.import12NodesNetwork;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CommonCracCreation {

    private CommonCracCreation() {

    }

    public static Crac create() {
        return create(CracFactory.findDefault());
    }

    public static Crac create(CracFactory cracFactory) {

        Crac crac = cracFactory.create("idSimpleCracTestUS", "nameSimpleCracTestUS");

        // Contingencies
        crac.newContingency()
            .withId("Contingency FR1 FR3")
            .withName("Trip of FFR1AA1 FFR3AA1 1")
            .withNetworkElement("FFR1AA1  FFR3AA1  1")
            .add();
        crac.newContingency()
            .withId("Contingency FR1 FR2")
            .withName("Trip of FFR1AA1 FFR2AA1 1")
            .withNetworkElement("FFR1AA1  FFR2AA1  1")
            .add();

        // Cnecs
        crac.newFlowCnec()
            .withId("cnec1basecase")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(Instant.PREVENTIVE)
            .withOptimized(true)
            .withOperator("operator1")
            .newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-1500.)
                .withMax(1500.)
                .add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();

        crac.newFlowCnec()
            .withId("cnec1stateCurativeContingency1")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(Instant.CURATIVE)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator1")
            .newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-1500.)
                .withMax(1500.)
                .add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();

        crac.newFlowCnec()
            .withId("cnec1stateCurativeContingency2")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(Instant.CURATIVE)
            .withContingency("Contingency FR1 FR2")
            .withOptimized(true)
            .withOperator("operator1")
            .newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-1500.)
                .withMax(1500.)
                .add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();

        crac.newFlowCnec()
            .withId("cnec2basecase")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(Instant.PREVENTIVE)
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-1500.)
                .withMax(1500.)
                .add()
            .newThreshold()
                .withUnit(Unit.PERCENT_IMAX)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-0.3)
                .withMax(0.3)
                .add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();

        crac.newFlowCnec()
            .withId("cnec2stateCurativeContingency1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(Instant.CURATIVE)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-1500.)
                .withMax(1500.)
                .add()
            .newThreshold()
                .withUnit(Unit.PERCENT_IMAX)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-0.3)
                .withMax(0.3)
                .add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();

        crac.newFlowCnec()
            .withId("cnec2stateCurativeContingency2")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(Instant.CURATIVE)
            .withContingency("Contingency FR1 FR2")
            .withOptimized(true)
            .withOperator("operator2")
            .withReliabilityMargin(95.)
            .newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-1500.)
                .withMax(1500.)
                .add()
            .newThreshold()
                .withUnit(Unit.PERCENT_IMAX)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-0.3)
                .withMax(0.3)
                .add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();

        return crac;
    }

    public static Crac createWithPreventivePstRange() {
        Crac crac = create();
        Network network = import12NodesNetwork();
        PstHelper pstHelper = new PstHelper("BBE2AA1  BBE3AA1  1", network);

        crac.newPstRangeAction()
            .withId("pst")
            .withNetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name")
            .withOperator("operator1")
            .newFreeToUseUsageRule()
                .withInstant(Instant.PREVENTIVE)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
            .newTapRange()
                .withRangeType(RangeType.ABSOLUTE)
                .withMinTap(-16)
                .withMaxTap(16)
                .add()
            .withInitialTap(pstHelper.getInitialTap())
            .withTapToAngleConversionMap(pstHelper.getTapToAngleConversionMap())
            .add();

        return crac;
    }

    public static Crac createWithCurativePstRange() {
        Crac crac = create();
        Network network = import12NodesNetwork();
        PstHelper pstHelper = new PstHelper("BBE2AA1  BBE3AA1  1", network);

        crac.newPstRangeAction()
            .withId("pst")
            .withNetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name")
            .withOperator("operator1")
            .newOnStateUsageRule()
                .withInstant(Instant.CURATIVE)
                .withContingency("Contingency FR1 FR3")
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
            .newTapRange()
                .withRangeType(RangeType.ABSOLUTE)
                .withMinTap(-16)
                .withMaxTap(16)
                .add()
            .withInitialTap(pstHelper.getInitialTap())
            .withTapToAngleConversionMap(pstHelper.getTapToAngleConversionMap())
            .add();

        return crac;
    }

    public static Crac createWithSwitchTopologicalAction() {
        Crac crac = create();

        crac.newNetworkAction()
            .withId("switch_ra")
            .withName("switch_ra_name")
            .withOperator("operator1")
            .newTopologicalAction()
                .withNetworkElement("NNL3AA11 NNL3AA12 1", "NNL3AA11 NNL3AA12 1 name")
                .withActionType(ActionType.OPEN)
                .add()
            .newFreeToUseUsageRule()
                .withUsageMethod(UsageMethod.AVAILABLE)
                .withInstant(Instant.PREVENTIVE)
                .add()
            .add();

        return crac;
    }

}
