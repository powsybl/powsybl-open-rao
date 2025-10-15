/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs.networkextension;

import com.powsybl.commons.extensions.Extension;
import com.powsybl.iidm.network.Injection;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface AssignedVirtualHub<T extends Injection<T>> extends Extension<T> {

    @Override
    default String getName() {
        return "assignedVirtualHub";
    }

    String getCode();

    String getEic();

    boolean isMcParticipant();

    String getNodeName();

    String getRelatedMa();
}
