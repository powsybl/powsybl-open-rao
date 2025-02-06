/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.openrao.searchtreerao.result.api.LinearProblemStatus;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class InterTemporalIteratingLinearOptimizationResult {
    private LinearProblemStatus status;
    private int nbOfIteration;

    public InterTemporalIteratingLinearOptimizationResult(LinearProblemStatus status, int nbOfIteration) {
        this.status = status;
        this.nbOfIteration = nbOfIteration;
    }

    public void setStatus(LinearProblemStatus status) {
        this.status = status;
    }

    public LinearProblemStatus getStatus() {
        return status;
    }

    public int getNbOfIteration() {
        return nbOfIteration;
    }

    public void setNbOfIteration(int nbOfIteration) {
        this.nbOfIteration = nbOfIteration;
    }

}
