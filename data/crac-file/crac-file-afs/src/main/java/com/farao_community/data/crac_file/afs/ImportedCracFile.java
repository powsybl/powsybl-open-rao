/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.data.crac_file.afs;

import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectFileCreationContext;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.CracFileProvider;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Implementation of a JSON CRAC project file in AFS
 * <p>
 * The CRAC file object is stored as a JSON blob in AFS
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class ImportedCracFile extends ProjectFile implements CracFileProvider {

    public static final String PSEUDO_CLASS = "importedCracFile";

    static final int VERSION = 0;

    static final String CRAC_FILE_JSON_NAME = "cracData";

    public ImportedCracFile(ProjectFileCreationContext context) {
        super(context, VERSION);
    }

    public CracFile read() {
        try (InputStream is = storage.readBinaryData(info.getId(), CRAC_FILE_JSON_NAME).orElse(null)) {
            return JsonCracFile.read(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public CracFile getCracFile() {
        return read();
    }
}
