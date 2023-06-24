package ctw.user.service.impl;
import java.util.Date;

import static ctw.base.constant.RedisConstants.*;
import static ctw.base.constant.UserConstant.*;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ctw.base.common.ErrorCode;
import ctw.base.common.IpUtils;
import ctw.base.constant.CommonConstant;
import ctw.base.exception.BusinessException;
import ctw.base.utils.RedisCacheClient;
import ctw.base.utils.RegexUtils;
import ctw.user.dao.userDao;
import ctw.user.model.dto.UserDTO;
import ctw.user.model.dto.UserLoginRequest;
import ctw.user.model.dto.UserQueryRequest;
import ctw.user.model.entity.User;
import ctw.user.model.enums.UserRoleEnum;
import ctw.user.model.mapper.UserMapper;
import ctw.user.model.vo.LoginUserVO;
import ctw.user.model.vo.UserVO;
import ctw.user.service.AvatarService;
import ctw.user.service.UserService;
import ctw.base.utils.SqlUtils;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * 用户服务实现
 *
 * @author  liva
 * @date 2023/6/13
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private userDao userDao;

    @Resource
    private RedisCacheClient redisCacheClient;

    @Resource
    private AvatarService avatarService;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "ctw";

    @Override
    public long userRegister(String account, String password, String checkPassword, Integer gender) {
        // 1. 校验
        if (StringUtils.isAnyBlank(account, password, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 账户必须是邮箱格式或者手机号
        if (RegexUtils.isEmailInvalid(account) && RegexUtils.isPhoneInvalid(account)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号格式错误");
        }
        // 密码必须在8位以上
        if (password.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码必须同时包含数字和字母
        if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*[0-9].*")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码必须同时包含数字和字母");
        }
        // 密码和校验密码相同
        if (!password.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (account.intern()) {
            // 账户不能重复
            User userFromDb = userDao.findUserByAccount(account);
            if (userFromDb != null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密 todo 后期更换加密方式
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
            // 3. 初始化新用户数据
            User user = initUserByAccount(account,encryptPassword,gender);
            // 4. 插入到数据库中
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    /**
     * 初始化用户信息
     * @param account 邮箱或者手机号
     */
    private User initUserByAccount(String account,String encryptPassword,Integer gender) {
        User user = new User();
        if (account.contains("@")){
            // 邮箱注册
            user.setAccount(account);
        } else {
            // 手机号注册
            user.setPhone(account);
        }
        user.setPassword(encryptPassword);
        user.setGender(gender);
        user.setUser_level(1);
        user.setIs_vip(0);
        user.setVip_level(0);
        user.setName("创客#" + RandomUtil.randomNumbers(6));
        user.setAvatar(avatarService.getDefaultAvatar(gender));
        user.setRole("user");
        user.setCreate_ip(IpUtils.getIpAddr());
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setIsDelete(0);
        return user;
    }

    @Override
    public LoginUserVO userLoginByPassword(String account, String password, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(account, password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 账户必须是邮箱或者手机号
        if (RegexUtils.isEmailInvalid(account) && RegexUtils.isPhoneInvalid(account)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号格式错误");
        }
        if (password.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        // 查询用户是否存在
        User user = new User();
        if (account.contains("@")){
            // 邮箱登录
            user = userDao.findUserByAccountAndPassword(account, encryptPassword);
        } else {
            // 密码登录
            user = userDao.findUserByPhoneAndPassword(account, encryptPassword);
        }
        // 用户不存在
        if (user == null) {
            log.info("user login failed, account cannot match password");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString();
        redisCacheClient.setWithTtl(user.getId(), user, 2, TimeUnit.HOURS);
        return this.getLoginUserVO(user);
    }

    @Override
    public String userLoginByPhone(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 校验手机号
        String phone = userLoginRequest.getAccount();
        if (RegexUtils.isPhoneInvalid(phone)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式错误");
        }
        // 校验验证码，从redis中获取  todo 获取redis验证码为空  2023/6/23
        String cacheCode = redisCacheClient.get(LOGIN_CODE_KEY + phone);
        String code = userLoginRequest.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            // 不一致，报错
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
        // 查询数据库手机号是否存在
        User user = userDao.findUserByPhone(phone);
        // 如果不存在则注册用户再返回user
        if (user == null){
            user = CreateUserWithPhone(phone);
        }
        // 生成token并保存登录状态
        String token = genTokenAndSaveLoginStatus(user);
        // 返回token
        return token;
    }

    @Override
    public String sendCode(String phone) {
        // 获取用户的ip地址
        String ip = IpUtils.getIpAddr();
        // 获取用户最近一次请求的时间戳
        String lastSendTime = redisCacheClient.get(CODE_USER_IP_KEY + ip);
        // 如果距离现在时间小于60秒，则不允许发送
        if (lastSendTime != null && System.currentTimeMillis() - Long.parseLong(lastSendTime) < 60 * 1000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求过于频繁");
        }
        // 生成6位随机验证码,todo 对接到短信服务商
        String code = RandomUtil.randomNumbers(6);
        // 保存验证码到redis，ttl 5 分钟
        redisCacheClient.setWithTtl(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 将用户ip存入redis，设置过期时间为60秒
        redisCacheClient.setWithTtl(CODE_USER_IP_KEY + ip, System.currentTimeMillis() + "", 60, TimeUnit.SECONDS);
        // 发送验证码
        log.info("发送验证码成功: {}",code);
        return code;
    }

    private User CreateUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setName("创客#" + RandomUtil.randomNumbers(6));
        user.setRole("user");
        user.setCreate_ip(IpUtils.getIpAddr());
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        save(user);
        return user;
    }

    /**
     * 生成token并保存登录状态
     * @param user
     * @return
     */
    private String genTokenAndSaveLoginStatus(User user) {
        // 用户脱敏：复制属性，可以第二个参数可以传入一个new出来的对象，也可以传入一个字节码文件
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 存储
        // 因为存储Hash格式，所以需要将user类型转换为map,
        // 设置CopyOptions是因为如果转换的user里有long，long类型是无法转换为string的，而这里使用的是StringRedisTemplate
        // 如果转换的user里没有long类型的话，则可以不用传入第二和第三个参数。
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

        // 随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString();
        // 保存用户信息到redis中
        String tokenKey = LOGIN_USER_KEY + token;
        redisCacheClient.setHashWithMapAndTtl(tokenKey, userMap,2,TimeUnit.HOURS);
        // 返回token
        return token;
    }

    @Override
    public LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request) {
        String unionId = wxOAuth2UserInfo.getUnionId();
        String mpOpenId = wxOAuth2UserInfo.getOpenid();
        // 单机锁
        synchronized (unionId.intern()) {
            // 查询用户是否已存在
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("unionId", unionId);
            User user = this.getOne(queryWrapper);
            // 被封号，禁止登录
            if (user != null && UserRoleEnum.BAN.getValue().equals(user.getRole())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "该用户已被封，禁止登录");
            }
            // 用户不存在则创建
            if (user == null) {
                user = new User();
                user.setUnionId(unionId);
                user.setMpOpenId(mpOpenId);
                user.setAccount(wxOAuth2UserInfo.getHeadImgUrl());
                user.setName(wxOAuth2UserInfo.getNickname());
                boolean result = this.save(user);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败");
                }
            }
            // 记录用户的登录态
            request.getSession().setAttribute(USER_LOGIN_STATE, user);
            return getLoginUserVO(user);
        }
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getName();
        String userProfile = userQueryRequest.getProfile();
        String userRole = userQueryRequest.getRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
