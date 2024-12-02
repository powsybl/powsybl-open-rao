package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.cnec.CnecValue;

public record FlowCnecValue(Double side1Value, Double side2Value) implements CnecValue {
}
