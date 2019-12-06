/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;
import java.util.ArrayList;
import java.util.List;

/**
 * Business object of the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class SimpleCrac extends AbstractIdentifiable implements Crac {
    private List<Cnec> cnecs;
    @JsonProperty("contingencies")
    private List<Contingency> contingencies;
    private List<RangeAction> rangeActions;
    private List<NetworkAction> networkActions;

    public SimpleCrac(String id, String name) {
        super(id, name);
        cnecs = new ArrayList<>();
        contingencies = new ArrayList<>();
        rangeActions = new ArrayList<>();
        networkActions = new ArrayList<>();
    }

    @JsonCreator
    public SimpleCrac(@JsonProperty("id") String id, @JsonProperty("name") String name,
                      @JsonProperty("cnecs") List<Cnec> cnecs, @JsonProperty("rangeActions") List<RangeAction> rangeActions,
                      @JsonProperty("networkActions") List<NetworkAction> networkActions) {
        super(id, name);
        this.cnecs = cnecs;
        contingencies = new ArrayList<>();
        this.rangeActions = rangeActions;
        this.networkActions = networkActions;
    }

    @Override
    public List<Cnec> getCnecs() {
        return cnecs;
    }

    @Override
    public void setCnecs(List<Cnec> cnecs) {
        this.cnecs = cnecs;
    }

    @Override
    public List<RangeAction> getRangeActions() {
        return rangeActions;
    }

    @Override
    public void setRangeActions(List<RangeAction> rangeActions) {
        this.rangeActions = rangeActions;
    }

    @Override
    public List<NetworkAction> getNetworkActions() {
        return networkActions;
    }

    @Override
    public void setNetworkActions(List<NetworkAction> networkActions) {
        this.networkActions = networkActions;
    }

    @Override
    @JsonProperty("cnecs")
    public void addCnec(Cnec cnec) {
        cnecs.add(cnec);
    }

    @JsonProperty("contingency")
    @Override
    public void addContingency(Contingency contingency) {
        contingencies.add(contingency);
    }

    @JsonProperty("networkActions")
    @Override
    public void addNetworkRemedialAction(NetworkAction networkAction) {
        networkActions.add(networkAction);
    }

    @Override
    @JsonProperty("rangeActions")
    public void addRangeRemedialAction(RangeAction rangeAction) {
        rangeActions.add(rangeAction);
    }

    @Override
    public List<RangeAction> getRangeActions(Network network, UsageMethod usageMethod) {
        return null;
    }

    @Override
    public List<NetworkAction> getNetworkActions(Network network, UsageMethod usageMethod) {
        return null;
    }

    @Override
    public List<NetworkElement> getCriticalNetworkElements() {
        List<NetworkElement> criticalNetworkElements = new ArrayList<>();
        cnecs.forEach(cnec -> criticalNetworkElements.add(cnec.getCriticalNetworkElement()));
        return criticalNetworkElements;
    }

    @Override
    public List<Contingency> getContingencies() {
        return contingencies;
    }

    @Override
    public void synchronize(Network network) {
        cnecs.forEach(cnec -> cnec.synchronize(network));
    }

    @Override
    public void desynchronize() {
        cnecs.forEach(Synchronizable::desynchronize);
    }

    @Override
    public void generateValidityReport(Network network) {
        throw new UnsupportedOperationException();
    }
}
