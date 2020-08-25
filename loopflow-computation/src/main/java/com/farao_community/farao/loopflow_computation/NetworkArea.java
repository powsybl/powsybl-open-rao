/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

/**
 * NetworkArea defines an area for balances adjustment as a net position provider, calculated on a Network object
 *
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Mathieu Bague {@literal <mathieu.bague at rte-france.com>}
 */
public interface NetworkArea {
    /**
     * Computes the net position of the area on a given network object.
     * Net position sign convention is positive when flows are leaving the area (export) and negative
     * when flows feed the area (import).
     *
     * @return Sum of the flows leaving the area
     */
    double getNetPosition();
}
