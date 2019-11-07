/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_file_impl;

/**
 * Range remedial actions
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public abstract class AbstractRangeLever {
    protected NetworkElement networkElement;

    protected double minAbsolute;
    protected double maxAbsolute;

    protected double minRelative;
    protected double maxRelative;

    protected AbstractRangeLever(NetworkElement networkElement, double minRelative, double maxRelative, double minAbsolute, double maxAbsolute) {
        this.networkElement = networkElement;
        this.minAbsolute = minAbsolute;
        this.maxAbsolute = maxAbsolute;
        this.minRelative = minRelative;
        this.maxRelative = maxRelative;
    }

    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }

    public boolean hasRelativeRange() {
        return !Double.isNaN(minRelative) && !Double.isNaN(minRelative);
    }

    public boolean hasAbsoluteRange() {
        return !Double.isNaN(minAbsolute) && !Double.isNaN(maxAbsolute);
    }
}
