/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.api.io;

import com.farao_community.farao.data.glsk.api.GlskDocument;

import java.io.InputStream;

/**
 * Interface for GLSK object import
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public interface GlskDocumentImporter {

    GlskDocument importGlsk(InputStream inputStream);

    boolean canImport(InputStream inputStream);
}
