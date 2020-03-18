package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.RangeAction;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionResultExtension<I extends RangeAction<I>, S extends RangeActionResult> extends ResultExtension<I, S> {
    /**
     * Extension name
     */
    @Override
    public String getName() {
        return "RangeActionResultExtension";
    }
}
