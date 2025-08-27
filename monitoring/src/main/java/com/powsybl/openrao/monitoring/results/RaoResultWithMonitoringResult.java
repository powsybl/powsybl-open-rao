/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.SecurityStatus;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface RaoResultWithMonitoringResult<I extends Cnec<?>, J extends CnecResult<I>> extends RaoResult {
    SecurityStatus getSecurityStatus();
}
