/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;

import java.util.*;
import java.util.stream.Collectors;

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
    public InjectionRangeAction addWithInitialSetpointFromNetwork(Network network) {
        runCheckBeforeAdding();

        Map<NetworkElement, Double> neAndDk = addNetworkElements();
        this.initialSetpoint = getCurrentSetpoint(network, neAndDk);
        InjectionRangeAction injectionRangeAction = new InjectionRangeActionImpl(this.id, this.name, this.operator, this.groupId, this.usageRules, this.ranges, this.initialSetpoint, neAndDk, speed, activationCost, variationCosts);
        this.getCrac().addInjectionRangeAction(injectionRangeAction);
        return injectionRangeAction;
    }

    @Override
    public InjectionRangeAction add() {
        runCheckBeforeAdding();
        Map<NetworkElement, Double> neAndDk = addNetworkElements();
        InjectionRangeAction injectionRangeAction = new InjectionRangeActionImpl(this.id, this.name, this.operator, this.groupId, this.usageRules, this.ranges, null, neAndDk, speed, activationCost, variationCosts);
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

    public static double getCurrentSetpoint(Network network, Map<NetworkElement, Double> injectionDistributionKeys) {
        List<Double> currentSetpoints = injectionDistributionKeys.entrySet().stream()
            .map(entry -> getInjectionSetpoint(network, entry.getKey().getId(), entry.getValue()))
            .collect(Collectors.toList());

        if (currentSetpoints.size() == 1) {
            return currentSetpoints.get(0);
        } else {
            Collections.sort(currentSetpoints);
            if (Math.abs(currentSetpoints.get(0) - currentSetpoints.get(currentSetpoints.size() - 1)) < 1) {
                return currentSetpoints.get(0);
            } else {
                throw new OpenRaoException(String.format("Cannot evaluate reference setpoint of InjectionRangeAction, as the injections are not distributed according to their key"));
            }
        }
    }

    public static double getInjectionSetpoint(Network network, String injectionId, double distributionKey) {
        Generator generator = network.getGenerator(injectionId);
        if (generator != null) {
            return generator.getTargetP() / distributionKey;
        }

        Load load = network.getLoad(injectionId);
        if (load != null) {
            return -load.getP0() / distributionKey;
        }

        if (network.getIdentifiable(injectionId) == null) {
            throw new OpenRaoException(String.format("Injection %s not found in network", injectionId));
        } else {
            throw new OpenRaoException(String.format("%s refers to an object of the network which is not an handled Injection (not a Load, not a Generator)", injectionId));
        }
    }

    private void checkNetworkElements() {
        distributionKeys.forEach(dK -> assertAttributeNotNull(dK.networkElementId, INJECTION_RANGE_ACTION, "network element", "withNetworkElementAndKey()"));
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
