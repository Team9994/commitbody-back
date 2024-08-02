package team9499.commitbody.global.authorization.service;


import team9499.commitbody.domain.Member.domain.LoginType;

import java.util.Map;

public interface AuthorizationService {

    Map<String,String> authenticateOrRegisterUser(LoginType loginType,String socialJwt);
}
