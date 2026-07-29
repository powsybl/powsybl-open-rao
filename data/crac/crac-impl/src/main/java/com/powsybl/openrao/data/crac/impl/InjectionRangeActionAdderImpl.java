/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotEmpty;
import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InjectionRangeActionAdderImpl extends AbstractStandardRangeActionAdder<InjectionRangeActionAdder> implements InjectionRangeActionAdder {

    public static final String INJECTION_RANGE_ACTION = "InjectionRangeAction";
    private final List<DistributionKeyOnNetworkElement> distributionKeys;

    @Override
    protected String getTypeDescription() {
        return INJECTION_RANGE_ACTION;
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
        if (Math.abs(key) > 1e-3) {
            this.getCrac().addNetworkElement(networkElementId, networkElementName);
        }
        return this;
    }

    @Override
    public InjectionRangeActionAdder withNetworkElement(String networkElementId) {
        if (distributionKeys.isEmpty()) {
            return withNetworkElementAndKey(1.0, networkElementId, networkElementId);
        } else {
            throw new OpenRaoException("There are already NetworkElements tied to this injection. Use instead withNetworkElementAndKey() to add multiple NetworkElements");
        }
    }

    @Override
    public InjectionRangeActionAdder withNetworkElement(String networkElementId, String networkElementName) {
        if (distributionKeys.isEmpty()) {
            return withNetworkElementAndKey(1.0, networkElementId, networkElementName);
        } else {
            throw new OpenRaoException("There are already NetworkElements tied to this injection. Use instead withNetworkElementAndKey() to add multiple NetworkElements");
        }
    }

    @Override
    public InjectionRangeAction add() {
        runCheckBeforeAdding();
        Map<NetworkElement, Double> neAndDk = getDistributionKeyMap();
        InjectionRangeAction injectionRangeAction = new InjectionRangeActionImpl(
            this.id, this.name, this.operator, this.groupId, this.usageRules, this.ranges, this.initialSetpoint, neAndDk, speed, activationCost, variationCosts
        );
        this.getCrac().addInjectionRangeAction(injectionRangeAction);
        return injectionRangeAction;
    }

    public void runCheckBeforeAdding() {
        checkId();
        checkAutoUsageRules();
        if (!Objects.isNull(getCrac().getRemedialAction(id))) {
            throw new OpenRaoException(String.format("A remedial action with id %s already exists", id));
        }

        // check network elements
        checkNetworkElements();
        assertAttributeNotEmpty(distributionKeys, INJECTION_RANGE_ACTION, "injection distribution key", "withNetworkElementAndKey()");

        // check ranges
        assertAttributeNotEmpty(ranges, INJECTION_RANGE_ACTION, "range", "newRange()");

        // check usage rules
        if (usageRules.isEmpty()) {
            BUSINESS_WARNS.warn("InjectionRangeAction {} does not contain any usage rule, by default it will never be available", id);
        }
    }

    private void checkNetworkElements() {
        distributionKeys.forEach(dK -> assertAttributeNotNull(dK.networkElementId, INJECTION_RANGE_ACTION, "network element", "withNetworkElementAndKey()"));
    }

    private Map<NetworkElement, Double> getDistributionKeyMap() {
        Map<NetworkElement, Double> distributionKeyMap = new HashMap<>();
        distributionKeys.forEach(sK -> {
            if (Math.abs(sK.distributionKey) > 1e-3) {
                NetworkElement networkElement = this.getCrac().getNetworkElement(sK.networkElementId);
                distributionKeyMap.merge(networkElement, sK.distributionKey, Double::sum);
            }
        });
        return distributionKeyMap;
    }

    private static final class DistributionKeyOnNetworkElement {
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
