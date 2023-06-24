package ctw.user.model.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 用户注册请求体
 *
 * @author  liva
 * @date 2023/6/13
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String account;

    private String password;

    private String checkPassword;

    private Integer gender;
}
