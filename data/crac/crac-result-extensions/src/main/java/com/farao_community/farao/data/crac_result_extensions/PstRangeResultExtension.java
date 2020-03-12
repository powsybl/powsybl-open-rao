package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.PstRange;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstRangeResultExtension<I extends PstRange<I>> extends RangeActionResultExtension<I, PstRangeResult> {

    @Override
    public PstRangeResult getVariant(String variantId) {
        return super.getVariant(variantId);
    }
}
