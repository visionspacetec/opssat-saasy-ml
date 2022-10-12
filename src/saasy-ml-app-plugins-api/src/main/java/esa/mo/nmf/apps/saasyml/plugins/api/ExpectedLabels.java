package esa.mo.nmf.apps.saasyml.plugins.api;

import org.pf4j.ExtensionPoint;
import java.util.Map;

public interface ExpectedLabels extends ExtensionPoint {
    public Map<String, Boolean> getLabels(Map<String, Double> params);
}
