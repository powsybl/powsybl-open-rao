/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creator_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.native_crac_api.NativeCrac;

import java.util.List;

/**
 *  Common interface of a Crac creation context
 *
 *  A CracCreationContext contains the results of a CRAC creation, notably the created Crac
 *  object.
 *
 *  It also contains additional information on how the {@link Crac} has been created from a
 *  {@link NativeCrac}, for instance on how the object of the NativeCrac has been mapped in
 *  the created Crac.
 *
 *  The CracCreationContext is notably required by some Crac exporters, so as to rollback
 *  some of the Crac information with their native values.
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

    /**
     * Get a report with important information about the creation process
     */
    List<String> getCreationReport();
}
