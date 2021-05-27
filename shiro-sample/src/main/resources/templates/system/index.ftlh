<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="Author" content="SuperDrew">
    <meta name="Description" content="Form Validation">

</head>

<body>
<#if users??>
    <#list users as user>
        <td>${user.id}</td>
        <td>${user.name}</td>
        <td>${user.age}</td>
    </#list>
</#if>
<button>
    <a></a>
</button>
</body>

</html>