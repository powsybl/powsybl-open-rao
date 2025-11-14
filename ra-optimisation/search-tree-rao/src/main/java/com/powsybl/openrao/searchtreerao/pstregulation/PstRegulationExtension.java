/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.pstregulation;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.crac.api.Crac;

import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class PstRegulationExtension extends AbstractExtension<Crac> {
    private final Map<String, Set<PstRegulationInput>> regulationInputs;

    public PstRegulationExtension(Map<String, Set<PstRegulationInput>> regulationInputs) {
        this.regulationInputs = regulationInputs;
    }

    public Map<String, Set<PstRegulationInput>> getRegulationInputs() {
        return regulationInputs;
    }

    @Override
    public String getName() {
        return "pst-regulation";
    }
}
