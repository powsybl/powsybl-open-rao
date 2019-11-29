/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
package com.farao_community.farao.afs.crac_file.xlsx;

import com.google.auto.service.AutoService;
import com.powsybl.afs.FileCreationContext;
import com.powsybl.afs.FileExtension;

@AutoService(FileExtension.class)
public class AfsXlsxCracFileExtension implements FileExtension<AfsXlsxCracFile> {
    @Override
    public Class getFileClass() {
        return AfsXlsxCracFile.class;
    }

    @Override
    public String getFilePseudoClass() {
        return AfsXlsxCracFile.PSEUDO_CLASS;
    }

    @Override
    public AfsXlsxCracFile createFile(FileCreationContext fileCreationContext) {
        return new AfsXlsxCracFile(fileCreationContext);
    }
}
