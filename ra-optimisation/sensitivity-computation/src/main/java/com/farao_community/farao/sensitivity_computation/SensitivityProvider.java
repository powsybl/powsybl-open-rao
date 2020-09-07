/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_computation;

import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.sensitivity.SensitivityFactorsProvider;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public interface SensitivityProvider extends SensitivityFactorsProvider, ContingenciesProvider {

}
