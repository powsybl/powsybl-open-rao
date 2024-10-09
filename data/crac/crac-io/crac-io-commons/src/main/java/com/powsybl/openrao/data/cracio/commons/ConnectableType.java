/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons;

import com.powsybl.iidm.network.*;

public enum ConnectableType {

    INTERNAL_LINE,
    TIE_LINE,
    DANGLING_LINE,
    VOLTAGE_TRANSFORMER,
    PST,
    HVDC,
    SWITCH,
    UNKNOWN;

    public static ConnectableType getType(Identifiable<?> iidmConnectable) {

        if (iidmConnectable instanceof TieLine) {
            return ConnectableType.TIE_LINE;
        } else if (iidmConnectable instanceof DanglingLine) {
            return ConnectableType.DANGLING_LINE;
        } else if (iidmConnectable instanceof TwoWindingsTransformer twt) {
            if (twt.getPhaseTapChanger() != null) {
                return ConnectableType.PST;
            }
            return ConnectableType.VOLTAGE_TRANSFORMER;
        } else if (iidmConnectable instanceof Branch) {
            return ConnectableType.INTERNAL_LINE;
        } else if (iidmConnectable instanceof HvdcLine) {
            return ConnectableType.HVDC;
        } else if (iidmConnectable instanceof Switch) {
            return ConnectableType.SWITCH;
        } else {
            return ConnectableType.UNKNOWN;
        }
    }
}
