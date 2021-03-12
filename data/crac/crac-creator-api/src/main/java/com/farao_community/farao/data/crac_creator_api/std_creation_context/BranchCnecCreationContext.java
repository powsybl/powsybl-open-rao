/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creator_api.std_creation_context;

import com.farao_community.farao.data.crac_api.Instant;

import java.util.Map;
import java.util.Optional;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface BranchCnecCreationContext {

    /**
     * Get the native id the CNEC
     */
    String getNativeCnecId();

    /**
     * Get the native branch definition of the CNEC
     */
    NativeBranch getNativeBranch();

    /**
     * Get a boolean indicating whether the native CNEC is monitored in preventive, or after a contingency
     * Underlying assumption: one native CNEC cannot be monitored in preventive and after a contingency
     */
    boolean isBaseCase();

    /**
     * Get the id of the native CNEC contingency
     * Underlying assumption: one native CNEC definition do not cover several contingencies
     * @return Optional of the contingency id, or empty Optional if isBaseCase()
     */
    Optional<String> getContingencyId();

    /**
     * Get a boolean indicating whether or not the native CNEC has been imported
     */
    boolean isImported();

    //idea: add here a method getStatus, which returns an enum with additional information on why the
    //native Cnec was not imported

    /**
     * Get a map of created CNECs id, whose key is the instant on which the created CNEC is monitored
     */
    Map<Instant, String> getCreatedCnecsIds();

    /**
     * Get a boolean indicating whether or not the direction of created CNECs is inverted compared
     * to the one of the native CNEC
     */
    boolean isDirectionInvertedInNetwork();
}
