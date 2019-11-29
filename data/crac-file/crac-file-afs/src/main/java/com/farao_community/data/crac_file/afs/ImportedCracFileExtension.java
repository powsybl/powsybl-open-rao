/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.data.crac_file.afs;

import com.google.auto.service.AutoService;
import com.powsybl.afs.ProjectFileBuildContext;
import com.powsybl.afs.ProjectFileBuilder;
import com.powsybl.afs.ProjectFileCreationContext;
import com.powsybl.afs.ProjectFileExtension;

/**
 * AFS CRAC project file integration extension.
 *
 * Used to register AFS CRAC project file
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(ProjectFileExtension.class)
public class ImportedCracFileExtension implements ProjectFileExtension<ImportedCracFile, ImportedCracFileBuilder> {
    @Override
    public Class<ImportedCracFile> getProjectFileClass() {
        return ImportedCracFile.class;
    }

    @Override
    public String getProjectFilePseudoClass() {
        return ImportedCracFile.PSEUDO_CLASS;
    }

    @Override
    public Class<ImportedCracFileBuilder> getProjectFileBuilderClass() {
        return ImportedCracFileBuilder.class;
    }

    @Override
    public ImportedCracFile createProjectFile(ProjectFileCreationContext context) {
        return new ImportedCracFile(context);
    }

    @Override
    public ProjectFileBuilder<ImportedCracFile> createProjectFileBuilder(ProjectFileBuildContext context) {
        return new ImportedCracFileBuilder(context);
    }
}
