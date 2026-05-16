package com.bookcollection.dto;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private String bio;
    private Integer role;
    private String token;
}
