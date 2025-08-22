/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.openrao.data.crac.api.cnec.Cnec;

/**
 * Class representing the instants at which a {@link Cnec} can be monitored and
 * a {@link RemedialAction} applied.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public interface Instant extends Identifiable<Instant>, Comparable<Instant> {

    int getOrder();

    InstantKind getKind();

    @Override
    String toString();

    default boolean comesBefore(Instant otherInstant) {
        return getOrder() < otherInstant.getOrder();
    }

    default boolean comesAfter(Instant otherInstant) {
        return getOrder() > otherInstant.getOrder();
    }

    default boolean isPreventive() {
        return InstantKind.PREVENTIVE.equals(getKind());
    }

    default boolean isOutage() {
        return InstantKind.OUTAGE.equals(getKind());
    }

    default boolean isAuto() {
        return InstantKind.AUTO.equals(getKind());
    }

    default boolean isCurative() {
        return InstantKind.CURATIVE.equals(getKind());
    }

    @Override
    default int compareTo(Instant otherInstant) {
        return Integer.compare(getOrder(), otherInstant.getOrder());
    }

    static Instant min(Instant instant1, Instant instant2) {
        if (instant1 == null || instant2 == null) {
            return null;
        }
        return instant1.comesBefore(instant2) ? instant1 : instant2;
    }
}
