/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.range_action.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotEmpty;
import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InjectionShiftRangeActionAdderImpl extends AbstractStandardRangeActionAdder<InjectionShiftRangeActionAdder> implements InjectionShiftRangeActionAdder {

    private static final Logger LOGGER = LoggerFactory.getLogger(InjectionShiftRangeActionAdderImpl.class);

    private List<ShiftKeyOnNetworkElement> shiftKeys;

    @Override
    protected String getTypeDescription() {
        return "InjectionShiftRangeAction";
    }

    InjectionShiftRangeActionAdderImpl(CracImpl owner) {
        super(owner);
        shiftKeys = new ArrayList<>();
    }

    @Override
    public InjectionShiftRangeActionAdder withNetworkElementAndKey(double key, String networkElementId) {
        return withNetworkElementAndKey(key, networkElementId, networkElementId);
    }

    @Override
    public InjectionShiftRangeActionAdder withNetworkElementAndKey(double key, String networkElementId, String networkElementName) {
        shiftKeys.add(new ShiftKeyOnNetworkElement(networkElementId, networkElementName, key));
        return this;
    }

    @Override
    public InjectionShiftRangeAction add() {
        checkId();
        if (!Objects.isNull(getCrac().getRemedialAction(id))) {
            throw new FaraoException(String.format("A remedial action with id %s already exists", id));
        }

        // check network elements
        checkNetworkElements();
        assertAttributeNotEmpty(shiftKeys, "InjectionShiftRangeAction", "injection shift key", "withNetworkElementAndKey()");

        // check ranges
        assertAttributeNotEmpty(ranges, "InjectionShiftRangeAction", "range", "newRange()");

        // check usage rules
        if (usageRules.isEmpty()) {
            LOGGER.warn("InjectionShiftRangeAction {} does not contain any usage rule, by default it will never be available", id);
        }

        Map<NetworkElement, Double> shiftKeys = addNetworkElements();
        InjectionShiftRangeAction injectionShift = new InjectionShiftRangeActionImpl(this.id, this.name, this.operator, this.groupId, this.usageRules, this.ranges, shiftKeys);
        this.getCrac().addInjectionShiftRangeAction(injectionShift);
        return injectionShift;
    }

    private void checkNetworkElements() {
        shiftKeys.forEach(sK -> assertAttributeNotNull(sK.networkElementId, "InjectionShiftRangeAction", "network element", "withNetworkElementAndKey()"));
    }

    private Map<NetworkElement, Double> addNetworkElements() {
        Map<NetworkElement, Double> shiftKeyMap = new HashMap<>();
        shiftKeys.forEach(sK -> {
            if (sK.shiftKey != 0.0) {
                NetworkElement networkElement = this.getCrac().addNetworkElement(sK.networkElementId, sK.networkElementName);
                shiftKeyMap.merge(networkElement, sK.shiftKey, Double::sum);
            }
        });
        return shiftKeyMap;
    }

    private static class ShiftKeyOnNetworkElement {
        String networkElementId;
        String networkElementName;
        double shiftKey;

        ShiftKeyOnNetworkElement(String networkElementId, String networkElementName, double shiftKey) {
            this.networkElementId = networkElementId;
            this.networkElementName = networkElementName;
            this.shiftKey = shiftKey;
        }
    }
}
