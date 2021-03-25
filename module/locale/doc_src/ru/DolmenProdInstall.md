<!--dolmen
label=Пример установки production
-->

% Пример установки production среды Дольмен

## Общие сведения
В этом примере устанавливаем *Dolmen Application Server* и *Dolmen Web Server* на один сервер, а *PostgreSQL* на другой.
Будет настроен доступ по *https* и аутентификация пользователей через *Active Directory.*


### Серверная операционная система 

*Windows Server 2019*

### URL веб-интерфейса  

[https://dolmensystem.corp.example.com/dolmensite](https://dolmensystem.corp.example.com/dolmensite)

### DNS имена

| Описание | DNS имя | IP | 
|---|---|---|
|FQDN имя домена Active Directory|corp.example.com|
|сервер PostgreSQL|dlm-db.corp.example.com| 10.23.45.12 |
|сервер Дольмен|dlm01.corp.example.com| 10.23.45.11 |
|имя для доступа через браузер|dolmensystem.corp.example.com| 10.23.45.11 |

### Пароли

| Описание | Логин, пароль |
|---|---|
| Администратор сервера Дольмен | admin : P@$$w0rd |
| Суперпользователь PostgreSQL | postgres : pgPw0rd |
| Пользователь сервера Дольмен в PostgreSQL| dlm_pguser : hT4BhLYK |
| Закрытый ключ в файле .pfx | Pass1234 |
| Учетная запись сервера Дольмен для реализации SSO| CORP\\dolmensrv_user : lDtGF4Ypk56# |
| Учетная запись для запуска сервиса |  CORP\\dolmensrv_user : lDtGF4Ypk56# или CORP\\dolmensrv_gmsa, или localservice |


## Установка и настройка PostgreSQL

Команда для автоматической установки сервера *PostgreSQL*

~~~
postgresql-11.10-1-windows-x64.exe --mode unattended --superpassword pgPw0rd
~~~

Если сервер СУБД установлен не на том же сервере, где сервер Дольмен, то
разрешить доступ c IP адреса 10.23.45.11 на файрволле

~~~
netsh advfirewall firewall add rule name="postgres5432" dir=in action=allow protocol=TCP localport=5432 remoteip=10.23.45.11
~~~

и в файле `C:\Program Files\PostgreSQL\11\data\pg_hba.conf`

~~~
host    all    all    10.23.45.11/32    md5
~~~

Создать пользователя для сервера Дольмен и схему

~~~
set PGPASSWORD=pgPw0rd
"c:\Program Files\PostgreSQL\11\bin\psql.exe" -U postgres -w
create user dlm_pguser with password 'hT4BhLYK';
create schema dlmprod authorization dlm_pguser;
\q
set PGPASSWORD=
~~~


## Установка DolmenHome
Файлы дистрибутива 

- *dlm.server.{xxxx}.zip*
- *dlm.gui.{xxxx}.zip*
- *dlm.appserv-tomcat{xxxx}.zip*
- *dlm.jdk.{xxxx}.zip*
- *dlm.doc.{xxxx}.zip*


разархивировать в папку `c:\dolmen\DolmenHome`
   
Должна получиться следующая структура папок

~~~
c:\dolmen
  DolmenHome
    doc
    rt
    site
    templates
    thirdparty
~~~


## Установка Workspace
Создать *workspace* типа *prod* и установить пароль администратора

~~~
c:\dolmen\DolmenHome\rt\dolmen.bat c:\dolmen\Workspace bundle-install prod
c:\dolmen\Workspace\configure.bat set-password dolmen P@$$w0rd
~~~


## Доступ к СУБД

Отредактировать файл `c:\dolmen\Workspace\webserver\webapps\dolmen\secure_store.dat`

~~~
"keys":{
    "db_dolmen":"hT4BhLYK"
},
~~~


Отредактировать файл `c:\dolmen\Workspace\webserver\webapps\dolmen\server.conf`. Если PostgreSQL на том же сервере, то вместо *dlm-db.corp.example.com* оставить *127.0.0.1*

~~~
"database":{
	"url": "dlm_pguser/$(SS:db_dolmen)@jdbc:postgresql://dlm-db.corp.example.com:5432/postgres",
	"schema": "dlmprod",
~~~

## Trusted Hosts

Отредактировать файл `c:\dolmen\Workspace\webserver\webapps\dolmen\server.conf`

~~~
"trustedHosts":[
	"dolmensystem.corp.example.com"
],
~~~



## Настройка SSO

Настроить SSO в `c:\dolmen\Workspace\webserver\webapps\dolmen\server.conf`

~~~
"sso":{
	"spn": "HTTP/dolmensystem.corp.example.com@CORP.EXAMPLE.COM",
	"realm": "CORP.EXAMPLE.COM",
	"ktabFile": "C:/dolmen/Workspace/webserver/webapps/dolmen/dolmen.ktab"
},
~~~


На контроллере домена создать пользователя в AD и настроить  SPN

~~~
dsadd user "CN=dolmensrv_user,CN=Users,DC=corp,DC=example,DC=com" –pwd lDtGF4Ypk56# -pwdneverexpires yes -canchpwd no -fn dolmensrv_user
setspn -U -S HTTP/dolmensystem.corp.example.com dolmensrv_user
~~~

На сервере Дольмен создать ktab файл

~~~
c:\dolmen\DolmenHome\rt\java\jdk\bin\ktab.exe -k FILE:C:\dolmen\Workspace\webserver\webapps\dolmen\dolmen.ktab -a HTTP/dolmensystem.corp.example.com@CORP.EXAMPLE.COM lDtGF4Ypk56#
~~~


## Настройка *https*

### Настройки сервера Дольмен для работы по *https*

Выпустить сертификат и закрытый ключ для веб-сервера, *cn=dlm01.corp.example.com, san=dlm01.corp.example.com, san=dolmensystem.corp.example.com*

Экспортировать сертификат и закрытый ключ в файл `c:\dolmen\Workspace\webserver\webapps\dolmen\dolmensystem.pfx`

Узнать alias закрытого ключа в файле `c:\dolmen\Workspace\webserver\webapps\dolmen\dolmensystem.pfx` (при помощи *keytool* или [KeyStore Explorer](https://keystore-explorer.org/))

~~~
c:\dolmen\DolmenHome\rt\java\jdk\bin\keytool -list -keystore c:\dolmen\Workspace\webserver\webapps\dolmen\dolmensystem.pfx -storepass Pass1234
~~~

Отредактировать `C:\dolmen\Workspace\server.properties`, установить *https="true"* и настроить доступ к хранилищу ключей (*httpsKeyAlias* взять из предыдущего шага)

~~~
https="true"
httpsKeyAlias="te-webserver-7f670036-e593-4381-b9bc-1c599b6eca7c"
httpsKSFile="C:\dolmen\Workspace\webserver\webapps\dolmen\dolmensystem.pfx"
httpsKSPass="Pass1234"
~~~

Настроить доступ к серверу Дольмен по *https* и отключить доступ по *http*

~~~
C:\dolmen\Workspace\configure.bat serv-url dolmensite https://dolmensystem.corp.example.com/dolmen
C:\dolmen\Workspace\configure.bat httpsonly dolmen true
C:\dolmen\Workspace\configure.bat httpsonly dolmensite true
C:\dolmen\Workspace\update.bat
~~~

### Настройка файрволла
Открыть порт *TCP 443*

~~~
netsh advfirewall firewall add rule name="dolmen443" dir=in action=allow protocol=TCP localport=443
~~~

### Настройки браузера
Обеспечить наличие сертификата удостоверяющего центра, который выдал сертификат сервера, на пользовательских компьютерах

Добавить *https://dolmensystem.corp.example.com* в *local intranet*

- Панель управления: *Internet Options - Local intranet - Sites - Advanced*
- Или групповые политики: *User Configuration/Policies/Administrative Templates/Windows Components/Internet Explorer/Internet Control Panel/Security Page/Site to Zone Assignment List* - установить *Enabled* и добавить *value name = https://dolmensystem.corp.example.com, value = 1*

## Настройка запуска сервера Дольмен как сервиса Windows

Запустить *webserver_instance.bat* c аргументами *service install.* Будет установлен сервис *Dolmen-1* (Display name *Apache Tomcat 8.5 Dolmen-1)* для запуска под *Local system account*

~~~
c:\dolmen\Workspace\webserver_instance.bat service install
~~~

При необходимости можно изменить учетную запись для сервиса.

Например, 

- для запуска под *Local Service*

	~~~
	sc config Dolmen-1 obj= "NT AUTHORITY\LocalService" password= "" 
	~~~

- под доменной учетной записью *dolmensrv_user*

	~~~
	sc config Dolmen-1 obj= "CORP\dolmensrv_user" password= "lDtGF4Ypk56#" 
	~~~


- под доменной gMSA  *dolmensrv_gmsa*

	~~~
	sc config Dolmen-1 obj= "CORP\dolmensrv_gmsa$" password= "" 
	~~~



## Разрешения на `c:\dolmen`
У папки `c:\dolmen` и ее подпапок и файлов убрать разрешения для *Domain Users*. Назначить разрешения *Full Control* для администраторов сервера, администраторов приложения Дольмен и для учетной записи, под которой запускается сервис *Dolmen-1.*


## Запуск сервера и установка модулей

- Запустить сервис

	~~~
	sc start Dolmen-1
	~~~

- Открыть в Chrome    [https://dolmensystem.corp.example.com/dolmensite](https://dolmensystem.corp.example.com/dolmensite)
- Войти, используя имя пользователя   *admin*  и пароль *P@$$w0rd* 
- Установить модули и зарегистрировать сервер в базе данных
    - Перейти на экран  *Обслуживание -> Администрирование -> Сервер и модули -> Сервер (управление)*
	- Нажать *Установить все модули*
	- Нажать *Зарегистрировать*
	- Обновить страницу браузера
	- В поле *Запрос состояния* выбрать *Запущен*
	- Обновить страницу браузера






 



