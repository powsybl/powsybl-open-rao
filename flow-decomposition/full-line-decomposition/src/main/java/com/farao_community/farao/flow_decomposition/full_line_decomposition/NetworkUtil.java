/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.stream.Stream;

/**
 * Utility class for network manipulation
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class NetworkUtil {
    private NetworkUtil() {
    }

    public static boolean isConnectedAndInMainSynchronous(Branch branch) {
        return branch.getTerminal1().getBusView().getBus() != null
            && branch.getTerminal2().getBusView().getBus() != null
            && branch.getTerminal1().getBusView().getBus().isInMainSynchronousComponent()
            && branch.getTerminal2().getBusView().getBus().isInMainSynchronousComponent();
    }

    public static boolean isConnectedAndInMainSynchronous(Injection injection) {
        return injection.getTerminal().getBusView().getBus() != null
                && injection.getTerminal().getBusView().getBus().isInMainSynchronousComponent();
    }

    public static Stream<Injection> getInjectionStream(Bus bus) {
        Stream returnStream = Stream.empty();
        returnStream = Stream.concat(bus.getGeneratorStream(), returnStream);
        returnStream = Stream.concat(bus.getLoadStream(), returnStream);
        returnStream = Stream.concat(bus.getDanglingLineStream(), returnStream);
        return returnStream;
    }

    public static Stream<Injection> getInjectionStream(Network network) {
        Stream returnStream = Stream.empty();
        returnStream = Stream.concat(network.getGeneratorStream(), returnStream);
        returnStream = Stream.concat(network.getLoadStream(), returnStream);
        returnStream = Stream.concat(network.getDanglingLineStream(), returnStream);
        return returnStream;
    }

    public static boolean branchIsPst(Branch branch) {
        if (!(branch instanceof TwoWindingsTransformer)) {
            return false;
        }
        return ((TwoWindingsTransformer) branch).getPhaseTapChanger() != null;
    }

    public static Country getBranchSideCountry(Branch branch, Branch.Side side) {
        return branch.getTerminal(side).getVoltageLevel().getSubstation().getCountry().orElse(null);
    }

    public static Injection getInjectionFrom(Network network, String id) {
        Identifiable identifiable = network.getIdentifiable(id);
        if (identifiable instanceof Injection) {
            return (Injection) identifiable;
        }
        return null;
    }
}
