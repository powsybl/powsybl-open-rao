package com.powsybl.openrao.data.raoresultapi;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;

import java.io.OutputStream;
import java.util.Set;

public interface Exporter {
    String getFormat();

    void exportData(RaoResult raoResult, Crac crac, Set<Unit> flowUnits, OutputStream outputStream);
}
