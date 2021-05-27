<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="Author" content="SuperDrew">
    <meta name="Description" content="Form Validation">
    <style>
        #msg, #msg2 {
            color: red;
        }

        body, input {
            font-family: "微软雅黑", serif;
        }
    </style>
    <script>
        function verifyTel1() {
            let regexp1 = /^[a-z0-9]{3,9}$/;
            let uname = document.getElementById("username").value;
            if (!regexp1.test(uname)) {
                document.getElementById("msg").style.display = "inline-block";
                document.getElementById("msg").innerHTML = "用户名是3-9位不得使用_+!@#$等特殊符号";
                document.getElementById("username").focus(); //让输入框获得焦点
                document.getElementById("username").select(); //选中输入框的内容
                return false;
            } else {
                document.getElementById("msg").style.display = "none";
                return true;
            }
        }

        function verifyTel2() {
            let regexp2 = /^[a-z0-9]{6,13}$/;
            let pwd = document.getElementById("pwd").value;
            if (!regexp2.test(pwd)) {
                document.getElementById("msg2").style.display = "inline-block";
                document.getElementById("msg2").innerHTML = "请输入正确的密码！";
                document.getElementById("pwd").focus(); //让输入框获得焦点
                document.getElementById("pwd").select(); //选中输入框的内容
                return false;
            } else {
                document.getElementById("msg2").style.display = "none";
                return true;
            }
        }

        //提交表单
        function submitForm() {
            if (verifyTel1() && verifyTel2()) {
                document.getElementById("allow").submit();
            }
        }
    </script>
</head>

<body>
<center>
    <form method="post" action="/login" id="allow">
        <div id="form">
            <div class="int">
                <label for="username">账 号：</label>
                <input type="text" id="username" name="username" class="input"/>
            </div>
            <div class="int">
                <label for="pwd">密 码：</label>
                <input type="password" id="pwd" name="password" class="input"/>
            </div>
            <div id="msg"></div>
            <div id="msg2"></div>
            <br>
            <input type="button" value="提交" onclick="submitForm()">
            <input type="reset" value="重置"/>
    </form>
</center>
</body>

</html>