/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext;

import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;

import java.util.Map;
import java.util.Optional;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface BranchCnecCreationContext extends ElementaryCreationContext {

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
     * Underlying assumption: one native CNEC definition does not cover several contingencies
     * @return Optional of the contingency id, or empty Optional if isBaseCase()
     */
    Optional<String> getContingencyId();

    /**
     * Get a map of created CNECs id, whose key is the instant id on which the created CNEC is monitored
     */
    Map<String, String> getCreatedCnecsIds();

    /**
     * Get a boolean indicating whether the direction of created CNECs is inverted compared
     * to the one of the native CNEC
     */
    boolean isDirectionInvertedInNetwork();
}
