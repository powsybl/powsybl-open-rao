/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.raw_crac_api;

/**
 * Common interface for all RawCrac objects.
 *
 * A RawCrac is an object which contains all the raw information of a CRAC file. Where CRAC
 * stands for 'Contingency list, Remedial Actions and additional Constraints'.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface RawCrac {

    /**
     * Get a unique identifier of the format from which the RawCrac has been imported.
     */
    String getFormat();
}
