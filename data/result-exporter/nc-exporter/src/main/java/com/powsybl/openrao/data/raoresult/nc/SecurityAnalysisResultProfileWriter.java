package com.powsybl.openrao.data.raoresult.nc;

import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SecurityAnalysisResultProfileWriter implements NcProfileWriter {
    @Override
    public String getKeyword() {
        return "SAR";
    }

    @Override
    public void addProfileContent(Document document, Element rootRdfElement, RaoResult raoResult, CsaProfileCracCreationContext ncCracCreationContext) {

    }
}
