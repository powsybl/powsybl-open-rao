/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_file_impl;

/**
 * Elementary HVDC range remedial action
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class HvdcLever extends AbstractRangeLever {

    private HvdcLever(NetworkElement networkElement, double minRelative, double maxRelative, double minAbsolute, double maxAbsolute) {
        super(networkElement, minRelative, maxRelative, minAbsolute, maxAbsolute);
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
}
