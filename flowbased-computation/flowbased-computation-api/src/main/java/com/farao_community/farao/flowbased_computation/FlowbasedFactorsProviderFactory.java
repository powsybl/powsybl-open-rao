/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.powsybl.sensitivity.SensitivityFactorsProvider;

/**
 * * Interface for the factory which create a sensitivity factor provider using GLSK documents
 *
 * @author Di Gallo Luc  {@literal <luc.di-gallo at rte-france.com>}
 * @see SensitivityFactorsProvider
 */
public interface FlowbasedFactorsProviderFactory {


    /**
     * @return a sensitivity factor provider
     */
    SensitivityFactorsProvider create(CriticalBranchesValuesProvider criticalBranchesValuesProviderIn,
                                      GlskValuesProvider glskValuesProviderIn);
}
