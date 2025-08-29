/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.angle;

import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.monitoring.SecurityStatus;
import com.powsybl.openrao.monitoring.results.AbstractMonitoringResult;
import com.powsybl.openrao.monitoring.results.CnecResult;

import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AngleMonitoringResult extends AbstractMonitoringResult<AngleCnec> {
    public AngleMonitoringResult(Set<CnecResult<AngleCnec>> angleCnecResults, Map<State, Set<NetworkAction>> appliedRas, SecurityStatus status) {
        super(PhysicalParameter.ANGLE, angleCnecResults, appliedRas, status);
    }
}
