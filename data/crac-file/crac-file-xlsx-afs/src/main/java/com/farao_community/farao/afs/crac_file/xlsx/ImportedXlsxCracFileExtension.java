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
import com.powsybl.afs.ProjectFileBuildContext;
import com.powsybl.afs.ProjectFileBuilder;
import com.powsybl.afs.ProjectFileCreationContext;
import com.powsybl.afs.ProjectFileExtension;

@AutoService(ProjectFileExtension.class)
public class ImportedXlsxCracFileExtension implements ProjectFileExtension<ImportedXlsxCracFile, ImportedXlsxCracFileBuilder> {
    @Override
    public Class<ImportedXlsxCracFile> getProjectFileClass() {
        return ImportedXlsxCracFile.class;
    }

    @Override
    public String getProjectFilePseudoClass() {
        return ImportedXlsxCracFile.PSEUDO_CLASS;
    }

    @Override
    public Class<ImportedXlsxCracFileBuilder> getProjectFileBuilderClass() {
        return ImportedXlsxCracFileBuilder.class;
    }

    @Override
    public ImportedXlsxCracFile createProjectFile(ProjectFileCreationContext context) {
        return new ImportedXlsxCracFile(context);
    }

    @Override
    public ProjectFileBuilder<ImportedXlsxCracFile> createProjectFileBuilder(ProjectFileBuildContext context) {
        return new ImportedXlsxCracFileBuilder(context);
    }
}
