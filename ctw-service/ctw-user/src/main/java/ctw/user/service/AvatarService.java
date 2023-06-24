package ctw.user.service;

import org.springframework.stereotype.Service;

/**
 * 头像服务类
 * @Author liva
 * @Date 2023/6/24

 */
@Service
public class AvatarService {

    public static final String girl1 = "http://101.43.93.24:9000/ctw-ai/common_avatar/girl1.png";
    public static final String girl2 = "http://101.43.93.24:9000/ctw-ai/common_avatar/girl2.png";
    public static final String boy1 = "http://101.43.93.24:9000/ctw-ai/common_avatar/boy1.png";
    public static final String boy2 = "http://101.43.93.24:9000/ctw-ai/common_avatar/boy2.png";

    /**
     * 获取默认头像
     * @return
     */
    public String getDefaultAvatar(int gender) {

        if (gender == 0){
            // 如果当前时间是偶数，则返回url1，否则返回url2
            if (System.currentTimeMillis() % 2 == 0) {
                return girl1;
            }else {
                return girl2;
            }
        }else {
            // 如果当前时间是偶数，则返回url1，否则返回url2
            if (System.currentTimeMillis() % 2 == 0) {
                return boy1;
            }else {
                return boy2;
            }
        }
    }
}
