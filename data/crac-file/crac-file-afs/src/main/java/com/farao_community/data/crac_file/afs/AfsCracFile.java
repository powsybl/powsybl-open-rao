/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.data.crac_file.afs;

import com.powsybl.afs.File;
import com.powsybl.afs.FileCreationContext;
import com.powsybl.afs.storage.AppStorageDataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;

/**
 * Implementation of a JSON CRAC file in AFS
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class AfsCracFile extends File {

    public static final String PSEUDO_CLASS = "cracFile";
    public static final int VERSION = 0;

    public AfsCracFile(FileCreationContext context) {
        super(context, VERSION);
    }

    public ReadOnlyDataSource getDataSource() {
        return new AppStorageDataSource(storage, info.getId(), info.getName());
    }
}
