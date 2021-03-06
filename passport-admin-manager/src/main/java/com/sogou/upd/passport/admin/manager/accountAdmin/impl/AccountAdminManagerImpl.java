package com.sogou.upd.passport.admin.manager.accountAdmin.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sogou.upd.passport.admin.common.CommonConstant;
import com.sogou.upd.passport.admin.common.enums.AccountTypeMappingEnum;
import com.sogou.upd.passport.admin.common.utils.MessageUtil;
import com.sogou.upd.passport.admin.common.utils.UuidUtil;
import com.sogou.upd.passport.admin.manager.accountAdmin.AccountAdminManager;
import com.sogou.upd.passport.admin.manager.form.AccountSearchParam;
import com.sogou.upd.passport.admin.manager.model.AccountDetailInfo;
import com.sogou.upd.passport.common.CacheConstant;
import com.sogou.upd.passport.common.lang.StringUtil;
import com.sogou.upd.passport.common.parameter.AccountDomainEnum;
import com.sogou.upd.passport.common.parameter.AccountStatusEnum;
import com.sogou.upd.passport.common.parameter.OperateLogEnum;
import com.sogou.upd.passport.common.result.APIResultSupport;
import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.common.utils.*;
import com.sogou.upd.passport.model.account.Account;
import com.sogou.upd.passport.model.account.AccountInfo;
import com.sogou.upd.passport.model.operatelog.OperateHistoryLog;
import com.sogou.upd.passport.service.OperateHistoryLog.OperateHistoryLogService;
import com.sogou.upd.passport.service.account.AccountInfoService;
import com.sogou.upd.passport.service.account.AccountService;
import com.sogou.upd.passport.service.account.MobilePassportMappingService;
import com.sogou.upd.passport.service.account.UniqNamePassportMappingService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: chenjiameng
 * Date: 13-6-14
 * Time: 下午2:59
 * To change this template use File | Settings | File Templates.
 */
@Component
public class AccountAdminManagerImpl implements AccountAdminManager {

    private static final Logger logger = LoggerFactory.getLogger(AccountAdminManagerImpl.class);

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountInfoService accountInfoService;

    @Autowired
    private MobilePassportMappingService mobilePassportMappingService;


    @Autowired
    private UniqNamePassportMappingService uniqNamePassportMappingService;

    @Autowired
    private OperateHistoryLogService operateHistoryLogService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private DBShardRedisUtils dbShardRedisUtils;

    private static final String KEY_SPLITTER = "|";

    private static final String VALUE_SPLITTER = ":";

    private static final String OPERATE_LOG_DATA_FORMAT_1 = "%s:%s";

    private static final String OPERATE_LOG_DATA_FORMAT_2 = "%s:%s|%s:%s|%s:%s";

    //重置密码
    private static final String RESET_PWD = "rp";

    //绑定手机
    private static final String BIND_MOBILE = "bm";

    //绑定邮箱
    private static final String BIND_EMAIL = "be";

    //批量删除手机号
    private static final String PATCH_DELETE_MOBILE = "dm";

    //未操作
    private static final String OPERATE_NO = "0";

    //已经操作
    private static final String OPERATE_YES = "1";


    @Override
    public Account queryAccountByUserName(String username) {
        try {
            String passportId = username;
            //判断登录用户类型
            AccountDomainEnum accountDomainEnum = AccountDomainEnum.getAccountDomain(username);
            //默认是sogou.com
            if (AccountDomainEnum.PHONE.equals(accountDomainEnum)) {
                passportId = mobilePassportMappingService.queryPassportIdByUsername(username);
            } else if (AccountDomainEnum.UNKNOWN.equals(accountDomainEnum)) {
                passportId = passportId + "@sogou.com";
            }

            return accountService.queryAccountByPassportId(passportId);
        } catch (Exception e) {
            logger.error("queryAccountByUserName fail,userId:" + username, e);
            return null;
        }
    }

    @Override
    public boolean updateState(Account account, int newState) {
        try {
            return accountService.updateState(account, newState);
        } catch (Exception e) {
            logger.error("updateState fail,userId:" + account.getPassportId(), e);
            return false;
        }
    }

    @Override
    public boolean resetPassword(String sohuPassportId,Account account, String password, boolean needMD5) {
        try {
            return accountService.resetPassword(sohuPassportId,account, password, needMD5);
        } catch (Exception e) {
            logger.error("resetPassword fail,userId:" + account.getPassportId(), e);
            return false;
        }
    }

    @Override
    public AccountDetailInfo getAccountDetailInfo(AccountSearchParam accountSearchParam) {
        AccountDetailInfo accountDetail = new AccountDetailInfo();
        String passportId = StringUtils.EMPTY;
        try {
            if (accountSearchParam != null && !Strings.isNullOrEmpty(accountSearchParam.getUserName())) {
                passportId = accountSearchParam.getUserName();
            } else if (accountSearchParam != null && !Strings.isNullOrEmpty(accountSearchParam.getNickName())) {
                passportId = uniqNamePassportMappingService.checkUniqName(accountSearchParam.getNickName());
            } else if (Strings.isNullOrEmpty(passportId)) {
                return accountDetail;
            }

            //判断登录用户类型
            AccountDomainEnum accountDomainEnum = AccountDomainEnum.getAccountDomain(passportId);
            //默认是sogou.com
            if (AccountDomainEnum.PHONE.equals(accountDomainEnum)) {
                passportId = mobilePassportMappingService.queryPassportIdByUsername(passportId);
            } else if (AccountDomainEnum.UNKNOWN.equals(accountDomainEnum)) {
                passportId = passportId + "@sogou.com";
            }

            Account account = accountService.queryAccountByPassportId(passportId);
            if (account != null) {
                accountDetail.setPassportId(passportId);
                accountDetail.setPassword(account.getPassword());
                accountDetail.setAccountTypeName(AccountTypeMappingEnum.indexOf(accountDomainEnum.getValue()).getAccountTypeName());
                accountDetail.setAccountType(accountDomainEnum.getValue());
                accountDetail.setMobile(Strings.isNullOrEmpty(account.getMobile()) ? StringUtils.EMPTY : StringUtil.processMobile(account.getMobile()));
                accountDetail.setMobileOriginal(Strings.isNullOrEmpty(account.getMobile()) ? StringUtils.EMPTY : account.getMobile());
                accountDetail.setRegIp(Strings.isNullOrEmpty(account.getRegIp()) ? StringUtils.EMPTY : account.getRegIp());
                accountDetail.setRegTime(account.getRegTime().toString());
                accountDetail.setUniqname(Strings.isNullOrEmpty(account.getUniqname()) ? StringUtils.EMPTY : account.getUniqname());
                accountDetail.setFlag(account.getFlag());
            }

            AccountInfo accountInfo = accountInfoService.queryAccountInfoByPassportId(passportId);
            if (accountInfo != null) {
                accountDetail.setEmail(Strings.isNullOrEmpty(accountInfo.getEmail()) ? StringUtils.EMPTY : StringUtil.processEmail(accountInfo.getEmail()));
                accountDetail.setEmailOriginal(Strings.isNullOrEmpty(accountInfo.getEmail()) ? StringUtils.EMPTY : accountInfo.getEmail());
                accountDetail.setProvince(ProvinceAndCityUtil.getProvinceByPCode(accountInfo.getProvince()));
                accountDetail.setCity(ProvinceAndCityUtil.getCityByCityCode(accountInfo.getCity()));
                accountDetail.setGender(!Strings.isNullOrEmpty(accountInfo.getGender()) && accountInfo.getGender().equalsIgnoreCase("0") ? "女" : "男");
                accountDetail.setFullname(Strings.isNullOrEmpty(accountInfo.getFullname()) ? StringUtils.EMPTY : accountInfo.getFullname());
                accountDetail.setPersonalid(Strings.isNullOrEmpty(accountInfo.getPersonalid()) ? StringUtils.EMPTY : accountInfo.getPersonalid());
            }
        } catch (Exception e) {
            logger.error("getAccountDetailInfo error,passportId:" + passportId, e);
        }
        return accountDetail;
    }

    @Override
    public Result resetUserPassword(boolean needMD5, OperateHistoryLog operateHistoryLog) {
        Result result = new APIResultSupport(false);
        try {
            if (Strings.isNullOrEmpty(operateHistoryLog.getOperate_user())) {
                result.setCode(ErrorUtil.ERR_CODE_COM_REQURIE);
                result.setSuccess(false);
                result.setMessage(ErrorUtil.ERR_CODE_MSG_MAP.get(ErrorUtil.ERR_CODE_COM_REQURIE));
                return result;
            }

            //搜狐域账号，密码重置需要到搜狐重置
//            AccountDomainEnum domain = AccountDomainEnum.getAccountDomain(operateHistoryLog.getOperate_user());
//            if (domain == AccountDomainEnum.SOHU) {
//                result.setCode(ErrorUtil.ERR_CODE_COM_REQURIE);
//                result.setSuccess(false);
//                result.setMessage(CommonConstant.NOT_HANDLED_REST_PWD_FOR_SOHU);
//                return result;
//            }

            Account account = accountService.queryAccountByPassportId(operateHistoryLog.getOperate_user());
            if (account != null) {
                //重置密码
                String newPassword = UuidUtil.generatePassword();
                boolean resetResult = accountService.resetPassword(operateHistoryLog.getOperate_user(),account, newPassword, needMD5);
                if (resetResult) {
                    result.setSuccess(true);
                    result.setMessage(MessageUtil.RESET_PP_SUCCESS);
                    result.setDefaultModel("newPassword", newPassword);
                }
            } else {
                result.setCode(ErrorUtil.INVALID_ACCOUNT);
                return result;
            }

            //记录操作历史
            operateHistoryLog.setOperate_type(OperateLogEnum.RESET_PWD.getIndex());
            operateHistoryLog.setOperate_before_status(String.format(OPERATE_LOG_DATA_FORMAT_1, RESET_PWD, OPERATE_NO));
            operateHistoryLog.setOperate_after_status(String.format(OPERATE_LOG_DATA_FORMAT_1, RESET_PWD, OPERATE_YES));

            saveOperateLog(operateHistoryLog);

        } catch (Exception e) {
            logger.error("resetPassword error.operate_userid:{},operate_user:{}", operateHistoryLog.getOperate_userid(), operateHistoryLog.getOperate_user());
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
        }
        return result;
    }

    /**
     * 解绑定手机
     *
     * @param mobile
     * @return
     */
    @Override
    public Result unbundlingMobile(String mobile, OperateHistoryLog operateHistoryLog) {
        Result result = new APIResultSupport(false);

        try { //清除mobile_passport_id_mapping 映射
            //根据传入的手机号、查询相应的passportId
            String mobileMappingPassportId = mobilePassportMappingService.queryPassportIdByMobile(mobile);
            if (Strings.isNullOrEmpty(mobileMappingPassportId)) {
                //根据给出的手机号、查询不到对应的账户
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_BIND_NOTEXIST);
                result.setMessage(ErrorUtil.ERR_CODE_MSG_MAP.get(ErrorUtil.ERR_CODE_ACCOUNT_BIND_NOTEXIST));
                return result;
            }
            if (operateHistoryLog.getOperate_user().equals(mobileMappingPassportId)) {
                //执行清除手机映射
                boolean deleteMobileMapping = mobilePassportMappingService.deleteMobilePassportMapping(mobile);
                if (deleteMobileMapping) {
                    //清除手机映射成功、清空account_info 用户mobile 信息
                    Account account = accountService.queryAccountByPassportId(mobileMappingPassportId);
                    if (account != null) {
                        boolean clearAccountBindMobile = accountService.modifyMobile(account, StringUtils.EMPTY);
                        if (clearAccountBindMobile) {
                            result.setSuccess(true);

                            //记录操作历史
                            operateHistoryLog.setOperate_type(OperateLogEnum.UNBIND_PHONE.getIndex());
                            operateHistoryLog.setOperate_before_status(String.format(OPERATE_LOG_DATA_FORMAT_1, BIND_MOBILE, mobile));
                            operateHistoryLog.setOperate_after_status(String.format(OPERATE_LOG_DATA_FORMAT_1, BIND_MOBILE, OPERATE_YES));
                            saveOperateLog(operateHistoryLog);
                        } else {
                            result.setCode(ErrorUtil.ERR_CODE_PHONE_UNBIND_FAILED);
                            result.setMessage(ErrorUtil.ERR_CODE_MSG_MAP.get(ErrorUtil.ERR_CODE_PHONE_UNBIND_FAILED));
                        }
                    } else {
                        result.setCode(ErrorUtil.INVALID_ACCOUNT);
                        result.setMessage(ErrorUtil.ERR_CODE_MSG_MAP.get(ErrorUtil.INVALID_ACCOUNT));
                    }
                } else {
                    result.setCode(ErrorUtil.ERR_CODE_PHONE_UNBIND_FAILED);
                    result.setMessage(ErrorUtil.ERR_CODE_MSG_MAP.get(ErrorUtil.ERR_CODE_PHONE_UNBIND_FAILED));
                }
            } else {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_PHONE_BINDED);
                result.setMessage(ErrorUtil.ERR_CODE_MSG_MAP.get(ErrorUtil.ERR_CODE_ACCOUNT_PHONE_BINDED));
            }

            result.setMessage(CommonConstant.UN_BIND_SUCCESS);
        } catch (Exception e) {
            logger.error("unbind Mobile error.operate_userid:{},operate_user:{}", operateHistoryLog.getOperate_userid(), operateHistoryLog.getOperate_user());
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
        }
        return result;
    }


    /**
     * 解绑邮箱
     *
     * @param operateHistoryLog
     * @return
     */
    @Override
    public Result unbundlingEmail(OperateHistoryLog operateHistoryLog) {
        Result result = new APIResultSupport(false);
        try {
            //更新 account ,清空用户绑定的 email 信息
            Account account = accountService.queryAccountByPassportId(operateHistoryLog.getOperate_user());
            if (account != null) {
                AccountInfo accountInfo = accountInfoService.queryAccountInfoByPassportId(operateHistoryLog.getOperate_user());
                if (accountInfo != null && !Strings.isNullOrEmpty(accountInfo.getEmail())) {
                    boolean clearBindEmail = accountInfoService.updateBindMEmail(accountInfo, StringUtils.EMPTY);
                    if (!clearBindEmail) {
                        result.setCode(ErrorUtil.ERR_CODE_EMAIL_UNBIND_FAIL);
                        result.setMessage(ErrorUtil.getERR_CODE_MSG_MAP().get(ErrorUtil.ERR_CODE_EMAIL_UNBIND_FAIL));
                    }

                    //记录操作历史
                    operateHistoryLog.setOperate_type(OperateLogEnum.UNBIND_EMAIL.getIndex());
                    operateHistoryLog.setOperate_before_status(String.format(OPERATE_LOG_DATA_FORMAT_1, BIND_EMAIL, accountInfo.getEmail()));
                    operateHistoryLog.setOperate_after_status(String.format(OPERATE_LOG_DATA_FORMAT_1, BIND_EMAIL, OPERATE_YES));
                    saveOperateLog(operateHistoryLog);
                }
            } else {
                result.setCode(ErrorUtil.INVALID_ACCOUNT);
                result.setMessage(ErrorUtil.getERR_CODE_MSG_MAP().get(ErrorUtil.INVALID_ACCOUNT));
                return result;
            }
        } catch (Exception e) {
            logger.error("unbind email  error.operate_userid:{},operate_user:{}", operateHistoryLog.getOperate_userid(), operateHistoryLog.getOperate_user());
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
        }
        result.setSuccess(true);
        result.setMessage(CommonConstant.UN_BIND_SUCCESS);
        return result;
    }

    @Override
    public Result unBindMobiles(List<String> mobileList) {
        Result result = new APIResultSupport(false);
        List<String> failed = Lists.newArrayList();//解除绑定失败结果
        String failUnBinds = StringUtils.EMPTY;
        try {
            if (!CollectionUtils.isEmpty(mobileList)) {
                for (String mobile : mobileList) {
                    if (PhoneUtil.verifyPhoneNumberFormat(mobile)) {
                        String mobileMappingPassportId = mobilePassportMappingService.queryPassportIdByMobile(mobile);
                        if (!Strings.isNullOrEmpty(mobileMappingPassportId)) {
                            //执行清除手机映射
                            boolean deleteMobileMapping = mobilePassportMappingService.deleteMobilePassportMapping(mobile);
                            if (deleteMobileMapping) {
                                //清除手机映射成功、清空account_info 用户mobile 信息
                                Account account = accountService.queryAccountByPassportId(mobileMappingPassportId);
                                if (account != null) {
                                    boolean clearAccountBindMobile = accountService.modifyMobile(account, StringUtils.EMPTY);
                                    if (!clearAccountBindMobile) {
                                        failed.add(mobile);
                                        result.setCode(ErrorUtil.ERR_CODE_PHONE_UNBIND_FAILED);
                                        result.setMessage(ErrorUtil.ERR_CODE_MSG_MAP.get(ErrorUtil.ERR_CODE_PHONE_UNBIND_FAILED));
                                    }
                                } else {
                                    failed.add(mobile);
                                    result.setCode(ErrorUtil.INVALID_ACCOUNT);
                                    result.setMessage(ErrorUtil.ERR_CODE_MSG_MAP.get(ErrorUtil.INVALID_ACCOUNT));
                                }
                            } else {
                                failed.add(mobile);
                                result.setCode(ErrorUtil.ERR_CODE_PHONE_UNBIND_FAILED);
                                result.setMessage(ErrorUtil.ERR_CODE_MSG_MAP.get(ErrorUtil.ERR_CODE_PHONE_UNBIND_FAILED));
                            }
                        } else {
                            failed.add(mobile);
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
            }
            if (failed != null && !failed.isEmpty()) {
                failUnBinds = StringUtils.join(failed, ",");
            }
        } catch (Exception e) {
            logger.error("unBindMobiles error.", e);
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
        }
        result.getModels().put("failed", failUnBinds);
        result.setSuccess(true);
        result.setMessage(CommonConstant.UN_BIND_SUCCESS);
        return result;
    }

    @Override
    public Result deleteRegMobiles(List<String> regMobileList) {
        Result result = new APIResultSupport(false);
        List<String> failed = Lists.newArrayList();//解除绑定失败结果
        String failDelMobiles = StringUtils.EMPTY;
        try {
            if (!CollectionUtils.isEmpty(regMobileList)) {
                for (String mobile : regMobileList) {
                    if (PhoneUtil.verifyPhoneNumberFormat(mobile)) {
                        boolean deleteSuccess = accountService.deleteOrUnbindMobile(mobile);
                        if (!deleteSuccess) {
                            failed.add(mobile);
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
            }
            if (failed != null && !failed.isEmpty()) {
                failDelMobiles = StringUtils.join(failed, ",");
            }


        } catch (Exception e) {
            logger.error("deleteRegMobiles error.", e);
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
        }
        result.getModels().put("failed", failDelMobiles);
        result.setSuccess(true);
        result.setMessage(CommonConstant.UN_BIND_SUCCESS);
        return result;
    }


    @Override
    public String deleteRestriction(List<String> passportIds) {
        String result = "处理结果如下：\n";
      //  String printPassportIds = "PassportIds are: \n";
        Account account;
        String cacheAccountKey;
        String leakCacheKey;
        String leakCacheValue;
        String leakKeyPre = CommonConstant.LEAK_PREKEY;

        List<String> nonExistAccount = Lists.newArrayList();
        List<String> leakInRedis = Lists.newArrayList();
        List<String> leakInDb = Lists.newArrayList();
        List<String> noInLeakList = Lists.newArrayList();
        List<String> successList = Lists.newArrayList();

        int flag;

        try {
            if (!CollectionUtils.isEmpty(passportIds)) {
                for (String passportId : passportIds) {
                    //printPassportIds = printPassportIds + passportId + "\n";
                    account = accountService.queryAccountByPassportId(passportId);
                    if (null == account) {
                        nonExistAccount.add(passportId);
                        continue;
                    }

                    leakCacheKey = leakKeyPre + passportId;
                    leakCacheValue =   redisUtils.get(leakCacheKey);
                    if(!Strings.isNullOrEmpty(leakCacheValue)){
                        redisUtils.delete(leakCacheKey);
                        leakInRedis.add(passportId);
                        successList.add(passportId);
                        continue;
                    }

                    flag = account.getFlag();
                    cacheAccountKey = CacheConstant.CACHE_PREFIX_PASSPORT_ACCOUNT + passportId;
                    if(3 == flag){
                        account.setFlag(AccountStatusEnum.REGULAR.getValue());
                        dbShardRedisUtils.delete(cacheAccountKey);
                        accountService.updateState(account,1);
                        leakInDb.add(passportId);
                        successList.add(passportId);
                        continue;
                    }

                    noInLeakList.add(passportId);
                }

                if(!successList.isEmpty()){
                    result = result + StringUtils.join(successList,',');
                    result = result + CommonConstant.ACCOUNT_UNFREEZED + "\n";
                }

                if(!noInLeakList.isEmpty()){
                    result = result + StringUtils.join(noInLeakList,',');
                    result = result + CommonConstant.ACCOUNT_NOTIN_RESTRICTION + "\n";
                }
                if(!nonExistAccount.isEmpty()){
                    result = result + StringUtils.join(nonExistAccount,',');
                    result = result + CommonConstant.ACCOUNT_NONEXISTANT + "\n";
                }
            }
        } catch (Exception e) {
            logger.error("deleteRestriction error");
            return "解除失败";
        }
        return result;
    }

    /**
     * 记录操作历史
     *
     * @param operateHistoryLog
     */
    private void saveOperateLog(OperateHistoryLog operateHistoryLog) {
        //记录操作历史、便于追溯
        boolean saveOperateLog = operateHistoryLogService.insertOperateHistoryLog(operateHistoryLog);
        if (!saveOperateLog) {
            logger.warn("resetUserPassword save operate log error. operate_userid:{},operate_userip:{},operate_user:{}", new Object[]{operateHistoryLog.getOperate_userid(), operateHistoryLog.getOperate_userip(), operateHistoryLog.getOperate_user()});
        }
    }


}
