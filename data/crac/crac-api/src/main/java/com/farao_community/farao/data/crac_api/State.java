/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import java.util.Optional;

/**
 * A State is a situation defined by an {@link Instant} and an optional {@link Contingency}
 *
 * It can be the preventive state, which takes place at Instant.PREVENTIVE.
 *
 * Or a post-contingency state, which takes place at another instant than
 * Instant.PREVENTIVE, and after a given contingency.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface State extends Comparable<State> {

    /**
     * Get the id of the state
     */
    String getId();

    /**
     * Get the instant of the state
     */
    Instant getInstant();

    /**
     * Get the contingency of the state. It is empty for the preventive state
     */
    Optional<Contingency> getContingency();

    /**
     * Returns a boolean indicating whether or not the state is the preventive one
     */
    default boolean isPreventive() {
        return getContingency().isEmpty();
    }

    @Override
    default int compareTo(State state) {
        return getInstant().getOrder() - state.getInstant().getOrder();
    }
}
