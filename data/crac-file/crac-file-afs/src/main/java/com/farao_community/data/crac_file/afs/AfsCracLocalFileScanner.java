/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.data.crac_file.afs;

import com.google.auto.service.AutoService;
import com.powsybl.afs.local.storage.LocalFile;
import com.powsybl.afs.local.storage.LocalFileScanner;
import com.powsybl.afs.local.storage.LocalFileScannerContext;
import org.apache.commons.io.FilenameUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Local file scanner service dedicated to find JSON CRAC files in local filesystem
 * Currently filters all JSON files that validate the schema
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(LocalFileScanner.class)
public class AfsCracLocalFileScanner implements LocalFileScanner {

    private static final String CRAC_FILE_SCHEMA_JSON = "/CracFileSchema.json";

    private static final String JSON_EXTENSION = "json";

    private static final Schema SCHEMA_JSON;

    static {
        JSONObject jsonSchema = new JSONObject(
                new JSONTokener(AfsCracLocalFileScanner.class.getResourceAsStream(CRAC_FILE_SCHEMA_JSON)));
        SCHEMA_JSON = SchemaLoader.load(jsonSchema);
    }

    @Override
    public LocalFile scanFile(Path path, LocalFileScannerContext localFileScannerContext) {
        if (Files.isRegularFile(path) && FilenameUtils.getExtension(path.toString()).equals(JSON_EXTENSION) && validCracFile(path)) {
            return new AfsCracLocalFile(path);
        }
        return null;
    }

    private boolean validCracFile(Path path) {
        try {
            JSONObject jsonSubject = new JSONObject(
                    new JSONTokener(Files.newInputStream(path)));
            SCHEMA_JSON.validate(jsonSubject);
            return true;
        } catch (Exception ve) {
            return false;

        }
    }
}
