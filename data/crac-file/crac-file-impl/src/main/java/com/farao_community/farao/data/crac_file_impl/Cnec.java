/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_file_impl;

/**
 * Critical network element and contingency
 *
 * @author Xxx Xxx {@literal <xxx.xxx at rte-france.com>}
 */
public class Cnec extends AbstractIdentifiable {
    private NetworkElement criticalNetworkElement;
    private AbstractThreshold threshold;
    private State state;

    public Cnec(String id, String name, NetworkElement criticalNetworkElement, AbstractThreshold threshold, State state) {
        super(id, name);
        this.criticalNetworkElement = criticalNetworkElement;
        this.threshold = threshold;
        this.state = state;
    }

    public NetworkElement getCriticalNetworkElement() {
        return criticalNetworkElement;
    }

    public void setCriticalNetworkElement(NetworkElement criticalNetworkElement) {
        this.criticalNetworkElement = criticalNetworkElement;
    }

    public AbstractThreshold getThreshold() {
        return threshold;
    }

    public void setThreshold(AbstractThreshold threshold) {
        this.threshold = threshold;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isBasecase() {
        return !state.getContingency().isPresent();
    }

    @Override
    protected String getTypeDescription() {
        return "Critical network element and contingency";
    }
}
