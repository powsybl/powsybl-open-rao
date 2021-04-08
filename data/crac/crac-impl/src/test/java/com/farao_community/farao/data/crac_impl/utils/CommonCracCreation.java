/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.utils;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.NetworkActionImpl;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.TopologicalActionImpl;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstRangeActionImpl;
import com.farao_community.farao.data.crac_impl.usage_rule.OnStateImpl;

import java.util.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CommonCracCreation {

    private CommonCracCreation() {

    }

    public static SimpleCrac create() {
        SimpleCrac crac = new SimpleCrac("idSimpleCracTestUS", "nameSimpleCracTestUS");

        ComplexContingency contingency1 = new ComplexContingency("Contingency FR1 FR3", "Trip of FFR1AA1 FFR3AA1 1",
            new HashSet<>(Collections.singletonList(new NetworkElement("FFR1AA1  FFR3AA1  1"))));
        crac.addContingency(contingency1);
        ComplexContingency contingency2 = new ComplexContingency("Contingency FR1 FR2", "Trip of FFR1AA1 FFR2AA1 1",
            new HashSet<>(Collections.singletonList(new NetworkElement("FFR1AA1  FFR2AA1  1"))));
        crac.addContingency(contingency2);

        //NetworkElement
        NetworkElement monitoredElement1 = new NetworkElement("BBE2AA1  FFR3AA1  1");
        NetworkElement monitoredElement2 = new NetworkElement("FFR2AA1  DDE3AA1  1");

        crac.newBranchCnec()
            .setId("cnec1basecase")
            .addNetworkElement(monitoredElement1)
            .newThreshold().setUnit(Unit.MEGAWATT).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-1500.).setMax(1500.).add()
            .setInstant(Instant.PREVENTIVE)
            .optimized()
            .setOperator("operator1")
            .add();

        crac.newBranchCnec()
            .setId("cnec1stateCurativeContingency1")
            .addNetworkElement(monitoredElement1)
            .newThreshold().setUnit(Unit.MEGAWATT).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-1500.).setMax(1500.).add()
            .setInstant(Instant.CURATIVE)
            .setContingency(contingency1)
            .optimized()
            .setOperator("operator1")
            .add();

        crac.newBranchCnec()
            .setId("cnec1stateCurativeContingency2")
            .addNetworkElement(monitoredElement1)
            .newThreshold().setUnit(Unit.MEGAWATT).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-1500.).setMax(1500.).add()
            .setInstant(Instant.CURATIVE)
            .setContingency(contingency2)
            .optimized()
            .setOperator("operator1")
            .add();

        crac.newBranchCnec()
            .setId("cnec2basecase")
            .addNetworkElement(monitoredElement2)
            .newThreshold().setUnit(Unit.MEGAWATT).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-1500.).setMax(1500.).add()
            .newThreshold().setUnit(Unit.PERCENT_IMAX).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-0.3).setMax(0.3).add()
            .setInstant(Instant.PREVENTIVE)
            .optimized()
            .setOperator("operator2")
            .add();

        crac.newBranchCnec()
            .setId("cnec2stateCurativeContingency1")
            .addNetworkElement(monitoredElement2)
            .newThreshold().setUnit(Unit.MEGAWATT).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-1500.).setMax(1500.).add()
            .newThreshold().setUnit(Unit.PERCENT_IMAX).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-0.3).setMax(0.3).add()
            .setInstant(Instant.CURATIVE)
            .setContingency(contingency1)
            .optimized()
            .setOperator("operator2")
            .add();

        crac.newBranchCnec()
            .setId("cnec2stateCurativeContingency2")
            .addNetworkElement(monitoredElement2)
            .newThreshold().setUnit(Unit.MEGAWATT).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-1500.).setMax(1500.).add()
            .newThreshold().setUnit(Unit.PERCENT_IMAX).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-0.3).setMax(0.3).add()
            .setInstant(Instant.CURATIVE)
            .setContingency(contingency2)
            .optimized()
            .add();

        return crac;
    }

    public static SimpleCrac createWithPstRange() {
        SimpleCrac crac = create();

        //NetworkElement
        NetworkElement pstElement = new NetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name");

        PstRangeActionImpl pstRangeAction = new PstRangeActionImpl("pst", pstElement);
        pstRangeAction.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState()));
        crac.addRangeAction(pstRangeAction);

        return crac;
    }

    public static SimpleCrac createWithCurativePstRange() {
        SimpleCrac crac = create();

        //NetworkElement
        NetworkElement pstElement = new NetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name");

        PstRangeActionImpl pstRangeAction = new PstRangeActionImpl("pst", pstElement);
        pstRangeAction.setOperator("operator1");
        pstRangeAction.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getState("Contingency FR1 FR3", Instant.CURATIVE)));
        crac.addRangeAction(pstRangeAction);

        return crac;
    }

    public static SimpleCrac createWithSwitch() {
        SimpleCrac crac = create();

        NetworkElement switchElement = new NetworkElement("NNL3AA11 NNL3AA12 1", "NNL3AA11 NNL3AA12 1 name");
        TopologicalActionImpl topologicalAction = new TopologicalActionImpl(switchElement, ActionType.OPEN);
        NetworkActionImpl topologicalRa = new NetworkActionImpl(
            "switch_ra",
            "switch_ra_name",
            "OPERATOR",
            Collections.singletonList(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState())),
            Collections.singleton(topologicalAction)
        );

        crac.addNetworkAction(topologicalRa);
        return crac;
    }

}
