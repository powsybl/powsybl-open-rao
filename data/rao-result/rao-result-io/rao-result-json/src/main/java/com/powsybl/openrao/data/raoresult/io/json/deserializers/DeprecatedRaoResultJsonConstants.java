/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class DeprecatedRaoResultJsonConstants {

    private DeprecatedRaoResultJsonConstants() { }

    // keep here for retro-compatibility
    // private-package in deserializer package, to ensure that there are not used by the serializers

    //   - network elements of PST and HVDC range actions were written in versions <= 1.1
    static final String PST_NETWORKELEMENT_ID = "pstNetworkElementId";
    static final String HVDC_NETWORKELEMENT_ID = "hvdcNetworkElementId";

    //   - only type of StandardRangeAction was HvdcRangeAction in versions <= 1.1
    static final String HVDCRANGEACTION_RESULTS = "hvdcRangeActionResults";
    static final String HVDCRANGEACTION_ID = "hvdcRangeActionId";
}
