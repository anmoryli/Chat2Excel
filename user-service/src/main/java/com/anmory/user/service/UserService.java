package com.anmory.user.service;

import com.anmory.user.dto.request.AuthRequest;
import com.anmory.user.dto.request.ChangePasswordRequest;
import com.anmory.user.dto.response.AuthResponse;
import com.anmory.user.dto.response.ChangePasswordResponse;
import com.anmory.user.dto.response.UserInfoResponse;

/**
 * 用户服务接口
 */
public interface UserService {
    public AuthResponse auth(AuthRequest request);

    UserInfoResponse getUserInfo(String authorization);

    ChangePasswordResponse changePassword(ChangePasswordRequest request, String authorization);

    void logout(String authorization);
}
