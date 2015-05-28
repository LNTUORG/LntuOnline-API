/*
 * Copyright (C) 2015-2016 LNTU.ORG (http://lntu.org)
 * Copyright (C) 2014-2015 TakWolf <takwolf@foxmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package com.lntu.online.server.resource;

import com.lntu.online.server.capture.CookieShip;
import com.lntu.online.server.dao.User;
import com.lntu.online.server.exception.ArgsErrorException;
import com.lntu.online.server.exception.PasswordErrorException;
import com.lntu.online.server.exception.RemoteAuthorizedException;
import com.lntu.online.server.model.LoginInfo;
import com.lntu.online.server.model.UserType;
import com.lntu.online.server.util.TextUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Date;

@Path("account")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class AccountResource {

    @POST
    @Path("login")
    public LoginInfo login(
            @FormParam("userId") String userId,
            @FormParam("password") String password
    ) {
        // 判断参数
        if (TextUtils.isEmpty(userId)) {
            throw new ArgsErrorException("UserId should not be null.");
        }
        if (TextUtils.isEmpty(password)) {
            throw new ArgsErrorException("Password should not be null.");
        }
        // 检测是否为管理员
        User user = User.dao.findByIdWithUserTypeAdmin(userId);
        if (user != null) {
            if (!user.verifyPassword(password)) { //密码不正确
                throw new PasswordErrorException();
            }
        } else { // 非管理员用户，则远程验证普通用户
            try {
                CookieShip.value(userId, password);
            } catch (RemoteAuthorizedException e) {
                throw new PasswordErrorException();
            }
            // 远程认证成功
            user = User.dao.findById(userId);
            if (user == null) { // 用户首次创建
                user = new User();
                user.setId(userId);
                user.setType(userId.length() == 10 ? UserType.STUDENT : UserType.TEACHER);
                user.setCreateAt(new Date());
                user.save();
            }
            user.setPassword(password); //更新密码
        }
        // 生成LoginInfo
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setUserId(user.getId());
        loginInfo.setUserType(user.getType());
        loginInfo.setLoginToken(user.renewLoginToken()); // 这一步会更新LoginToken
        loginInfo.setExpiresAt(user.getExpiresAt());
        // 保存user并返还loginToken
        user.update();
        return loginInfo;
    }

}