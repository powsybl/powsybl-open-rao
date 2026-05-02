package com.powsybl.openrao.searchtreerao.commons.network;

import com.powsybl.iidm.network.Network;

import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Function;

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
