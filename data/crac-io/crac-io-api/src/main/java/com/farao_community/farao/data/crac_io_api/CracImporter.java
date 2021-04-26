/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.time.OffsetDateTime;

/**
 * Interface for CRAC object import
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

public interface CracImporter {

    Crac importCrac(InputStream inputStream, @Nonnull CracFactory cracFactory, @Nullable OffsetDateTime timeStampFilter);

    Crac importCrac(InputStream inputStream, @Nullable OffsetDateTime timeStampFilter);

    boolean exists(String fileName, InputStream inputStream);
}
