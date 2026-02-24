package com.example.hcc.cognito;

import com.example.hcc.enums.Role;
import com.medryte.CognitoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupType;

import java.util.List;


@Service
public class CognitoService {

    @Autowired
	private CognitoUtils cognitoUtils;

    private final String userPoolId = "us-east-1_LN4I1DaPa";

    public String adminCreateUser(AdminCreateUserRequestModel addUserRequestModel){

        // Create User
        AdminCreateUserResponse adminCreateUserResponse = cognitoUtils.adminCreateUser(userPoolId, addUserRequestModel.getEmail(), addUserRequestModel.getPassword());

        String cognitoId = adminCreateUserResponse.user().username();

        // Add user to specified groups
        adminAddUserToAllGroups(addUserRequestModel.getEmail(), addUserRequestModel.getGroups());

        // Mark user's email as verified
        cognitoUtils.adminUpdateUserAttributes(userPoolId, addUserRequestModel.getEmail(), "email_verified", "true");

        return cognitoId;

    }

    public void adminUpdateGroupsForUser(String username, List<Role> newGroups){
        
        List<Role> currentGroups = adminListGroupsForUser(username);

        adminRemoveUserFromAllGroups(username, currentGroups);

        adminAddUserToAllGroups(username, newGroups);

    }

    public void adminAddUserToAllGroups(String username, List<Role> groups) {
        for(Role group: groups){
            cognitoUtils.adminAddUserToGroup(userPoolId, username, group.name());
        }
    }

    public void adminRemoveUserFromAllGroups(String username, List<Role> groups){

        for(Role group: groups){
            cognitoUtils.adminRemoveUserFromGroup(userPoolId, username, group.name());
        }

    }

    public List<Role> adminListGroupsForUser(String username){

        AdminListGroupsForUserResponse adminListGroupsForUserResponse
            = cognitoUtils.adminListGroupsForUser(userPoolId, username);

        List<GroupType> groups = adminListGroupsForUserResponse.groups();

        return groups.stream().map(item -> Role.valueOf(item.groupName())).toList();

    }

    public void adminDeleteUser(AdminDeleteUserRequestModel adminDeleteUserRequestModel){

        cognitoUtils.adminDeleteUser(userPoolId, adminDeleteUserRequestModel.getEmail());

    }

    public void adminResetPassword(AdminResetUserPasswordRequestModel adminResetUserPasswordRequestModel) {
        
        cognitoUtils.adminResetUserPassword(
            userPoolId, 
            adminResetUserPasswordRequestModel.getEmail(), 
            adminResetUserPasswordRequestModel.getPassword(), 
            false
        );

    }
    
}
