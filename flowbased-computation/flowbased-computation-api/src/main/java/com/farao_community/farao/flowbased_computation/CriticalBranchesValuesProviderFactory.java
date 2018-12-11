/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

/**
 * Interface for the factory which create critical branches values provider
 *
 * @author Di Gallo Luc  {@literal <luc.di-gallo at rte-france.com>}
 * @see CriticalBranchesValuesProviderFactory
 */
public interface CriticalBranchesValuesProviderFactory {
    /**
     *
     * @param ecbFileNameIn name of the critical branch file
     * @return critical branches description provider
     */
    CriticalBranchesValuesProvider create(String ecbFileNameIn);
}
