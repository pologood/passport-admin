<%@ page pageEncoding="GBK" contentType="text/html;charset=utf-8" %>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>passport后台</title>
    <%@ include file="/pages/admin/head.jsp" %>
    <%--<script src="/js/jquery.md5.js" type="text/javascript"></script>--%>

    <!-- 最新 Bootstrap 核心 CSS 文件 -->
    <link rel="stylesheet" href="http://cdn.bootcss.com/twitter-bootstrap/3.0.3/css/bootstrap.min.css">

    <!-- 可选的Bootstrap主题文件（一般不用引入） -->
    <link rel="stylesheet" href="http://cdn.bootcss.com/twitter-bootstrap/3.0.3/css/bootstrap-theme.min.css">

    <!-- jQuery文件。务必在bootstrap.min.js 之前引入 -->
    <%--<script src="http://cdn.bootcss.com/jquery/1.10.2/jquery.min.js"></script>--%>

    <!-- 最新的 Bootstrap 核心 JavaScript 文件 -->
    <%--<script src="http://cdn.bootcss.com/twitter-bootstrap/3.0.3/js/bootstrap.min.js"></script>--%>
    <style>
        div#header {
            display: none;
            background-color: #f5f5dc;
        }

        div#logo {
            width: 130px;
            height: 37px;
            background: url(/img/admin/logo.png);
        }

        #menu_name {
            display: none;
        }

            /* 去掉菜单标题栏 */
        iframe#main {
            margin: 0;
            padding: 0;
        }
    </style>
</head>
<body>
<div id="page">
    <!-- 引入顶部信息 -->
    <%@ include file="/pages/admin/include_top.jsp" %>
    <div id="pageBd">
        <!-- 引入菜单 -->
        <%@ include file="/pages/admin/include_menu.jsp" %>
        <div id="pageCanvas" class="canvas">
            <div id="pageCanvasInt" class="canvasInt">
                <div id="pageCrumbs" class="crumbs">
                    当前位置：<strong>后台管理</strong>
                    <span class="step">&gt;</span><strong>用户账号管理</strong>
                </div>
                <h2 id="pageTitle">用户账号管理</h2>

                <form action="/admin/alterAccount/queryAccount" method="post">
                    请输入用户名，如(example@sogou.com)或手机号：
                    <input type="text" name="username"/>
                                <span class="button button-main">
                                  <input type="submit" value="提交" class="button"/>
                                </span>
                    <c:if test="${exist==false}">
                        <div style="color:#ff0000">账号不存在</div>
                    </c:if>
                </form>

                <form class="navbar-form navbar-left" role="search">
                    <div class="form-group">
                        <input type="text" class="form-control" placeholder="Search">
                    </div>
                    <button type="submit" class="btn btn-default">Submit</button>
                </form>

                <hr>
                <form id="accountForm" action="" method="post">

                    <div class="panel panel-default">
                        <!-- Default panel contents -->
                        <div class="panel-heading">用户账号信息</div>
                        <c:if test="${account != null}">
                        <!-- Table -->
                        <table class="table">
                            <tbody>
                            <input type="hidden" id="passportId" name="passportId" value="${account.passportId}"/>
                            <input type="hidden" id="newState" name="newState" value="${account.flag}"/>
                            <tr>
                                <td style="color: #005AA0">用户名</td>
                                <td style="color: #005AA0">密码</td>
                                <td style="color: #005AA0">绑定手机</td>
                                <td style="color: #005AA0">绑定邮箱</td>
                                <td style="color: #005AA0">注册时间</td>
                                <td style="color: #005AA0">注册IP</td>
                                <td style="color: #005AA0">账号类型</td>
                                <td style="color: #005AA0">封/解禁</td>
                                <td style="color: #005AA0">重置密码</td>
                            </tr>
                            <tr>
                                <td>${account.passportId}</td>
                                <td><input type="text" id="oldPasswd" value="${account.password}" disabled="false"/>
                                    <input type="text" id="newPasswd" name="newPasswd" style="display: none"/>
                                </td>
                                <td>${account.mobile}</td>
                                <td>${account.email}</td>
                                <td>${account.regTime}</td>
                                <td>${account.regIp}</td>
                                <td>${account.accountTypeName}</td>
                                <td>
                                    <c:if test="${account.flag == 1}">
                                        <input type="button" value="封禁" onclick="forbid()"/>
                                    </c:if>
                                    <c:if test="${account.flag == 3}">
                                        <input type="button" value="解禁" onclick="unForbid()"/>
                                    </c:if>
                                </td>
                                <td>
                                    <input id="oldPasswdBtn" type="button" value="重置" onclick="resetUserPassword()"/>
                                    <input id="newPasswdBtn" type="button" value="保存" onclick="resetUserPassword()"
                                           style="display: none"/>
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                    </c:if>
                </form>

            </div>
            <!-- pageCanvasInt End -->
        </div>
        <!-- pageCanvas End -->
    </div>
    <!-- pageBd End -->
</div>
<!-- page End -->

<script type="text/javascript">
    function forbid() {
        document.getElementById('newState').value = 3;
        document.getElementById('accountForm').action = '/admin/alterAccount/updateState';
        document.getElementById('accountForm').submit();
    }
    function unForbid() {
        document.getElementById('newState').value = 1;
        document.getElementById('accountForm').action = '/admin/alterAccount/updateState';
        document.getElementById('accountForm').submit();
    }
    function resetPassword() {
        document.getElementById('oldPasswd').style.display = "none";
        document.getElementById('newPasswd').style.display = "block";
        document.getElementById('oldPasswdBtn').style.display = "none";
        document.getElementById('newPasswdBtn').style.display = "block";
    }
    function savePassword() {
        var newpasswdstr = document.getElementById('newPasswd').value;
        if (newpasswdstr == "") {
            alert("请输入新密码！");
            return;
        }
//            newpasswdstr = $.md5(newpasswdstr);
        document.getElementById('newPasswd').value = newpasswdstr;
        document.getElementById('accountForm').action = '/admin/alterAccount/resetPassword';
        document.getElementById('accountForm').submit();
    }


    function resetUserPassword() {
        document.getElementById('oldPasswd').style.display = "none";
        document.getElementById('newPasswd').style.display = "block";
        var passport_id = $("#passportId").val();
        $.post('/admin/resetPassword', {passportId: passport_id}, function (res) {
            if (res.status == 0) {
                var data = res.data;
                $('#newPasswd').val(data.newPassword);
            }
        }, 'json');
    }

    function showMsg() {
        <c:if test ="${msg!=null}">
        alert("${msg}");
        </c:if>
    }
    showMsg();
</script>
</body>
</html>