/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import java.util.Optional;

/**
 * Business object for a state (instant and contingency) in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class State {

    private Contingency contingency;
    private Instant instant;

    public State(Optional<Contingency> contingency, Instant instant) {
        this.contingency = contingency.orElse(null);
        this.instant = instant;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public Optional<Contingency> getContingency() {
        return Optional.ofNullable(contingency);
    }

    public void setContingency(Optional<Contingency> contingency) {
        this.contingency = contingency.orElse(null);
    }
}
