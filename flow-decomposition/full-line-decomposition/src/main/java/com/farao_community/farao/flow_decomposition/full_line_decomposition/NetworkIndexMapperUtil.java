/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class dedicated to network object  to index mapping
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class NetworkIndexMapperUtil {
    private NetworkIndexMapperUtil() {
        // No constructor
    }

    private static int getNextIndex(Map<?, Integer> keyToIndexMap) {
        // Must be sure it has been constructed well
        assert keyToIndexMap != null;
        assert keyToIndexMap.values().stream().allMatch(index -> index < keyToIndexMap.size()); // All below size
        assert keyToIndexMap.values().stream().noneMatch(index -> index < 0); // all positive
        assert keyToIndexMap.values().stream().distinct().count() == keyToIndexMap.values().stream().count(); // All unique

        return keyToIndexMap.size();
    }

    private static void mapBusToIndex(Bus bus, Map<Bus, Integer> busToIndexMap) {
        // Method just used at the beginning, bus must not have been already indexed
        assert bus != null;
        assert busToIndexMap != null;
        assert !busToIndexMap.containsKey(bus);

        int busIndex = getNextIndex(busToIndexMap);
        busToIndexMap.put(bus, busIndex);
    }

    public static Map<Bus, Integer> generateBusMapping(Network network) {
        Map<Bus, Integer> busToIndexMap = new HashMap<>();
        network.getBusView().getBusStream()
                .filter(Bus::isInMainSynchronousComponent)
                .forEach(bus -> mapBusToIndex(bus, busToIndexMap));
        return busToIndexMap;
    }

    private static void mapBranchToIndex(Branch branch, Map<Branch, Integer> branchToIndexMap) {
        // Method just used at the beginning, bus must not have been already indexed
        assert branch != null;
        assert branchToIndexMap != null;
        assert !branchToIndexMap.containsKey(branch);

        int branchIndex = getNextIndex(branchToIndexMap);
        branchToIndexMap.put(branch, branchIndex);
    }

    public static Map<Branch, Integer> generateBranchMapping(Network network) {
        Map<Branch, Integer> branchToIndexMap = new HashMap<>();
        network.getBranchStream()
                .filter(NetworkUtil::isConnectedAndInMainSynchronous)
                .forEach(branch -> mapBranchToIndex(branch, branchToIndexMap));
        return branchToIndexMap;
    }
}
