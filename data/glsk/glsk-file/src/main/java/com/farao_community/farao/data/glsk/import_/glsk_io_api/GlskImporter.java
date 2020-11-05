/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.import_.glsk_io_api;

import com.farao_community.farao.data.glsk.import_.glsk_provider.Glsk;
import org.joda.time.DateTime;

import java.io.InputStream;
import java.util.Optional;

/**
 * Interface for GLSK object import
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public interface GlskImporter {

    Glsk importGlsk(InputStream inputStream, Optional<DateTime> timeStampFilter);

    boolean exists(String fileName, InputStream inputStream);
}
