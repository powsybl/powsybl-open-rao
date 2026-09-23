package com.powsybl.openrao.commons;

import com.powsybl.iidm.network.Area;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Builds an adjacency graph of Areas of a given AreaType, based on network topology
 * (two areas are considered neighbors if some branch connects a voltage level
 * belonging to one to a voltage level belonging to the other).
 */
public class AreaGraph {
    private final Set<AreaPairBoundary> boundaries;

    public AreaGraph(Network network, String areaType) {
        boundaries = new HashSet<>();

        network.getBranchStream()
                .forEach(branch -> {
                    Optional<Area> area1 = getTerminalArea(branch.getTerminal1(), areaType);
                    Optional<Area> area2 = getTerminalArea(branch.getTerminal2(), areaType);

                    if (area1.isPresent() && area2.isPresent()) {
                        if (!area1.equals(area2)) {
                            boundaries.add(new AreaPairBoundary(area1.get().getId(), area2.get().getId()));
                        }
                    } else {
                        OpenRaoLoggerProvider.TECHNICAL_LOGS.debug(
                                "Areas of type {} are not defined on both sides of branch {}", areaType, branch.getId());
                    }
                });
    }

    private Optional<Area> getTerminalArea(Terminal terminal, String areaType) {
        return terminal.getVoltageLevel().getArea(areaType);
    }

    /** Get all direct neighbor areas of a given area. */
    public Set<String> getNeighbours(String area) {
        Set<String> neighbours = new HashSet<>();
        for (AreaPairBoundary boundary : boundaries) {
            if (boundary.getAreaLeft().equals(area)) {
                neighbours.add(boundary.getAreaRight());
            } else if (boundary.getAreaRight().equals(area)) {
                neighbours.add(boundary.getAreaLeft());
            }
        }
        return neighbours;
    }
}
