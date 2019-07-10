package com.thomascook.ids.bdd.util;

import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by eugene on 08.05.17.
 */
public class JSONAssertCompareIgnoreValues extends DefaultComparator {

    private Set<String> ignoredPaths = new HashSet<>();

    public JSONAssertCompareIgnoreValues(JSONCompareMode mode, String... ignorePath) {
        super(mode);
        ignoredPaths.addAll(Arrays.asList(ignorePath));
    }

    @Override
    public void compareValues(String prefix, Object expectedValue, Object actualValue, JSONCompareResult result) throws JSONException {
        if (!ignoredPaths.contains(prefix)) {
            super.compareValues(prefix, expectedValue, actualValue, result);
        }
    }
}
