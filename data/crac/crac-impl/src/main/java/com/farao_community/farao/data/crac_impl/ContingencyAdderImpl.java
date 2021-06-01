/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.ContingencyAdder;
import com.farao_community.farao.data.crac_api.NetworkElement;

import java.util.*;

import static java.lang.String.format;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ContingencyAdderImpl extends AbstractIdentifiableAdder<ContingencyAdder> implements ContingencyAdder {
    CracImpl owner;
    private final Set<NetworkElement> networkElements = new HashSet<>();

    ContingencyAdderImpl(CracImpl owner) {
        Objects.requireNonNull(owner);
        this.owner = owner;
    }

    @Override
    protected String getTypeDescription() {
        return "Contingency";
    }

    @Override
    public ContingencyAdder withNetworkElement(String networkElementId) {
        return this.withNetworkElement(networkElementId, networkElementId);
    }

    @Override
    public ContingencyAdder withNetworkElement(String networkElementId, String networkElementName) {
        /*
         * A contingency contains several network elements to trip. When adding a contingency, all the contained
         * network elements have to be in the networkElements list of the crac.
         * Here we go through all the network elements of the contingency, if an equal element is already present in
         * the crac list we can directly pick its reference, if not we first have to create a new element of the
         * list copying the network element contained in the contingency.
         * Then we can create a new contingency referring to network elements already presents in the crac.
         */
        NetworkElement networkElement = this.owner.addNetworkElement(networkElementId, networkElementName);
        this.networkElements.add(networkElement);
        return this;
    }

    @Override
    public Contingency add() {
        checkId();
        Contingency contingency = new ContingencyImpl(id, name, networkElements);
        if (owner.getContingency(id) != null) {
            if (owner.getContingency(id).equals(contingency)) {
                // If the same contingency exists in the crac
                return owner.getContingency(id);
            } else {
                throw new FaraoException(format("A contingency with the same ID (%s) but a different name or network elements already exists.", this.id));
            }
        } else {
            owner.addContingency(contingency);
            return owner.getContingency(id);
        }
        // TODO : create additional states if there are RAs with "FreeToUse" usage rule (on curative/auto instant)
        // not required as as soon as there is no RA on AUTO instant
    }
}
