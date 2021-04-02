package com.farao_community.farao.data.crac_impl.json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.InjectionSetpointImpl;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.PstSetpoint;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.TopologicalActionImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.farao_community.farao.data.crac_impl.json.JsonSerializationNames.*;

final class ElementaryActionsDeserializer {

    private ElementaryActionsDeserializer() {
    }

    static Set<ElementaryAction> deserialize(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard deserializer as it requires the simpleCrac to compare
        // the networkElement ids of the NetworkAction with the NetworkElements of the Crac

        Set<ElementaryAction> elementaryActions = new HashSet<>();

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {

            ElementaryAction elementaryAction = null;
            String type = null;
            String id = null;
            String name = null;
            String networkElementId = null;

            ActionType actionType = null; // useful only if type is "topology"
            double setPoint = 0; // useful only if type is "pst-setpoint" or "injection-set-point"
            RangeDefinition rangeDefinition = null;  // useful only if type is "pst-setpoint"

            while (!jsonParser.nextToken().isStructEnd()) {

                switch (jsonParser.getCurrentName()) {

                    case TYPE:
                        type = jsonParser.nextTextValue();
                        break;

                    case ID:
                        id = jsonParser.nextTextValue();
                        break;

                    case NAME:
                        name = jsonParser.nextTextValue();
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
                        setPoint = jsonParser.getDoubleValue();
                        break;

                    case RANGE_DEFINITION:
                        jsonParser.nextToken();
                        rangeDefinition = jsonParser.readValueAs(RangeDefinition.class);
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
                    elementaryAction = new TopologicalActionImpl(id, name, ne, actionType);
                    break;

                case INJECTION_SETPOINT_TYPE:
                    elementaryAction = new InjectionSetpointImpl(id, name, ne, setPoint);
                    break;

                case PST_SETPOINT_TYPE:
                    elementaryAction = new PstSetpoint(id, name, ne, setPoint, rangeDefinition);
                    break;

                default:
                    throw new FaraoException(String.format("Type of elementary action [%s] invalid", type)); // should never be thrown
            }

            elementaryActions.add(elementaryAction);
        }
        return elementaryActions;
    }
}
