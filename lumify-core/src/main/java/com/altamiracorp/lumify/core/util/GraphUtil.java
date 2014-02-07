package com.altamiracorp.lumify.core.util;

import static com.altamiracorp.lumify.core.model.properties.EntityLumifyProperties.*;

import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.securegraph.Direction;
import com.altamiracorp.securegraph.Edge;
import com.altamiracorp.securegraph.Element;
import com.altamiracorp.securegraph.ElementMutation;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Text;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.altamiracorp.securegraph.type.GeoPoint;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GraphUtil {
    private static final String VISIBILITY_PROPERTY = "_visibility";
    private static final String VISIBILITY_SOURCE_PROPERTY = "_visibilitySource";

    public static JSONArray toJson(Iterable<? extends Element> elements) {
        JSONArray result = new JSONArray();
        for (Element element : elements) {
            result.put(toJson(element));
        }
        return result;
    }

    public static JSONObject toJson(Element element) {
        if (element instanceof Vertex) {
            return toJson((Vertex) element);
        }
        if (element instanceof Edge) {
            return toJson((Edge) element);
        }
        throw new RuntimeException("Unexpected element type: " + element.getClass().getName());
    }

    public static JSONObject toJson(Vertex vertex) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", vertex.getId());
            json.put("properties", toJsonProperties(vertex.getProperties()));
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static JSONObject toJson(Edge edge) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", edge.getId());
            json.put("label", edge.getLabel());
            json.put("sourceVertexId", edge.getVertexId(Direction.OUT));
            json.put("destVertexId", edge.getVertexId(Direction.IN));
            json.put("properties", toJsonProperties(edge.getProperties()));
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static Double[] parseLatLong(Object val) {
        Double[] result = new Double[2];
        if (val instanceof String) {
            String valStr = (String) val;
            String[] latlong = valStr.substring(valStr.indexOf('[') + 1, valStr.indexOf(']')).split(",");
            result[0] = Double.parseDouble(latlong[0]);
            result[1] = Double.parseDouble(latlong[1]);
        } else if (val instanceof GeoPoint) {
            GeoPoint valGeoPoint = (GeoPoint) val;
            result[0] = valGeoPoint.getLatitude();
            result[1] = valGeoPoint.getLongitude();
        }
        return result;
    }

    public static JSONObject toJsonProperties(Iterable<Property> properties) {
        JSONObject resultsJson = new JSONObject();
        for (Property property : properties) {
            if (property.getValue() instanceof StreamingPropertyValue) {
                continue;
            }
            resultsJson.put(property.getName(), GraphUtil.toJsonProperty(property));
        }
        return resultsJson;
    }

    public static JSONObject toJsonProperty(Property property) {
        JSONObject result = new JSONObject();

        Object value = property.getValue();
        if (value instanceof Text) {
            result.put("value", ((Text) value).getText());
        } else if (value instanceof Date) {
            result.put("value", ((Date) value).getTime());
        } else if (value instanceof GeoPoint) {
            GeoPoint geoPoint = (GeoPoint) property.getValue();
            result.put("latitude", geoPoint.getLatitude());
            result.put("longitude", geoPoint.getLongitude());
            if (geoPoint.getAltitude() != null) {
                result.put("altitude", geoPoint.getAltitude());
            }
        } else {
            result.put("value", value);
        }

        if (property.getVisibility() != null) {
            result.put(VISIBILITY_PROPERTY, property.getVisibility().toString());
        }
        if (property.getMetadata() != null && property.getMetadata().get(VISIBILITY_SOURCE_PROPERTY) != null) {
            result.put(VISIBILITY_SOURCE_PROPERTY, property.getMetadata().get(VISIBILITY_SOURCE_PROPERTY));
        }

        return result;
    }

    public static <T extends Element> ElementMutation<T> setProperty(T element, String propertyName, Object value, String visibilitySource,
            VisibilityTranslator visibilityTranslator) {
        Property oldProperty = element.getProperty(propertyName);
        Map<String, Object> propertyMetadata;
        if (oldProperty != null) {
            propertyMetadata = oldProperty.getMetadata();
        } else {
            propertyMetadata = new HashMap<String, Object>();
        }
        ElementMutation<T> elementMutation = element.prepareMutation();

        Visibility visibility = visibilityTranslator.toVisibility(visibilitySource);
        propertyMetadata.put(VISIBILITY_SOURCE_PROPERTY, visibilitySource);

        if (GEO_LOCATION.getKey().equals(propertyName)) {
            GeoPoint geoPoint = (GeoPoint) value;
            GEO_LOCATION.setProperty(elementMutation, geoPoint, propertyMetadata, visibility);
            GEO_LOCATION_DESCRIPTION.setProperty(elementMutation, "", propertyMetadata, visibility);
        } else {
            elementMutation.setProperty(propertyName, value, propertyMetadata, visibility);
        }
        return elementMutation;
    }
}