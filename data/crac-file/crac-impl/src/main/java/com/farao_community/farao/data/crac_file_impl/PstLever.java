/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_file_impl;

/**
 * Elementary PST range remedial action
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class PstLever extends AbstractRangeLever {

    private PstLever(NetworkElement networkElement, double minRelative, double maxRelative, double minAbsolute, double maxAbsolute) {
        super(networkElement, minRelative, maxRelative, minAbsolute, maxAbsolute);
    }

    public static PstLever withAbsoluteRange(NetworkElement networkElement, double minAbsolute, double maxAbsolute) {
        return new PstLever(networkElement, Double.NaN, Double.NaN, minAbsolute, maxAbsolute);
    }

    public static PstLever withRelativeRange(NetworkElement networkElement, double minRelative, double maxRelative) {
        return new PstLever(networkElement, minRelative, maxRelative, Double.NaN, Double.NaN);
    }

    public static PstLever create(NetworkElement networkElement, double minRelative, double maxRelative, double minAbsolute, double maxAbsolute) {
        return new PstLever(networkElement, minRelative, maxRelative, minAbsolute, maxAbsolute);
    }
}
