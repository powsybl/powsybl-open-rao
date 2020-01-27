/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public interface Threshold {

    @JsonIgnore
    Optional<Double> getMinThreshold() throws SynchronizationException;

    @JsonIgnore
    Optional<Double> getMinThreshold(Unit unit) throws SynchronizationException;

    @JsonIgnore
    Optional<Double> getMaxThreshold() throws SynchronizationException;

    @JsonIgnore
    Optional<Double> getMaxThreshold(Unit unit) throws SynchronizationException;

    @JsonIgnore
    Unit getUnit();

    boolean isMinThresholdOvercome(Network network, Cnec cnec) throws SynchronizationException;

    boolean isMaxThresholdOvercome(Network network, Cnec cnec) throws SynchronizationException;

    double computeMargin(Network network, Cnec cnec) throws SynchronizationException;

    void synchronize(Network network, Cnec cnec);

    void desynchronize();
}
