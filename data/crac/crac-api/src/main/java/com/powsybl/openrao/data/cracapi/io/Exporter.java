package com.powsybl.openrao.data.cracapi.io;

import com.powsybl.openrao.data.cracapi.Crac;

import java.io.OutputStream;

public interface Exporter {

    String getFormat();

    void exportData(Crac crac, OutputStream outputStream);
}
