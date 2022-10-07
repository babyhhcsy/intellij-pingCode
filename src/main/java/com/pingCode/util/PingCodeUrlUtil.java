package com.pingCode.util;

import com.intellij.openapi.util.text.StringUtil;
import com.pingCode.api.PCRepositoryPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PingCodeUrlUtil {
    private static final String DEFAULT_GITEE_HOST = "open.pingcode.com";

    @NotNull
    public static String removeProtocolPrefix(String url) {
        int index = url.indexOf('@');
        if (index != -1) {
            return url.substring(index + 1).replace(':', '/');
        }
        index = url.indexOf("://");
        if (index != -1) {
            return url.substring(index + 3);
        }
        return url;
    }

    @NotNull
    public static String removeTrailingSlash(@NotNull String s) {
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * Will only work correctly after {@link #removeProtocolPrefix(String)}
     */
    @NotNull
    public static String removePort(@NotNull String url) {
        int index = url.indexOf(':');
        if (index == -1) return url;
        int slashIndex = url.indexOf('/');
        if (slashIndex != -1 && slashIndex < index) return url;

        String beforePort = url.substring(0, index);
        if (slashIndex == -1) {
            return beforePort;
        }
        else {
            return beforePort + url.substring(slashIndex);
        }
    }

    @NotNull
    public static String getApiUrl(@NotNull String urlFromSettings) {
        return getApiProtocolFromUrl(urlFromSettings) + getApiUrlWithoutProtocol(urlFromSettings);
    }

    @NotNull
    public static String getApiProtocolFromUrl(@NotNull String urlFromSettings) {
        if (StringUtil.startsWithIgnoreCase(urlFromSettings.trim(), "http://")){
            return "http://";
        }

        return "https://";
    }

    /**
     * E.g.: https://gitee.com/suffix/ -> gitee.com
     *       gitee.com:8080/ -> gitee.com
     */
    @NotNull
    public static String getHostFromUrl(@NotNull String url) {
        String path = removeProtocolPrefix(url).replace(':', '/');
        int index = path.indexOf('/');
        if (index == -1) {
            return path;
        }
        else {
            return path.substring(0, index);
        }
    }

    @NotNull
    public static String getApiUrlWithoutProtocol(@NotNull String urlFromSettings) {
        String url = removeTrailingSlash(removeProtocolPrefix(urlFromSettings.toLowerCase()));

        final String API_SUFFIX = "/api/v3";

        if (url.equals(DEFAULT_GITEE_HOST)) {
            return url + API_SUFFIX;
        }
        else if (url.equals(DEFAULT_GITEE_HOST + API_SUFFIX)) {
            return url;
        }
        else{
            // have no custom Gitee url yet.
            return DEFAULT_GITEE_HOST + API_SUFFIX;
        }
    }

    public static boolean isGiteeUrl(@NotNull String url, @NotNull String host) {
        host = getHostFromUrl(host);
        url = removeProtocolPrefix(url);
        return StringUtil.startsWithIgnoreCase(url, host)
                && !(url.length() > host.length() && ":/".indexOf(url.charAt(host.length())) == -1);
    }

    /**
     * assumed isGiteeUrl(remoteUrl)
     *
     * git@gitee.com:user/repo.git -> user/repo
     */
    @Nullable
    public static PCRepositoryPath getUserAndRepositoryFromRemoteUrl(@NotNull String remoteUrl) {
        remoteUrl = removeProtocolPrefix(removeEndingDotGit(remoteUrl));
        int index1 = remoteUrl.lastIndexOf('/');
        if (index1 == -1) {
            return null;
        }
        String url = remoteUrl.substring(0, index1);
        int index2 = Math.max(url.lastIndexOf('/'), url.lastIndexOf(':'));
        if (index2 == -1) {
            return null;
        }
        final String username = remoteUrl.substring(index2 + 1, index1);
        final String reponame = remoteUrl.substring(index1 + 1);
        if (username.isEmpty() || reponame.isEmpty()) {
            return null;
        }
        return new PCRepositoryPath(username, reponame);
    }

    @NotNull
    private static String removeEndingDotGit(@NotNull String url) {
        url = removeTrailingSlash(url);
        final String DOT_GIT = ".git";
        if (url.endsWith(DOT_GIT)) {
            return url.substring(0, url.length() - DOT_GIT.length());
        }
        return url;
    }

    @Nullable
    public static String makeGiteeRepoUrlFromRemoteUrl(@NotNull String remoteUrl, @NotNull String host) {
        PCRepositoryPath repo = getUserAndRepositoryFromRemoteUrl(remoteUrl);
        if (repo == null) {
            return null;
        }
        return host + '/' + repo.getOwner() + '/' + repo.getRepository();
    }
}
