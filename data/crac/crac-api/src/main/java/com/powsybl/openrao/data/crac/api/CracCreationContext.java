/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import java.time.OffsetDateTime;

/**
 *  Common interface of a Crac creation context
 * <p>
 *  A CracCreationContext contains the results of a CRAC creation, notably the created Crac
 *  object.
 * <p>
 *  It also contains additional information on how the {@link Crac} has been created from a
 *  native Crac, for instance on how the object of the NativeCrac has been mapped in
 *  the created Crac.
 * <p>
 *  The CracCreationContext is notably required by some Crac exporters, to roll-back
 *  some Crac information with their native values.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface CracCreationContext {

    /**
     * Get a boolean indicating whether the Crac creation was successful or not
     */
    boolean isCreationSuccessful();

    /**
     * Get the created Crac object
     */
    Crac getCrac();

    /**
     * Get the timestamp for which the CRAC has been created
     */
    OffsetDateTime getTimeStamp();

    /**
     * Get the name of the network used to create the CRAC
     */
    String getNetworkName();

    /**
     * Get a report with important information about the creation process
     */
    CracCreationReport getCreationReport();
}
