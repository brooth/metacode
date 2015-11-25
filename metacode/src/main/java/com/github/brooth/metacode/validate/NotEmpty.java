package com.github.brooth.metacode.validate;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 *
 */
public class NotEmpty implements Validator {

    @Override
    public void validate(Object object, String name, List<String> errors) {
        boolean result;
        if (object == null) {
            result = true;

        } else if (object instanceof String) {
            result = object.toString().isEmpty();

        } else if (object instanceof Object[]) {
            result = ((Object[]) object).length < 1;

        } else if ((object instanceof Collection)) {
            result = ((Collection) object).isEmpty();

        } else if ((object instanceof Map)) {
            result = ((Map) object).isEmpty();

        } else {
            throw new ValidationException("Can't check '" + object.getClass().getCanonicalName() + "' is empty");
        }

        if (result)
            errors.add(name + " is empty");
    }
}
