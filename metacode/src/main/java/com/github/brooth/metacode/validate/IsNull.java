package com.github.brooth.metacode.validate;

import java.util.Set;

/**
 *
 */
public class IsNull implements Validator {

    @Override
    public void validate(Object object, String name, Set<String> errors) {
        if (object != null)
            errors.add(name + " is not null");
    }
}
