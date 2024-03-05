/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi;

import com.powsybl.openrao.data.cracapi.cnec.Cnec;

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

    boolean comesBefore(Instant otherInstant);

    boolean comesAfter(Instant otherInstant);

    boolean isPreventive();

    boolean isOutage();

    boolean isAuto();

    boolean isCurative();
}
