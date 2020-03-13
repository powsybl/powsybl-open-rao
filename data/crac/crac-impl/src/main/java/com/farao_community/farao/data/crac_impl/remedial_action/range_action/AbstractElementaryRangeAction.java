package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityValue;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractElementaryRangeAction extends AbstractRangeAction {
    protected NetworkElement networkElement;

    public AbstractElementaryRangeAction(String id, String name, String operator, List<UsageRule> usageRules, List<Range> ranges, NetworkElement networkElement) {
        super(id, name, operator, usageRules, ranges);
        this.networkElement = networkElement;
    }

    public AbstractElementaryRangeAction(String id, String name, String operator, NetworkElement networkElement) {
        super(id, name, operator);
        this.networkElement = networkElement;
    }

    public AbstractElementaryRangeAction(String id, String operator, NetworkElement networkElement) {
        super(id, operator);
        this.networkElement = networkElement;
    }

    public AbstractElementaryRangeAction(String id, NetworkElement networkElement) {
        super(id);
        this.networkElement = networkElement;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
    }

    @Override
    public double getSensitivityValue(SensitivityComputationResults sensitivityComputationResults, Cnec cnec) {
        List<SensitivityValue> sensitivityValueStream = sensitivityComputationResults.getSensitivityValues().stream()
            .filter(sensitivityValue -> sensitivityValue.getFactor().getVariable().getId().equals(networkElement.getId()))
            .filter(sensitivityValue -> sensitivityValue.getFactor().getFunction().getId().equals(cnec.getId()))
            .collect(Collectors.toList());

        if (sensitivityValueStream.size() > 1) {
            throw new FaraoException(String.format("More than one sensitivity value found for couple Cnec %s - RA %s", cnec.getId(), this.getId()));
        }
        if (sensitivityValueStream.isEmpty()) {
            throw new FaraoException(String.format("No sensitivity value found for couple Cnec %s - RA %s", cnec.getId(), this.getId()));
        }

        return sensitivityValueStream.get(0).getValue();
    }
}
