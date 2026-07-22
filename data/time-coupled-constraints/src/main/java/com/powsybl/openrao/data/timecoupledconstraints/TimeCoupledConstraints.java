/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.timecoupledconstraints;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class TimeCoupledConstraints {
    private final Set<GeneratorConstraints> generatorConstraints;
    private final Set<AdjustmentConstraints> adjustmentConstraints;

    public TimeCoupledConstraints() {
        this.generatorConstraints = new TreeSet<>(Comparator.comparing(GeneratorConstraints::getGeneratorId));
        this.adjustmentConstraints = new TreeSet<>(Comparator.comparing(AdjustmentConstraints::getRangeActionId));
    }

    public TimeCoupledConstraints(Set<GeneratorConstraints> generatorConstraints) {
        this.generatorConstraints = generatorConstraints;
        this.adjustmentConstraints = new TreeSet<>(Comparator.comparing(AdjustmentConstraints::getRangeActionId));
    }

    public void addGeneratorConstraints(GeneratorConstraints generatorConstraints) {
        this.generatorConstraints.add(generatorConstraints);
    }

    public void addAdjustmentConstraints(AdjustmentConstraints adjustmentConstraints) {
        this.adjustmentConstraints.add(adjustmentConstraints);
    }

    public Set<GeneratorConstraints> getGeneratorConstraints() {
        return generatorConstraints;
    }

    public Set<AdjustmentConstraints> getAdjustmentConstraints() {
        return adjustmentConstraints;
    }
}
