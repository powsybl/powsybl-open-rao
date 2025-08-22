/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public interface CnecSensitivityProvider extends ContingenciesProvider {

    Set<FlowCnec> getFlowCnecs();

    void setRequestedUnits(Set<Unit> requestedUnits);

    void disableFactorsForBaseCaseSituation();

    void enableFactorsForBaseCaseSituation();

    List<SensitivityFactor> getBasecaseFactors(Network network);

    List<SensitivityFactor> getContingencyFactors(Network network, List<Contingency> contingencies);

    List<SensitivityFactor> getAllFactors(Network network);

    List<SensitivityVariableSet> getVariableSets();

    Map<String, HvdcRangeAction> getHvdcs();
}
