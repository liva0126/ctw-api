package ctw.user.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账号，默认为邮箱
     */
    private String account;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 密码
     */
    private String password;

    /**
     * 性别。0-女，1-男
     */
    private Integer gender;

    /**
     * 国家
     */
    private String country;

    /**
     * 省/州
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 用户等级
     */
    private Integer user_level;

    /**
     * 用户状态
     */
    private Integer user_status;

    /**
     * 是否为vip
     */
    private Integer is_vip;

    /**
     * vip等级。0-非vip 1-普通 2-进阶 3-畅享 4-已过期
     */
    private Integer vip_level;

    /**
     * vip过期日期
     */
    private Date vip_exceed_day;

    /**
     * 用户昵称
     */
    private String name;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 用户简介/个性签名
     */
    private String profile;

    /**
     * 用户角色：user/admin/ban
     */
    private String role;

    /**
     * 微信开放平台id
     */
    private String unionId;

    /**
     * 公众号openId
     */
    private String mpOpenId;

    /**
     * 创建ip
     */
    private String create_ip;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}