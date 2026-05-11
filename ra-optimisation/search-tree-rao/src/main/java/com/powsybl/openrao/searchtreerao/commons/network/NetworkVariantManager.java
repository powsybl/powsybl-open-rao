/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public interface NetworkVariantManager {

    Network getNetwork();

    void setWorkingVariant(String fromVariantId, String newVariantId);

    void removeWorkingVariants();

    void applyRangeAction(RangeAction<?> rangeAction, double setpoint);

    void applyNetworkAction(NetworkAction networkAction);

    void compute(SensitivityComputer sensitivityComputer);
}
