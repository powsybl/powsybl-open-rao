/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business object of the CRAC file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class Crac extends AbstractIdentifiable {
    private List<Cnec> cnecs;
    private List<RemedialAction> remedialActions;

    Crac(String id, String name, List<Cnec> cnecs, List<RemedialAction> remedialActions) {
        super(id, name);
        this.cnecs = cnecs;
        this.remedialActions = remedialActions;
    }

    public List<Cnec> getCnecs() {
        return cnecs;
    }

    public void setCnecs(List<Cnec> cnecs) {
        this.cnecs = cnecs;
    }

    public List<RemedialAction> getRemedialActions() {
        return remedialActions;
    }

    public void setRemedialActions(List<RemedialAction> remedialActions) {
        this.remedialActions = remedialActions;
    }

    public void addCnec(Cnec cnec) {
        cnecs.add(cnec);
    }

    public void addRemedialAction(RemedialAction remedialAction) {
        remedialActions.add(remedialAction);
    }

    public List<NetworkElement> getCriticalNetworkElements() {
        List<NetworkElement> criticalNetworkElements = new ArrayList<>();
        cnecs.forEach(cnec -> criticalNetworkElements.add(cnec.getCriticalNetworkElement()));
        return criticalNetworkElements;
    }

    public List<UsageRule> getUsageRules() {
        List<UsageRule> usageRules = new ArrayList<>();
        remedialActions.forEach(remedialAction -> usageRules.addAll(remedialAction.getUsageRules()));
        return usageRules;
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
}
