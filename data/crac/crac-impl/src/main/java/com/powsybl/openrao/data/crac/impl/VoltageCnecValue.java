package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.cnec.CnecValue;

public record VoltageCnecValue(Double minValue, Double maxValue) implements CnecValue {
}
