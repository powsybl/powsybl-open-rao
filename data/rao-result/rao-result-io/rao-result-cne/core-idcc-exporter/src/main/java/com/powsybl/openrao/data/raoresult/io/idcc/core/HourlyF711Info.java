/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresult.io.idcc.core;

import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.CriticalBranchType;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.IndependantComplexVariant;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class HourlyF711Info {

    private List<CriticalBranchType> criticalBranches;
    private List<IndependantComplexVariant> complexVariants;

    HourlyF711Info(List<CriticalBranchType> criticalBranches) {
        this.criticalBranches = criticalBranches;
        this.complexVariants = new ArrayList<>();
    }

    HourlyF711Info(List<CriticalBranchType> criticalBranches, List<IndependantComplexVariant> complexVariants) {
        this.criticalBranches = criticalBranches;
        this.complexVariants = complexVariants;
    }

    List<CriticalBranchType> getCriticalBranches() {
        return criticalBranches;
    }

    List<IndependantComplexVariant> getComplexVariants() {
        return complexVariants;
    }
}
