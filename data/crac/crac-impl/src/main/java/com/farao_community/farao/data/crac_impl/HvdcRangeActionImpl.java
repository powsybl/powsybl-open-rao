package com.farao_community.farao.data.crac_impl; /*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 */

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.range_action.HvdcRange;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Elementary HVDC range remedial action.
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class HvdcRangeActionImpl extends AbstractRangeAction implements HvdcRangeAction {
    private NetworkElement networkElement;
    private List<HvdcRange> ranges;

    HvdcRangeActionImpl(String id, String name, String operator, List<UsageRule> usageRules, List<HvdcRange> ranges,
                        NetworkElement networkElement, String groupId) {
        super(id, name, operator, usageRules, groupId);
        this.networkElement = networkElement;
        this.ranges = ranges;
    }

    @Override
    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    @Override
    public List<HvdcRange> getRanges() {
        return ranges;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement); }

    /**
     * Min angle value allowed by all ranges and the physical limitations of the PST itself
     */
    @Override
    public double getMinAdmissibleSetpoint(double previousInstantSetPoint) {
        return ranges.stream().mapToDouble(HvdcRange::getMin).max().orElseThrow();
    }

    /**
     * Max angle value allowed by all ranges and the physical limitations of the PST itself
     */
    @Override
    public double getMaxAdmissibleSetpoint(double previousInstantSetPoint) {
        return ranges.stream().mapToDouble(HvdcRange::getMax).min().orElseThrow();
    }

    @Override
    public void apply(Network network, double targetSetpoint) {
        getHvdcLine(network).setActivePowerSetpoint(targetSetpoint);
    }

    private HvdcLine getHvdcLine(Network network) {
        HvdcLine hvdcLine = network.getHvdcLine(networkElement.getId());
        if (hvdcLine == null) {
            throw new FaraoException(String.format("HvdcLine %s does not exist in the current network.", networkElement.getId()));
        }
        return hvdcLine;
    }

    @Override
    public double getCurrentSetpoint(Network network) {
        return getHvdcLine(network).getActivePowerSetpoint();
    }

}
