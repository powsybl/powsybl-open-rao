package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.range_action.CounterTradeRangeAction;
import com.farao_community.farao.data.crac_api.range_action.CounterTradeRangeActionAdder;
import com.powsybl.iidm.network.Country;

import java.util.Objects;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;
import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotEmpty;
import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com}
 */
public class CounterTradeRangeActionAdderImpl extends AbstractStandardRangeActionAdder<CounterTradeRangeActionAdder> implements CounterTradeRangeActionAdder {

    public static final String COUNTER_TRADE_RANGE_ACTION = "CounterTradeRangeAction";
    public Country exportingCountry;

    @Override
    protected String getTypeDescription() {
        return COUNTER_TRADE_RANGE_ACTION;
    }

    CounterTradeRangeActionAdderImpl(CracImpl owner) {
        super(owner);
    }

    @Override
    public CounterTradeRangeActionAdder withExportingCountry(Country exportingCountry) {
        this.exportingCountry = exportingCountry;
        return this;
    }

    @Override
    public CounterTradeRangeAction add() {
        checkId();
        checkAutoUsageRules();
        if (!Objects.isNull(getCrac().getRemedialAction(id))) {
            throw new FaraoException(String.format("A remedial action with id %s already exists", id));
        }

        // check exporting country
        assertAttributeNotNull(exportingCountry, COUNTER_TRADE_RANGE_ACTION, "exporting country", "withExportingCountry()");

        // check ranges
        assertAttributeNotEmpty(ranges, COUNTER_TRADE_RANGE_ACTION, "range", "newRange()");

        // check usage rules
        if (usageRules.isEmpty()) {
            BUSINESS_WARNS.warn("InjectionRangeAction {} does not contain any usage rule, by default it will never be available", id);
        }

        CounterTradeRangeAction counterTradeRangeAction = new CounterTradeRangeActionImpl(this.id, this.name, this.operator, this.groupId, this.usageRules, this.ranges, this.initialSetpoint, speed, this.exportingCountry);
        this.getCrac().addCounterTradeRangeAction(counterTradeRangeAction);
        return counterTradeRangeAction;

    }

}
