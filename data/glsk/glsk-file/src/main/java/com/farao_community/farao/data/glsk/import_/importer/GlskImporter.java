/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.import_.importer;

import com.farao_community.farao.data.glsk.import_.GlskDocument;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Interface for GLSK object import
 *
 * @author Viktor Terrier {literal <viktor.terrier at rte-france.com>}
 */

public interface GlskImporter {

    /**
     * @param filepathstring absolute file path in string
     * @return GLSKDocument object
     */
    static GlskDocument importGlsk(String filepathstring) {
        return null;
    }

    /**
     * @param filepath file path in java Path
     * @return GlskDocument object
     */
    static GlskDocument importGlsk(Path filepath) {
        return null;
    }

    /**
     * @param data InputStream of GLSKDocument
     * @return GlskDocument
     */
    static GlskDocument importGlsk(InputStream data) {
        return null;
    }

    //boolean exists(String fileName, InputStream inputStream);
}
