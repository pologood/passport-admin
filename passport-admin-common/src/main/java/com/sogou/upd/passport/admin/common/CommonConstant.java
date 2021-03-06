package com.sogou.upd.passport.admin.common;

/**
 * 常量基类
 * User: chengang
 * Date: 14-6-26
 * Time: 下午3:07
 */
public final class CommonConstant {

    /**
     * 解除绑定成功
     */
    public static final String UN_BIND_SUCCESS = "解绑成功";

    /**
     * 解除绑定失败
     */
    public static final String UN_BIND_FAILURE = "解绑失败";

    /**
     * 不予解绑
     */
    public static final String UN_DO_BIND = "不予解绑";


    /**
     * 手机、外域邮箱账号 写分离完成前 暂不支持重置密码
     */
    public static final String UN_DO_RESET_PWD = "手机、外域账号类型暂不支持重置密码";

    /**
     * souhu域账号，重置密码，需要到搜狐passport申请重置
     */
    public static final String NOT_HANDLED_REST_PWD_FOR_SOHU = "搜狐域账号不支持重置密码，请到搜狐passport申请重置";


    /**
     * 完成批量解绑手机操作
     */
    public static final String DONE_UN_BIND_MOBILE = "操作完成";


    /**
     * 重置密码成功
     */
    public static final String RESET_PWD_SUCCESS = "重置密码成功";

    /**
     * 重置密码失败
     */
    public static final String RESET_PWD_FAILURE = "重置密码失败";


    /**
     * 字符分割
     */
    public static final String COMMON_STRING_SPLIT = ",";

    /**
     * 回车换行 分隔符
     */
    public static final String COMMON_STRING_SPLIT_RETURN = "\r\n";

    /**
     * 不具备操作权限
     */
    public static final String NO_OPERATE_POWER = "不具备操作权限";

    /**
     * 黑名单redis key
     */
    public static final String CACHE_KEY_BLACKLIST = "BLACKLIST";

    /**
     * 账号信息不存在
     */
    public static final String ACCOUNT_NONEXISTANT = "用户账号信息不存在";

    /**
     * 账号信息不在列表中
     */
    public static final String ACCOUNT_NOTIN_RESTRICTION = "用户账号信息不在限制列表中";

    /**
     * 账号信息解除限制成功
     */
    public static final String ACCOUNT_UNFREEZED = "用户账号信息成功解除限制";

    /**
     * Redis 中泄露账号信息的key前缀
     */
    public static final String LEAK_PREKEY = "SP.PASSPORTID:SOGOULEAKLIST_";

}
