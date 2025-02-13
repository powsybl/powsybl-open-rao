/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * A State is a situation defined by an {@link Instant} and an optional {@link Contingency}
 *
 * It can be the preventive state, which takes place at an instant of kind InstantKind.PREVENTIVE.
 *
 * Or a post-contingency state, which takes place at another instant which is not of kind
 * InstantKind.PREVENTIVE, and after a given contingency.
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
     * Get the timestamp of the state. It is the same as the CRAC's
     */
    Optional<OffsetDateTime> getTimestamp();

    /**
     * Returns a boolean indicating whether the state is the preventive one
     */
    default boolean isPreventive() {
        return getContingency().isEmpty();
    }

    @Override
    default int compareTo(State state) {
        if (state.getTimestamp().equals(getTimestamp())) {
            return getInstant().getOrder() - state.getInstant().getOrder();
        }
        Optional<OffsetDateTime> timestamp = getTimestamp();
        Optional<OffsetDateTime> otherTimestamp = state.getTimestamp();
        if (timestamp.isEmpty() || otherTimestamp.isEmpty()) {
            throw new OpenRaoException("Cannot compare states with no timestamp");
        }
        return timestamp.get().compareTo(otherTimestamp.get());
    }
}
