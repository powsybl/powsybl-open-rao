/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

/**
 * General test file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.*;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.*;
import com.farao_community.farao.data.crac_impl.remedial_action.range_domain.AbsoluteFixedRange;
import com.farao_community.farao.data.crac_impl.remedial_action.range_domain.Range;
import com.farao_community.farao.data.crac_impl.remedial_action.range_domain.RelativeDynamicRange;
import com.farao_community.farao.data.crac_impl.remedial_action.range_domain.RelativeFixedRange;
import com.farao_community.farao.data.crac_impl.threshold.FlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.VoltageThreshold;
import com.farao_community.farao.data.crac_impl.remedial_action.usage_rule.AbstractUsageRule;
import com.farao_community.farao.data.crac_impl.remedial_action.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_impl.remedial_action.usage_rule.OnConstraint;
import com.farao_community.farao.data.crac_impl.remedial_action.usage_rule.OnContingency;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.data.crac_api.ActionType.*;
import static com.farao_community.farao.data.crac_api.Direction.*;
import static com.farao_community.farao.data.crac_api.Side.*;
import static org.junit.Assert.*;

public class CracFileTest {

    @Test
    public void testCrac() {

        NetworkElement networkElement1 = new NetworkElement("idNE1", "My Element 1");

        // Redispatching
        NetworkElement generator = new NetworkElement("idGenerator", "My Generator");
        Redispatching rd = new Redispatching(10, 20, 18, 1000, 12, generator);
        rd.setMinimumPower(rd.getMinimumPower() + 1);
        rd.setMaximumPower(rd.getMaximumPower() + 1);
        rd.setTargetPower(rd.getTargetPower() + 1);
        rd.setStartupCost(rd.getStartupCost() + 1);
        rd.setMarginalCost(rd.getMarginalCost() + 1);

        // Range domain
        RelativeFixedRange relativeFixedRange = new RelativeFixedRange(0, 1);
        relativeFixedRange.setMin(1);
        relativeFixedRange.setMax(10);
        RelativeDynamicRange relativeDynamicRange = new RelativeDynamicRange(0, 1);
        relativeDynamicRange.setMin(100);
        relativeDynamicRange.setMax(1000);
        AbsoluteFixedRange absoluteFixedRange = new AbsoluteFixedRange(0, 1);
        absoluteFixedRange.setMin(10);
        absoluteFixedRange.setMax(1000);

        // PstRange
        NetworkElement pst1 = new NetworkElement("idPst1", "My Pst 1");
        PstRange pstRange1 = new PstRange(null);
        pstRange1.setNetworkElement(pst1);

        // HvdcRange
        NetworkElement hvdc1 = new NetworkElement("idHvdc1", "My Hvdc 1");
        HvdcRange hvdcRange1 = new HvdcRange(null);
        hvdcRange1.setNetworkElement(hvdc1);

        // GeneratorRange
        NetworkElement generator1 = new NetworkElement("idGen1", "My Generator 1");
        InjectionRange injectionRange1 = new InjectionRange(null);
        injectionRange1.setNetworkElement(generator1);

        // Countertrading
        Countertrading countertrading = new Countertrading();

        // Topology
        NetworkElement line1 = new NetworkElement("idLine1", "My Line 1");
        Topology topology1 = new Topology(line1, OPEN);
        NetworkElement switch1 = new NetworkElement("idSwitch1", "My Switch 1");
        Topology topology2 = new Topology(null, null);
        topology2.setNetworkElement(switch1);
        topology2.setActionType(CLOSE);

        // Hvdc setpoint
        HvdcSetpoint hvdcSetpoint = new HvdcSetpoint(switch1, 0);
        hvdcSetpoint.setNetworkElement(line1);
        hvdcSetpoint.setSetpoint(1000);

        // Pst setpoint
        PstSetpoint pstSetpoint = new PstSetpoint(switch1, 0);
        pstSetpoint.setNetworkElement(pst1);
        pstSetpoint.setSetpoint(5);

        // Injection setpoint
        InjectionSetpoint injectionSetpoint = new InjectionSetpoint(switch1, 0);
        injectionSetpoint.setNetworkElement(generator1);
        injectionSetpoint.setSetpoint(100);

        NetworkElement line2 = new NetworkElement("idLine2", "My Line 2");
        NetworkElement line3 = new NetworkElement("idLine3", "My Line 3");

        List<NetworkElement> elementsList = new ArrayList<>(Arrays.asList(line2, line3));
        Contingency contingency = new Contingency("idContingency", "My contingency", null);
        contingency.setNetworkElements(elementsList);
        contingency.addNetworkElement(networkElement1);

        // Instant
        Instant basecase = new Instant(0);
        Instant curative = new Instant(-1);
        curative.setDuration(200);

        // State
        State stateBasecase = new State(Optional.empty(), basecase);
        State stateCurative = new State(Optional.empty(), null);
        stateCurative.setContingency(Optional.of(contingency));
        stateCurative.setInstant(curative);

        NetworkElement monitoredElement = new NetworkElement("idMR", "Monitored Element");

        // Thresholds
        FlowThreshold threshold1 = new FlowThreshold(LEFT, IN, 1000);
        threshold1.setSide(RIGHT);
        threshold1.setDirection(OUT);
        threshold1.setMaxValue(999);
        VoltageThreshold threshold2 = new VoltageThreshold(280, 300);
        threshold2.setMinValue(275);
        threshold2.setMaxValue(305);

        // CNECs
        Cnec cnec1 = new Cnec("idCnec", "Cnec", null, threshold1, stateCurative);
        cnec1.setCriticalNetworkElement(monitoredElement);
        Cnec cnec2 = new Cnec("idCnec2", "Cnec 2", monitoredElement, null, null);
        cnec2.setState(stateBasecase);
        cnec2.setThreshold(threshold2);

        // Usage rules
        FreeToUse freeToUse = new FreeToUse(null, null);
        freeToUse.setUsageMethod(UsageMethod.AVAILABLE);
        freeToUse.setState(stateBasecase);
        OnContingency onContingency = new OnContingency(UsageMethod.FORCED, stateCurative, null);
        onContingency.setContingency(contingency);
        OnConstraint onConstraint = new OnConstraint(UsageMethod.FORCED, stateCurative, null);
        onConstraint.setCnec(cnec1);

        // NetworkAction
        NetworkAction networkAction1 = new NetworkAction("id1", "name1", new ArrayList<>(Arrays.asList(freeToUse)), new ArrayList<>(Arrays.asList(hvdcSetpoint)));
        networkAction1.addNetworkAction(topology2);
        NetworkAction networkAction2 = new NetworkAction("id2", "name2", new ArrayList<>(Arrays.asList(freeToUse)), new ArrayList<>(Arrays.asList(pstSetpoint)));

        // RangeAction
        RangeAction rangeAction1 = new RangeAction("idRangeAction", "myRangeAction", null, null, null);
        List<Range> ranges = new ArrayList<>(Arrays.asList(absoluteFixedRange, relativeDynamicRange));
        rangeAction1.setRanges(ranges);
        rangeAction1.addRange(relativeFixedRange);
        List<ApplicableRangeAction> elementaryRangeActions = new ArrayList<>(Arrays.asList(pstRange1));
        rangeAction1.setApplicableRangeActions(elementaryRangeActions);
        rangeAction1.addElementaryRangeAction(hvdcRange1);
        List<AbstractUsageRule> usageRules =  new ArrayList<>(Arrays.asList(freeToUse, onConstraint));
        rangeAction1.setUsageRules(usageRules);
        rangeAction1.addUsageRule(onContingency);

        RangeAction rangeAction2 = new RangeAction("idRangeAction2", "myRangeAction2", usageRules, ranges, new ArrayList<>(Arrays.asList(pstRange1)));

        List<Cnec> cnecs = new ArrayList<>();
        cnecs.add(cnec1);

        Crac crac = new Crac("idCrac", "name", new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        crac.setCnecs(cnecs);
        crac.addCnec(cnec2);
        crac.setNetworkActions(new ArrayList<>(Arrays.asList(networkAction1)));
        crac.addNetworkRemedialAction(networkAction2);
        crac.setRangeActions(new ArrayList<>(Arrays.asList(rangeAction1)));
        crac.addRangeRemedialAction(rangeAction2);

        crac.getCnecs().forEach(
            cnec -> {
                cnec.getState().getInstant();
                cnec.getState().getContingency();
            });

        crac.getRangeActions().forEach(
            abstractRemedialAction -> abstractRemedialAction.getUsageRules().forEach(
                abstractUsageRule -> {
                    abstractUsageRule.getUsageMethod();
                }));

        assertTrue(crac.getId().equals("idCrac"));
        assertTrue(cnec2.isBasecase());
    }
}
