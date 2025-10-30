/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.intertemporalconstraints;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class IntertemporalConstraints {
    private final Set<GeneratorConstraints> generatorConstraints;

    public IntertemporalConstraints() {
        this.generatorConstraints = new HashSet<>();
    }

    public void addGeneratorConstraints(GeneratorConstraints generatorConstraints) {
        this.generatorConstraints.add(generatorConstraints);
    }

    public Set<GeneratorConstraints> getGeneratorConstraints() {
        return generatorConstraints;
    }
}
