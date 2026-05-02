package com.powsybl.openrao.searchtreerao.commons.network;

import com.powsybl.iidm.network.Network;

import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Function;

public class NetworkVariantManager {

    private final Function<Network, NetworkVariant> networkVariantSupplier;

    private final WeakHashMap<Network, NetworkVariant> variants = new WeakHashMap<>();

    public NetworkVariantManager(Function<Network, NetworkVariant> networkVariantSupplier) {
        this.networkVariantSupplier = Objects.requireNonNull(networkVariantSupplier);
    }

    public NetworkVariant getVariant(Network network) {
        return variants.computeIfAbsent(network, networkVariantSupplier);
    }
}
