/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.data.crac_file.afs;

import com.google.auto.service.AutoService;
import com.powsybl.afs.FileCreationContext;
import com.powsybl.afs.FileExtension;

/**
 * AFS CRAC file integration extension.
 *
 * Used to register AFS CRAC file
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(FileExtension.class)
public class AfsCracFileExtension implements FileExtension<AfsCracFile> {
    @Override
    public Class getFileClass() {
        return AfsCracFile.class;
    }

    @Override
    public String getFilePseudoClass() {
        return AfsCracFile.PSEUDO_CLASS;
    }

    @Override
    public AfsCracFile createFile(FileCreationContext fileCreationContext) {
        return new AfsCracFile(fileCreationContext);
    }
}
