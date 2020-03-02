
package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultAdderImpl;
import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionAdderProvider;
import com.powsybl.iidm.network.DanglingLine;

@AutoService(ExtensionAdderProvider.class)
public class CnecResultAdderImplProvider implements ExtensionAdderProvider<Cnec, CnecResult, CnecResultAdderImpl> {

    @Override
    public String getImplementationName() {
        return "Default";
    }

    @Override
    public Class<CnecResultAdderImpl> getAdderClass() {
        return CnecResultAdderImpl.class;
    }

    @Override
    public CnecResultAdderImpl newAdder(Cnec extendable) {
        return new CnecResultAdderImpl(extendable);
    }

}
