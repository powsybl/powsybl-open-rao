/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_impl.AbstractRemedialAction;
import com.farao_community.farao.data.crac_impl.remedial_action.usage_rule.AbstractUsageRule;
import com.powsybl.iidm.network.Network;
import java.util.List;

/**
 * Group of simple elementary remedial actions (setpoint, open/close, ...).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class NetworkAction extends AbstractRemedialAction implements ApplicableNetworkAction {

    private List<ApplicableNetworkAction> applicableNetworkActions;

    public NetworkAction(String id, String name, List<AbstractUsageRule> usageRules, List<ApplicableNetworkAction> applicableNetworkActions) {
        super(id, name, usageRules);
        this.applicableNetworkActions = applicableNetworkActions;
    }

    public void setApplicableNetworkActions(List<ApplicableNetworkAction> applicableNetworkActions) {
        this.applicableNetworkActions = applicableNetworkActions;
    }

    public List<ApplicableNetworkAction> getApplicableNetworkActions() {
        return applicableNetworkActions;
    }

    @Override
    public void apply(Network network) {
        applicableNetworkActions.forEach(applicableNetworkAction -> applicableNetworkAction.apply(network));
    }

    public void addApplicableNetworkAction(ApplicableNetworkAction networkAction) {
        this.applicableNetworkActions.add(networkAction);
    }
}
