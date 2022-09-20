package org.xgvela.oam.controller;

import cn.hutool.core.date.format.FastDateFormat;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspur.cnet.common.core.entity.response.Response;
import com.inspur.cnet.common.core.entity.response.ResponseEnum;
import com.inspur.cnet.common.core.entity.response.ResponseFactory;
import com.inspur.cnet.common.core.exception.ServiceException;
import com.inspur.cnet.security.entity.Developer;
import com.inspur.cnet.security.entity.User;
import com.inspur.cnet.security.entity.UserRole;
import com.inspur.cnet.security.mapper.UserRoleMapper;
import com.inspur.cnet.security.service.DeveloperServiceImpl;
import com.inspur.cnet.security.service.UserServiceImpl;
import com.inspur.cnet.security.service.utils.JwtUtils;
import com.inspur.cnet.security.service.utils.SecurityUtils;
import org.xgvela.oam.annotation.Log;
import org.xgvela.oam.entity.auth.OmcStrategyConf;
import org.xgvela.oam.entity.auth.UserVo;
import org.xgvela.oam.service.UserVoServiceImpl;
import org.xgvela.oam.service.auth.IOmcStrategyConfService;
import org.xgvela.oam.utils.JsonUtils;
import org.xgvela.oam.utils.TimeUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Api(tags = "sysmanage-auth")
@RestController
@AllArgsConstructor
@RequestMapping(value = "api/auth")
public class AuthController {

    private final UserServiceImpl userService;
    private final DeveloperServiceImpl developerService;
    private final UserVoServiceImpl userVoService;
    private final IOmcStrategyConfService omcStrategyConfService;
    private final UserRoleMapper userRoleMapper;
    private final com.inspur.cnet.security.mapper.RoleMapper roleMapper;

    private final static int DELAY_TIME = 5; //5分钟

    @Log("login")
    @ApiOperation(value = "get token")
    @PostMapping("tokens")
    public Response<Map<String, Object>> login(@RequestParam(value = "uid") String username,
                                               @RequestParam(value = "pwd") String password,
                                               @RequestHeader(value = "Authorization", required = false) String authorization,
                                               HttpServletResponse response) {
        Assert.notNull(username, "The username cannot be empty");
        Assert.notNull(password, "The password cannot be empty");

        User user = userService.getByAccount(username);
        if (null == user) {
            throw new UnknownAccountException("The user does not exist");
        }
        if (user.getUserStatus() == 1) {
            throw new ServiceException("The user is locked out");
        }

        if (!"admin".equals(username) && !timeRangeCheck(user.getId())) {
            throw new ServiceException("The user is not logged on within the time specified in the settings");
        }
//

        UserVo userVo = userVoService.getOne(new QueryWrapper<UserVo>().eq("user_id", user.getId()));
        if (null == userVo) {
            userVo = new UserVo();
            userVo.setUserId(user.getId());
        }
        userVo.setLoginTime(new Date());
        userVoService.create(userVo);

        Date delayTime = userVo.getAccessErrorTime();
        if (Objects.nonNull(delayTime) && new Date().compareTo(delayTime) < 0) {
            throw new ServiceException("The user needs to log in late multiple times by logging in incorrectly,Please login after"+ FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss").format(delayTime));
        }

        String encodePassword = SecurityUtils.md5(password, user.getUserActivationKey());
        if (!encodePassword.equals(user.getUserPass())) {
            if (!user.getUserLogin().equals("admin")) {
                errorPasswordCount(user);
            }
            accessErrorCount(user,userVo);
            throw new IncorrectCredentialsException("Wrong username or password");
        } else {
            if (userVo != null) {
                userVo.setAccessErrorCount(0);
                userVo.setAccessErrorTime(new Date(System.currentTimeMillis() - (long) DELAY_TIME * 60 * 1000));
                userVoService.saveOrUpdate(userVo);
            }
        }
        Date nowDate = new Date(System.currentTimeMillis());
        if (userVo != null) {
            Long roleId = userRoleMapper.selectOne(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userVo.getUserId())).getRoleId();
            String roleName = roleMapper.selectById(roleId).getName();
            if (userVo.getUserValidity() != null && nowDate.compareTo(userVo.getUserValidity()) > 0 && !"admin".equals(roleName.trim())) {
                user.setUserStatus(1);
            }
            /*if (userVo.getUserPsValidity() != null && nowDate.compareTo(userVo.getUserPsValidity()) > 0) {
                user.setUserStatus(1);
            }*/
            userService.saveOrUpdate(user);
        }

        //关联的develop
        Developer developer = developerService.getDeveloperByAdmin(username);
        if (developer != null) {
            user.setDeveloperId(developer.getId());
        }
        String token = JwtUtils.sign(username, user.getId(), encodePassword);
        //写入header
        response.setHeader(SecurityUtils.REQUEST_AUTH_HEADER, token);
        response.setHeader("Access-Control-Expose-Headers", SecurityUtils.REQUEST_AUTH_HEADER);
        user.setUserPass("");
        user.setUserActivationKey("");
        Map<String, String> data = new HashMap<>();
        data.put("user", JsonUtils.o2js(user));
        data.put("token", token);
        //记录活动用户
//        recordActiveUser(user);
        Subject currentUser = org.apache.shiro.SecurityUtils.getSubject();
        Session session = currentUser.getSession();
        OmcStrategyConf omcStrategyConf = omcStrategyConfService.findLoginTimeout();
        long timeout = 30L;
        if (omcStrategyConf != null) {
            timeout = Long.parseLong(omcStrategyConf.getValue());
        }
        session.setTimeout(timeout * 60 * 1000);
        session.setAttribute("user", user.getUserLogin());
        return ResponseFactory.getSuccessData("user", user, "token", token);
    }


    /**
     * accessErrorCount
     * @param user
     * @param userVo
     */
    private void accessErrorCount(User user, UserVo userVo) {
        try {
            if (userVo != null) {

                OmcStrategyConf omcStrategyConfLock = omcStrategyConfService.getOne(new QueryWrapper<OmcStrategyConf>().eq("type", "access").eq("name", "passwordErrorLock").eq("flag", 1));
                if (omcStrategyConfLock != null) {
                    int lockCount = Integer.parseInt(omcStrategyConfLock.getValue());
                    int count = userVo.getAccessErrorCount() + 1;
                    if (count > lockCount) {
                        if (user.getUserLogin().equals("admin")) {
                            userVo.setAccessErrorCount(count);
                            userVo.setAccessErrorTime(new Date(System.currentTimeMillis() + (long) DELAY_TIME * 60 * 1000));
                            userVoService.saveOrUpdate(userVo);
                        } else {
                            user.setUserStatus(1);
                            userService.saveOrUpdate(user);
                        }
                        return;
                    }
                }


                OmcStrategyConf omcStrategyConfDelay = omcStrategyConfService.getOne(new QueryWrapper<OmcStrategyConf>().eq("type", "access").eq("name", "passwordErrorDelay").eq("flag", 1));
                if (omcStrategyConfDelay != null) {
                    int delayCount = Integer.parseInt(omcStrategyConfDelay.getValue());
                    int count = userVo.getAccessErrorCount() + 1;
                    if (count > delayCount) {
                        userVo.setAccessErrorTime(new Date(System.currentTimeMillis() + (long) DELAY_TIME *(count - delayCount)* 60 * 1000));
                    }
                    userVo.setAccessErrorCount(count);
                    userVoService.saveOrUpdate(userVo);
                }
            }

        } catch (Exception ex) {
            log.error("Error logon delay processing:" + ex);
        }
    }


    private boolean timeRangeCheck(Long id) {
        try {
            UserVo userVo = userVoService.getById(id);
            if (userVo != null) {
                SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                Date newDate = df.parse(df.format(new Date()));
                String timeRange = userVo.getTimeRange();
                if (StringUtils.isNoneEmpty(timeRange)) {
                    boolean flag = false;
                    String[] times = timeRange.split(";");
                    for (String timeScope : times) {
                        String[] time = timeScope.split(",");
                        if (timeRange(newDate, df.parse(time[0]), df.parse(time[1]))) {
                            flag = true;
                            break;
                        }
                    }
                    return flag;
                }
            }
        } catch (Exception ex) {
            log.error("The logon time check error occurred:" + ex);
        }
        return true;
    }

    private void errorPasswordCount(User user) {
        try {
            OmcStrategyConf omcStrategyConf = omcStrategyConfService.getOne(new QueryWrapper<OmcStrategyConf>().eq("type", "access").eq("name", "passwordCount").eq("flag", 1));
            if (omcStrategyConf != null) {
                String[] value = omcStrategyConf.getValue().split("-");
                int count = Integer.parseInt(value[0]);
                int time = Integer.parseInt(value[1]);
                UserVo userVo = userVoService.getById(user.getId());
                if (userVo != null) {
                    int currentCount = Optional.of(userVo.getLoginErrorCount()).orElse(0) + 1;
                    if (userVo.getLoginErrorTime() != null) {
                        if (new Date(System.currentTimeMillis() - (long) time * 60 * 1000).compareTo(userVo.getLoginErrorTime()) < 0) {
                            if (currentCount >= count) {
                                user.setUserStatus(1);
                                userService.saveOrUpdate(user);
                            } else if (currentCount == 1) {
                                userVo.setLoginErrorCount(currentCount);
                                userVo.setLoginErrorTime(new Date());
                                userVoService.saveOrUpdate(userVo);
                            } else {
                                userVo.setLoginErrorCount(currentCount);
                                userVoService.saveOrUpdate(userVo);
                            }
                        } else {
                            userVo.setLoginErrorCount(1);
                            userVo.setLoginErrorTime(new Date());
                            userVoService.saveOrUpdate(userVo);
                        }
                    } else {
                        userVo.setLoginErrorCount(1);
                        userVo.setLoginErrorTime(new Date());
                        userVoService.saveOrUpdate(userVo);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Error password check error:" + ex);
        }
    }


    private boolean timeRange(Date currentDate, Date startTime, Date endTime) {
        Calendar date = Calendar.getInstance();
        date.setTime(currentDate);

        Calendar begin = Calendar.getInstance();
        begin.setTime(startTime);

        Calendar end = Calendar.getInstance();
        end.setTime(endTime);

        return date.after(begin) && date.before(end);
    }



    @Log("No permissions")
    @RequestMapping(path = "/401")
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Response unauthorized(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseFactory.getResponse(ResponseEnum.please_login);
    }

    @Log("Registered users")
    @PostMapping("register")
    @ApiOperation(value = "Registered users")
    public Response register(@RequestParam(value = "account") String account,
                             @RequestParam(value = "pwd") String pwd,
                             @RequestParam(value = "name") String name,
                             @RequestParam(value = "code") String code,
                             @RequestParam(value = "inviteCode", required = false) String inviteCode) {

        if (StringUtils.isEmpty(account) || StringUtils.isEmpty(pwd) || StringUtils.isEmpty(name) || StringUtils.isEmpty(code)) {
            return ResponseFactory.getError("Please complete the registration information");
        }

        OmcStrategyConf omcStrategyConf = omcStrategyConfService.getOne(new QueryWrapper<OmcStrategyConf>().eq("type", "createAccount").eq("name", "accountLength").eq("flag", 1));
        if (!checkAccountLength(account, omcStrategyConf)) {
            return ResponseFactory.getError(String.format("The account length must be greater than or equal to(%s)", omcStrategyConf.getValue()));
        }

        pwd = new String(Base64.decodeBase64(pwd.getBytes()));

        if (userService.isExist(account)) {
            return ResponseFactory.getError("The account is registered");
        }

        Map<String, OmcStrategyConf> omcStrategyConfMap = getOmcStategyConf();


        OmcStrategyConf omcStrategyLength = omcStrategyConfMap.get("length");
        if (!checkPwdLength(pwd, omcStrategyLength)) {
            return ResponseFactory.getError(String.format("The password length does not conform to the password policy length range(%s)", omcStrategyLength.getValue()));
        }

        OmcStrategyConf omcStrategyStrength = omcStrategyConfMap.get("strength");
        if (!checkPwdStrength(pwd, omcStrategyStrength)) {
            return ResponseFactory.getError(String.format("The password strength does not meet the required strength of the password policy(%s)", MapUtils.putAll(new HashMap<String, String>(), new Object[]{"1", "弱", "2", "简单", "3", "一般", "4", "强"}).get(omcStrategyStrength.getValue())));
        }

        User user = userService.register(account, pwd, name);
//        if (StringUtils.isNotEmpty(inviteCode)) {
            /*developerUserInviteRemoteService.finish(inviteCode, account);
            DeveloperUserInvite developerUserInvite = developerUserInviteRemoteService.getByInviteCode(inviteCode);
            if (developerUserInvite != null) {
                User doInviteUser = userService.getByAccount(developerUserInvite.getCreateUser());
                developerService.saveAdmin(account, name, developerUserInvite.getDevelopId(), "admin", doInviteUser != null ? doInviteUser.getId() : null);
            }*/
//        }
        UserVo userVo = new UserVo();
        userVo.setUserId(user.getId());
        userVo.setUserPsValidity(TimeUtil.getDayAfterDate(90));
        userVo.setUserPsUpdate(new Date());
        userVo.setUserValidity(TimeUtil.getDayAfterDate(90));
        pwdValidity(userVo, omcStrategyConfMap.get("validDate"));
        userVoService.create(userVo);
        return ResponseFactory.getSuccessData(user);
    }

    private boolean checkAccountLength(String account, OmcStrategyConf omcStrategyConf) {
        try {
            if (null != omcStrategyConf) {
                if (account.length() < Integer.parseInt(omcStrategyConf.getValue())) {
                    return false;
                }
            }
        } catch (Exception ex) {
            log.error("Account length detection:" + ex);
        }
        return true;
    }

    private Map<String, OmcStrategyConf> getOmcStategyConf() {
        Map<String, OmcStrategyConf> omcStrategyConfMap = new HashMap<>();
        List<OmcStrategyConf> omcStrategyConfList = omcStrategyConfService.list(new QueryWrapper<OmcStrategyConf>().eq("type", "password").eq("flag", 1));
        if (omcStrategyConfList != null) {
            for (OmcStrategyConf omcStrategyConf : omcStrategyConfList) {
                omcStrategyConfMap.put(omcStrategyConf.getName(), omcStrategyConf);
            }
        }
        return omcStrategyConfMap;
    }

    @Log("User logoff")
    @ApiOperation(value = "User logoff")
    @PostMapping("/logout")
    public Response logout() {
        Subject currentUser = org.apache.shiro.SecurityUtils.getSubject();
        if (currentUser.isAuthenticated()) {
//            omcUserActiveService.removeById(SecurityUtils.getCurrentUserId());
            currentUser.logout();
        }
        return ResponseFactory.getSuccess();
    }

    @Log("session expired")
    @ApiOperation(value = "The user times out automatically to exit")
    @PostMapping("/timeout")
    public Response timeout() {
        log.info("-------------The user times out automatically to exit--------------");
        Subject currentUser = org.apache.shiro.SecurityUtils.getSubject();
        if (currentUser.isAuthenticated()) {
            currentUser.logout();
        }
        return ResponseFactory.getError("The connection timed out");
    }

    private boolean checkPwd(String password) {
        return password.matches("^(?![A-Za-z0-9]+$)(?![a-z0-9\\W]+$)(?![A-Za-z\\W]+$)(?![A-Z0-9\\W]+$)[a-zA-Z0-9\\W]{8,16}$");
    }

    private boolean checkPwdLength(String password, OmcStrategyConf omcStrategyConf) {
        if (omcStrategyConf != null) {
            String[] len = omcStrategyConf.getValue().split("-");
            if (len.length == 2) {
                int pwdLen = password.length();
                return pwdLen >= Integer.parseInt(len[0]) && pwdLen <= Integer.parseInt(len[1]);
            }
        }
        return true;
    }

    private Boolean checkPwdStrength(String password, OmcStrategyConf omcStrategyConf) {
        if (omcStrategyConf != null) {
            int level = checkStrongValue(password);
            return compareLevel(level, omcStrategyConf);
        }
        return true;
    }

    private boolean compareLevel(int level, OmcStrategyConf omcStrategyConf) {
        boolean flag = true;
        switch (Integer.parseInt(omcStrategyConf.getValue())) {
            case 1:
                if (level < 25) {
                    flag = false;
                }
                break;
            case 2:
                if (level < 50) {
                    flag = false;
                }
                break;
            case 3:
                if (level < 75) {
                    flag = false;
                }
                break;
            case 4:
                if (level < 100) {
                    flag = false;
                }
                break;
            default:
        }
        return flag;
    }

    private int checkStrongValue(String password) {
        int per = 0;
        if (password.length() < 1) {
            return per;
        } else if (password.length() > 7) {
            if (password.matches("([\\w\\W]*)[0-9]+([\\w\\W]*)")) {
                per += 25;
            }
            if (password.matches("([\\w\\W]*)[a-z]+([\\w\\W]*)")) {
                per += 25;
            }
            if (password.matches("([\\w\\W]*)[A-Z]+([\\w\\W]*)")) {
                per += 25;
            }
            if (password.matches("([\\w\\W]*)[`~!@#$^%&*()=|{}':;',\\\\[\\\\].<>/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？]+([\\w\\W]*)")) {
                per += 25;
            }
        }
        return per;
    }

    private void pwdValidity(UserVo userVo, OmcStrategyConf omcStrategyConf) {
        if (omcStrategyConf != null) {
            int num = 1;
            switch (Integer.parseInt(omcStrategyConf.getValue())) {
                case 1:
                    num = 1;
                    break;
                case 2:
                    num = 2;
                    break;
                case 3:
                    num = 3;
                    break;
                case 6:
                    num = 6;
                    break;
                case 12:
                    num = 12;
                    break;

            }
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, num);
            Date newTime = calendar.getTime();
            userVo.setUserValidity(newTime);
            userVo.setUserPsValidity(newTime);
        }
    }
}