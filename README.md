# Telegram Submission Bot
![Kotlin](https://img.shields.io/badge/kotlin-a879f6?style=for-the-badge&logo=kotlin&logoColor=orange)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=Gradle&logoColor=white)  
[![GitHub Release](https://img.shields.io/github/v/release/LittleMengBot/submissionBot?logo=github)](https://github.com/LittleMengBot/submissionBot/releases)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/LittleMengBot/submissionBot/build.yml?logo=github)
![works on my machine](https://img.shields.io/badge/works%20on-my%20machine-brightgreen)
- [简体中文文档](https://github.com/LittleMengBot/submissionBot/blob/main/README_ZH-CN.md)
## Features
- [x] 100% Kotlin
- [x] Cross-platform
- [x] Easy to deploy
- [x] High availability based on Redis
- [x] Minimal data storage
- [x] Supports all message formats
- [x] Supports multiple images/videos/files
- [x] Multi-language support
- [x] Supports anonymous submission
- [x] User management (ban/unban)
- [x] Simple submission statistics

## Prerequisites
1. Create a bot from @BotFather.
2. Create a review group and invite the bot to the group.
3. Invite the bot to the channel you want to associate with and grant it permissions.

## Deployment
- If you plan to run this project on Ubuntu or CentOS, **ignore the latter part of this document** and simply run the following command, then follow the prompts to configure (make sure the network environment is normal):

```shell
curl -o install.sh -fsSL https://raw.githubusercontent.com/LittleMengBot/submissionBot/main/install.sh && chmod +x install.sh && sudo ./install.sh
```
- Please reserve at least 1GB of hard disk space and at least 512MB of free memory for a fresh system.
- This script will automatically configure the runtime environment and start the bot to test if the bot has any issues. If everything goes well, please press Ctrl+C to exit the script and run:
```shell
service [the service name you set in the script] start
```
### Dependencies (jdk>=17, Redis>=7.0)
### Configuration file
Note: The configuration file is stored in the same directory as the jar package and named config.properties:
```shell
cp config.properties.example config.properties
vim config.properties
```
Parameters:  
```properties
admin=1234567890
channel=@durov
group=-1001011111111
lang=zh-CN
redisHost=127.0.0.1:6379
token=1234567890:AABBBBBBBBBBBBB_ZZ_CCCCCDDDDDDDEEEE
```
Field Description:

- ```token``` (required): Get it from @BotFather.
- ```admin``` (required): The user_id of the administrator, which can be obtained through @userinfobot.
- ```channel``` (required): The name of the channel. If it is a private channel, please obtain the channel ID through @userinfobot and directly fill in it.
- ```group```: optional. If not set, after the project is started, you need to add your bot to the review group. The bot will forward the submissions received to the review group, and everyone in the group can review the submissions. If you don't have a review group, you should create one. After adding the bot to the review group, use the /setgroup command in the review group to initialize the bot (this command can set the current group as the review group), which only needs to be run once.
- ```lang```: language. You can check and set it through the ```/setlang``` command.
- ```redisHost```: optional, default is ```127.0.0.1:6379```.
### Running
```java -jar release.jar```
- Warn：If you need to run on Windows directly(recommend to use [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install)):
```
chcp 65001
java -Dfile.encoding=UTF-8 -jar release.jar
```
Check if the program reports any errors and configure it according to the prompts and this document.

It is recommended to manage the operation of the bot through the corresponding service management tool of the system. For example, systemd, launchd, Windows Service Manager, etc.

## Description
- Permanent user data stored by this bot: Only the user_id of the user.
- Data stored by this bot but will be automatically destroyed after a certain period (up to one month after receiving the submission): The content of the submission (excluding any messages from the contributor).

## Compilation and Packaging
First, clone this repository. Then, run the following command in the project root directory:
```shell
./gradlew jar
```
The output will be located at $PROJECT/build/libs.

**It is recommended to use IDEA for development.**
## Acknowledgments
Refer to ```build.gradle.kts``` for details.
