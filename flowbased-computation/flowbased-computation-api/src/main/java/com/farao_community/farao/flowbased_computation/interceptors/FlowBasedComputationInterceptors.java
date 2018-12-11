/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.interceptors;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.util.ServiceLoaderCache;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
public final class FlowBasedComputationInterceptors {

    private static final Supplier<Map<String, FlowBasedComputationInterceptorExtension>> EXTENSIONS
            = Suppliers.memoize(FlowBasedComputationInterceptors::loadExtensions);

    private static Map<String, FlowBasedComputationInterceptorExtension> loadExtensions() {
        return new ServiceLoaderCache<>(FlowBasedComputationInterceptorExtension.class).getServices().stream()
                .collect(Collectors.toMap(FlowBasedComputationInterceptorExtension::getName, e -> e));
    }

    public static Set<String> getExtensionNames() {
        return EXTENSIONS.get().keySet();
    }

    public static FlowBasedComputationInterceptor createInterceptor(String name) {
        Objects.requireNonNull(name);

        FlowBasedComputationInterceptorExtension extension = EXTENSIONS.get().get(name);
        if (extension == null) {
            throw new IllegalArgumentException("The extension '" + name + "' doesn't exist");
        }

        return extension.createInterceptor();
    }

    private FlowBasedComputationInterceptors() {
    }
}
