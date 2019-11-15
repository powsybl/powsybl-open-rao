/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.NetworkAction;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.RangeAction;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business object of the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class Crac extends AbstractIdentifiable {
    private List<Cnec> cnecs;
    private List<RangeAction> rangeActions;
    private List<NetworkAction> networkActions;

    Crac(String id, String name, List<Cnec> cnecs, List<RangeAction> rangeActions) {
        super(id, name);
        this.cnecs = cnecs;
        this.rangeActions = rangeActions;
    }

    public List<Cnec> getCnecs() {
        return cnecs;
    }

    public void setCnecs(List<Cnec> cnecs) {
        this.cnecs = cnecs;
    }

    public List<RangeAction> getRangeActions() {
        return rangeActions;
    }

    public void setRangeActions(List<RangeAction> rangeActions) {
        this.rangeActions = rangeActions;
    }

    public List<NetworkAction> getNetworkActions() {
        return networkActions;
    }

    public void setNetworkActions(List<NetworkAction> networkActions) {
        this.networkActions = networkActions;
    }

    public void addCnec(Cnec cnec) {
        cnecs.add(cnec);
    }

    public void addRangeRemedialAction(RangeAction rangeAction) {
        rangeActions.add(rangeAction);
    }

    public List<RangeAction> getRangeActions(Network network, UsageMethod usageMethod) {
        return null;
    }

    public List<NetworkAction> getNetworkActions(Network network, UsageMethod usageMethod) {
        return null;
    }

    public List<NetworkElement> getCriticalNetworkElements() {
        List<NetworkElement> criticalNetworkElements = new ArrayList<>();
        cnecs.forEach(cnec -> criticalNetworkElements.add(cnec.getCriticalNetworkElement()));
        return criticalNetworkElements;
    }

    public List<Contingency> getContingencies() {
        List<Contingency> contingencies = new ArrayList<>();
        cnecs.forEach(cnec -> {
            Optional<Contingency> contingency = cnec.getState().getContingency();
            contingency.ifPresent(contingencies::add);
        });
        return contingencies;
    }

    @Override
    protected String getTypeDescription() {
        return "Crac file";
    }

    public void synchronize(Network network) {
        throw new UnsupportedOperationException();
    }

    public void generateValidityReport(Network network) {
        throw new UnsupportedOperationException();
    }
}
