/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.commons.extensions.Extendable;

/**
 * An object that is part of the network model and that is identified uniquely
 * by a <code>String</code> id.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
@JsonIgnoreProperties(value = {"extensions"})
public interface Identifiable<I extends Identifiable<I>> extends Extendable<I> {

    /**
     * Get the unique identifier of the object.
     *
     * @return The object unique identifier as a String.
     */
    String getId();

    /**
     * Get an the (optional) name  of the object.
     *
     * @return The object optional name as a String.
     */
    String getName();
}
