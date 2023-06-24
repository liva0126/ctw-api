package ctw.user.config;

import ctw.user.interceptor.LoginInterceptor;
import ctw.user.interceptor.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * 配置两个拦截器
 *  1.拦截所有请求，刷新redis中token的ttl
 *  2.拦截所有除登录外的亲戚
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器
        // todo 路径待完善
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/user/code",
                        "/user/login/phone",
                        "/user/login/code/**",
                        "/user/register",
                        "/user/login",
                        // swagger
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources",
                        "/v2/**",
                        "/favicon.ico",
                        "/api/error"
                ).order(1);
        // token刷新的拦截器
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);
    }
}
