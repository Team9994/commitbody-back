package team9499.commitbody.global.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;

public class CustomMySQLDialect implements FunctionContributor {

    private static final String FUNCTION_PATTERN = "match (?1) against (?2 in boolean mode)";
    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        BasicType<Double> resultType = functionContributions
                .getTypeConfiguration()
                .getBasicTypeRegistry()
                .resolve(StandardBasicTypes.DOUBLE);

        functionContributions.getFunctionRegistry()
                .registerPattern("match",FUNCTION_PATTERN,resultType);
    }
}
