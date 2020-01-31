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

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CnecMock implements Cnec {
    private String id;
    private ThresholdMock thresholdMock;

    public CnecMock(String id, double min, double max) {
        this.id = id;
        thresholdMock = new ThresholdMock(min, max);
    }

    @Override
    public State getState() {
        return null;
    }

    @Override
    public NetworkElement getCriticalNetworkElement() {
        return null;
    }

    @Override
    public double computeMargin(Network network) throws SynchronizationException {
        return 0;
    }

    @Override
    public Threshold getThreshold() {
        return thresholdMock;
    }

    @Override
    public boolean isMinThresholdViolated(Network network) throws SynchronizationException {
        return false;
    }

    @Override
    public boolean isMaxThresholdViolated(Network network) throws SynchronizationException {
        return false;
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
    public void synchronize(Network network) {

    }

    @Override
    public void desynchronize() {

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
