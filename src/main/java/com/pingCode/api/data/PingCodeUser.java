/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.pingCode.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.mandatory.RestModel;

import java.util.Objects;

@RestModel
@SuppressWarnings("UnusedDeclaration")
public class PingCodeUser {
  @NotNull public static final PingCodeUser UNKNOWN = createUnknownUser();

  private Long id;
  private String login;

  // https://gitee.com/api/v5/user/[name]
  private String url;
  // https://gitee.com/[zyuyou]
  private String htmlUrl;

  private String avatarUrl;

  private String type;
  private Boolean siteAdmin;



  @NotNull
  public String getNodeId() {
    return "nodeId";
  }

  @NotNull
  public String getLogin() {
    return login;
  }

  @NotNull
  public String getHtmlUrl() {
    return htmlUrl;
  }

  @Nullable
  public String getAvatarUrl() {
    return avatarUrl;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof PingCodeUser)) return false;

    PingCodeUser user = (PingCodeUser) o;
    return id.equals(user.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @NotNull
  private static PingCodeUser createUnknownUser() {
    PingCodeUser user = new PingCodeUser();
    user.id = -1L;
    user.login = "ghost";
    return user;
  }
}
