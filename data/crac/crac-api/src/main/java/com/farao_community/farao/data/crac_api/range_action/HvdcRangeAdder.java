package com.farao_community.farao.data.crac_api.range_action;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface HvdcRangeAdder {

    HvdcRangeAdder withMin(double minSetpoint);

    HvdcRangeAdder withMax(double maxSetpoint);

    HvdcRangeActionAdder add();

}
