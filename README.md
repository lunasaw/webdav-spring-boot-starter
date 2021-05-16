# luna-webdav

Httpd操作封装,包括文件上传下载，检测是否存在等操作集成springboot starter

## Maven依赖：

```xml

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <dependency>
        <groupId>org.apache.jackrabbit</groupId>
        <artifactId>jackrabbit-webdav</artifactId>
        <version>${jackrabbit-webdav.version}</version>
    </dependency>

    <dependency>
        <groupId>com.github.czy1024</groupId>
        <artifactId>luna-common</artifactId>
        <version>1.1.4</version>
    </dependency>
</dependencies>
```

## webdav-spring-boot-starter使用示例：

```text
     /**
     * 上传文件 路径不存在则创建
     * 
     * @param url 网络文件路径
     * @param fis 文件流
     * @return
     */
    public boolean upload(String url, FileInputStream fis) 
      
      /**
     * 上传文件 路径不存在则递归创建目录 不能覆盖
     *
     * @param url 网络文件路径
     * @param path 文件路径
     * @return
     */
    public boolean upload(String url, String path, boolean isCreate)
    
    /**
     * 上传文件 路径不存在则递归创建目录 默认覆盖 不存在则创建
     *
     * @param url 网络文件路径
     * @param path 文件流
     * @return
     */
    public boolean upload(String url, String path) 
    
     /**
     * 上传文件 路径不存在则递归创建目录，文件存在则覆盖
     * 
     * @param url 网络路径
     * @param path 文件
     * @param isCreate 路径不存在是否创建文件夹
     * @param cover 是否覆盖
     * @return
     * @throws IOException
     */
    public boolean upload(String url, String path, boolean isCreate, boolean cover)
    
    /**
     * 递归创建文件
     * 
     * @param path 文件网络路径
     * @throws IOException
     */
    public void makeDirs(String path) 
    
    /**
     * 判断文件或者文件夹是否存在
     * 
     * @param url 路径
     * @return
     */
    public boolean existDir(String url)
    
    /**
     * 删除文件或者文件夹
     * 
     * @param url 文件路径
     * @return
     */
    public Boolean delete(String url)
    
    
    /**
     * 创建文件夹
     * 
     * @param url 路径
     * @return
     */
    private boolean makeDir(String url) 
    
     /**
     * 下载文件
     * 
     * @param url 路径
     * @param filePath 文件存储路径
     * @throws IOException
     */
    public void download(String url, String filePath) 
      
```

