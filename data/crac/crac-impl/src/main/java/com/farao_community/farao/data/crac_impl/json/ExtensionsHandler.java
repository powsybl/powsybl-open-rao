/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionJsonSerializer;
import com.powsybl.commons.extensions.ExtensionProviders;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class ExtensionsHandler {

    private ExtensionsHandler() { }

    public interface CnecExtensionSerializer<E extends Extension<Cnec>> extends ExtensionJsonSerializer<Cnec, E> { }

    private static final Supplier<ExtensionProviders<CnecExtensionSerializer>> CNEC_SUPPLIER =
            Suppliers.memoize(() -> ExtensionProviders.createProvider(CnecExtensionSerializer.class, "cnec"));

    /**
     * Gets the known Cnec extension serializers.
     */
    public static ExtensionProviders<CnecExtensionSerializer> getCnecExtensionSerializers() {
        return CNEC_SUPPLIER.get();
    }

    public interface CracExtensionSerializer<E extends Extension<Crac>> extends ExtensionJsonSerializer<Crac, E> { }

    private static final Supplier<ExtensionProviders<CnecExtensionSerializer>> CRAC_SUPPLIER =
        Suppliers.memoize(() -> ExtensionProviders.createProvider(CnecExtensionSerializer.class, "crac"));

    /**
     * Gets the known Crac extension serializers.
     */
    public static ExtensionProviders<CnecExtensionSerializer> getCracExtensionSerializers() {
        return CRAC_SUPPLIER.get();
    }
}
