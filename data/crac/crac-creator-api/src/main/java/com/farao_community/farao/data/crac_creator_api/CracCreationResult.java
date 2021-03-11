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
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface CracCreationResult<T extends NativeCrac, S extends CracCreationContext<T>> {
    /**
     * Get a boolean indicating whether the Crac creation was successful or not
     */
    public boolean isCreationSuccessful();

    /**
     * Get the created Crac object
     */
    public Crac getCrac();

    /**
     * Get the {@link CracCreationContext}
     */
    public S getCracCreationContext();

    /**
     * Get the creation report, given as a list of logs
     */
    public List<String> getCreationReport();
}
