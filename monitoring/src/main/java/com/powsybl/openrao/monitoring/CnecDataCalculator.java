/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface CnecDataCalculator<I extends Cnec<?>> {
    /**
     * @param cnec: the cnec we seek to compute the value of
     * @param network: the network object used to look for actual result of the Cnec
     * @param unit: the unit object used to look for the kind of the {@link Cnec} and the kind of the {@link CnecValue}
     * @return a CnecValue  as result of the {@link Cnec} depending on the cnec kind
     */
    CnecValue<I> computeValue(I cnec, Network network, Unit unit);

    /**
     * @param cnec: the cnec we seek to compute the margin of
     * @param network: the network object used to look for actual result of the Cnec
     * @param unit: the unit object used to look for the kind of the {@link Cnec}
     * @return a double as the worst margin of a @{@link CnecValue} relatively to the @{@link Cnec} thresholds
     */
    double computeMargin(I cnec, Network network, Unit unit);

    /**
     * @param cnec: the cnec we seek to compute the security status of
     * @param network: the network object used to look for actual result of the Cnec
     * @param unit: the unit object used to look for the kind of the {@link Cnec}
     * Returns a {@link SecurityStatus} describing the {@link Cnec} result compared to the thresholds
     */
    SecurityStatus computeSecurityStatus(I cnec, Network network, Unit unit);
}
