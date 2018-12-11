/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
package com.farao_community.farao.afs.crac_file.xlsx;

import com.powsybl.afs.File;
import com.powsybl.afs.FileCreationContext;
import com.powsybl.afs.storage.AppStorageDataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;

public class AfsXlsxCracFile extends File {

    public static final String PSEUDO_CLASS = "xlsxCracFile";
    public static final int VERSION = 0;

    public AfsXlsxCracFile(FileCreationContext context) {
        super(context, VERSION);
    }

    public ReadOnlyDataSource getDataSource() {
        return new AppStorageDataSource(storage, info.getId(), info.getName());
    }
}
