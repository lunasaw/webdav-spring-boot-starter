# webdav 安装教程

## 介绍

[WebDAV](http://www.webdav.org/)是 HTTP 协议的扩展，允许用户管理远程服务器上的文件。有多种使用 WebDAV 服务器的方法，例如，您可以通过将 Word 或 Excel 文档上传到您的 WebDAV 服务器与同事共享它们。您还可以通过简单地给他们一个 URL 与您的家人和朋友分享您的音乐收藏。所有这一切都可以在他们不安装任何额外软件的情况下实现，因为一切都内置在他们的操作系统中。

在本文中，您将配置一个 Apache Web 服务器，以通过 SSL 和密码身份验证从 Windows、Mac 和 Linux 启用 WebDAV 访问。

## 安装方式

### mac

##### 自带服务器

mac有自带的apache2服务器，位置在/etc/apache2下。您可以使用sudo命令执行，开启webdav场景下，您需要确保项目路径拥有www用户权限，或者是您配置的权限，具体见[Mac 配置自带apache2的webdav服务](https://www.jianshu.com/p/d55d342f823f )

##### brew安装

也可以使用mac超好用的brew服务安装，【推荐】[brew 安装apache2](https://cloud.tencent.com/developer/article/1624764)

### win和其他

[官方下载](https://cloud.tencent.com/developer/article/1698069) 附上我的本地配置[httpd.conf](./config/httpd.conf),[httpd-dav.conf](./config/httpd-dav.conf)。下面是一个详细的安装例子

## 先决条件

在开始本指南之前，您需要具备以下条件：

- 一个 Ubuntu 20.04 服务器。按照[Ubuntu 20.04](https://www.digitalocean.com/community/tutorials/initial-server-setup-with-ubuntu-20-04)的[初始服务器设置](https://www.digitalocean.com/community/tutorials/initial-server-setup-with-ubuntu-20-04)创建一个启用 sudo 的非 root 用户。其他安装方式包括yum，apt，brew直接安装依赖包，可以手动修改配置文件httpd.conf如下。
- 解析为您服务器的公共 IP 地址的域名。该[域名和DNS](https://www.digitalocean.com/docs/networking/dns/)介绍如何设置此。
- 配置为为您的域提供服务的 Apache Web 服务器，您可以完成[如何在 Ubuntu 20.04 上安装 Apache Web 服务器](https://www.digitalocean.com/community/tutorials/how-to-install-the-apache-web-server-on-ubuntu-20-04)教程来进行设置——确保按照**步骤**[5——](https://www.digitalocean.com/community/tutorials/how-to-install-the-apache-web-server-on-ubuntu-20-04)**设置虚拟主机**作为此先决条件的一部分。
- 您的域名的 SSL 证书。请按照[如何在 Ubuntu 20.04 上使用 Let’s Encrypt 来保护 Apache](https://www.digitalocean.com/community/tutorials/how-to-secure-apache-with-let-s-encrypt-on-ubuntu-20-04)指南以获取有关如何执行此操作的说明。

WebDAV 需要很少的服务器资源，因此任何大小的虚拟机都足以让您的 WebDAV 服务器启动并运行。以启用 sudo 的非 root 用户身份登录到您的服务器以开始第一步。

## 步骤 1 — 启用 WebDAV Apache 模块

Apache Web 服务器提供了许多作为可选模块的功能。您可以启用和禁用这些模块以从 Apache 添加和删除它们的功能。它的 WebDAV 功能包含在您与 Apache 一起安装的模块中，但默认情况下未启用。

您可以使用[a2enmod](http://man.he.net/man8/a2enmod)实用程序为 Apache 启用 WebDAV 模块。以下两个命令将启用 WebDAV 模块：

```bash
sudo a2enmod dav
sudo a2enmod dav_fs
```

现在，重新启动 Apache 以加载新模块：

```bash
sudo systemctl restart apache2.service
```

WebDAV 模块现在已加载并运行。在下一步中，您将配置 Apache 以通过 WebDAV 为您的文件提供服务。

## 第 2 步 – 配置 Apache

在此步骤中，您将创建 Apache 实现 WebDAV 服务器所需的所有配置。

首先，创建 WebDAV 根文件夹，`/var/www/webdav`该文件夹将保存您要通过 WebDAV 提供的文件：

```bash
sudo mkdir /var/www/webdav
```

然后，将 Apache 的用户 设置`www-data`为 WebDAV 目录的所有者：

```bash
sudo chown www-data:www-data /var/www/webdav
```

接下来，您需要为 Apache 用来管理和锁定 WebDAV 用户正在访问的文件的数据库文件创建一个位置。该文件需要 Apache 可读和可写，但不能从网站上获取，因为这可能会泄露敏感信息。

使用`mkdir`数据库文件的实用程序在以下位置创建一个新目录`/usr/local/apache/var/`：

```bash
sudo mkdir -p /usr/local/apache/var/
```

该`-p`选项告诉`mkdir`实用程序在您指定的路径中创建所有目录（如果它们不存在）。

接下来，使用该`chown`实用程序将新目录的所有者和组设置为 Apache 的用户和组：

```bash
sudo chown www-data:www-data /usr/local/apache/var
```

现在，您需要编辑包含有关域名的 Apache 配置的[VirtualHost](http://httpd.apache.org/docs/2.4/vhosts/examples.html)文件。如果您使用 Certbot 注册 SSL 证书`/etc/apache2/sites-enabled/`，`le-ssl.conf`则此文件位于并结束于。

使用文本编辑器打开 VirtualHost 文件：

```bash
sudo nano /etc/apache2/sites-enabled/your_domain-le-ssl.conf
```

在第一行，添加`DavLockDB`指令：

/etc/apache2/sites-enabled/your_domain-le-ssl.conf

```bash
DavLockDB /usr/local/apache/var/DavLock
. . .
```

接下来，在所有其他指令之后的标签中添加以下`Alias`和指令：`Directory``<VirtualHost>`

/etc/apache2/sites-enabled/your_domain-le-ssl.conf

```bash
. . .
Alias /webdav /var/www/webdav

<Directory /var/www/webdav>
    DAV On
</Directory>
```

该[`Alias`](https://httpd.apache.org/docs/2.4/mod/mod_alias.html)指令将请求映射`http://your.server/webdav`到`/var/www/webdav`文件夹。

该[`Directory`](https://httpd.apache.org/docs/current/mod/core.html#directory)指令告诉 Apache 为该`/var/www/webdav`文件夹启用 WebDAV 。您可以[`mod_dav`](https://httpd.apache.org/docs/2.4/mod/mod_dav.html)从 Apache 文档中找到更多信息。

您最终的虚拟主机文件将作如下安排，其中包括`DavLockDB`，`Alias`，和`Directory`在正确的位置指令：

/etc/apache2/sites-enabled/your_domain-le-ssl.conf

```bash
DavLockDB /usr/local/apache/var/DavLock
<IfModule mod_ssl.c>
<VirtualHost *:443>
        ServerAdmin admin@your_domain
        ServerName your_domain
        ServerAlias your_domain
        DocumentRoot /var/www/your_domain
        ErrorLog ${APACHE_LOG_DIR}/error.log
        CustomLog ${APACHE_LOG_DIR}/access.log combined

        SSLCertificateFile /etc/letsencrypt/live/your_domain/fullchain.pem
        SSLCertificateKeyFile /etc/letsencrypt/live/your_domain/privkey.pem
        Include /etc/letsencrypt/options-ssl-apache.conf

        Alias /webdav /var/www/webdav

        <Directory /var/www/webdav>
            DAV On
        </Directory>

</VirtualHost>
</IfModule>
```

如果您在编辑 Apache 的配置时出现任何语法错误，它将拒绝启动。在重新启动 Apache 之前检查您的 Apache 配置是一个很好的做法。

使用该`apachectl`实用程序检查配置：

```bash
sudo apachectl configtest
```

如果您的配置没有错误，`apachectl`将打印`Syntax OK`. 收到此消息后，可以安全地重新启动 Apache 以加载新配置：

```bash
sudo systemctl restart apache2.service
```

您现在已经将 Apache 配置为 WebDAV 服务器来提供来自`/var/www/webdav`. 但是，您尚未配置或启用身份验证，因此可以访问您的服务器的任何人都可以读取、写入和编辑您的文件。在下一部分中，您将启用和配置 WebDAV 身份验证。

## 第 3 步 – 向 WebDAV 添加身份验证

您将使用的身份验证方法称为[摘要身份验证](https://en.wikipedia.org/wiki/Digest_access_authentication#Deprecations)。摘要式身份验证是更安全的 WebDAV 身份验证方法，尤其是与 HTTPS 结合使用时。

摘要式身份验证使用一个文件，该文件存储允许访问 WebDAV 服务器的用户的用户名和密码。就像`DavLockDB`摘要文件需要存储在 Apache 可以读取和写入且无法从您的网站提供服务的位置一样。

由于您已经`/usr/local/apache/var/`为此目的创建了摘要文件，因此您也将在那里放置摘要文件。

首先，使用该实用程序创建一个名为`users.password`at的空文件：`/usr/local/apache/var/``touch`

```bash
sudo touch /usr/local/apache/var/users.password
```

然后将所有者和组更改为，`www-data`以便 Apache 可以对其进行读写：

```bash
sudo chown www-data:www-data /usr/local/apache/var/users.password
```

使用该`htdigest`实用程序将新用户添加到 WebDAV 。以下命令添加用户**sammy**：

```bash
sudo htdigest /usr/local/apache/var/users.password webdav sammy
```

在`webdav`此命令是*境界*，应该被认为是该组要添加新用户的。它也是用户在访问您的 WebDAV 服务器时输入用户名和密码时向用户显示的文本。您可以选择最能描述您的用例的领域。

`htdigest` 运行时会提示你输入密码并确认：

```
OutputAdding user sammy in realm webdav
New password:
Re-type new password:
```

接下来，您将告诉 Apache 要求对 WebDAV 访问进行身份验证并使用该`users.password`文件。

打开您的 VirtualHost 文件：

```bash
sudo nano /etc/apache2/sites-enabled/your_domain-le-ssl.conf
```

然后，在`Directory`指令块中添加以下几行：

/etc/apache2/sites-enabled/your_domain-le-ssl.conf

```bash
AuthType Digest
AuthName "webdav"
AuthUserFile /usr/local/apache/var/users.password
Require valid-user
```

这些指令执行以下操作：

- `AuthType Digest`：使用摘要认证方法。
- `AuthName "webdav"`: 只允许来自`webdav`领域的用户。
- `AuthUserFile /usr/local/apache/var/users.password`：使用包含在`/usr/local/apache/var/users.password`.
- `Require valid-user`：允许访问`users.password`文件中列出的提供正确密码的任何用户。

您的`<Directory>`指令如下：

/etc/apache2/sites-enabled/your_domain-le-ssl.conf

```bash
<Directory /var/www/webdav>
  DAV On
  AuthType Digest
  AuthName "webdav"
  AuthUserFile /usr/local/apache/var/users.password
  Require valid-user
</Directory>
```

接下来，启用`auth_digest`Apache 模块，以便 Apache 知道如何使用摘要式身份验证方法：

```bash
sudo a2enmod auth_digest
```

最后，重新启动 Apache 以加载所有新配置：

```bash
sudo systemctl restart apache2.service
```

您现在已将 WebDAV 服务器配置为使用 HTTPS 和摘要式身份验证。它已准备好开始向您的用户提供文件。在下一部分中，您将从 Windows、Linux 或 macOS 访问 WebDAV 服务器。

## 第 4 步 – 访问 WebDAV

在此步骤中，您将使用 macOS、Windows 和 Linux（KDE 和 GNOME）的本机文件浏览器访问 WebDAV 服务器。

在开始访问您的 WebDAV 服务器之前，您应该将一个文件放入 WebDAV 文件夹中，这样您就有了一个要测试的文件。

使用文本编辑器打开一个新文件：

```bash
sudo nano /var/www/webdav/webdav-testfile.txt
```

添加一些文本，然后保存并退出。现在，将此文件的所有者和组设置为`www-data`：

```bash
sudo chown www-data:www-data /var/www/webdav/webdav-testfile.txt
```

您现在已准备好开始访问和测试您的 WebDAV 服务器。



