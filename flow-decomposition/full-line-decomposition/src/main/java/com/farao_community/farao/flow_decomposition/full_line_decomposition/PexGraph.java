/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.flow_decomposition.full_line_decomposition.FullLineDecompositionParameters.InjectionStrategy;
import org.jgrapht.graph.DirectedMultigraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Vertex business object in PEX graph
 * Stands for network buses
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class PexGraphVertex {
    private Bus associatedBus;
    private double associatedGeneration;
    private double associatedLoad;

    PexGraphVertex(Bus associatedBus, InjectionStrategy injectionStrategy) {
        this.associatedBus = Objects.requireNonNull(associatedBus);

        double totalGeneration = -NetworkUtil.getInjectionStream(associatedBus)
                .mapToDouble(injection -> injection.getTerminal().getP())
                .filter(d -> !Double.isNaN(d))
                .filter(d -> d < 0)
                .sum();

        double totalLoad = NetworkUtil.getInjectionStream(associatedBus)
                .mapToDouble(injection -> injection.getTerminal().getP())
                .filter(d -> !Double.isNaN(d))
                .filter(d -> d > 0)
                .sum();

        if (injectionStrategy == InjectionStrategy.SUM_INJECTIONS) {
            this.associatedGeneration = totalGeneration > totalLoad ? totalGeneration - totalLoad : 0;
            this.associatedLoad = totalLoad > totalGeneration ? totalLoad - totalGeneration : 0;
        } else if (injectionStrategy == InjectionStrategy.DECOMPOSE_INJECTIONS) {
            this.associatedGeneration = totalGeneration;
            this.associatedLoad = totalLoad;
        }
    }

    double getAssociatedLoad() {
        return associatedLoad;
    }

    double getAssociatedGeneration() {
        return associatedGeneration;
    }

    Bus getAssociatedBus() {
        return associatedBus;
    }

    @Override
    public String toString() {
        return associatedBus.getId();
    }
}

/**
 * Edge business object in PEX graph
 * Stands for network branches
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class PexGraphEdge {
    private Branch associatedBranch;

    PexGraphEdge(Branch associatedBranch) {
        this.associatedBranch = Objects.requireNonNull(associatedBranch);
    }

    double getAssociatedFlow() {
        if (Double.isNaN(associatedBranch.getTerminal1().getP())) {
            return 0.;
        } else {
            return Math.abs(associatedBranch.getTerminal1().getP());
        }
    }

    Branch getAssociatedBranch() {
        return associatedBranch;
    }

    @Override
    public String toString() {
        return associatedBranch.getId();
    }
}


/**
 * Business object for PEX graph
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PexGraph extends DirectedMultigraph<PexGraphVertex, PexGraphEdge> {
    private Map<Bus, PexGraphVertex> vertexPerBus = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(PexGraph.class);

    private void addBusAsVertex(Bus bus, InjectionStrategy injectionStrategy) {
        assert bus != null;
        PexGraphVertex vertex = new PexGraphVertex(bus, injectionStrategy);
        addVertex(vertex);
        vertexPerBus.put(bus, vertex);
    }

    private void addBranchAsEdge(Branch branch) {
        assert branch != null;

        Bus bus1 = branch.getTerminal1().getBusView().getBus();
        Bus bus2 = branch.getTerminal2().getBusView().getBus();

        if (Math.abs(branch.getTerminal1().getP()) < 1e-5) {
            // To avoid possibe cycles, remove 0 transfer lines
            LOGGER.debug("Branch {} filtered because of a flow too low : {} MW", branch.getId(), branch.getTerminal1().getP());
        } else if (branch.getTerminal1().getP() > 0) {
            addEdge(vertexPerBus.get(bus1), vertexPerBus.get(bus2), new PexGraphEdge(branch));
        } else {
            addEdge(vertexPerBus.get(bus2), vertexPerBus.get(bus1), new PexGraphEdge(branch));
        }
    }

    private void checkGraph() {
        for (PexGraphVertex vertex : vertexSet()) {
            double nodalGeneration = vertex.getAssociatedGeneration() + incomingEdgesOf(vertex).stream()
                .mapToDouble(PexGraphEdge::getAssociatedFlow).sum();

            double nodalLoad = vertex.getAssociatedLoad() + outgoingEdgesOf(vertex).stream()
                .mapToDouble(PexGraphEdge::getAssociatedFlow).sum();

            assert Math.abs(nodalGeneration - nodalLoad) < 1e-3;
        }
    }

    public PexGraph(Network network, Map<Bus, Integer> busMapper, InjectionStrategy injectionStrategy) {
        super(PexGraphEdge.class);
        Objects.requireNonNull(busMapper);

        network.getBusView().getBusStream()
                .filter(busMapper::containsKey)
                .forEach(bus -> addBusAsVertex(bus, injectionStrategy));
        network.getBranchStream()
                .filter(branch -> busMapper.containsKey(branch.getTerminal1().getBusView().getBus())
                        && busMapper.containsKey(branch.getTerminal2().getBusView().getBus()))
                .filter(branch -> branch.getTerminal1().isConnected() && branch.getTerminal2().isConnected())
                .forEach(this::addBranchAsEdge);

        checkGraph();
    }
}
