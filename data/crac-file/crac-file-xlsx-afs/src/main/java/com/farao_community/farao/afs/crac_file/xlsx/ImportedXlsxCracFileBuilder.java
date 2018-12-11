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

public class ImportedXlsxCracFileBuilder implements ProjectFileBuilder<ImportedXlsxCracFile> {

    private final ProjectFileBuildContext context;

    private String name;

    private ReadOnlyDataSource dataSource;

    private String hour;

    public ImportedXlsxCracFileBuilder(ProjectFileBuildContext context) {
        this.context = context;
    }

    public ImportedXlsxCracFileBuilder withName(String name) {
        this.name = Objects.requireNonNull(name);
        return this;
    }

    public ImportedXlsxCracFileBuilder withAfsCracFile(AfsXlsxCracFile file) {
        Objects.requireNonNull(file);
        if (name == null) {
            name = file.getName();
        }
        dataSource = file.getDataSource();
        return this;
    }

    public ImportedXlsxCracFileBuilder withDataSource(ReadOnlyDataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
        return this;
    }

    public ImportedXlsxCracFileBuilder withHour(String hour) {
        this.hour = Objects.requireNonNull(hour);
        return this;
    }

    @Override
    public ImportedXlsxCracFile build() {
        if (dataSource == null) {
            throw new FaraoException("Xlsx CRAC file is not set");
        }
        if (name == null) {
            throw new FaraoException("Name is not set");
        }
        if (hour == null) {
            throw new FaraoException("Hour is not set");
        }

        if (context.getStorage().getChildNode(context.getFolderInfo().getId(), name).isPresent()) {
            throw new FaraoException("Parent folder already contains a '" + name + "' node");
        }

        NodeGenericMetadata metadata = new NodeGenericMetadata();
        metadata.setString("OriginalFileName", name);
        metadata.setString("OriginalHour", hour);

        String nodeName = name + "_" + hour.replaceFirst("TIME_", "");
        //TODO check if node name already used in node folder

        // create project file
        NodeInfo info = context.getStorage().createNode(context.getFolderInfo().getId(), nodeName, ImportedXlsxCracFile.PSEUDO_CLASS,
                "", ImportedXlsxCracFile.VERSION, metadata);

        // store parameters
        try (InputStream is = dataSource.newInputStream(ImportedXlsxCracFile.CRAC_FILE_XLSX_NAME);
             OutputStream os = context.getStorage().writeBinaryData(info.getId(), ImportedXlsxCracFile.CRAC_FILE_XLSX_NAME)) {
            ByteStreams.copy(is, os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        context.getStorage().flush();
        return new ImportedXlsxCracFile(new ProjectFileCreationContext(info, context.getStorage(), context.getProject()));
    }
}
