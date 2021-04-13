/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_impl.json.serializers.ContingencyImplSerializer;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.*;
import com.powsybl.iidm.network.*;

import java.util.*;

/**
 * Business object for a contingency in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("complex-contingency")
@JsonSerialize(using = ContingencyImplSerializer.class)
public class ContingencyImpl extends AbstractIdentifiable<Contingency> implements Contingency {

    private Set<NetworkElement> networkElements;

    @Deprecated
    // TODO : make private package
    public ContingencyImpl(String id, String name, final Set<NetworkElement> networkElements) {
        super(id, name);
        this.networkElements = networkElements;
    }

    @Deprecated
    public ContingencyImpl(String id, final Set<NetworkElement> networkElements) {
        this(id, id, networkElements);
    }

    @Deprecated
    public ContingencyImpl(String id) {
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
        // This implementation is unused for now as long as the contingencies are applied in the systematic sensitivity computation
        // It might be used to implement curative features
        com.powsybl.contingency.Contingency contingency = new com.powsybl.contingency.Contingency(getId(), new ArrayList<>());
        getNetworkElements().forEach(contingencyElement -> {
            Identifiable element = network.getIdentifiable(contingencyElement.getId());
            if (element instanceof Branch) {
                contingency.addElement(new BranchContingency(contingencyElement.getId()));
            } else if (element instanceof Generator) {
                contingency.addElement(new GeneratorContingency(contingencyElement.getId()));
            } else if (element instanceof HvdcLine) {
                contingency.addElement(new HvdcLineContingency(contingencyElement.getId()));
            } else if (element instanceof BusbarSection) {
                contingency.addElement(new BusbarSectionContingency(contingencyElement.getId()));
            } else if (element instanceof DanglingLine) {
                contingency.addElement(new DanglingLineContingency(contingencyElement.getId()));
            } else {
                throw new FaraoException("Unable to apply contingency element " + contingencyElement.getId());
            }
        });
        com.powsybl.contingency.Contingency.checkValidity(Collections.singletonList(contingency), network);
        contingency.toTask().modify(network, computationManager);
    }

    /**
     * Check if complex contingencies are equals. Complex contingencies are considered equals when IDs are equals
     * and all the contained network elements are also equals. So sets of network elements have to be strictly equals.
     *
     * @param o: If it's null or another object than ContingencyImpl it will return false.
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
        ContingencyImpl contingency = (ContingencyImpl) o;
        return super.equals(o) && new HashSet<>(contingency.getNetworkElements()).equals(new HashSet<>(networkElements));
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public void synchronize(Network network) {
        // nothing to do
    }

    @Override
    public void desynchronize() {
        // nothing to do
    }

    @Override
    public boolean isSynchronized() {
        return true;
    }
}
