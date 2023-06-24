package ctw.user.model.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 用户更新请求
 *
 * @author  liva
 * @date 2023/6/13
 */
@Data
public class UserUpdateRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String name;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 简介
     */
    private String profile;

    /**
     * 用户角色：user/admin/ban
     */
    private String role;

    private static final long serialVersionUID = 1L;
}