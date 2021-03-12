/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creator_api.std_creation_context;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;

import java.util.Map;
import java.util.Optional;

public interface CnecCreationContext {
    String getNativeCnecId();

    NativeBranch getNativeBranch();

    boolean isBaseCase();

    Optional<Contingency> getContingency();

    boolean isImported();

    //idea: add here a method getStatus, which returns an enum with additional information on why the
    //native Cnec was not imported

    boolean isDirectionInvertedInNetwork();

    Map<Instant, String> getCreatedCnecsIds();
}
