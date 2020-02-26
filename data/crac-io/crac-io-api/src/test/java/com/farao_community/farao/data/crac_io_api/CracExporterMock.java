package com.farao_community.farao.data.crac_io_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.google.auto.service.AutoService;

import java.io.OutputStream;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@AutoService(CracExporter.class)
public class CracExporterMock implements CracExporter {

    public String getFormat() {
        return "";
    }

    public void exportCrac(Crac crac, OutputStream outputStream) {

    }
}
