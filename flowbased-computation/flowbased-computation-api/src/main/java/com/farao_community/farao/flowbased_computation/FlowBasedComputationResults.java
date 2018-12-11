/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.powsybl.sensitivity.SensitivityComputationResults;

/**
 * Interface Flowbased results
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public interface FlowBasedComputationResults {

    /**
     * @param sensResultsIn set results from the flowbased computation
     */
    void setSensiResults(SensitivityComputationResults sensResultsIn);

    /**
     * @return SensitivityComputationResults
     */
    SensitivityComputationResults getSensiResults();
}
