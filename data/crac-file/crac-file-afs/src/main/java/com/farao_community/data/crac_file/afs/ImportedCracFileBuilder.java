/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.data.crac_file.afs;

import com.google.common.io.ByteStreams;
import com.powsybl.afs.ProjectFileBuildContext;
import com.powsybl.afs.ProjectFileBuilder;
import com.powsybl.afs.ProjectFileCreationContext;
import com.powsybl.afs.storage.NodeGenericMetadata;
import com.powsybl.afs.storage.NodeInfo;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.farao_community.farao.commons.FaraoException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * JSON CRAC project file builder
 * <p>
 * The CRAC file object is stored as a JSON blob in AFS
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class ImportedCracFileBuilder implements ProjectFileBuilder<ImportedCracFile> {

    private final ProjectFileBuildContext context;

    private String name;

    private ReadOnlyDataSource dataSource;

    private String baseName;

    public ImportedCracFileBuilder(ProjectFileBuildContext context) {
        this.context = context;
    }

    public ImportedCracFileBuilder withName(String name) {
        this.name = Objects.requireNonNull(name);
        return this;
    }

    public ImportedCracFileBuilder withBaseName(String baseName) {
        this.baseName = Objects.requireNonNull(baseName);
        return this;
    }

    public ImportedCracFileBuilder withAfsCracFile(AfsCracFile file) {
        Objects.requireNonNull(file);
        if (name == null) {
            name = file.getName();
        }
        dataSource = file.getDataSource();
        return this;
    }

    public ImportedCracFileBuilder withDataSource(ReadOnlyDataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
        return this;
    }

    @Override
    public ImportedCracFile build() {
        if (dataSource == null) {
            throw new FaraoException("CRAC file is not set");
        }
        if (baseName == null) {
            baseName = dataSource.getBaseName();
        }
        if (name == null) {
            throw new FaraoException("Name is not set");
        }
        if (context.getStorage().getChildNode(context.getFolderInfo().getId(), name).isPresent()) {
            throw new FaraoException("Parent folder already contains a '" + name + "' node");
        }
        try {
            if (!dataSource.exists(baseName)) {
                throw new FaraoException("Source with basename '" + baseName + "' does not exist in datasource");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // create project file
        NodeInfo info = context.getStorage().createNode(context.getFolderInfo().getId(), name, ImportedCracFile.PSEUDO_CLASS,
                "", ImportedCracFile.VERSION, new NodeGenericMetadata());

        // store parameters
        try (InputStream is = dataSource.newInputStream(baseName);
             OutputStream os = context.getStorage().writeBinaryData(info.getId(), ImportedCracFile.CRAC_FILE_JSON_NAME)) {
            ByteStreams.copy(is, os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        context.getStorage().setConsistent(info.getId());
        context.getStorage().flush();

        return new ImportedCracFile(new ProjectFileCreationContext(info, context.getStorage(), context.getProject()));
    }
}
