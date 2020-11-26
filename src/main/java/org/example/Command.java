package org.example;

import java.util.HashMap;
import java.util.Map;

public class Command {
    private CommandType commandType;
    private Map<Argument, String> parameters;

    public Command(CommandType commandType){
        parameters = new HashMap<>();
        this.commandType = commandType;
    }

    public void setParameter(String key, String value){
        Argument argument = Argument.getArgument(key);
        if (!argument.equals(Argument.ILLEGAL_ARGUMENT)){
            parameters.put(argument , value);
        }

    }

    public CommandType getCommandType() {
        return commandType;
    }

    public String getParameterValue(Argument key){
        return parameters.get(key);
    }

    public Map<Argument, String> getParameters(){
        return parameters;
    }
}
