# luna-webdav

提前安装httpd服务器设置指定目录，Httpd操作封装,包括文件上传下载，检测是否存在等操作集成springboot starter

## Maven依赖：

```xml
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>webdav-spring-boot-starter</artifactId>
    <version>${latest.version}</version>
</dependency>
```

[Api文档链接](https://lunasaw.github.io/webdav-spring-boot-starter/api_doc/)

## webdav-spring-boot-starter使用示例：

### 引入以来

### 添加配置

指定luna.webdav.host后，spring启动会自动引入`io.github.lunasaw.webdav.config.WebDavAutoConfiguration`初始化配置。

```yml
luna:
  webdav:
    host: http://127.0.0.1:8080 #指定host
    path: /webdav/project #指定跟路径
    scope: test #指定项目scope 可选，后续上传可自定义scpoe
    maxTotal: 80 # 最大链接数量通 优化httpclient
    defaultMaxPerRoute: 100
    username: luna # webdav 的basic配置用户名
    password: luna # webdav 的basic配置密码
```

### 使用方法

所有方法都封装在`io.github.lunasaw.webdav.WebDavUtils`使用可以参见`webdav-spring-boot-starter-test`,只需要注入即可使用

```java
    @Autowired
    private WebDavUtils webDavUtils;
```

#### 上传



#### 下载

#### 查看路径

#### 
