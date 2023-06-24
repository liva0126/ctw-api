package ctw.base.utils;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
 
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
/**
 * 获取 HttpServletRequest
 * @author liva
 * @date 2023/6/23
 */
public class RequestHolder {
 
    public static HttpServletRequest getRequest(){
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return req;
    }
 
    public static HttpServletResponse getResponse(){
        HttpServletResponse resp = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        return resp;
    }
}