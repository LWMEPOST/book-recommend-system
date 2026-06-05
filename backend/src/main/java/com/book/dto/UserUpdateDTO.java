package com.book.dto;

import javax.validation.constraints.Size;

public class UserUpdateDTO {

    private String nickname;
    private String email;
    private String phone;
    private String avatar;

    public UserUpdateDTO() {
    }

    public UserUpdateDTO(String nickname, String email, String phone, String avatar) {
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
        this.avatar = avatar;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
