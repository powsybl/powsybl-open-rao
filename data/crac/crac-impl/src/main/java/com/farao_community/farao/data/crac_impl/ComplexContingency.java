/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.AbstractIdentifiable;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_impl.json.serializers.ComplexContingencySerializer;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.util.HashSet;
import java.util.Set;

/**
 * Business object for a contingency in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("complex-contingency")
@JsonSerialize(using = ComplexContingencySerializer.class)
public class ComplexContingency extends AbstractIdentifiable implements Contingency {

    private Set<NetworkElement> networkElements;

    public ComplexContingency(String id, String name, final Set<NetworkElement> networkElements) {
        super(id, name);
        this.networkElements = networkElements;
    }

    public ComplexContingency(String id, final Set<NetworkElement> networkElements) {
        this(id, id, networkElements);
    }

    public ComplexContingency(String id) {
        super(id, id);
        this.networkElements = new HashSet<>();
    }

    public void addNetworkElement(NetworkElement networkElement) {
        networkElements.add(networkElement);
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return networkElements;
    }

    @Override
    public void apply(Network network, ComputationManager computationManager) {
        getNetworkElements().forEach(contingencyElement -> {
            Identifiable element = network.getIdentifiable(contingencyElement.getId());
            if (element instanceof Branch) {
                BranchContingency contingency = new BranchContingency(contingencyElement.getId());
                contingency.toTask().modify(network, computationManager);
            } else {
                throw new FaraoException("Unable to apply contingency element " + contingencyElement.getId());
            }
        });
    }

    /**
     * Check if complex contingencies are equals. Complex contingencies are considered equals when IDs are equals
     * and all the contained network elements are also equals. So sets of network elements have to be strictly equals.
     *
     * @param o: If it's null or another object than ComplexContingency it will return false.
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
        ComplexContingency contingency = (ComplexContingency) o;
        return super.equals(o) && new HashSet<>(contingency.getNetworkElements()).equals(new HashSet<>(networkElements));
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
