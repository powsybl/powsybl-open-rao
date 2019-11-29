/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.afs.crac_file.xlsx;

import com.farao_community.farao.data.crac_file.xlsx.service.CracFileScanner;
import com.google.auto.service.AutoService;
import com.powsybl.afs.local.storage.LocalFile;
import com.powsybl.afs.local.storage.LocalFileScanner;
import com.powsybl.afs.local.storage.LocalFileScannerContext;
import org.apache.commons.io.FilenameUtils;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
@AutoService(LocalFileScanner.class)
public class AfsXlsxCracLocalFileScanner implements LocalFileScanner {
    @Override
    public LocalFile scanFile(Path path, LocalFileScannerContext localFileScannerContext) {
        if (Files.isRegularFile(path) && FilenameUtils.getExtension(path.toString()).equals("xlsx")) {
            try {
                new CracFileScanner().checkCracFileSheet(new FileInputStream(path.toString()));
            } catch (Exception e) {
                //Impossible to parse, return null
                return null;
            }
            return new AfsXlsxCracLocalFile(path);
        }
        return null;
    }
}
