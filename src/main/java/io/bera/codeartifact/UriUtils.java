package io.bera.codeartifact;

import java.util.LinkedHashMap;
import java.util.Map;

public class UriUtils {
    public static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new LinkedHashMap<>();
        if (query != null) {
            if (query.startsWith("?")) {
                query = query.substring(1);
            }
            String[] split = query.split("&");
            for (String nameValue : split) {
                String name;
                String value;
                int equalsIndex = nameValue.indexOf('=');
                if (equalsIndex > 0) {
                    name = nameValue.substring(0, equalsIndex);
                    value = nameValue.substring(equalsIndex + 1);
                } else {
                    name = nameValue;
                    value = null;
                }

                params.put(name, value);
            }
        }

        return params;
    }
}
