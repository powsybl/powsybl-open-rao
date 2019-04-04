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

import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectFileCreationContext;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.CracFileProvider;
import com.farao_community.farao.data.crac_file.xlsx.model.TimesSeries;
import com.farao_community.farao.data.crac_file.xlsx.service.ImportService;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class ImportedXlsxCracFile extends ProjectFile implements CracFileProvider {
    public static final String PSEUDO_CLASS = "importedXlsxCracFile";

    static final int VERSION = 0;

    static final String CRAC_FILE_XLSX_NAME = "cracXlsxData";

    public ImportedXlsxCracFile(ProjectFileCreationContext context) {
        super(context, VERSION);
    }

    public CracFile read() {
        try (InputStream is = storage.readBinaryData(info.getId(), CRAC_FILE_XLSX_NAME).orElse(null)) {
            String fileName = info.getGenericMetadata().getString("OriginalFileName");
            String hour = info.getGenericMetadata().getString("OriginalHour");
            ImportService importService = new ImportService();
            return importService.importContacts(is, TimesSeries.valueOf(hour), fileName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public CracFile getCracFile() {
        return read();
    }
}
