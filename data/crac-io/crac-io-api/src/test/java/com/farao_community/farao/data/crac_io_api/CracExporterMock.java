package com.farao_community.farao.data.crac_io_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creator_api.CracCreationContext;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;

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

    @Override
    public void exportCrac(Crac crac, Network network, OutputStream outputStream) {

    }

    @Override
    public void exportCrac(Crac crac, Network network, CracCreationContext cracCreationContext, String initialVariantId, String postPraVariantId, String postCraVariantId, OutputStream outputStream) {

    }
}
