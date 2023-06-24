package ctw.user.model.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 用户登录请求
 *
 * @author  liva
 * @date 2023/6/13
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String account;
    private String password;
    private String code;
}
