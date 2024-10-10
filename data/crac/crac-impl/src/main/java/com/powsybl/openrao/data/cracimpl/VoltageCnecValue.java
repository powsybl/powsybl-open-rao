package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.cnec.CnecValue;

public record VoltageCnecValue(Double minValue, Double maxValue) implements CnecValue {
}
