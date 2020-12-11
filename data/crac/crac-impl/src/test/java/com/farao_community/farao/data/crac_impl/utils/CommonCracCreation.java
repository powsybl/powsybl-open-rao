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
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
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

        // Instant
        Instant basecase = crac.newInstant().setId("initial").setSeconds(0).add();
        Instant curative = crac.newInstant().setId("curative").setSeconds(1200).add();

        //NetworkElement
        NetworkElement monitoredElement1 = new NetworkElement("BBE2AA1  FFR3AA1  1");
        NetworkElement monitoredElement2 = new NetworkElement("FFR2AA1  DDE3AA1  1");

        // State
        State stateBasecase = new SimpleState(Optional.empty(), basecase);
        State stateCurativeContingency1 = new SimpleState(Optional.of(contingency1), curative);
        State stateCurativeContingency2 = new SimpleState(Optional.of(contingency2), curative);

        crac.newBranchCnec()
            .setId("cnec1basecase")
            .addNetworkElement(monitoredElement1)
            .newThreshold().setUnit(Unit.MEGAWATT).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-1500.).setMax(1500.).add()
            .setInstant(stateBasecase.getInstant())
            .optimized()
            .add();

        crac.newBranchCnec()
            .setId("cnec1stateCurativeContingency1")
            .addNetworkElement(monitoredElement1)
            .newThreshold().setUnit(Unit.MEGAWATT).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-1500.).setMax(1500.).add()
            .setInstant(stateCurativeContingency1.getInstant())
            .setContingency(stateCurativeContingency1.getContingency().orElseThrow())
            .optimized()
            .add();

        crac.newBranchCnec()
            .setId("cnec1stateCurativeContingency2")
            .addNetworkElement(monitoredElement1)
            .newThreshold().setUnit(Unit.MEGAWATT).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-1500.).setMax(1500.).add()
            .setInstant(stateCurativeContingency2.getInstant())
            .setContingency(stateCurativeContingency2.getContingency().orElseThrow())
            .optimized()
            .add();

        crac.newBranchCnec()
            .setId("cnec2basecase")
            .addNetworkElement(monitoredElement2)
            .newThreshold().setUnit(Unit.MEGAWATT).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-1500.).setMax(1500.).add()
            .newThreshold().setUnit(Unit.PERCENT_IMAX).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-0.3).setMax(0.3).add()
            .setInstant(stateBasecase.getInstant())
            .optimized()
            .add();

        crac.newBranchCnec()
            .setId("cnec2stateCurativeContingency1")
            .addNetworkElement(monitoredElement2)
            .newThreshold().setUnit(Unit.MEGAWATT).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-1500.).setMax(1500.).add()
            .newThreshold().setUnit(Unit.PERCENT_IMAX).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-0.3).setMax(0.3).add()
            .setInstant(stateCurativeContingency1.getInstant())
            .setContingency(stateCurativeContingency1.getContingency().orElseThrow())
            .optimized()
            .add();

        crac.newBranchCnec()
            .setId("cnec2stateCurativeContingency2")
            .addNetworkElement(monitoredElement2)
            .newThreshold().setUnit(Unit.MEGAWATT).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-1500.).setMax(1500.).add()
            .newThreshold().setUnit(Unit.PERCENT_IMAX).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMin(-0.3).setMax(0.3).add()
            .setInstant(stateCurativeContingency2.getInstant())
            .setContingency(stateCurativeContingency2.getContingency().orElseThrow())
            .optimized()
            .add();

        return crac;
    }

    public static SimpleCrac createWithPstRange() {
        SimpleCrac crac = create();

        //NetworkElement
        NetworkElement pstElement = new NetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name");

        PstWithRange pstWithRange = new PstWithRange("pst", pstElement);
        pstWithRange.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState()));
        crac.addRangeAction(pstWithRange);

        return crac;
    }

}
