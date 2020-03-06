/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.RangeAction;
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

    public interface RangeActionExtensionSerializer<I extends RangeAction<I>, E extends Extension<I>> extends ExtensionJsonSerializer<I, E> { }

    private static final Supplier<ExtensionProviders<RangeActionExtensionSerializer>> RANGE_ACTION_SUPPLIER =
        Suppliers.memoize(() -> ExtensionProviders.createProvider(RangeActionExtensionSerializer.class, "range-action"));

    /**
     * Gets the known range action extension serializers.
     */
    public static ExtensionProviders<RangeActionExtensionSerializer> getRangeActionExtensionSerializers() {
        return RANGE_ACTION_SUPPLIER.get();
    }

    public interface NetworkActionExtensionSerializer<E extends Extension<NetworkAction>> extends ExtensionJsonSerializer<NetworkAction, E> { }

    private static final Supplier<ExtensionProviders<NetworkActionExtensionSerializer>> NETWORK_ACTION_SUPPLIER =
        Suppliers.memoize(() -> ExtensionProviders.createProvider(NetworkActionExtensionSerializer.class, "network-action"));

    /**
     * Gets the known network action extension serializers.
     */
    public static ExtensionProviders<NetworkActionExtensionSerializer> getNetworkActionExtensionSerializers() {
        return NETWORK_ACTION_SUPPLIER.get();
    }
}
