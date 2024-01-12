/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.cse;

import com.powsybl.openrao.data.craccreation.creator.cse.xsd.CRACDocumentType;
import com.powsybl.openrao.data.nativecracapi.NativeCrac;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CseCrac implements NativeCrac {
    private final CRACDocumentType cracDocumentType;

    public CseCrac(CRACDocumentType cracDocumentType) {
        this.cracDocumentType = cracDocumentType;
    }

    @Override
    public String getFormat() {
        return "CseCrac";
    }

    public CRACDocumentType getCracDocument() {
        return cracDocumentType;
    }
}
