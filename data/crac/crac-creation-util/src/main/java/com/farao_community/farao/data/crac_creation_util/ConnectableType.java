package com.farao_community.farao.data.crac_creation_util;

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
        } else if (iidmConnectable instanceof TwoWindingsTransformer) {
            if (((TwoWindingsTransformer) iidmConnectable).getPhaseTapChanger() != null) {
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
