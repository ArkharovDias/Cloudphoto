package org.example;

public enum Argument {
    PATH("-p"), ALBUM("-a"), ILLEGAL_ARGUMENT("illegal argument");

    private String argumentName;

    Argument(String argumentName) {
        this.argumentName = argumentName;
    }

    public String getArgumentName() {
        return argumentName;
    }

    public static Argument getArgument(String value){
        for (Argument argument: values()){
            if (argument.argumentName.equals(value)){
                return argument;
            }
        }
        return ILLEGAL_ARGUMENT;
    }


}
