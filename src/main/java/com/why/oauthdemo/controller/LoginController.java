package com.why.oauthdemo.controller;

import com.alibaba.fastjson.JSONObject;
import com.why.oauthdemo.bean.Oauth2Properties;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Controller
public class LoginController {
    private final Oauth2Properties oauth2Properties;

    public LoginController(Oauth2Properties oauth2Properties){
        this.oauth2Properties = oauth2Properties;
    }

    /**
     * 让用户跳转到 Github
     * 这里不能加 @ResponseBody，因为这里是要跳转而不是返回响应
     * 所以 LoginController 只能用 @Controller 而不能用 @RestController
     * @return 跳转URL
     */
    @GetMapping("/authorize")
    public String authorize(){
        String url = oauth2Properties.getAuthorizeUrl() +
                "?client_id=" + oauth2Properties.getClientId() +
                "&redirect_uri=" + oauth2Properties.getRedirectUrl();
        log.info("授权url:{}",url);
        return "redirect:" + url;
    }

    /**
     * 回调接口，用户同意授权后，Github 会重定向到此路径
     * @param code Github重定向时附加的授权码，只能用一次
     * @return
     */
    @GetMapping("/oauth/redirect")
    public String callback(@RequestParam("code")String code){
        log.info("code:{}", code);
        // code 换 token
        String accessToken = getAccessToken(code);
        // token 换 userInfo
        String userInfo = getUserInfo(accessToken);
        log.info("重定向到home");
        return "redirect:/home";
    }

    /**
     * 获取 access_token 后的跳转
     * 现实中我们可以把获取后的数据拿过来用
     * @return
     */
    @GetMapping("/home")
    @ResponseBody
    public String home(){
        return "hello world";
    }

    /**
     * 根据 code 获取 AccessToken
     * @param code
     * @return
     */
    public String getAccessToken(String code){
        String url = oauth2Properties.getAccessTokenUrl() +
                "?client_id=" + oauth2Properties.getClientId() +
                "&client_secret=" + oauth2Properties.getClientSecret() +
                "&code=" + code +
                "&grant_type=authorization_code";

        log.info("getAccessToken url:{}", url);
        // 构建请求头
        HttpHeaders requestHeaders = new HttpHeaders();
        // 指定响应返回的 json 格式
        requestHeaders.add("accept", "application/json");
        // 构建请求实体
        HttpEntity<String> requestEntity = new HttpEntity<>(requestHeaders);
        RestTemplate restTemplate = new RestTemplate();
        // post 请求方式
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        String responseStr = response.getBody();
        log.info("responseStr={}", responseStr);

        // 解析响应 json 字符串
        JSONObject jsonObject = JSONObject.parseObject(responseStr);
        String accessToken = jsonObject.getString("access_token");
        log.info("accessToken={}", accessToken);
        return accessToken;
    }

    /**
     * 根据 AccessToken 获取用户信息
     * @param accessToken
     * @return
     */
    private String getUserInfo(String accessToken) {
        String url = oauth2Properties.getUserInfoUrl();
        log.info("getUserInfo url:{}", url);
        // 构建请求头
        HttpHeaders requestHeaders = new HttpHeaders();
        // 指定响应返回 json 格式
        requestHeaders.add("accept","application/json");
        // AccessToken 放在请求头中
        requestHeaders.add("Authorization", "token " + accessToken);
        // 构建请求实体
        HttpEntity<String> requestEntity = new HttpEntity<>(requestHeaders);
        RestTemplate restTemplate = new RestTemplate();

        // get 请求方式
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        String userInfo = response.getBody();
        log.info("userInfo={}", userInfo);
        return userInfo;
    }
}
