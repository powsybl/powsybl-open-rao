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

import java.util.*;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;
import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotEmpty;
import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InjectionRangeActionAdderImpl extends AbstractStandardRangeActionAdder<InjectionRangeActionAdder> implements InjectionRangeActionAdder {

    private final List<DistributionKeyOnNetworkElement> distributionKeys;

    @Override
    protected String getTypeDescription() {
        return "InjectionRangeAction";
    }

    InjectionRangeActionAdderImpl(CracImpl owner) {
        super(owner);
        distributionKeys = new ArrayList<>();
    }

    @Override
    public InjectionRangeActionAdder withNetworkElementAndKey(double key, String networkElementId) {
        return withNetworkElementAndKey(key, networkElementId, networkElementId);
    }

    @Override
    public InjectionRangeActionAdder withNetworkElementAndKey(double key, String networkElementId, String networkElementName) {
        distributionKeys.add(new DistributionKeyOnNetworkElement(key, networkElementId, networkElementName));
        return this;
    }

    @Override
    public InjectionRangeAction add() {
        checkId();
        if (!Objects.isNull(getCrac().getRemedialAction(id))) {
            throw new FaraoException(String.format("A remedial action with id %s already exists", id));
        }

        // check network elements
        checkNetworkElements();
        assertAttributeNotEmpty(distributionKeys, "InjectionRangeAction", "injection distribution key", "withNetworkElementAndKey()");

        // check ranges
        assertAttributeNotEmpty(ranges, "InjectionRangeAction", "range", "newRange()");

        // check usage rules
        if (usageRules.isEmpty()) {
            BUSINESS_WARNS.warn("InjectionRangeAction {} does not contain any usage rule, by default it will never be available", id);
        }

        Map<NetworkElement, Double> neAndDk = addNetworkElements();
        InjectionRangeAction injectionRangeAction = new InjectionRangeActionImpl(this.id, this.name, this.operator, this.groupId, this.usageRules, this.ranges, neAndDk);
        this.getCrac().addInjectionRangeAction(injectionRangeAction);
        return injectionRangeAction;
    }

    private void checkNetworkElements() {
        distributionKeys.forEach(dK -> assertAttributeNotNull(dK.networkElementId, "InjectionRangeAction", "network element", "withNetworkElementAndKey()"));
    }

    private Map<NetworkElement, Double> addNetworkElements() {
        Map<NetworkElement, Double> distributionKeyMap = new HashMap<>();
        distributionKeys.forEach(sK -> {
            if (Math.abs(sK.distributionKey) > 1e-3) {
                NetworkElement networkElement = this.getCrac().addNetworkElement(sK.networkElementId, sK.networkElementName);
                distributionKeyMap.merge(networkElement, sK.distributionKey, Double::sum);
            }
        });
        return distributionKeyMap;
    }

    private static class DistributionKeyOnNetworkElement {
        String networkElementId;
        String networkElementName;
        double distributionKey;

        DistributionKeyOnNetworkElement(double distributionKey, String networkElementId, String networkElementName) {
            this.networkElementId = networkElementId;
            this.networkElementName = networkElementName;
            this.distributionKey = distributionKey;
        }
    }
}
