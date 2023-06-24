package ctw.base.constant;

public class RedisConstants {

    // 发送验证码ip记录前缀
    public static final String CODE_USER_IP_KEY = "code:ip:";

    // 登录验证码前缀
    public static final String LOGIN_CODE_KEY = "login:code:";
    // 登录验证码过期时间，单位：分钟
    public static final int LOGIN_CODE_TTL = 5;
    // 登录用户token前缀
    public static final String LOGIN_USER_KEY = "login:token:";
    // 登录用户token过期时间，单位：秒
    public static final int LOGIN_USER_TTL = 36000;

    public static final int CACHE_NULL_TTL = 2;
    public static final int CACHE_SHOP_TTL = 30;
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final String CACHE_TYPELIST = "catch:typelist:";
    public static final int LOCK_SHOP_TTL = 10;

    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feeds:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";

    public static final String FOLLOW_PREFIX = "follow:";
    public static final Long PAGESIZE = 3L;
}