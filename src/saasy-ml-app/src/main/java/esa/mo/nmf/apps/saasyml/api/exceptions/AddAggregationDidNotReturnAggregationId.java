package esa.mo.nmf.apps.saasyml.api.exceptions;

public class AddAggregationDidNotReturnAggregationId extends Exception {
    private static final long serialVersionUID = 1L;

    public AddAggregationDidNotReturnAggregationId() {
        super();
    }

    public AddAggregationDidNotReturnAggregationId(String errorMessage) {
        super(errorMessage);
    }
    
    public AddAggregationDidNotReturnAggregationId(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
