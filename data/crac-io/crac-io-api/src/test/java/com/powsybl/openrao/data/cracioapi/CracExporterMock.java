package com.powsybl.openrao.data.cracioapi;

import com.powsybl.openrao.data.cracapi.Crac;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import java.io.OutputStream;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@AutoService(CracExporter.class)
public class CracExporterMock implements CracExporter {

    public String getFormat() {
        return "Mock";
    }

    @Override
    public void exportCrac(Crac crac, OutputStream outputStream) {
        throw new NotImplementedException("not implemented");
    }

    @Override
    public void exportCrac(Crac crac, Network network, OutputStream outputStream) {
        throw new NotImplementedException("not implemented");
    }
}
