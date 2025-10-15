/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionJsonSerializer;
import com.powsybl.commons.extensions.ExtensionProviders;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class RaoResultJsonUtils {

    private RaoResultJsonUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public interface ExtensionSerializer<E extends Extension<RaoResult>> extends ExtensionJsonSerializer<RaoResult, E> {
    }

    private static final Supplier<ExtensionProviders<ExtensionSerializer>> SUPPLIER =
        Suppliers.memoize(() -> ExtensionProviders.createProvider(ExtensionSerializer.class, "rao-result"));

    public static ExtensionProviders<ExtensionSerializer> getExtensionSerializers() {
        return SUPPLIER.get();
    }
}
