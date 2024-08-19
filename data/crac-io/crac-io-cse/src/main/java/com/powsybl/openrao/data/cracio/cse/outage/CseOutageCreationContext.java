/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cse.outage;

import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.api.StandardElementaryCreationContext;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CseOutageCreationContext extends StandardElementaryCreationContext {
    public CseOutageCreationContext(String outageId, ImportStatus importStatus, String importStatusDetail) {
        super(outageId, outageId, ImportStatus.IMPORTED.equals(importStatus) ? outageId : null, importStatus, importStatusDetail, false);
    }

    static CseOutageCreationContext notImported(String outageID, ImportStatus importStatus, String importStatusDetail) {
        return new CseOutageCreationContext(outageID, importStatus, importStatusDetail);
    }

    static CseOutageCreationContext imported(String outageID) {
        return new CseOutageCreationContext(outageID, ImportStatus.IMPORTED, null);
    }
}
