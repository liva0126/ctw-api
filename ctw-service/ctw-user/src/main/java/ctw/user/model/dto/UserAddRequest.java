package ctw.user.model.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 用户创建请求
 *
 * @author  liva
 * @date 2023/6/13
 */
@Data
public class UserAddRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String name;

    /**
     * 账号
     */
    private String account;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 用户角色: user, admin
     */
    private String role;

    private static final long serialVersionUID = 1L;
}