package cl.luxury.catalogo.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Guarda los datos anidados del producto (humor, notas, colores...) como texto JSON.
 * En MySQL la columna es de tipo JSON; en las pruebas (H2) es un VARCHAR.
 */
@Converter
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(JsonNode nodo) {
        if (nodo == null || nodo.isNull()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(nodo);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON inválido", e);
        }
    }

    @Override
    public JsonNode convertToEntityAttribute(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(texto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON corrupto en la base de datos", e);
        }
    }
}
