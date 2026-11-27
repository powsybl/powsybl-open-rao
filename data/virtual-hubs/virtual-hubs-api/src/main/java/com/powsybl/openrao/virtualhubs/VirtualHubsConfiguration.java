/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Virtual hubs configuration POJO
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class VirtualHubsConfiguration {
    private Region region = null;
    private final List<SynchronousArea> synchronousAreas = new ArrayList<>();
    private final List<ControlArea> controlAreas = new ArrayList<>();
    private final List<MarketArea> marketAreas = new ArrayList<>();
    private final List<VirtualHub> virtualHubs = new ArrayList<>();
    private final List<BorderDirection> borderDirections = new ArrayList<>();
    private final List<InternalHvdc> internalHvdcs = new ArrayList<>();

    public void setRegion(final Region region) {
        this.region = region;
    }

    public void addSynchronousArea(SynchronousArea synchronousArea) {
        synchronousAreas.add(Objects.requireNonNull(synchronousArea, "Virtual hubs configuration does not allow adding null synchronous area"));
    }

    public void addControlArea(ControlArea controlArea) {
        controlAreas.add(Objects.requireNonNull(controlArea, "Virtual hubs configuration does not allow adding null control area"));
    }

    public void addMarketArea(MarketArea marketArea) {
        marketAreas.add(Objects.requireNonNull(marketArea, "Virtual hubs configuration does not allow adding null market area"));
    }

    public void addVirtualHub(VirtualHub virtualHub) {
        virtualHubs.add(Objects.requireNonNull(virtualHub, "Virtual hubs configuration does not allow adding null virtual hub"));
    }

    public void addBorderDirection(BorderDirection borderDirection) {
        borderDirections.add(Objects.requireNonNull(borderDirection, "Virtual hubs configuration does not allow adding null border direction"));
    }

    public void addInternalHvdc(InternalHvdc internalHvdc) {
        internalHvdcs.add(Objects.requireNonNull(internalHvdc, "Virtual hubs configuration does not allow adding null internal hvdc"));
    }

    public Region getRegion() {
        return region;
    }

    public List<SynchronousArea> getSynchronousAreas() {
        return synchronousAreas;
    }

    public List<ControlArea> getControlAreas() {
        return controlAreas;
    }

    public List<MarketArea> getMarketAreas() {
        return marketAreas;
    }

    public List<VirtualHub> getVirtualHubs() {
        return virtualHubs;
    }

    public List<BorderDirection> getBorderDirections() {
        return borderDirections;
    }

    public List<InternalHvdc> getInternalHvdcs() {
        return internalHvdcs;
    }
}
