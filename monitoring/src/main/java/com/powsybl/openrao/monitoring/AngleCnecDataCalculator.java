/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface AngleCnecDataCalculator extends CnecDataCalculator<AngleCnec> {
    /**
     * @param angleCnec: the angle CNEC we seek to compute the angle of
     * @param network: the network object used to look for actual result of the Cnec
     * @param unit: the unit object used to look for the kind of the {@link Cnec} and the kind of the {@link CnecValue}
     * @return the angle of the CNEC in the network
     */
    Double computeAngle(AngleCnec angleCnec, Network network, Unit unit);
}
