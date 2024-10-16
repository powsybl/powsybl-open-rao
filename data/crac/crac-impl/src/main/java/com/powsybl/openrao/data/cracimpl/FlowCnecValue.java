package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.cnec.CnecValue;

public record FlowCnecValue(Double side1Value, Double side2Value) implements CnecValue {
}
