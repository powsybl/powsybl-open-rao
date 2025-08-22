/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

/**
 * Approximation level for PTDF computations
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public enum PtdfApproximation {
    FIXED_PTDF, // compute PTDFs only once at beginning of RAO (best performance, worst accuracy)
    UPDATE_PTDF_WITH_TOPO, // recompute PTDFs after every topological change in the network (worse performance, better accuracy for AC, best accuracy for DC)
    UPDATE_PTDF_WITH_TOPO_AND_PST; // recompute PTDFs after every topological or PST change in the network (worst performance, best accuracy for AC)

    public boolean shouldUpdatePtdfWithTopologicalChange() {
        return !this.equals(FIXED_PTDF);
    }

    public boolean shouldUpdatePtdfWithPstChange() {
        return this.equals(UPDATE_PTDF_WITH_TOPO_AND_PST);
    }
}
