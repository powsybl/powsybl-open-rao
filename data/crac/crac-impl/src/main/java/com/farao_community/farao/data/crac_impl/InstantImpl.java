/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.cnec.Cnec;

/**
 * Class representing the instants at which a {@link Cnec} can be monitored and
 * a {@link RemedialAction} applied.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class InstantImpl extends AbstractIdentifiable<InstantImpl> implements Instant<InstantImpl> {

    private final InstantKind instantKind;
    private final Instant previous;
    private final int order;

    public InstantImpl(String id, InstantKind instantKind, Instant previous) {
        super(id);
        if (previous == null) {
            // TODO should first instant always be a preventive one ?
            this.order = 0;
        } else {
            this.order = previous.getOrder() + 1;
        }
        this.instantKind = instantKind;
        this.previous = previous;
    }

    public int getOrder() {
        return order;
    }

    public InstantKind getInstantKind() {
        return instantKind;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean comesBefore(Instant otherInstant) {
        return this.order < otherInstant.getOrder();
    }

    public Instant getPreviousInstant() {
        return previous;
    }
}
