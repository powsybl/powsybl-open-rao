/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface IdentifiableAdder<I> {

    /**
     * Set the ID of the {@link Identifiable}
     * @param id: ID to set
     * @return the {@code IdentifiableAdder} instance
     */
    I setId(String id);

    /**
     * Set the name of the {@link Identifiable}
     * @param name: Name to set
     * @return the {@code IdentifiableAdder} instance
     */
    I setName(String name);
}
