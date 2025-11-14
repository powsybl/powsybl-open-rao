/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.extensions;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.crac.api.Crac;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class PstRegulation extends AbstractExtension<Crac> {
    private final Set<PstRegulationInput> pstRegulationInputs;

    public PstRegulation() {
        this(new HashSet<>());
    }

    public PstRegulation(Set<PstRegulationInput> pstRegulationInputs) {
        this.pstRegulationInputs = pstRegulationInputs;
    }

    public Set<PstRegulationInput> getRegulationInputs() {
        return pstRegulationInputs;
    }

    public void addPstRegulationInput(PstRegulationInput pstRegulationInput) {
        pstRegulationInputs.add(pstRegulationInput);
    }

    @Override
    public String getName() {
        return "pst-regulation";
    }
}
