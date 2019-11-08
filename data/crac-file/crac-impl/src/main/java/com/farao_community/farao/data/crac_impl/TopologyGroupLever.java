/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import java.util.List;

/**
 * Topological remedial group
 *
 * @author Xxx Xxx {@literal <xxx.xxx at rte-france.com>}
 */
public class TopologyGroupLever implements RemedialActionLever {
    private List<TopologyModification> topologyModifications;

    public TopologyGroupLever(List<TopologyModification> topologyModifications) {
        this.topologyModifications = topologyModifications;
    }

    public List<TopologyModification> getTopologyModifications() {
        return topologyModifications;
    }

    public void setTopologyModifications(List<TopologyModification> topologyModifications) {
        this.topologyModifications = topologyModifications;
    }

    public void addTopologyModification(TopologyModification topologyModification) {
        this.topologyModifications.add(topologyModification);
    }
}
