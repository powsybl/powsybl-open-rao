/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

/**
 * Elementary HVDC range remedial action
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class HvdcLever extends AbstractRemedialActionRange {

    private NetworkElement networkElement;

    private double minAbsolute;
    private double maxAbsolute;

    private double minRelative;
    private double maxRelative;

    private HvdcLever(NetworkElement networkElement, double minRelative, double maxRelative, double minAbsolute, double maxAbsolute) {
        this.networkElement = networkElement;
        this.minAbsolute = minAbsolute;
        this.maxAbsolute = maxAbsolute;
        this.minRelative = minRelative;
        this.maxRelative = maxRelative;
    }

    public static HvdcLever withAbsoluteRange(NetworkElement networkElement, double minAbsolute, double maxAbsolute) {
        return new HvdcLever(networkElement, Double.NaN, Double.NaN, minAbsolute, maxAbsolute);
    }

    public static HvdcLever withRelativeRange(NetworkElement networkElement, double minRelative, double maxRelative) {
        return new HvdcLever(networkElement, minRelative, maxRelative, Double.NaN, Double.NaN);
    }

    public static HvdcLever create(NetworkElement networkElement, double minRelative, double maxRelative, double minAbsolute, double maxAbsolute) {
        return new HvdcLever(networkElement, minRelative, maxRelative, minAbsolute, maxAbsolute);
    }

    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }

    public boolean hasRelativeRange() {
        return !Double.isNaN(minRelative) && !Double.isNaN(maxRelative);
    }

    public boolean hasAbsoluteRange() {
        return !Double.isNaN(minAbsolute) && !Double.isNaN(maxAbsolute);
    }
}
