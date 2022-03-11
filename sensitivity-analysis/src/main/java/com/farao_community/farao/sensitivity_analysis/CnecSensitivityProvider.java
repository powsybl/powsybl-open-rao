/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.List;
import java.util.Set;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public interface CnecSensitivityProvider extends ContingenciesProvider {

    Set<FlowCnec> getFlowCnecs();

    void setRequestedUnits(Set<Unit> requestedUnits);

    void disableFactorsForBaseCaseSituation();

    List<SensitivityFactor> getBasecaseFactors(Network network);

    List<SensitivityFactor> getContingencyFactors(Network network, List<Contingency> contingencies);

    List<SensitivityFactor> getAllFactors(Network network);

    List<SensitivityVariableSet> getVariableSets();
}
