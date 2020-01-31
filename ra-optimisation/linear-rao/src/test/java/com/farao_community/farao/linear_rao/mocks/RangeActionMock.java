/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.mocks;

import com.farao_community.farao.data.crac_api.*;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.iidm.network.Network;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionMock implements RangeAction {
    private String id;
    private NetworkElement networkElement;
    private double minValue;
    private double maxValue;

    public RangeActionMock(String id, String networkElementId, double minValue, double maxValue) {
        this.id = id;
        networkElement = new NetworkElement(networkElementId);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public Set<ApplicableRangeAction> getApplicableRangeActions() {
        return Collections.singleton(new ApplicableRangeActionMock(networkElement));
    }

    @Override
    public void apply(Network network, double setpoint) {

    }

    @Override
    public String getOperator() {
        return null;
    }

    @Override
    public UsageMethod getUsageMethod(Network network, State state) {
        return null;
    }

    @Override
    public List<UsageRule> getUsageRules() {
        return null;
    }

    @Override
    public void addUsageRule(UsageRule usageRule) {

    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return null;
    }

    @Override
    public double getMinValue(Network network) {
        return minValue;
    }

    @Override
    public double getMaxValue(Network network) {
        return maxValue;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return id;
    }

    @Override
    public void addExtension(Class aClass, Extension extension) {

    }

    @Override
    public Extension getExtension(Class aClass) {
        return null;
    }

    @Override
    public Extension getExtensionByName(String s) {
        return null;
    }

    @Override
    public boolean removeExtension(Class aClass) {
        return false;
    }

    @Override
    public Collection getExtensions() {
        return null;
    }
}
