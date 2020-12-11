package org.example.services;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.lambda.waiters.LambdaWaiter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;

public class LambdaService {

    private LambdaClient lambdaClient;

    private LambdaService(){}

    public LambdaService(LambdaClient lambdaClient){
        /*lambdaClient = LambdaClient.builder()
                .region(region)
                .build();*/

        this.lambdaClient = lambdaClient;
    }

    public String createFunction(String functionName, String filePath, String role, String handler, String bucketName, IOService ioService) {

        ListFunctionsRequest listFunctionsRequest = ListFunctionsRequest.builder().build();
        ListFunctionsResponse listFunctionsResponse = lambdaClient.listFunctions(listFunctionsRequest);

        Optional<FunctionConfiguration> searchedFunctions = listFunctionsResponse.functions().stream()
                .filter(n -> n.functionName().equals(functionName))
                .findAny();

        String resultFunctionArn = "";

        if (!searchedFunctions.isPresent()){
            int attempts = 0;
            int maxAttempts = 3;
            int delay = 5000;
            boolean flag = true;

            while (flag & attempts < maxAttempts){
                try {
                    Thread.sleep(delay);
                    //InputStream is = new FileInputStream(filePath);
                    InputStream is = ioService.getInputStream(filePath);
                    SdkBytes fileToUpload = SdkBytes.fromInputStream(is);

                    FunctionCode code = FunctionCode.builder()
                            .zipFile(fileToUpload)
                            .build();

                    CreateFunctionRequest functionRequest = CreateFunctionRequest.builder()
                            .functionName(functionName)
                            .description("Created by the Lambda Java API")
                            .code(code)
                            .handler(handler)
                            .runtime(Runtime.JAVA8)
                            .role(role)
                            .memorySize(300)
                            .timeout(60)
                            .build();

                    CreateFunctionResponse functionResponse = lambdaClient.createFunction(functionRequest);
                    addPermission(functionName, bucketName);
                    resultFunctionArn = functionResponse.functionArn();
                    System.out.println("Your Lambda name: " + functionResponse.functionName());
                    flag = false;
                } catch(LambdaException lambdaException) {
                    System.err.println(lambdaException.getMessage());
                    //System.exit(1);
                    attempts++;
                /*}catch (FileNotFoundException fileNotFoundException){
                    System.out.println(fileNotFoundException.getMessage());
                    flag = false;*/
                }catch (InterruptedException e) {
                    flag = false;
                    e.printStackTrace();
                }
            }
        }else {
            resultFunctionArn = searchedFunctions.get().functionArn();
            System.out.println("Your Lambda name: " + searchedFunctions.get().functionName());
        }

        return resultFunctionArn;
    }

    public void addPermission(String functionName, String sourceArn){
        lambdaClient.addPermission(AddPermissionRequest.builder()
                .functionName(functionName)
                .principal("s3.amazonaws.com")
                .statementId("s3invoke")
                .action("lambda:InvokeFunction")
                .sourceArn("arn:aws:s3:::" + sourceArn)//arn:aws:s3:::sourcebucket
                //.sourceAccount("260714947828")//account id
                .build());


        //lambdaClient.addPermission(addPermissionRequest);
    }

}
