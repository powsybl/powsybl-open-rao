/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.commons.extensions.Extension;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.CnecValue;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.threshold.BranchThreshold;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class FlowCnecMock implements FlowCnec {

    private final Set<BranchThreshold> thresholds;

    public FlowCnecMock(Set<BranchThreshold> thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public Optional<Double> getIMax(TwoSides side) {
        return Optional.empty();
    }

    @Override
    public boolean isConnected(Network network) {
        return false;
    }

    @Override
    public NetworkElement getNetworkElement() {
        return null;
    }

    @Override
    public Set<BranchThreshold> getThresholds() {
        return thresholds;
    }

    @Override
    public Double getNominalVoltage(TwoSides side) {
        return 0.0;
    }

    @Override
    public Optional<Double> getLowerBound(TwoSides side, Unit unit) {
        return Optional.empty();
    }

    @Override
    public Optional<Double> getUpperBound(TwoSides side, Unit unit) {
        return Optional.empty();
    }

    @Override
    public double computeMargin(double actualValue, TwoSides side, Unit unit) {
        return 0;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Set.of();
    }

    @Override
    public State getState() {
        return null;
    }

    @Override
    public double getReliabilityMargin() {
        return 0;
    }

    @Override
    public PhysicalParameter getPhysicalParameter() {
        return null;
    }

    @Override
    public CnecValue computeValue(Network network, Unit unit) {
        return null;
    }

    @Override
    public double computeMargin(Network network, Unit unit) {
        return 0;
    }

    @Override
    public SecurityStatus computeSecurityStatus(Network network, Unit unit) {
        return null;
    }

    @Override
    public boolean isOptimized() {
        return false;
    }

    @Override
    public boolean isMonitored() {
        return false;
    }

    @Override
    public String getOperator() {
        return "";
    }

    @Override
    public String getBorder() {
        return "";
    }

    @Override
    public void setMonitored(boolean monitored) {

    }

    @Override
    public void setOptimized(boolean optimized) {

    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public <E extends Extension<FlowCnec>> void addExtension(Class<? super E> aClass, E e) {

    }

    @Override
    public <E extends Extension<FlowCnec>> E getExtension(Class<? super E> aClass) {
        return null;
    }

    @Override
    public <E extends Extension<FlowCnec>> E getExtensionByName(String s) {
        return null;
    }

    @Override
    public <E extends Extension<FlowCnec>> boolean removeExtension(Class<E> aClass) {
        return false;
    }

    @Override
    public <E extends Extension<FlowCnec>> Collection<E> getExtensions() {
        return List.of();
    }
}
