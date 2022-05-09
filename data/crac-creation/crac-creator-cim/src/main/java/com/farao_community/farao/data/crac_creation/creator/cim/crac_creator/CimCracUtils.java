/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.ContingencySeries;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class CimCracUtils {
    private CimCracUtils() { }

    public static Optional<Contingency> getContingencyFromCrac(ContingencySeries cimContingency, Crac crac) {
        String createdContingencyId = cimContingency.getMRID();
        Contingency contingency = crac.getContingency(createdContingencyId);
        return Objects.isNull(contingency) ? Optional.empty() : Optional.of(contingency);
    }

}
