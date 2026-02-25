/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs.networkextensionbuilder;

import com.powsybl.iidm.network.*;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.networkextension.AssignedVirtualHubAdder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class VirtualHubAssigner {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualHubAssigner.class);
    private List<VirtualHub> virtualHubs;

    public VirtualHubAssigner(List<VirtualHub> virtualHubs) {
        this.virtualHubs = virtualHubs;
    }

    public void addVirtualLoads(Network network) {
        virtualHubs.forEach(vh -> addVirtualLoad(network, vh));
    }

    private void addVirtualLoad(Network network, VirtualHub virtualHub) {

        Optional<Bus> bus = findBusById(network, virtualHub.nodeName());
        if (bus.isPresent()) {
            // virtual hub is on a real network node
            addVirtualHubOnNewFictitiousLoad(bus.get(), virtualHub);
            return;
        }

        Optional<DanglingLine> danglingLine = findDanglingLineWithXNode(network, virtualHub.nodeName());
        if (danglingLine.isPresent()) {
            // virtual hub is on a Xnode which has been merged in a dangling line during network import
            if (danglingLine.get().getTerminal().isConnected()) {
                addVirtualHubOnNewFictitiousLoad(danglingLine.get().getTerminal().getBusBreakerView().getConnectableBus(), virtualHub);
            } else {
                LOGGER.warn("Virtual hub {} was not assigned on node {} as it is disconnected from the main network", virtualHub.eic(), virtualHub.nodeName());
            }
            return;
        }

        LOGGER.warn("Virtual hub {} cannot be assigned on node {} as it was not found in the network", virtualHub.eic(), virtualHub.nodeName());
    }

    private void addVirtualHubOnNewFictitiousLoad(Bus bus, VirtualHub virtualHub) {
        // add a fictitious load to this bus
        Load load = bus.getVoltageLevel().newLoad()
            .setBus(bus.getId())
            .setId(virtualHub.eic() + "_virtualLoad")
            .setEnsureIdUnicity(true)
            .setLoadType(LoadType.FICTITIOUS)
            .setP0(0.).setQ0(0.)
            .add();

        // the virtual hub is assigned on this load
        load.newExtension(AssignedVirtualHubAdder.class)
            .withCode(virtualHub.code())
            .withEic(virtualHub.eic())
            .withMcParticipant(virtualHub.isMcParticipant())
            .withNodeName(virtualHub.nodeName())
            .withRelatedMa(Objects.isNull(virtualHub.relatedMa()) ? null : virtualHub.relatedMa().code())
            .add();

        LOGGER.info("A fictitious load {} has been added to {} in order to assign the virtual hub {}", load.getId(), bus.getId(), virtualHub.eic());
    }

    private Optional<Bus> findBusById(Network network, String id) {
        return network.getVoltageLevelStream()
            .flatMap(vl -> vl.getBusBreakerView().getBusStream())
            .filter(bus -> bus.getId().equals(id))
            .findFirst();
    }

    private Optional<DanglingLine> findDanglingLineWithXNode(Network network, String xNodeId) {
        return network.getDanglingLineStream()
            .filter(danglingLine -> danglingLine.getPairingKey().equals(xNodeId))
            .findFirst();
    }
}
