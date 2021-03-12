/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creator_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.native_crac_api.NativeCrac;

/**
 *  Common interface for CracCreationContext
 *
 *  A CracCreationContext contains the results of a CRAC creation, notably the created Crac
 *  object.
 *
 *  It can also contain additional information on how the {@link Crac} has been created from a
 *  {@link NativeCrac}, for instance on how the object of the NativeCrac has been mapped in
 *  the Crac.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface CracCreationContext {

    /**
     * Get a boolean indicating whether the Crac creation was successful or not
     */
    public boolean isCreationSuccessful();

    /**
     * Get the created Crac object
     */
    public Crac getCrac();
}
