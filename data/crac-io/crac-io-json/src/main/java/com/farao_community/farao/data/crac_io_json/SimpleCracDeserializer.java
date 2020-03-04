package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.*;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.*;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_impl.usage_rule.OnConstraint;
import com.farao_community.farao.data.crac_impl.usage_rule.OnContingency;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.iidm.network.Network;
import jdk.nashorn.internal.parser.JSONParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.ArrayUtils.toArray;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SimpleCracDeserializer extends StdDeserializer<SimpleCrac> {
    private static final String NETWORK_ELEMENT = "networkElement";
    private static final String NETWORK_ELEMENTS = "networkElements";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String OPERATOR = "operator";
    private static final String CONTINGENCY = "contingency";
    private static final String CONTINGENCIES = "contingencies";
    private static final String COMPLEX_CONTINGENCY_TYPE = "complex-contingency";
    private static final String STATES = "states";
    private static final String STATE = "state";
    private static final String SIMPLE_STATE_TYPE = "simple-state";
    private static final String INSTANT = "instant";
    private static final String INSTANTS = "instants";
    private static final String USAGE_METHOD = "usageMethod";
    private static final String SETPOINT = "setpoint";
    private static final String TYPE = "type";
    private static final String CNECS = "cnecs";
    private static final String SIMPLE_CNEC_TYPE = "simple-cnec";
    private static final String THRESHOLD = "threshold";
    private static final String EXTENSIONS = "extensions";
    private static final String RANGE_ACTIONS = "rangeActions";
    private static final String PST_WITH_RANGE_TYPE = "pst-with-range";
    private static final String RANGES = "ranges";
    private static final String USAGE_RULES = "usageRules";
    private static final String FREE_TO_USE_TYPE = "free-to-use";
    private static final String CNEC = "cnec";
    private static final String ON_CONSTRAINT_TYPE = "on-constraint";
    private static final String ON_CONTINGENCY_TYPE = "on-contingency";
    private static final String TOPOLOGY_TYPE = "topology";
    private static final String PST_SETPOINT_TYPE = "pst-setpoint";
    private static final String ACTION_TYPE = "actionType";
    private static final String COMPLEX_NETWORK_ACTION_TYPE = "complex-network-action";
    private static final String ELEMENTARY_NETWOK_ACTIONS = "elementaryNetworkActions";
    private static final String NETWORK_ACTIONS = "networkActions";
    private static final String ALIGNED_RANGE_ACTIONS_TYPE = "aligned-range-action";

    public SimpleCracDeserializer() {
        this(null);
    }

    public SimpleCracDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SimpleCrac deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
       /*
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node.get(ID) == null || node.get(NAME) == null) {
            throw new FaraoException("Json crac has no field 'id' or no field 'name'");
        }
        SimpleCrac simpleCrac = new SimpleCrac(node.get(ID).asText(), node.get(NAME).asText());

        // todo : find a cleaner way to reset the parser or to initialize the Crac
        jsonParser.nextToken();

        */
       SimpleCrac simpleCrac = new SimpleCrac("id");

        while (jsonParser.currentToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.getCurrentName()) {

                case TYPE:
                case ID:
                case NAME:
                    jsonParser.nextToken();
                    break;

                case NETWORK_ELEMENTS:
                    jsonParser.nextToken();
                    Set<NetworkElement> networkElements = jsonParser.readValueAs(new TypeReference<Set<NetworkElement>>() {
                    });
                    networkElements.forEach(simpleCrac::addNetworkElement);
                    break;

                case INSTANTS:
                    jsonParser.nextToken();
                    Set<Instant> instants = jsonParser.readValueAs(new TypeReference<Set<Instant>>() {
                    });
                    instants.forEach(simpleCrac::addInstant);
                    break;

                case CONTINGENCIES:
                    jsonParser.nextToken();
                    deserializeContingencies(jsonParser, simpleCrac);
                    break;

                case STATES:
                    jsonParser.nextToken();
                    deserializeStates(jsonParser, simpleCrac);
                    break;

                case CNECS:
                    jsonParser.nextToken();
                    deserializeCnecs(jsonParser, deserializationContext, simpleCrac);
                    break;

                case RANGE_ACTIONS:
                    jsonParser.nextToken();
                    deserializeRangeActions(jsonParser, simpleCrac);
                    break;

                case NETWORK_ACTIONS:
                    jsonParser.nextToken();
                    Set<NetworkAction> networkActions = deserializeNetworkActions(jsonParser, simpleCrac);
                    networkActions.forEach(simpleCrac::addNetworkAction);
                    break;

                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());


            }
            jsonParser.nextToken();
        }

        return simpleCrac;
    }

    private void deserializeContingencies(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard ComplexContingency deserializer as it requires the simpleCrac to
        // compare the NetworkElement ids of the ComplexContingency with the NetworkElement of the SimpleCrac

        while(jsonParser.nextToken() != JsonToken.END_ARRAY) {

            String id = null;
            String name = null;
            ArrayList<String> networkElementsIds = new ArrayList<>();

            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    case TYPE:
                        if (!jsonParser.nextTextValue().equals(COMPLEX_CONTINGENCY_TYPE)) {
                           throw new FaraoException(String.format("SimpleCrac cannot deserialize other contingencies types than %s", COMPLEX_CONTINGENCY_TYPE));
                        }
                        break;

                    case ID:
                        id = jsonParser.nextTextValue();
                        break;

                    case NAME:
                        name = jsonParser.nextTextValue();
                        break;

                    case NETWORK_ELEMENTS:
                        jsonParser.nextToken();
                        networkElementsIds = jsonParser.readValueAs(new TypeReference<ArrayList<String>>() {
                        });
                        break;

                    default:
                        throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
                }
            }

            //add contingency in Crac
            Set<NetworkElement> networkElements = new HashSet<>();
            networkElementsIds.forEach(neId -> {
                NetworkElement ne = simpleCrac.getNetworkElement(neId);
                if (ne == null) {
                    throw new FaraoException(String.format("The network element [%s] mentioned in the contingencies is not defined", neId));
                }
                networkElements.add(ne);
            });

            simpleCrac.addContingency(new ComplexContingency(id, name, networkElements));

        }
    }

    private void deserializeStates(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard State deserializer as it requires the simpleCrac to compare
        // Contingency ids and Instant ids ids with what is in the SimpleCrac

        while(jsonParser.nextToken() != JsonToken.END_ARRAY) {

            String contingencyId = null;
            String instantId = null;

            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    case TYPE:
                        if (!jsonParser.nextTextValue().equals(SIMPLE_STATE_TYPE)) {
                            throw new FaraoException(String.format("SimpleCrac cannot deserialize other states types than %s", SIMPLE_STATE_TYPE));
                        }
                        break;

                    case ID:
                        // the id should be the concatenation of the contingency id and state id
                        jsonParser.nextToken();
                        break;

                    case CONTINGENCY:
                        contingencyId = jsonParser.nextTextValue();
                        break;

                    case INSTANT:
                        instantId = jsonParser.nextTextValue();
                        break;

                    default:
                        throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
                }
            }

            //add state in Crac
            simpleCrac.addState(contingencyId, instantId);
        }
    }


    private void deserializeCnecs(JsonParser jsonParser, DeserializationContext deserializationContext, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard State deserializer as it requires the simpleCrac to compare
        // the State id and NetworkElement id with what is in the Crac

        while(jsonParser.nextToken() != JsonToken.END_ARRAY) {

            String id = null;
            String name = null;
            String networkElementId = null;
            String stateId = null;
            AbstractThreshold threshold = null;
            List<Extension<Cnec>> extensions = new ArrayList<>();

            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    case TYPE:
                        if (!jsonParser.nextTextValue().equals(SIMPLE_CNEC_TYPE)) {
                            throw new FaraoException(String.format("SimpleCrac cannot deserialize other Cnecs types than %s", SIMPLE_CNEC_TYPE));
                        }
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

                    case STATE:
                        stateId = jsonParser.nextTextValue();
                        break;

                    case THRESHOLD:
                        jsonParser.nextToken();
                        threshold = jsonParser.readValueAs(AbstractThreshold.class);
                        break;

                    case EXTENSIONS:
                        jsonParser.nextToken();
                        extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getCnecExtensionSerializers());
                        break;

                    default:
                        throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
                }
            }

            //add SimpleCnec in Crac
            simpleCrac.addCnec(id, name, networkElementId, threshold, stateId);
            if (!extensions.isEmpty()) {
                ExtensionsHandler.getCnecExtensionSerializers().addExtensions(simpleCrac.getCnec(id), extensions);
            }
        }
    }


    private void deserializeRangeActions(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard RangeAction deserializer as it requires the simpleCrac to compare
        // the networkElement ids with what is in the SimpleCrac

        while(jsonParser.nextToken() != JsonToken.END_ARRAY) {

            RangeAction rangeAction;

            // first json Token should be the type of the range action
            jsonParser.nextToken();
            if(!jsonParser.getCurrentName().equals(TYPE)) {
                throw new FaraoException("Type of range action is missing");
            }

            // use the deserializer suited to range action type
            String type = jsonParser.nextTextValue();
            switch (type) {
                case PST_WITH_RANGE_TYPE:
                    rangeAction = deserializePstWithRange(jsonParser, simpleCrac);
                    break;

                case ALIGNED_RANGE_ACTIONS_TYPE:
                    rangeAction = deserializeAlignedRangeAction(jsonParser, simpleCrac);
                    break;

                default:
                    throw new FaraoException(String.format("Type of range action [%s] not handled by SimpleCrac deserializer.", type));

            }

            simpleCrac.addRangeAction(rangeAction);

        }
    }

    private AlignedRangeAction deserializeAlignedRangeAction(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard AlignedRangeAction deserializer as it requires the simpleCrac to compare
        // the networkElement ids with what is in the SimpleCrac

        String id = null;
        String name = null;
        String operator = null;
        List<UsageRule> usageRules = new ArrayList<>();
        List<Range> ranges = new ArrayList<>();
        List<String> networkElementsIds = new ArrayList<>();

        while (!jsonParser.nextToken().isStructEnd()) {
            {
                switch (jsonParser.getCurrentName()) {

                    case ID:
                        id = jsonParser.nextTextValue();
                        break;

                    case NAME:
                        name = jsonParser.nextTextValue();
                        break;

                    case OPERATOR:
                        operator = jsonParser.nextTextValue();
                        break;

                    case USAGE_RULES:
                        jsonParser.nextToken();
                        usageRules = deserializeUsageRules(jsonParser, simpleCrac);
                        break;

                    case RANGES:
                        jsonParser.nextToken();
                        ranges = jsonParser.readValueAs(new TypeReference<List<Range>>() {
                        });
                        break;

                    case NETWORK_ELEMENTS:
                        jsonParser.nextToken();
                        networkElementsIds = jsonParser.readValueAs(new TypeReference<ArrayList<String>>() {
                        });
                        break;

                    default:
                        throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());

                }
            }
        }

        //add contingency in Crac
        Set<NetworkElement> networkElements = new HashSet<>();
        networkElementsIds.forEach(neId -> {
            NetworkElement ne = simpleCrac.getNetworkElement(neId);
            if (ne == null) {
                throw new FaraoException(String.format("The network element [%s] mentioned in the contingencies is not defined", neId));
            }
            networkElements.add(ne);
        });

        return new AlignedRangeAction(id, name, operator, usageRules, ranges, networkElements);
    }


    private PstWithRange deserializePstWithRange(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard PstWithRange deserializer as it requires the simpleCrac to compare
        // the networkElement ids with what is in the SimpleCrac

        String id = null;
        String name = null;
        String operator = null;
        List<UsageRule> usageRules = new ArrayList<>();
        List<Range> ranges = new ArrayList<>();
        String networkElementId = null;

        while (!jsonParser.nextToken().isStructEnd()) {
            {
                switch (jsonParser.getCurrentName()) {

                    case ID:
                        id = jsonParser.nextTextValue();
                        break;

                    case NAME:
                        name = jsonParser.nextTextValue();
                        break;

                    case OPERATOR:
                        operator = jsonParser.nextTextValue();
                        break;

                    case USAGE_RULES:
                        jsonParser.nextToken();
                        usageRules = deserializeUsageRules(jsonParser, simpleCrac);
                        break;

                    case RANGES:
                        jsonParser.nextToken();
                        ranges = jsonParser.readValueAs(new TypeReference<List<Range>>() {
                        });
                        break;

                    case NETWORK_ELEMENT:
                        networkElementId = jsonParser.nextTextValue();
                        break;

                    default:
                        throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());

                }
            }
        }

        NetworkElement ne = simpleCrac.getNetworkElement(networkElementId);
        if (ne == null) {
            throw new FaraoException(String.format("The network element [%s] mentioned in the pst-with-range is not defined", networkElementId));
        }

        return new PstWithRange(id, name, operator, usageRules, ranges, ne);
    }


    private Set<NetworkAction> deserializeNetworkActions(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard NetworkAction deserializer as it requires the simpleCrac to compare
        // the networkElement ids with what is in the SimpleCrac

        Set<NetworkAction> networkActions = new HashSet<>();

        while(jsonParser.nextToken() != JsonToken.END_ARRAY) {

            NetworkAction networkAction;

            // first json Token should be the type of the range action
            jsonParser.nextToken();
            if(!jsonParser.getCurrentName().equals(TYPE)) {
                throw new FaraoException("Type of range action is missing");
            }

            // use the deserializer suited to range action type
            String type = jsonParser.nextTextValue();
            switch (type) {
                case TOPOLOGY_TYPE:
                    networkAction = deserializeTopology(jsonParser, simpleCrac);
                    break;

                case PST_SETPOINT_TYPE:
                    networkAction = deserializePstSetPoint(jsonParser, simpleCrac);
                    break;

                case COMPLEX_NETWORK_ACTION_TYPE:
                    networkAction = deserializeComplexNetworkAction(jsonParser, simpleCrac);
                    break;

                default:
                    throw new FaraoException(String.format("Type of range action [%s] not handled by SimpleCrac deserializer.", type));

            }

            networkActions.add(networkAction);
        }

        return networkActions;
    }

    private Topology deserializeTopology(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard PstWithRange deserializer as it requires the simpleCrac to compare
        // the networkElement ids with what is in the SimpleCrac

        String id = null;
        String name = null;
        String operator = null;
        List<UsageRule> usageRules = new ArrayList<>();
        String networkElementId = null;
        ActionType actionType = null;

        while (!jsonParser.nextToken().isStructEnd()) {
            {
                switch (jsonParser.getCurrentName()) {

                    case ID:
                        id = jsonParser.nextTextValue();
                        break;

                    case NAME:
                        name = jsonParser.nextTextValue();
                        break;

                    case OPERATOR:
                        operator = jsonParser.nextTextValue();
                        break;

                    case USAGE_RULES:
                        jsonParser.nextToken();
                        usageRules = deserializeUsageRules(jsonParser, simpleCrac);
                        break;

                    case NETWORK_ELEMENT:
                        networkElementId = jsonParser.nextTextValue();
                        break;

                    case ACTION_TYPE:
                        jsonParser.nextToken();
                        actionType = jsonParser.readValueAs(ActionType.class);
                        break;

                    default:
                        throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());

                }
            }
        }

        NetworkElement ne = simpleCrac.getNetworkElement(networkElementId);
        if (ne == null) {
            throw new FaraoException(String.format("The network element [%s] mentioned in the topology is not defined", networkElementId));
        }

        return new Topology(id, name, operator, usageRules, ne, actionType);
    }

    private PstSetpoint deserializePstSetPoint(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard PstSetPoint deserializer as it requires the simpleCrac to compare
        // the networkElement ids with what is in the SimpleCrac

        String id = null;
        String name = null;
        String operator = null;
        List<UsageRule> usageRules = new ArrayList<>();
        String networkElementId = null;
        double setPoint = 0;

        while (!jsonParser.nextToken().isStructEnd()) {
            {
                switch (jsonParser.getCurrentName()) {

                    case ID:
                        id = jsonParser.nextTextValue();
                        break;

                    case NAME:
                        name = jsonParser.nextTextValue();
                        break;

                    case OPERATOR:
                        operator = jsonParser.nextTextValue();
                        break;

                    case USAGE_RULES:
                        jsonParser.nextToken();
                        usageRules = deserializeUsageRules(jsonParser, simpleCrac);
                        break;

                    case NETWORK_ELEMENT:
                        networkElementId = jsonParser.nextTextValue();
                        break;

                    case SETPOINT:
                        jsonParser.nextToken();
                        setPoint = jsonParser.getDoubleValue();
                        break;

                    default:
                        throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());

                }
            }
        }

        NetworkElement ne = simpleCrac.getNetworkElement(networkElementId);
        if (ne == null) {
            throw new FaraoException(String.format("The network element [%s] mentioned in the topology is not defined", networkElementId));
        }

        return new PstSetpoint(id, name, operator, usageRules, ne, setPoint);
    }

    private ComplexNetworkAction deserializeComplexNetworkAction(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard PstSetPoint deserializer as it requires the simpleCrac to compare
        // the networkElement ids with what is in the SimpleCrac

        String id = null;
        String name = null;
        String operator = null;
        List<UsageRule> usageRules = new ArrayList<>();
        Set<AbstractElementaryNetworkAction> elementaryNetworkActions = new HashSet<>();

        while (!jsonParser.nextToken().isStructEnd()) {
            {
                switch (jsonParser.getCurrentName()) {

                    case ID:
                        id = jsonParser.nextTextValue();
                        break;

                    case NAME:
                        name = jsonParser.nextTextValue();
                        break;

                    case OPERATOR:
                        operator = jsonParser.nextTextValue();
                        break;

                    case USAGE_RULES:
                        jsonParser.nextToken();
                        usageRules = deserializeUsageRules(jsonParser, simpleCrac);
                        break;

                    case ELEMENTARY_NETWOK_ACTIONS:
                        jsonParser.nextToken();
                        Set<NetworkAction> networkActions = deserializeNetworkActions(jsonParser, simpleCrac);
                        networkActions.forEach(na -> {
                            if (! (na instanceof AbstractElementaryNetworkAction)) {
                                throw new FaraoException("A complex network action can only contain elementary network actions");
                            }
                            elementaryNetworkActions.add((AbstractElementaryNetworkAction) na);
                        });
                        break;

                    default:
                        throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());

                }
            }
        }

        return new ComplexNetworkAction(id, name, operator, usageRules, elementaryNetworkActions);
    }



    private List<UsageRule> deserializeUsageRules(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException{
        // cannot be done in a standard UsageRule deserializer as it requires the simpleCrac to compare
        // the networkElement ids with what is in the SimpleCrac

        List<UsageRule> usageRules = new ArrayList<>();

        while(jsonParser.nextToken() != JsonToken.END_ARRAY) {

            UsageRule usageRule;

            // first json Token should be the type of the range action
            jsonParser.nextToken();
            if(!jsonParser.getCurrentName().equals(TYPE)) {
                throw new FaraoException("Type of usage rule is missing");
            }

            // use the deserializer suited to the usage rule type
            String type = jsonParser.nextTextValue();
            switch (type) {
                case FREE_TO_USE_TYPE:
                    usageRule = deserializeFreeToUseUsageRule(jsonParser, simpleCrac);
                    break;

                case ON_CONSTRAINT_TYPE:
                    usageRule = deserializeOnConstraintUsageRule(jsonParser, simpleCrac);
                    break;

                case ON_CONTINGENCY_TYPE:
                    usageRule = deserializeOnContingencyUsageRule(jsonParser, simpleCrac);
                    break;

                default:
                    throw new FaraoException(String.format("Type of range action [%s] not handled by SimpleCrac deserializer.", type));

            }

            usageRules.add(usageRule);
        }

        return usageRules;

    }

    private FreeToUse deserializeFreeToUseUsageRule(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard FreeToUse deserializer as it requires the simpleCrac to compare
        // the state ids with what is in the SimpleCrac

        UsageMethod usageMethod = null;
        String stateId = null;

        while (!jsonParser.nextToken().isStructEnd()) {
            {
                switch (jsonParser.getCurrentName()) {

                    case USAGE_METHOD:
                        jsonParser.nextToken();
                        usageMethod = jsonParser.readValueAs(UsageMethod.class);
                        break;

                    case STATE:
                        stateId = jsonParser.nextTextValue();
                        break;

                    default:
                        throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());

                }
            }
        }

        State state = simpleCrac.getState(stateId);
        if (state == null) {
            throw new FaraoException(String.format("The state [%s] mentioned in the free-to-use usage rule is not defined", stateId));
        }

        return new FreeToUse(usageMethod, state);
    }

    private OnConstraint deserializeOnConstraintUsageRule(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard OnConstraint deserializer as it requires the simpleCrac to compare
        // the state and cnec ids with what is in the SimpleCrac

        UsageMethod usageMethod = null;
        String stateId = null;
        String cnecId = null;

        while (!jsonParser.nextToken().isStructEnd()) {
            {
                switch (jsonParser.getCurrentName()) {

                    case USAGE_METHOD:
                        jsonParser.nextToken();
                        usageMethod = jsonParser.readValueAs(UsageMethod.class);
                        break;

                    case STATE:
                        stateId = jsonParser.nextTextValue();
                        break;

                    case CNEC:
                        cnecId = jsonParser.nextTextValue();
                        break;

                    default:
                        throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());

                }
            }
        }

        State state = simpleCrac.getState(stateId);
        if (state == null) {
            throw new FaraoException(String.format("The state [%s] mentioned in the on-constraint usage rule is not defined", stateId));
        }

        Cnec cnec = simpleCrac.getCnec(cnecId);
        if (cnec == null) {
            throw new FaraoException(String.format("The cnec [%s] mentioned in the on-constraint usage rule is not defined", cnecId));
        }

        return new OnConstraint(usageMethod, state, cnec);
    }


    private OnContingency deserializeOnContingencyUsageRule(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard OnContingency deserializer as it requires the simpleCrac to compare
        // the state and contingency ids with what is in the SimpleCrac

        UsageMethod usageMethod = null;
        String stateId = null;
        String contingencyId = null;

        while (!jsonParser.nextToken().isStructEnd()) {
            {
                switch (jsonParser.getCurrentName()) {

                    case USAGE_METHOD:
                        jsonParser.nextToken();
                        usageMethod = jsonParser.readValueAs(UsageMethod.class);
                        break;

                    case STATE:
                        stateId = jsonParser.nextTextValue();
                        break;

                    case CONTINGENCY:
                        contingencyId = jsonParser.nextTextValue();
                        break;

                    default:
                        throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());

                }
            }
        }

        State state = simpleCrac.getState(stateId);
        if (state == null) {
            throw new FaraoException(String.format("The state [%s] mentioned in the on-contingency usage rule is not defined", stateId));
        }

        Contingency contingency = simpleCrac.getContingency(contingencyId);
        if (contingency == null) {
            throw new FaraoException(String.format("The contingency [%s] mentioned in the on-contingency usage rule is not defined", contingencyId));
        }

        return new OnContingency(usageMethod, state, contingency);
    }




    private static void createAndAddContingency(SimpleCrac simpleCrac, JsonNode contingency) {
        Set<NetworkElement> networkElements = new HashSet<>();
        for (Iterator<JsonNode> itNE = contingency.get(NETWORK_ELEMENTS).elements(); itNE.hasNext(); ) {
            JsonNode networkElement = itNE.next();
            networkElements.add(simpleCrac.getNetworkElement(networkElement.asText()));
        }
        simpleCrac.addContingency(new ComplexContingency(contingency.get(ID).asText(), contingency.get(NAME).asText(), networkElements));
    }

    private static void createAndAddState(SimpleCrac simpleCrac, JsonNode state) {
        Optional<Contingency> contingency;
        if (state.get(CONTINGENCY).isNull()) {
            contingency = Optional.empty();
        } else {
            contingency = Optional.of(simpleCrac.getContingency(state.get(CONTINGENCY).asText()));
        }
        simpleCrac.addState(new SimpleState(contingency, simpleCrac.getInstant(state.get("instant").asText())));
    }

    private static void createAndAddCnec(SimpleCrac simpleCrac, JsonNode cnecNode, JsonParser jsonParser, DeserializationContext deserializationContext) throws JsonProcessingException {
        SimpleCnec cnec = new SimpleCnec(
                cnecNode.get(ID).asText(),
                cnecNode.get(NAME).asText(),
                simpleCrac.getNetworkElement(cnecNode.get(NETWORK_ELEMENT).asText()),
                jsonParser.getCodec().treeToValue(cnecNode.get("threshold"), AbstractThreshold.class),
                simpleCrac.getState(cnecNode.get(STATE).asText())
        );

        // check for extensions
        try {
            if (cnecNode.get("extensions") != null) {

                List<Extension<Cnec>> extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getCnecExtensionSerializers());
                ExtensionsHandler.getCnecExtensionSerializers().addExtensions(cnec, extensions);
            }


        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new UncheckedIOException(e);
        }

        simpleCrac.addCnec(cnec);
    }


    private static List<UsageRule> getUsageRules(SimpleCrac simpleCrac, JsonNode abstractRemedialActionNode) {
        List<UsageRule> usageRules = new ArrayList<>();
        for (Iterator<JsonNode> itUsageRule = abstractRemedialActionNode.get("usageRules").elements(); itUsageRule.hasNext(); ) {
            JsonNode usageRule = itUsageRule.next();
            switch (usageRule.get(TYPE).asText()) {
                case "free-to-use":
                    usageRules.add(new FreeToUse(UsageMethod.valueOf(usageRule.get(USAGE_METHOD).asText()), simpleCrac.getState(usageRule.get(STATE).asText())));
                    break;
                case "on-constraint":
                    usageRules.add(new OnConstraint(
                        UsageMethod.valueOf(usageRule.get(USAGE_METHOD).asText()),
                        simpleCrac.getState(usageRule.get(STATE).asText()),
                        simpleCrac.getCnec(usageRule.get("cnec").asText())));
                    break;
                case "on-contingency":
                    usageRules.add(new OnContingency(
                        UsageMethod.valueOf(usageRule.get(USAGE_METHOD).asText()),
                        simpleCrac.getState(usageRule.get(STATE).asText()),
                        simpleCrac.getContingency(usageRule.get(CONTINGENCY).asText())));
                    break;
                default:
                    throw new FaraoException(format("Not implemented type %s", usageRule.get(TYPE).asText()));
            }
        }
        return usageRules;
    }

    private static void createAndAddRangeAction(SimpleCrac simpleCrac, JsonNode rangeAction, JsonParser jsonParser) throws JsonProcessingException {
        List<UsageRule> usageRules = getUsageRules(simpleCrac, rangeAction);
        List<Range> ranges = new ArrayList<>();
        for (Iterator<JsonNode> itElementary = rangeAction.get("ranges").elements(); itElementary.hasNext(); ) {
            JsonNode range = itElementary.next();
            ranges.add(jsonParser.getCodec().treeToValue(range, Range.class));
        }
        switch (rangeAction.get(TYPE).asText()) {
            case "aligned-range-action":
                Set<NetworkElement> networkElements = new HashSet<>();
                for (Iterator<JsonNode> itElementary = rangeAction.get(NETWORK_ELEMENTS).elements(); itElementary.hasNext(); ) {
                    JsonNode networkElement = itElementary.next();
                    networkElements.add(simpleCrac.getNetworkElement(networkElement.asText()));
                }
                simpleCrac.addRangeAction(new AlignedRangeAction(
                    rangeAction.get(ID).asText(),
                    rangeAction.get(NAME).asText(),
                    rangeAction.get(OPERATOR).asText(),
                    usageRules,
                    ranges,
                    networkElements
                ));
                break;
            case "pst-with-range":
                simpleCrac.addRangeAction(new PstWithRange(
                    rangeAction.get(ID).asText(),
                    rangeAction.get(NAME).asText(),
                    rangeAction.get(OPERATOR).asText(),
                    usageRules,
                    ranges,
                    simpleCrac.getNetworkElement(rangeAction.get(NETWORK_ELEMENT).asText())
                ));
                break;
            case "hvdc-range":
                simpleCrac.addRangeAction(new HvdcRange(
                    rangeAction.get(ID).asText(),
                    rangeAction.get(NAME).asText(),
                    rangeAction.get(OPERATOR).asText(),
                    usageRules,
                    ranges,
                    simpleCrac.getNetworkElement(rangeAction.get(NETWORK_ELEMENT).asText())
                ));
                break;
            case "injection-range":
                simpleCrac.addRangeAction(new InjectionRange(
                    rangeAction.get(ID).asText(),
                    rangeAction.get(NAME).asText(),
                    rangeAction.get(OPERATOR).asText(),
                    usageRules,
                    ranges,
                    simpleCrac.getNetworkElement(rangeAction.get(NETWORK_ELEMENT).asText())
                ));
                break;
            case "redispatching":
                simpleCrac.addRangeAction(new Redispatching(
                    rangeAction.get(ID).asText(),
                    rangeAction.get(NAME).asText(),
                    rangeAction.get(OPERATOR).asText(),
                    usageRules,
                    ranges,
                    rangeAction.get("minimumPower").asDouble(),
                    rangeAction.get("maximumPower").asDouble(),
                    rangeAction.get("targetPower").asDouble(),
                    rangeAction.get("startupCost").asDouble(),
                    rangeAction.get("marginalCost").asDouble(),
                    simpleCrac.getNetworkElement(rangeAction.get("generator").asText())
                ));
                break;
            case "countertrading":
            default:
                break;
        }
    }

    private static void createAndAddNetworkAction(SimpleCrac simpleCrac, JsonNode networkAction) {
        if (networkAction.get(TYPE).asText().equals("complex-network-action")) {
            List<UsageRule> usageRules = getUsageRules(simpleCrac, networkAction);
            Set<AbstractElementaryNetworkAction> abstractElementaryNetworkActions = new HashSet<>();
            for (Iterator<JsonNode> itElementary = networkAction.get("elementaryNetworkActions").elements(); itElementary.hasNext(); ) {
                JsonNode elementaryNetworkAction = itElementary.next();
                abstractElementaryNetworkActions.add(getAbstractElementaryNetworkAction(simpleCrac, elementaryNetworkAction));
            }
            simpleCrac.addNetworkAction(new ComplexNetworkAction(
                networkAction.get(ID).asText(),
                networkAction.get(NAME).asText(),
                networkAction.get(OPERATOR).asText(),
                usageRules,
                abstractElementaryNetworkActions
            ));
        } else {
            simpleCrac.addNetworkAction(getAbstractElementaryNetworkAction(simpleCrac, networkAction));
        }
    }

    private static AbstractElementaryNetworkAction getAbstractElementaryNetworkAction(SimpleCrac simpleCrac, JsonNode elementaryNetworkActionNode) {
        List<UsageRule> usageRules = getUsageRules(simpleCrac, elementaryNetworkActionNode);
        switch (elementaryNetworkActionNode.get(TYPE).asText()) {
            case "pst-setpoint":
                return new PstSetpoint(
                    elementaryNetworkActionNode.get(ID).asText(),
                    elementaryNetworkActionNode.get(NAME).asText(),
                    elementaryNetworkActionNode.get(OPERATOR).asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get(NETWORK_ELEMENT).asText()),
                    elementaryNetworkActionNode.get(SETPOINT).asDouble()
                );
            case "hvdc-setpoint":
                return new HvdcSetpoint(
                    elementaryNetworkActionNode.get(ID).asText(),
                    elementaryNetworkActionNode.get(NAME).asText(),
                    elementaryNetworkActionNode.get(OPERATOR).asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get(NETWORK_ELEMENT).asText()),
                    elementaryNetworkActionNode.get(SETPOINT).asDouble()
                );
            case "injection-setpoint":
                return new InjectionSetpoint(
                    elementaryNetworkActionNode.get(ID).asText(),
                    elementaryNetworkActionNode.get(NAME).asText(),
                    elementaryNetworkActionNode.get(OPERATOR).asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get(NETWORK_ELEMENT).asText()),
                    elementaryNetworkActionNode.get(SETPOINT).asDouble()
                );
            case "topology":
                return new Topology(
                    elementaryNetworkActionNode.get(ID).asText(),
                    elementaryNetworkActionNode.get(NAME).asText(),
                    elementaryNetworkActionNode.get(OPERATOR).asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get(NETWORK_ELEMENT).asText()),
                    ActionType.valueOf(elementaryNetworkActionNode.get("actionType").asText())
                );
            default:
                throw new FaraoException("Unknown type");
        }
    }
}
