/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.cim;

import com.powsybl.openrao.data.nativecracapi.NativeCrac;
import com.powsybl.openrao.data.craccreation.creator.cim.xsd.CRACMarketDocument;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class CimCrac implements NativeCrac {
    private final CRACMarketDocument cracDocument;

    public CimCrac(CRACMarketDocument cracDocument) {
        this.cracDocument = cracDocument;
    }

    @Override
    public String getFormat() {
        return "CimCrac";
    }

    public CRACMarketDocument getCracDocument() {
        return cracDocument;
    }
}
