/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Optional;

/**
 * The threshold describes the monitored physical parameter of a Cnec (flow, voltage
 * or angle), as well as the domain within which the Cnec can be operated securely. This
 * domain is defined by a min and/or a max. If the two are defined, the secure operating
 * range of the Cnec is [min, max], otherwise it is [-infinity, max] or [min, +infinity].
 *
 * Some Threshold need to be synchronized with a Network to be fully defined (for instance,
 * a Threshold defined as a percentage of a maximum Network limit). If a Threshold is not
 * synchronised, most of its method can throw SynchronizationExceptions.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface Threshold extends Synchronizable {

    /**
     * A Threshold consists in monitoring a given physical value (FLOW, VOLTAGE
     * or ANGLE). This physical value can be retrieved by the getPhysicalParameter()
     * method.
     */
    @JsonIgnore
    PhysicalParameter getPhysicalParameter();

    /**
     * If it is defined, this function returns the maximum limit of the Threshold,
     * below which a Cnec cannot be operated securely. Otherwise, this function
     * returns an empty Optional, which implicitly means that the Threshold is
     * unbounded above.
     * The returned value is given with the Unit given in argument of the function.
     */
    @JsonIgnore
    Optional<Double> getMinThreshold(Unit unit);

    /**
     * If it is defined, this function returns the maximum limit of the Threshold,
     * below which a Cnec cannot be operated securely. Otherwise, this function
     * returns an empty Optional, which implicitly means that the Threshold is
     * unbounded above.
     * The returned value is given with the unit given in argument of the function.
     */
    @JsonIgnore
    Optional<Double> getMaxThreshold(Unit unit);
}
