/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.range.TapRange;
import com.powsybl.openrao.data.cracapi.range.TapRangeAdder;
import com.powsybl.openrao.data.cracapi.rangeaction.*;
import com.powsybl.openrao.data.cracapi.usagerule.UsageRule;

import java.util.*;

import static com.powsybl.openrao.data.cracimpl.AdderUtils.assertAttributeNotNull;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PstRangeActionAdderImpl extends AbstractRemedialActionAdder<PstRangeActionAdder> implements PstRangeActionAdder {
    public static final String PST_RANGE_ACTION = "PstRangeAction";
    private String networkElementId;
    private String networkElementName;
    private List<TapRange> ranges;
    private String groupId = null;
    private Integer initialTap = null;
    private Map<Integer, Double> tapToAngleConversionMap;

    @Override
    protected String getTypeDescription() {
        return PST_RANGE_ACTION;
    }

    PstRangeActionAdderImpl(CracImpl owner) {
        super(owner);
        this.ranges = new ArrayList<>();
    }

    @Override
    public PstRangeActionAdder withNetworkElement(String networkElementId) {
        return withNetworkElement(networkElementId, networkElementId);
    }

    @Override
    public PstRangeActionAdder withNetworkElement(String networkElementId, String networkElementName) {
        this.networkElementId = networkElementId;
        this.networkElementName = networkElementName;
        return this;
    }

    @Override
    public PstRangeActionAdder withGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    @Override
    public PstRangeActionAdder withInitialTap(int initialTap) {
        this.initialTap = initialTap;
        return this;
    }

    @Override
    public PstRangeActionAdder withTapToAngleConversionMap(Map<Integer, Double> tapToAngleConversionMap) {
        this.tapToAngleConversionMap = tapToAngleConversionMap;
        return this;
    }

    @Override
    public TapRangeAdder newTapRange() {
        return new TapRangeAdderImpl(this);
    }

    @Override
    public PstRangeAction add() {
        checkId();
        checkAutoUsageRules();
        assertAttributeNotNull(networkElementId, PST_RANGE_ACTION, "network element", "withNetworkElement()");
        assertAttributeNotNull(initialTap, PST_RANGE_ACTION, "initial tap", "withInitialTap()");
        assertAttributeNotNull(tapToAngleConversionMap, PST_RANGE_ACTION, "tap to angle conversion map", "withTapToAngleConversionMap()");

        if (!Objects.isNull(getCrac().getRemedialAction(id))) {
            throw new OpenRaoException(String.format("A remedial action with id %s already exists", id));
        }

        List<TapRange> validRanges = checkRanges();
        checkTapToAngleConversionMap();

        if (usageRules.isEmpty()) {
            BUSINESS_WARNS.warn("PstRangeAction {} does not contain any usage rule, by default it will never be available", id);
        }

        NetworkElement networkElement = this.getCrac().addNetworkElement(networkElementId, networkElementName);
        PstRangeActionImpl pstWithRange = new PstRangeActionImpl(this.id, this.name, this.operator, this.usageRules, validRanges, networkElement, groupId, initialTap, tapToAngleConversionMap, speed, activationCost);
        this.getCrac().addPstRangeAction(pstWithRange);
        return pstWithRange;
    }

    void addRange(TapRange pstRange) {
        ranges.add(pstRange);
    }

    private boolean isPreventiveUsageRule(UsageRule usageRule) {
        return usageRule.getInstant().isPreventive();
    }

    private List<TapRange> checkRanges() {

        // filter RELATIVE_TO_PREVIOUS_INSTANT range if the RA is purely preventive
        List<TapRange> validRanges = new ArrayList<>();

        if (usageRules.stream().allMatch(this::isPreventiveUsageRule)) {
            ranges.forEach(range -> {
                if (range.getRangeType().equals(RangeType.RELATIVE_TO_PREVIOUS_INSTANT)) {
                    BUSINESS_WARNS.warn("RELATIVE_TO_PREVIOUS_INSTANT range has been filtered from PstRangeAction {}, as it is a preventive RA", id);
                } else {
                    validRanges.add(range);
                }
            });
        } else {
            validRanges.addAll(ranges);
        }

        if (validRanges.isEmpty()) {
            BUSINESS_WARNS.warn("PstRangeAction {} does not contain any valid range, by default the range of the network will be used", id);
        }
        return validRanges;
    }

    private void checkTapToAngleConversionMap() {

        if (tapToAngleConversionMap.size() < 2) {
            throw new OpenRaoException(String.format("TapToAngleConversionMap of PST %s should at least contain 2 entries.", id));
        }
        if (tapToAngleConversionMap.keySet().stream().anyMatch(Objects::isNull) || tapToAngleConversionMap.values().stream().anyMatch(Objects::isNull)) {
            throw new OpenRaoException(String.format("TapToAngleConversionMap of PST %s cannot contain null values", id));
        }

        int minTap = Collections.min(tapToAngleConversionMap.keySet());
        int maxTap = Collections.max(tapToAngleConversionMap.keySet());

        boolean isInverted = tapToAngleConversionMap.get(minTap) > tapToAngleConversionMap.get(maxTap);
        double previousTapAngle = tapToAngleConversionMap.get(minTap);

        for (int tap = minTap + 1; tap < maxTap; tap++) {
            if (!tapToAngleConversionMap.containsKey(tap)) {
                throw new OpenRaoException(String.format("TapToAngleConversionMap of PST %s should contain all the consecutive taps between %d and %d", id, minTap, maxTap));
            }
            if (!isInverted && tapToAngleConversionMap.get(tap) < previousTapAngle
                || isInverted && tapToAngleConversionMap.get(tap) > previousTapAngle) {
                throw new OpenRaoException(String.format("TapToAngleConversionMap of PST %s should be increasing or decreasing", id));
            }
            previousTapAngle = tapToAngleConversionMap.get(tap);
        }

        if (initialTap > maxTap || initialTap < minTap) {
            throw new OpenRaoException(String.format("initialTap of PST %s must be included into its tapToAngleConversionMap", id));
        }
    }

    void checkAutoUsageRules() {
        usageRules.forEach(usageRule -> {
            if (usageRule.getInstant().isAuto() && Objects.isNull(speed)) {
                throw new OpenRaoException("Cannot create an AUTO Pst range action without speed defined");
            }
        });
    }
}
