package org.example.services;

import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.iam.waiters.IamWaiter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RoleService {

    private IamClient iamClient;

    public RoleService(IamClient iamClient) {
        this.iamClient = iamClient;
    }

    public Role createRole(String roleName, String assumeRolePolicyDocument){

        ListRolesRequest listRolesRequest = ListRolesRequest.builder().build();
        ListRolesResponse listRolesResponse = iamClient.listRoles(listRolesRequest);

        Optional<Role> searchedRole = listRolesResponse.roles().stream()
                .filter(n -> n.roleName().equals(roleName))
                .findAny();

        Role roleResult = null;

        if (searchedRole.isPresent()){
            roleResult = searchedRole.get();
            System.out.println("Your Role Name: " + searchedRole.get().roleName());
        }else {
            try {
                // Create an IamWaiter object
                IamWaiter iamWaiter = iamClient.waiter();

                CreateRoleRequest createRoleRequest = CreateRoleRequest.builder()
                        .roleName(roleName)
                        .assumeRolePolicyDocument(assumeRolePolicyDocument)
                        .build();

                CreateRoleResponse createRoleResponse = iamClient.createRole(createRoleRequest);

                GetRoleRequest getRoleRequest = GetRoleRequest.builder()
                        .roleName(createRoleResponse.role().roleName())
                        .build();

                WaiterResponse<GetRoleResponse> waitUntilRoleExists = iamWaiter.waitUntilRoleExists(getRoleRequest);
                waitUntilRoleExists.matched().response().ifPresent(System.out::println);

                roleResult = waitUntilRoleExists.matched().response().get().role();
                //resultPolicy = response.policy();
                System.out.println("Your Role Name: " + roleName);

            } catch (IamException e) {
                System.err.println(e.awsErrorDetails().errorMessage());
                System.exit(1);
            }
        }


        return roleResult;
        //return  createRoleResponse.role();
    }

    private boolean isRoleExits(String roleName){
        ListRolesRequest listRolesRequest = ListRolesRequest.builder().build();
        ListRolesResponse listRolesResponse = iamClient.listRoles();

        Optional<Role> searchedRole = listRolesResponse.roles().stream()
                .filter(n -> n.equals(roleName))
                .findAny();

        return searchedRole.isPresent();
    }

    public void attachRolePolicy(String roleName, String policyArn){
        AttachRolePolicyRequest attachRolePolicyRequest = AttachRolePolicyRequest.builder()
                .roleName(roleName)
                .policyArn(policyArn)
                .build();

        AttachRolePolicyResponse attachRolePolicyResponse = iamClient.attachRolePolicy(attachRolePolicyRequest);

    }

    public void attachIAMRolePolicy(String roleName, String policyArn ) {

        try {
            IamWaiter waiter = iamClient.waiter();
            List<AttachedPolicy> matchingPolicies = new ArrayList<>();

            boolean done = false;
            String newMarker = null;

            while(!done) {

                ListAttachedRolePoliciesResponse response;

                if (newMarker == null) {
                    ListAttachedRolePoliciesRequest request =
                            ListAttachedRolePoliciesRequest.builder()
                                    .roleName(roleName).build();
                    response = iamClient.listAttachedRolePolicies(request);
                } else {
                    ListAttachedRolePoliciesRequest request =
                            ListAttachedRolePoliciesRequest.builder()
                                    .roleName(roleName)
                                    .marker(newMarker).build();
                    response = iamClient.listAttachedRolePolicies(request);
                }

                matchingPolicies.addAll(
                        response.attachedPolicies()
                                .stream()
                                .filter(p -> p.policyName().equals(roleName))
                                .collect(Collectors.toList()));

                if(!response.isTruncated()) {
                    done = true;

                } else {
                    newMarker = response.marker();
                }
            }

            if (matchingPolicies.size() > 0) {
                System.out.println(roleName +
                        " policy is already attached to this role.");
                return;
            }

            // snippet-start:[iam.java2.attach_role_policy.attach]
            AttachRolePolicyRequest attachRequest =
                    AttachRolePolicyRequest.builder()
                            .roleName(roleName)
                            .policyArn(policyArn).build();

            //iamClient.attachRolePolicy(attachRequest);

            // snippet-end:[iam.java2.attach_role_policy.attach]
            AttachRolePolicyResponse attachRolePolicyResponse = iamClient.attachRolePolicy(attachRequest);
        } catch (IamException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        // snippet-end:[iam.java2.attach_role_policy.main]
    }

    public Role getRole(String roleName){

        Role searchedRole = null;

        try {
            GetRoleRequest getRoleRequest = GetRoleRequest.builder()
                    .roleName(roleName)
                    .build();

            GetRoleResponse getRoleResponse = iamClient.getRole(getRoleRequest);

            searchedRole = getRoleResponse.role();
        }catch (IamException e){
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

        return searchedRole;


        //return iamClient.getRole(getRoleRequest).role();
    }


}
