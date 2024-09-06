package org.lucien;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 主类
 *
 * @author Lucien
 * @date 2024/9/4 1:05
 */
public class Main {

    private static final Log LOG = LogFactory.get();

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();

    private static final String FOCUS_URL = "https://tieba.baidu.com/mo/q/newmoindex";

    private static final String TBS_URL = "https://tieba.baidu.com/dc/common/tbs";

    private static final String SIGN_URL = "https://c.tieba.baidu.com/c/c/forum/sign";

    private static final int RETRY_TIMES = 2;

    /**
     * 主函数
     *
     * @param args 参数
     */
    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                throw new RuntimeException("Secrets is empty!");
            }
            String cookie = "BDUSS=" + args[0];
            String tbs = getTbs(cookie);
            List<String> focus = getFocus(cookie);
            signIn(cookie, tbs, focus);
        } catch (Exception e) {
            LOG.error(e, "Error!");
        } finally {
            shutdownOkHttpClient();
        }
    }

    /**
     * 关闭okhttp客户端资源
     */
    private static void shutdownOkHttpClient() {
        CLIENT.dispatcher().executorService().shutdown();
        CLIENT.connectionPool().evictAll();
    }

    /**
     * 签到
     *
     * @param cookie cookie
     * @param tbs tbs
     * @param focus 关注列表
     */
    private static void signIn(String cookie, String tbs, List<String> focus) {
        try {
            MD5 md5 = MD5.create();
            List<String> success = new ArrayList<>();
            for (int i = 0; i < RETRY_TIMES; i++) {
                for (String tiebaName : focus) {
                    RequestBody requestBody = new FormBody.Builder()
                            .add("kw", tiebaName)
                            .add("tbs", tbs)
                            .add("sign", md5.digestHex("kw=" + tiebaName + "tbs=" + tbs + "tiebaclient!!!"))
                            .build();
                    String responseStr = request(SIGN_URL, "POST", requestBody, cookie);
                    JSONObject responseObject = JSONUtil.parseObj(responseStr);
                    if ("0".equals(responseObject.getStr("error_code"))) {
                        success.add(tiebaName);
                    }
                    Thread.sleep(RandomUtil.randomInt(150, 500));
                }
                focus.removeAll(success);
                if (focus.isEmpty()) {
                    break;
                }
            }
            LOG.info("签到成功的贴吧：" + (success.isEmpty() ? "无" : String.join(",", success)));
            LOG.info("签到失败的贴吧：" + (focus.isEmpty() ? "无" : String.join(",", focus)));
        } catch (Exception e) {
            throw new RuntimeException("Sign in error!", e);
        }
    }

    /**
     * 获取tbs
     *
     * @param cookie cookie
     * @return tbs
     */
    private static String getTbs(String cookie) {
        try {
            String responseStr = request(TBS_URL, "GET", null, cookie);
            JSONObject responseObject = JSONUtil.parseObj(responseStr);
            if ("1".equals(responseObject.getStr("is_login"))) {
                return responseObject.getStr("tbs");
            } else {
                throw new RuntimeException(responseStr);
            }
        } catch (Exception e) {
            throw new RuntimeException("Get tbs error!", e);
        }
    }

    /**
     * 获取关注列表
     *
     * @param cookie cookie
     * @return 关注列表
     */
    private static List<String> getFocus(String cookie) {
        try {
            String responseStr = request(FOCUS_URL, "GET", null, cookie);
            JSONObject responseObject = JSONUtil.parseObj(responseStr);
            JSONArray likeForums = responseObject.getJSONObject("data").getJSONArray("like_forum");
            List<String> focus = new ArrayList<>();
            for (Object o : likeForums) {
                JSONObject likeForum = (JSONObject) o;
                if ("0".equals(likeForum.getStr("is_sign"))) {
                    String tiebaName = likeForum.getStr("forum_name");
                    focus.add(tiebaName);
                }
            }
            return focus;
        } catch (Exception e) {
            throw new RuntimeException("Get focus error!", e);
        }
    }

    /**
     * HTTP请求方法
     *
     * @param url         请求地址
     * @param method      请求方法
     * @param requestBody 请求体
     * @param cookie      cookie
     * @return 响应字符串
     */
    public static String request(String url, String method, RequestBody requestBody, String cookie) throws IOException {
        String responseString = "";
        Request request = new Request.Builder()
                .url(url)
                .header("Cookie", cookie)
                .method(method, requestBody)
                .build();
        try (Response response = CLIENT.newCall(request).execute();
             ResponseBody responseBody = response.body()) {
            if (responseBody != null) {
                responseString = responseBody.string();
            }
        }
        return responseString;
    }
}