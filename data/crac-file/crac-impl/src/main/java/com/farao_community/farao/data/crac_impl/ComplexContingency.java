/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.AbstractIdentifiable;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/**
 * Business object for a contingency in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class ComplexContingency extends AbstractIdentifiable implements Contingency {

    @JsonProperty("networkElements")
    private List<NetworkElement> networkElements;

    @JsonCreator
    public ComplexContingency(@JsonProperty("id") String id,  @JsonProperty("name") String name,
                              @JsonProperty("networkElements") final List<NetworkElement> networkElements) {
        super(id, name);
        this.networkElements = networkElements;
    }

    public ComplexContingency(String id, final List<NetworkElement> networkElements) {
        this(id, id, networkElements);
    }

    public List<NetworkElement> getNetworkElements() {
        return networkElements;
    }

    public void setNetworkElements(List<NetworkElement> networkElements) {
        this.networkElements = networkElements;
    }

    @JsonProperty("networkElements")
    public void addNetworkElement(NetworkElement networkElement) {
        networkElements.add(networkElement);
    }
}
