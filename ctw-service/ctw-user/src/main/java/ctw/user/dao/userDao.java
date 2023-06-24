package ctw.user.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import ctw.user.model.entity.User;
import ctw.user.model.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

@Repository
public class userDao {

    @Resource
    private UserMapper userMapper;

    public User findUserByAccount(String account){
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getAccount,account);
        return userMapper.selectOne(wrapper);
    }

    public User findUserByAccountAndPassword(String account, String password){
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getAccount,account);
        wrapper.eq(User::getPassword,password);
        return userMapper.selectOne(wrapper);
    }

    public User findUserByPhoneAndPassword(String phone, String encryptPassword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone,phone);
        wrapper.eq(User::getPassword,encryptPassword);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 根据手机号查询用户
     */
    public User findUserByPhone(String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        return userMapper.selectOne(wrapper);
    }
}
