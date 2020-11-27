/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.utils;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.BranchCnec;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.AbstractFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.RelativeFlowThreshold;
import com.farao_community.farao.data.crac_impl.usage_rule.OnState;

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
        Instant basecase = new Instant("initial", 0);
        Instant defaut = new Instant("default", 60);
        Instant curative = new Instant("curative", 1200);

        //NetworkElement
        NetworkElement monitoredElement1 = new NetworkElement("BBE2AA1  FFR3AA1  1");
        NetworkElement monitoredElement2 = new NetworkElement("FFR2AA1  DDE3AA1  1");

        // State
        State stateBasecase = new SimpleState(Optional.empty(), basecase);
        State stateCurativeContingency1 = new SimpleState(Optional.of(contingency1), curative);
        State stateCurativeContingency2 = new SimpleState(Optional.of(contingency2), curative);

        // Thresholds
        AbsoluteFlowThreshold thresholdAbsFlow = new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.BOTH, 1500);
        RelativeFlowThreshold thresholdRelativeFlow = new RelativeFlowThreshold(Side.LEFT, Direction.BOTH, 30);

        Set<AbstractFlowThreshold> thresholdsAbsFlow = Collections.singleton(thresholdAbsFlow);
        Set<AbstractFlowThreshold> thresholdsRelativeFlow = Collections.singleton(thresholdRelativeFlow);
        // CNECs
        BranchCnec cnec1basecase = new BranchCnec("cnec1basecase", "", monitoredElement1, thresholdsAbsFlow, stateBasecase);
        BranchCnec cnec1stateCurativeContingency1 = new BranchCnec("cnec1stateCurativeContingency1", "", monitoredElement1, thresholdsAbsFlow, stateCurativeContingency1);
        BranchCnec cnec1stateCurativeContingency2 = new BranchCnec("cnec1stateCurativeContingency2", "", monitoredElement1, thresholdsAbsFlow, stateCurativeContingency2);
        cnec1basecase.setThresholds(thresholdsAbsFlow);
        cnec1stateCurativeContingency1.setThresholds(thresholdsAbsFlow);
        cnec1stateCurativeContingency2.setThresholds(thresholdsAbsFlow);

        BranchCnec cnec2basecase = new BranchCnec("cnec2basecase", "", monitoredElement2, thresholdsAbsFlow, stateBasecase);
        BranchCnec cnec2stateCurativeContingency1 = new BranchCnec("cnec2stateCurativeContingency1", "", monitoredElement2, thresholdsAbsFlow, stateCurativeContingency1);
        BranchCnec cnec2stateCurativeContingency2 = new BranchCnec("cnec2stateCurativeContingency2", "", monitoredElement2, thresholdsAbsFlow, stateCurativeContingency2);
        cnec2basecase.setThresholds(thresholdsRelativeFlow);
        cnec2stateCurativeContingency1.setThresholds(thresholdsRelativeFlow);
        cnec2stateCurativeContingency2.setThresholds(thresholdsRelativeFlow);

        crac.addCnec(cnec1basecase);
        crac.addCnec(cnec1stateCurativeContingency1);
        crac.addCnec(cnec1stateCurativeContingency2);
        crac.addCnec(cnec2basecase);
        crac.addCnec(cnec2stateCurativeContingency1);
        crac.addCnec(cnec2stateCurativeContingency2);

        return crac;
    }

    public static SimpleCrac createWithPstRange() {
        SimpleCrac crac = create();

        //NetworkElement
        NetworkElement pstElement = new NetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name");

        PstWithRange pstWithRange = new PstWithRange("pst", pstElement);
        pstWithRange.addUsageRule(new OnState(UsageMethod.AVAILABLE, crac.getPreventiveState()));
        crac.addRangeAction(pstWithRange);

        return crac;
    }

}
