# Telegram 投稿机器人
![Kotlin](https://img.shields.io/badge/kotlin-a879f6?style=for-the-badge&logo=kotlin&logoColor=orange)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=Gradle&logoColor=white)  
[![GitHub Release](https://img.shields.io/github/v/release/LittleMengBot/submissionBot?logo=github)](https://github.com/LittleMengBot/submissionBot/releases)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/LittleMengBot/submissionBot/release.yml?logo=github)
![works on my machine](https://img.shields.io/badge/works%20on-my%20machine-brightgreen)
## 特性
- [x] 100% Kotlin
- [x] 跨平台
- [x] 易部署
- [x] 基于Redis的高可用性
- [x] 最低限度的数据存储
- [x] 支持全格式消息投稿
- [x] 支持多图/多视频/多文件投稿
- [x] 多语言支持
- [x] 支持匿名投稿
- [x] 用户管理（封禁、解封）
- [x] 简单投稿统计
## 准备工作
- 1.从 @BotFather 创建一个机器人。
- 2.建立一个审稿群并邀请机器人进群。
- 3.邀请机器人加入你想要关联的频道，并授予权限。
## 部署
- 如果您准备在Ubuntu或者CentOS上运行本项目，**请忽略本文档的后半部分**，直接运行下面的傻瓜命令，然后按照提示进行配置（确保网络环境正常）：
```shell
curl -o install.sh -fsSL https://raw.githubusercontent.com/LittleMengBot/submissionBot/main/install.sh && chmod +x install.sh && sudo ./install.sh
```
- 全新系统请预留至少1GB的硬盘空间与至少512MB的空闲内存 
- 本脚本会自动配置好运行环境并启动机器人以测试机器人是否有问题。如果一切顺利，请按Ctrl+C退出脚本，并运行：
```shell
service [你在脚本中设置的服务名称] start
```
### 依赖（jdk>=17，Redis>=7.0）
### 配置文件
注意：配置文件保存在jar包同级目录。 命名为```config.properties``` 
```shell
cp config.properties.example config.properties
vim config.properties
```
#### 参数：
```properties
admin=1234567890
channel=@durov
group=-1001011111111
lang=zh-CN
redisHost=127.0.0.1:6379
token=1234567890:AABBBBBBBBBBBBB_ZZ_CCCCCDDDDDDDEEEE
```
字段说明：
- ```token```（必填）：从 @BotFather 获取。
- ```admin```（必填）：管理员的user_id，可以通过 @userinfobot 获取。  
- ```channel```（必填）：频道名。如果是私有频道，请通过 @userinfobot 获取频道id并直接填写。  
- ```group```：可空。如果不进行设置，项目启动后，需要你的机器人添加到审稿群，机器人会将收到的稿件转发至审稿群，群内所有人皆可审核稿件，如果你没有审稿群，你应该建立一个。将机器人加到审稿群后，在审稿群使用 /setgroup 命令来初始化机器人 (此命令可设置当前群为审稿群)，只需运行一次。  
- ```lang```：语言。可以通过 ```/setlang``` 命令查看与设置。
- ```redisHost```：可空，默认为 ```127.0.0.1:6379```  
### 运行
```java -jar release.jar```  
- 注意：如果您需要在Windows系统上直接运行，请运行下面的命令（推荐使用[WSL2](https://learn.microsoft.com/en-us/windows/wsl/install)）：
```
chcp 65001
java -Dfile.encoding=UTF-8 -jar release.jar
```
检查程序是否报错，并根据提示信息与本文档进行配置。
#### 机器人的运行，建议通过系统对应的服务管理工具进行管理。例如systemd，launchd，Windows Service Manager等。  
以Ubuntu为例：
```shell
cp submissionBot.service.example /etc/systemd/system/submissionBot.service
```
然后修改```WorkingDirectory```与```ExecStart```的对应路径为jar文件所在的路径。
之后运行：
```shell
service submissionBot start
```
## 说明
- 本机器人长期存储的用户数据：仅有投稿者的user_id
- 本机器人存储但会定时销毁的数据（最长为自收到稿件起的一个月后）：稿件内容（不包含投稿者的任何消息）
## 编译打包
首先clone本仓库。在项目根目录下运行：  
```shell
./gradlew jar
```  
输出到```$PROJECT/build/libs```  
**推荐使用IDEA进行开发。**   
## 感谢  
详见```build.gradle.kts```
