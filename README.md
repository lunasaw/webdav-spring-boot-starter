# **[webdav-spring-boot-starter](https://github.com/lunasaw/webdav-spring-boot-starter)**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.lunasaw/webdav-spring-boot-starter)](https://mvnrepository.com/artifact/io.github.lunasaw/webdav-spring-boot-starter)
[![GitHub license](https://img.shields.io/badge/MIT_License-blue.svg)](https://raw.githubusercontent.com/lunasaw/webdav-spring-boot-starter/master/LICENSE)
[![Build Status](https://github.com/lunasaw/webdav-spring-boot-starter/actions/workflows/maven-publish.yml/badge.svg?branch=master)](https://github.com/lunasaw/webdav-spring-boot-starter/actions)

使用SpringBoot-Starter机制，基于`jackrabbit-webdav`打造的webdav-cleint，基于apache2的webdav模块测试，也可以使用其他webdav协议。

提前[安装httpd服务器](./web-dav-install.md)设置指定目录，Httpd操作封装，包括文件上传下载，检测文件是否存在等操作。具体使用见测试类和文档。

## 使用Maven依赖：

```xml
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>webdav-spring-boot-starter</artifactId>
    <version>${latest.version}</version>
</dependency>
```

> [Api文档链接](https://lunasaw.github.io/webdav-spring-boot-starter/docs/)

## webdav-spring-boot-starter使用示例：

### 1.引入依赖

### 2.添加配置

指定spring.webdav.host后，spring启动会自动引入`io.github.lunasaw.webdav.config.WebDavAutoConfiguration`初始化配置。

```yml
spring:
  webdav:
    host: http://127.0.0.1:8080 #指定host
    path: /webdav/project #指定跟路径
    scope: luna #指定项目scope 可选，后续上传可自定义scpoe
    maxTotal: 80 # 最大链接数量通 优化httpclient
    defaultMaxPerRoute: 100
    username: luna # webdav 的basic配置用户名
    password: luna # webdav 的basic配置密码
    auth-type: digest # webdav 认证类型，可选basic，digest，暂不支持：ntlm，kerberos，spnego，negotiate
    openLog: true # 是否开启日志 默认false 开启会打印每次返回的response
```

### 3.使用

所有方法都封装在`io.github.lunasaw.webdav.request.WebDavBaseUtils`使用可以参见`webdav-spring-boot-starter-test`,只需要注入即可使用，详细使用可见`io.github.lunasaw.WebDavTest`

下面是部分例子


```java
@Autowired
private WebDavUtils webDavUtils;
```

> SCOPE_PATH 使用前置测试方法，先获取到SCOPE_PATH，按照上述配置则为 http://127.0.0.1:8080/webdav/project/luna
>
>  @Before
> public void pre() {
>     **SCOPE_PATH** = **webDavSupport**.getScopePath();
> }

#### 上传

下载test模块resource下附带一个测试图片，引用上述配置后，`webDavUtils.upload(IMAGE, file.getAbsolutePath())`上传文件，即可在http://127.0.0.1:8080/webdav/project/luna/IMAGE 下看到所需文件。

```java
@Test
public void a_upload_test() throws FileNotFoundException {
    File file = ResourceUtils.getFile(ResourceUtils.CLASSPATH_URL_PREFIX + IMAGE);
    Assert.isTrue(webDavUtils.upload(IMAGE, file.getAbsolutePath()));
    Assert.isTrue(webDavUtils.exist(SCOPE_PATH + IMAGE));
}
```

#### 下载

下载同理，测试将文件刚刚上传的文件下载到本地目录。`webDavUtils.download(SCOPE_PATH + IMAGE, localPath);`即可

```java
@Test
public void download_test() {
    String localPath = FileTools.getUserHomePath() + "/buy_logo_{}.jpeg";
    localPath = StringTools.format(localPath, RandomUtils.nextInt());
    webDavUtils.download(SCOPE_PATH + IMAGE, localPath);
    Assert.isTrue(FileTools.isExists(localPath), localPath + "文件下载错误");
    FileTools.deleteIfExists(localPath);
}
```

其他使用见io.github.lunasaw.webdav.request.WebDavJackrabbitUtils
