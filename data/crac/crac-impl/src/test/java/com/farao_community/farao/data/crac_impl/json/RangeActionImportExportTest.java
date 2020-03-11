/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.json;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeDefinition;
import com.farao_community.farao.data.crac_impl.AbstractRemedialAction;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.range_domain.RangeType;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.farao_community.farao.data.crac_impl.json.RoundTripUtil.roundTrip;
import static org.junit.Assert.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionImportExportTest {
    @Test
    public void pstRangeJsonCreator() {
        PstWithRange pstRange = new PstWithRange(
            "pstRangeId",
            "pstRangeName",
            "RTE",
            new NetworkElement("neId")
        );
        PstWithRange transformedPstRange = roundTrip(pstRange, PstWithRange.class);
        assertEquals(transformedPstRange, pstRange);
    }

    @Test
    public void hvdcRangeJsonCreator() {
        HvdcRange hvdcRange = new HvdcRange(
            "hvdcRangeId",
            "hvdcRangeName",
            "RTE",
            new NetworkElement("neId")
        );
        HvdcRange transformedHvdcRange = roundTrip(hvdcRange, HvdcRange.class);
        assertEquals(transformedHvdcRange, hvdcRange);
    }

    @Test
    public void injectionRangeJsonCreator() {
        InjectionRange injectionRange = new InjectionRange(
            "injectionRangeId",
            "injectionRangeName",
            "RTE",
            new NetworkElement("neId")
        );
        InjectionRange transformedInjectionRange = roundTrip(injectionRange, InjectionRange.class);
        assertEquals(transformedInjectionRange, injectionRange);
    }

    @Test
    public void alignedRangeJsonCreator() {
        Set<NetworkElement> networkElements = new HashSet<>();
        networkElements.add(new NetworkElement("neId1"));
        networkElements.add(new NetworkElement("neId2"));
        AlignedRangeAction alignedRangeAction = new AlignedRangeAction(
            "alignedRangeActionId",
            "alignedRangeActionName",
            "RTE",
            networkElements
        );
        AlignedRangeAction transformedAlignedRangeAction = roundTrip(alignedRangeAction, AlignedRangeAction.class);
        assertEquals(transformedAlignedRangeAction, alignedRangeAction);
    }

    @Test
    public void countertradingJsonCreator() {
        Countertrading countertrading = new Countertrading(
            "countertradingId",
            "countertradingName",
            "RTE"
        );
        Countertrading transformedCountertrading = roundTrip(countertrading, Countertrading.class);
        assertEquals(transformedCountertrading, countertrading);
    }

    @Test
    public void redispatchingJsonCreator() {
        Redispatching redispatching = new Redispatching(
            "redispatchingId",
            "redispatchingName",
            "RTE",
            10,
            new NetworkElement("generatorId")
        );
        Redispatching transformedRedispatching = roundTrip(redispatching, Redispatching.class);
        assertEquals(transformedRedispatching, redispatching);
    }

    @Test
    public void abstractElementaryRangeActionWithPstJsonCreator() {
        AbstractElementaryRangeAction abstractElementaryRangeAction = new PstWithRange(
            "pstRangeId",
            "pstRangeName",
            "RTE",
            new NetworkElement("neId")
        );
        AbstractElementaryRangeAction transformedAbstractElementaryRangeAction = roundTrip(abstractElementaryRangeAction, AbstractElementaryRangeAction.class);
        assertEquals(transformedAbstractElementaryRangeAction, abstractElementaryRangeAction);
    }

    @Test
    public void abstractElementaryRangeActionWithHvdcJsonCreator() {
        AbstractElementaryRangeAction abstractElementaryRangeAction = new HvdcRange(
            "hvdcRangeId",
            "hvdcRangeName",
            "RTE",
            new NetworkElement("neId")
        );
        AbstractElementaryRangeAction transformedAbstractElementaryRangeAction = roundTrip(abstractElementaryRangeAction, AbstractElementaryRangeAction.class);
        assertEquals(transformedAbstractElementaryRangeAction, abstractElementaryRangeAction);
    }

    @Test
    public void abstractElementaryRangeActionWithInjectionJsonCreator() {
        AbstractElementaryRangeAction abstractElementaryRangeAction = new InjectionRange(
            "injectionRangeId",
            "injectionRangeName",
            "RTE",
            new NetworkElement("neId")
        );
        AbstractElementaryRangeAction transformedAbstractElementaryRangeAction = roundTrip(abstractElementaryRangeAction, AbstractElementaryRangeAction.class);
        assertEquals(transformedAbstractElementaryRangeAction, abstractElementaryRangeAction);
    }

    @Test
    public void abstractElementaryRangeActionWithRedispatchingJsonCreator() {
        AbstractElementaryRangeAction abstractElementaryRangeAction = new Redispatching(
            "redispatchingId",
            "redispatchingName",
            "RTE",
            10,
            new NetworkElement("generatorId")
        );
        AbstractElementaryRangeAction transformedAbstractElementaryRangeAction = roundTrip(abstractElementaryRangeAction, AbstractElementaryRangeAction.class);
        assertEquals(transformedAbstractElementaryRangeAction, abstractElementaryRangeAction);
    }

    @Test
    public void abstractRemedialActionWithPstJsonCreator() {
        AbstractRemedialAction abstractRemedialAction = new PstWithRange(
            "pstRangeId",
            "pstRangeName",
            "RTE",
            new NetworkElement("neId")
        );
        AbstractRemedialAction transformedAbstractRemedialAction = roundTrip(abstractRemedialAction, AbstractRemedialAction.class);
        assertEquals(transformedAbstractRemedialAction, abstractRemedialAction);
    }

    @Test
    public void abstractRemedialActionWithHvdcJsonCreator() {
        AbstractRemedialAction abstractRemedialAction = new HvdcRange(
            "hvdcRangeId",
            "hvdcRangeName",
            "RTE",
            new NetworkElement("neId")
        );
        AbstractRemedialAction transformedAbstractRemedialAction = roundTrip(abstractRemedialAction, AbstractRemedialAction.class);
        assertEquals(transformedAbstractRemedialAction, abstractRemedialAction);
    }

    @Test
    public void abstractRemedialActionWithInjectionJsonCreator() {
        AbstractRemedialAction abstractRemedialAction = new InjectionRange(
            "injectionRangeId",
            "injectionRangeName",
            "RTE",
            new NetworkElement("neId")
        );
        AbstractRemedialAction transformedAbstractRemedialAction = roundTrip(abstractRemedialAction, AbstractRemedialAction.class);
        assertEquals(transformedAbstractRemedialAction, abstractRemedialAction);
    }

    @Test
    public void abstractRemedialActionWithRedispatchingJsonCreator() {
        AbstractRemedialAction abstractRemedialAction = new Redispatching(
            "redispatchingId",
            "redispatchingName",
            "RTE",
            10,
            new NetworkElement("generatorId")
        );
        AbstractRemedialAction transformedAbstractRemedialAction = roundTrip(abstractRemedialAction, AbstractRemedialAction.class);
        assertEquals(transformedAbstractRemedialAction, abstractRemedialAction);
    }

    @Test
    public void abstractRemedialActionWithCountertradingJsonCreator() {
        AbstractRemedialAction abstractRemedialAction = new Countertrading(
            "countertradingId",
            "countertradingName",
            "RTE"
        );
        AbstractRemedialAction transformedAbstractRemedialAction = roundTrip(abstractRemedialAction, AbstractRemedialAction.class);
        assertEquals(transformedAbstractRemedialAction, abstractRemedialAction);
    }

    @Test
    public void abstractRemedialActionWithAlignedRangeActionJsonCreator() {
        Set<NetworkElement> networkElements = new HashSet<>();
        networkElements.add(new NetworkElement("neId1"));
        networkElements.add(new NetworkElement("neId2"));
        AbstractRemedialAction abstractRemedialAction = new AlignedRangeAction(
            "alignedRangeActionId",
            "alignedRangeActionName",
            "RTE",
            networkElements
        );
        AbstractRemedialAction transformedAbstractRemedialAction = roundTrip(abstractRemedialAction, AbstractRemedialAction.class);
        assertEquals(transformedAbstractRemedialAction, abstractRemedialAction);
    }

    @Test
    public void abstractRemedialActionWithAlignedRangeActionWithRangesJsonCreator() {
        List<Range> ranges = new ArrayList<>();
        ranges.add(new Range(0, 12, RangeType.ABSOLUTE_FIXED, RangeDefinition.STARTS_AT_ONE));
        ranges.add(new Range(-5, 5, RangeType.RELATIVE_FIXED, RangeDefinition.STARTS_AT_ONE));
        ranges.add(new Range(4, 12, RangeType.RELATIVE_DYNAMIC, RangeDefinition.STARTS_AT_ONE));
        ranges.add(new Range(-12, 12, RangeType.ABSOLUTE_FIXED, RangeDefinition.CENTERED_ON_ZERO));
        Set<NetworkElement> networkElements = new HashSet<>();
        networkElements.add(new NetworkElement("neId1"));
        networkElements.add(new NetworkElement("neId2"));
        AbstractRemedialAction abstractRemedialAction = new AlignedRangeAction(
            "alignedRangeActionId",
            "alignedRangeActionName",
            "RTE",
            ranges,
            networkElements
        );
        AbstractRemedialAction transformedAbstractRemedialAction = roundTrip(abstractRemedialAction, AbstractRemedialAction.class);
        assertEquals(transformedAbstractRemedialAction, abstractRemedialAction);
    }
}
