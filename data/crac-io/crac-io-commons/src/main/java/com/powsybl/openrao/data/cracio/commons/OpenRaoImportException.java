/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons;

import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OpenRaoImportException extends RuntimeException {
    private final ImportStatus importStatus;

    public OpenRaoImportException(ImportStatus importStatus, String msg) {
        super(msg);
        this.importStatus = importStatus;
    }

    public ImportStatus getImportStatus() {
        return importStatus;
    }
}
