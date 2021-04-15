package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.ElementaryAction;
import com.farao_community.farao.data.crac_impl.CracImpl;
import com.farao_community.farao.data.crac_impl.InjectionSetpointImpl;
import com.farao_community.farao.data.crac_impl.PstSetpointImpl;
import com.farao_community.farao.data.crac_impl.TopologicalActionImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationNames.*;

final class ElementaryActionsDeserializer {

    private ElementaryActionsDeserializer() {
    }

    static Set<ElementaryAction> deserialize(JsonParser jsonParser, CracImpl simpleCrac) throws IOException {
        // cannot be done in a standard deserializer as it requires the simpleCrac to compare
        // the networkElement ids of the NetworkAction with the NetworkElements of the Crac

        Set<ElementaryAction> elementaryActions = new HashSet<>();

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {

            ElementaryAction elementaryAction = null;
            String type = null;
            String networkElementId = null;

            ActionType actionType = null; // useful only if type is "topology"
            Double setpoint = null; // useful only if type is "pst-setpoint" or "injection-set-point"
            TapConvention rangeDefinition = null;  // useful only if type is "pst-setpoint"

            while (!jsonParser.nextToken().isStructEnd()) {

                switch (jsonParser.getCurrentName()) {

                    case TYPE:
                        type = jsonParser.nextTextValue();
                        break;

                    case NETWORK_ELEMENT:
                        networkElementId = jsonParser.nextTextValue();
                        break;

                    case ACTION_TYPE:
                        jsonParser.nextToken();
                        actionType = jsonParser.readValueAs(ActionType.class);
                        break;

                    case SETPOINT:
                        jsonParser.nextToken();
                        setpoint = jsonParser.getDoubleValue();
                        break;

                    case RANGE_DEFINITION:
                        jsonParser.nextToken();
                        rangeDefinition = jsonParser.readValueAs(TapConvention.class);
                        break;

                    default:
                        throw new FaraoException(UNEXPECTED_FIELD + jsonParser.getCurrentName());
                }
            }

            NetworkElement ne = simpleCrac.getNetworkElement(networkElementId);
            if (ne == null) {
                throw new FaraoException(String.format("The network element [%s] mentioned in the elementary action is not defined", networkElementId));
            }

            if (type == null) {
                throw new FaraoException("The Json CRAC contains an elementary action with no type defined");
            }

            switch (type) {
                case TOPOLOGY_TYPE:
                    if (actionType == null) {
                        throw new FaraoException("TopologicalAction must contain an actionType");
                    }
                    elementaryAction = new TopologicalActionImpl(ne, actionType);
                    break;

                case INJECTION_SETPOINT_TYPE:
                    if (setpoint == null) {
                        throw new FaraoException("InjectionSetPoint must contain a setpoint");
                    }
                    elementaryAction = new InjectionSetpointImpl(ne, setpoint);
                    break;

                case PST_SETPOINT_TYPE:
                    if (setpoint == null || rangeDefinition == null) {
                        throw new FaraoException("PstSetPoint must contain a setpoint and a range definition");
                    }
                    elementaryAction = new PstSetpointImpl(ne, setpoint, rangeDefinition);
                    break;

                default:
                    throw new FaraoException(String.format("Type of elementary action [%s] invalid", type)); // should never be thrown
            }

            elementaryActions.add(elementaryAction);
        }
        return elementaryActions;
    }
}
