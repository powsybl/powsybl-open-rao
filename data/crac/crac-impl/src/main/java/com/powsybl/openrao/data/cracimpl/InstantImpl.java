/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;

import java.util.Objects;

/**
 * Class representing the instants at which a {@link Cnec} can be monitored and
 * a {@link RemedialAction} applied.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class InstantImpl extends AbstractIdentifiable<Instant> implements Instant {

    private final InstantKind instantKind;
    private final Instant previous;
    private final int order;
    private final int hash;

    InstantImpl(String id, InstantKind instantKind, Instant previous) {
        super(id);
        if (Objects.equals(id, "initial")) {
            throw new OpenRaoException("Instant with id 'initial' can't be defined");
        }
        this.previous = previous;
        this.instantKind = instantKind;
        if (previous == null) {
            this.order = 0;
        } else {
            this.order = previous.getOrder() + 1;
        }
        this.hash = computeHashCode();
    }

    public int getOrder() {
        return order;
    }

    public InstantKind getKind() {
        return instantKind;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        InstantImpl instant = (InstantImpl) o;
        return order == instant.order && instantKind == instant.instantKind;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    private int computeHashCode() {
        return Objects.hash(super.hashCode(), instantKind, order);
    }

    Instant getInstantBefore() {
        return previous;
    }
}
