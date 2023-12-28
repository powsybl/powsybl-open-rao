/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.data.crac_api.cnec.Cnec;

/**
 * Class representing the instants at which a {@link Cnec} can be monitored and
 * a {@link RemedialAction} applied.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public interface Instant<T extends Instant<T>> extends Identifiable<T> {

    int getOrder();

    InstantKind getKind();

    @Override
    String toString();

    boolean comesBefore(Instant otherInstant);

    boolean isPreventive();

    boolean isOutage();

    boolean isAuto();

    boolean isCurative();
}
