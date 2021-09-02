/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creator_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.native_crac_api.NativeCrac;
import com.powsybl.iidm.network.Network;

import java.time.OffsetDateTime;

/**
 * Common interface for creators of {@link Crac} objects.
 *
 * @param <T> is the {@link NativeCrac} implementation used as input of the CracCreator
 * @param <S> is the {@link CracCreationContext} implementation returned by the CracCreator
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface CracCreator<T extends NativeCrac, S extends CracCreationContext> {

    /**
     * Get a unique identifier of the native Crac format handled by the CracCreator.
     */
    String getNativeCracFormat();

    /**
     * Create a Crac object from a NativeCrac, a Network and on OffsetDateTime.
     * Note that null offsetDateTime might be acceptable for CracCreator implementations
     */
    S createCrac(T nativeCrac, Network network, OffsetDateTime offsetDateTime, CracCreatorParameters cracCreatorParameters);
}
