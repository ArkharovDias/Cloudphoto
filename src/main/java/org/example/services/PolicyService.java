package org.example.services;

import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.iam.waiters.IamWaiter;

import java.util.Optional;

public class PolicyService {
    public static final String PolicyDocument =
            "{" +
                    "  \"Version\": \"2012-10-17\"," +
                    "  \"Statement\": [" +
                    "    {" +
                    "        \"Effect\": \"Allow\"," +
                    "        \"Action\": [" +
                    "            \"dynamodb:DeleteItem\"," +
                    "            \"dynamodb:GetItem\"," +
                    "            \"dynamodb:PutItem\"," +
                    "            \"dynamodb:Scan\"," +
                    "            \"dynamodb:UpdateItem\"" +
                    "       ]," +
                    "       \"Resource\": \"*\"" +
                    "    }" +
                    "   ]" +
                    "}";

    private IamClient iamClient;

    public PolicyService(IamClient iamClient) {//region should be Region.AWS_GLOBAL
        this.iamClient = iamClient;
    }

    public Policy createIAMPolicy(String policyName, String policyDocument) {
        ListPoliciesRequest listPoliciesRequest = ListPoliciesRequest.builder().build();
        ListPoliciesResponse listPoliciesResponse = iamClient.listPolicies(listPoliciesRequest);

        Optional<Policy> searchedPolicy = listPoliciesResponse.policies().stream()
                .filter(n -> n.policyName().equals(policyName))
                .findAny();

        Policy resultPolicy = null;

        if (searchedPolicy.isPresent()){
            resultPolicy = searchedPolicy.get();
            System.out.println("Your Policy Name: " + searchedPolicy.get().policyName());
        }else {
            try {
                // Create an IamWaiter object
                IamWaiter iamWaiter = iamClient.waiter();

                CreatePolicyRequest request = CreatePolicyRequest.builder()
                        .policyName(policyName)
                        .policyDocument(policyDocument).build();

                CreatePolicyResponse response = iamClient.createPolicy(request);

                // Wait until the policy is created
                GetPolicyRequest polRequest = GetPolicyRequest.builder()
                        .policyArn(response.policy().arn())
                        .build();

                WaiterResponse<GetPolicyResponse> waitUntilPolicyExists = iamWaiter.waitUntilPolicyExists(polRequest);
                waitUntilPolicyExists.matched().response().ifPresent(System.out::println);
                resultPolicy = waitUntilPolicyExists.matched().response().get().policy();

                System.out.println("Your Policy Name: " + policyName);

            } catch (IamException e) {
                System.err.println(e.awsErrorDetails().errorMessage());
                System.exit(1);
            }
        }
        return resultPolicy;
    }

    public Policy getPolicy(String policyArn){

        Policy searchedPolicy = null;

        try {

            GetPolicyRequest request = GetPolicyRequest.builder()
                    .policyArn(policyArn).build();

            GetPolicyResponse response = iamClient.getPolicy(request);
            searchedPolicy = response.policy();

        } catch (IamException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return searchedPolicy;
    }
}
