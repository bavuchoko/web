package com.pjs.web.common;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

@Component
public class WebCommon {


    /**
    * 기존에는 request.getRemoteAddr(); 를 통해 Client의 IP를 가져올 수 있는데 웹서버나 WAS 앞에 L4와 같은 Load balancers나 Proxy server, caching server등이 있는 경우는 원하는 결과를 얻지 못한다.
     * 이런 경우 웹서버 혹은 WAS에 HTTP나 AJP 요청을 보낸 후 받은 결과를 가공하여 클라이언트에 재전송하기 때문에 위와 같은 현상이 발생한다. ( 클라이언트  IP -> 로드밸런서, 프록시 장비 -> 웹서버 )
     * 그래서 등장한 것이 XFF(X-Forwarded-For).
     *
     *  XFF 에는 Client와 Proxy IP 가 콤마를 구분자로 들어가게 되어있는데 순서는 아래와 같기 때문에 첫번째 IP를 가져오면 Client의 IP를 획득할 수 있다.
     *      X-Forwarded-For: client, proxy1, proxy2
     *
     * XFF는 표준이지만 정식 RFC에 포함된게 아니라 모든 제품이 XFF 헤더를 사용하는 것이 아니다.
     * 예를 들어 WebLogic Connector(mod_wl) 는 XFF 헤더를 사용하지 않고 WL-Proxy-Client-IP 나 Proxy-Client-IP 헤더를 사용한다.
     * 따라서 제품의 종류에 영향을 받지 않고 Client IP를 가져오고자 한다면 아래와 같이 작성해야 한다
     *
     * localhost에서 테스트 하는 경우 0:0:0:0:0:0:0:1 값으로 넘어오는 경우 이 값은  IPv6 에서 IPv4의 127.0.0.1 과 같은 값임.
     * Tomcat으로 개발시 문제가 되는 경우 vs arguments에 -Djava.net.preferIPv4Stack=true 값을 넣어준다.
    * */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if(!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)){
            ip = request.getHeader("Proxy-Client-IP");
        }
        if(!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)){
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if(!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)){
            ip = request.getHeader("HTTP_CLIENT_IP");
        }if(!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)){
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if(!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)){
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
