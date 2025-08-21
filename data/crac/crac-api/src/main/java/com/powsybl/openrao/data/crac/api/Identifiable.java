/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.commons.extensions.Extendable;

/**
 * An object that is part of the Crac model and that is identified uniquely
 * by a <code>String</code> id.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public interface Identifiable<I extends Identifiable<I>> extends Extendable<I> {

    /**
     * Get the unique identifier of the object.
     */
    String getId();

    /**
     * Get an the (optional) name  of the object.
     */
    String getName();
}
