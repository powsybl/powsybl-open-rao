/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.network;

import com.powsybl.iidm.network.Network;

import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class NetworkVariantManagerProvider {

    private final Function<Network, NetworkVariantManager> networkVariantSupplier;

    private final WeakHashMap<Network, NetworkVariantManager> variants = new WeakHashMap<>();

    public NetworkVariantManagerProvider(Function<Network, NetworkVariantManager> networkVariantSupplier) {
        this.networkVariantSupplier = Objects.requireNonNull(networkVariantSupplier);
    }

    public NetworkVariantManager getVariant(Network network) {
        return variants.computeIfAbsent(network, networkVariantSupplier);
    }
}
