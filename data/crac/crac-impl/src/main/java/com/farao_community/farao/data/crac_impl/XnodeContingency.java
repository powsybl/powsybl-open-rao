/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.AbstractIdentifiable;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Business object for a xnode contingency in the CRAC file.
 * These kind of contingencies are defined between 2 Xnodes (branch absent from the network)
 * and should be synchronized with the network in order to map them to the existing branches
 * Xnode1-Xnode2 = RealNode1-Xnode1 + Xnode2-RealNode2
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class XnodeContingency extends AbstractIdentifiable<Contingency> implements Contingency {
    static final Logger LOGGER = LoggerFactory.getLogger(XnodeContingency.class);
    private Set<String> xnodeIds;
    private ComplexContingency realContingency;
    private boolean isSynchronized = false;

    public XnodeContingency(String id, String name, final Set<String> xnodeIds) {
        super(id, name);
        this.xnodeIds = new HashSet<>(xnodeIds);
    }

    public XnodeContingency(String id, final Set<String> xnodeIds) {
        this(id, id, xnodeIds);
    }

    public XnodeContingency(String id) {
        super(id, id);
        this.xnodeIds = new HashSet<>();
    }

    public void addXnode(String xnodeId) {
        isSynchronized = false;
        xnodeIds.add(xnodeId);
    }

    public Set<String> getXnodeIds() {
        return this.xnodeIds;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        if (!isSynchronized) {
            throw new NotSynchronizedException(String.format("Xnode contingency %s has not been synchronized so its network elements cannot be accessed", getId()));
        } else {
            return realContingency.getNetworkElements();
        }
    }

    @Override
    public void apply(Network network, ComputationManager computationManager) {
        if (!isSynchronized) {
            throw new NotSynchronizedException(String.format("Xnode contingency %s has not been synchronized so it cannot be applied to the network", getId()));
        } else {
            realContingency.apply(network, computationManager);
        }
    }

    @Override
    public void synchronize(Network network) {
        if (isSynchronized) {
            throw new AlreadySynchronizedException(String.format("Xnode contingency %s has already been synchronized", getId()));
        }
        Set<NetworkElement> networkElements = new HashSet<>();
        for (String xnode : this.xnodeIds) {
            DanglingLine danglingLine = findDanglingLine(xnode, network);
            if (danglingLine != null) {
                networkElements.add(new NetworkElement(danglingLine.getId()));
            } else {
                LOGGER.error("Xnode {} in contingency {} could not be mapped to a dangling line in the given network. It will be ignored when applying the contingency.", xnode, getId());
            }
        }
        this.realContingency = new ComplexContingency(getId() + "_onDanglingLines", networkElements);
        isSynchronized = true;
    }

    private DanglingLine findDanglingLine(String xnode, Network network) {
        return network.getDanglingLineStream().filter(danglingLine -> danglingLine.getUcteXnodeCode().equals(xnode)).findFirst().orElse(null);
    }

    @Override
    public void desynchronize() {
        isSynchronized = false;
    }

    @Override
    public boolean isSynchronized() {
        return isSynchronized;
    }

    /**
     * Check if xnode contingencies are equals. Xnode contingencies are considered equals when IDs are equals,
     * synchronization status is the same, and all the contained network elements are also equals.
     * So if they are not synchronized, Xnode IDs should be equals.
     * If they are synchronized, network elements should be equals (we can  check that the underlying
     * ComplexContingencies are equals)
     *
     * @param o: If it's null or another object than XnodeContingency it will return false.
     * @return A boolean true if objects are equals, otherwise false.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        XnodeContingency contingency = (XnodeContingency) o;
        if (this.isSynchronized != contingency.isSynchronized) {
            return false;
        } else if (!this.isSynchronized) {
            return super.equals(o) && new HashSet<>(contingency.getXnodeIds()).equals(new HashSet<>(xnodeIds));
        } else {
            return super.equals(o) && this.realContingency.equals(contingency.realContingency);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
