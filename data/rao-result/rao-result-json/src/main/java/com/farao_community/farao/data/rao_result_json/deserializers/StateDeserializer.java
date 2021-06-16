package com.farao_community.farao.data.rao_result_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;

public final class StateDeserializer {
    private StateDeserializer() {

    }

    public static State getState(Instant instant, String contingencyId, Crac crac, String parentType) {
        if (instant == null) {
            throw new FaraoException(String.format("Cannot deserialize RaoResult: no instant defined in activated states of %s", parentType));
        }
        if (instant == Instant.PREVENTIVE) {
            return crac.getPreventiveState();
        } else {
            if (contingencyId == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: no contingency defined in N-k activated states of %s", parentType));
            }
            State state = crac.getState(contingencyId, instant);
            if (state == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: State at instant %s with contingency %s not found in Crac", instant, contingencyId));
            }
            return state;
        }
    }
}
