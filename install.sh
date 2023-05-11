#!/bin/bash

function check_su() {
  if [[ $EUID -ne 0 ]]; then
    echo "权限不足。请使用sudo命令执行本脚本。"
    exit 1
  fi
}

function download_jar() {
  echo "正在同步程序主体..."
  LINK=$(curl -s https://api.github.com/repos/LittleMengBot/submissionBot/releases/latest | jq --raw-output '.assets[0] | .browser_download_url')
  curl -L -o release.jar "$LINK"
  echo "同步成功。"
}

function check_java() {
  JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')

  if [[ "$(echo "$JAVA_VERSION" | cut -d'.' -f1)" -ge "17" ]]; then
    echo "Java JDK "$JAVA_VERSION"已安装"
  else
    echo "未安装Java JDK 17或更高版本。"
    echo "正在尝试安装..."
    install_openjdk_17_jdk
  fi
}

function install_openjdk_17_jdk {
  if command -v apt-get &>/dev/null; then
    sudo apt-get update
    sudo apt-get install -y openjdk-17-jdk
  elif command -v yum &>/dev/null; then
    sudo yum update
    sudo yum install -y java-17-openjdk-devel
  elif command -v pacman &>/dev/null; then
    sudo pacman -Syu
    sudo pacman -S jdk-openjdk
  else
    echo "无法在当前系统中找到适合的软件包管理器。请手动安装Java JDK 17或更高版本。"
    exit 1
  fi
  JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
  if [[ "$(echo "$JAVA_VERSION" | cut -d'.' -f1)" -ge "17" ]]; then
    echo "Java JDK "$JAVA_VERSION"已安装"
  else
    echo "请手动安装Java JDK 17或更高版本。"
    exit 1
  fi
}

function check_redis {
  if ! command -v redis-cli &>/dev/null; then
    echo "Redis未安装"
    echo "正在安装Redis..."
    if command -v apt-get &>/dev/null; then
      sudo apt-get update
      sudo apt-get install -y software-properties-common
      sudo apt-get update
      sudo apt-get install -y redis
      check_redis
    elif command -v yum &>/dev/null; then
      sudo yum update
      sudo yum --enablerepo=remi install redis
      check_redis
    elif command -v pacman &>/dev/null; then
      sudo pacman -Syu
      sudo pacman -S redis
      check_redis
    else
      echo "请手动安装Redis。"
      exit 1
    fi
    return 1
  fi

  if ! redis-cli ping &>/dev/null; then
    echo "Redis已安装但未启动"
    echo "正在启动Redis..."
    for i in {1..3}; do
      sudo systemctl start redis
      if redis-cli ping &>/dev/null; then
        echo "Redis已成功启动"
        return 0
      fi
      echo "尝试启动Redis失败，正在重试..."
      sleep 2
    done
    echo "启动失败，请手动启动Redis。"
    exit 1
  fi

  echo "Redis已安装并已启动"
  return 0
}

function create_service_file() {
  read -p "请输入Service名称（不能有空格，纯英文，不超过20个字符，记住输入的内容）:" SERVICE_NAME
  while [[ ! $SERVICE_NAME =~ ^[a-zA-Z0-9_-]{1,20}$ ]]; do
    read -p "Service名称不符合规范，请重新输入（不能有空格，纯英文，不超过20个字符）:" SERVICE_NAME
  done

  read -p "请输入Service描述（不能有空格，纯英文，不超过20个字符）:" SERVICE_DESCRIPTION
  while [[ ! $SERVICE_DESCRIPTION =~ ^[a-zA-Z0-9_-]{1,20}$ ]]; do
    read -p "Service描述不符合规范，请重新输入（不能有空格，纯英文，不超过20个字符）:" SERVICE_DESCRIPTION
  done

  if command -v yum &>/dev/null; then
    SERVICE_FILE="/etc/init.d/${SERVICE_NAME}.service"
  else
    SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"
  fi
  WORKING_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

  cat /dev/null >"$SERVICE_FILE"
  echo "[Unit]" >"$SERVICE_FILE"
  echo "Description=$SERVICE_DESCRIPTION" >>"$SERVICE_FILE"
  echo "After=multi-user.target" >>"$SERVICE_FILE"
  echo "" >>"$SERVICE_FILE"
  echo "[Service]" >>"$SERVICE_FILE"
  echo "Type=idle" >>"$SERVICE_FILE"
  echo "WorkingDirectory=$WORKING_DIRECTORY" >>"$SERVICE_FILE"
  echo "ExecStart=/usr/bin/java -jar $WORKING_DIRECTORY/release.jar" >>"$SERVICE_FILE"
  echo "Restart=always" >>"$SERVICE_FILE"
  echo "RestartSec=1" >>"$SERVICE_FILE"
  echo "StartLimitInterval=0" >>"$SERVICE_FILE"
  echo "" >>"$SERVICE_FILE"
  echo "[Install]" >>"$SERVICE_FILE"
  echo "WantedBy=multi-user.target" >>"$SERVICE_FILE"

  echo "Service文件创建成功: $SERVICE_FILE"
}

function write_config() {
  echo "请按要求输入下面的配置字段:"
  read -p "Bot token（从 @BotFather 获取）:" token
  while [[ -z $token ]]; do
    read -p "不能为空，请重新输入:" token
  done

  read -p "管理员id（从 @userinfobot 获取）: " admin
  while [[ -z $admin ]]; do
    read -p "不能为空，请重新输入:" admin
  done

  read -p "频道名（例如 @durovChannel ）或频道id: " channel
  while ! [[ ${#channel} -le 30 ]]; do
    read -p "格式错误，请重新输入:" channel
  done

  read -p "审稿群id（可选，按回车跳过）: " group
  group=${group:--1}
  read -p "语言 （可选，按回车跳过，默认为中文）: " lang
  lang=${lang:-zh-CN}

  read -p "Redis地址 (可选，按回车跳过，默认为 127.0.0.1:6379): " redisHost
  redisHost=${redisHost:-127.0.0.1:6379}

  echo "正在写入到config.properties..."
  cat /dev/null >"$(dirname $0)/config.properties"
  echo "token=$token" >"$(dirname $0)/config.properties"
  echo "admin=$admin" >>"$(dirname $0)/config.properties"
  echo "channel=$channel" >>"$(dirname $0)/config.properties"
  [[ -n $group ]] && echo "group=$group" >>"$(dirname $0)/config.properties"
  echo "lang=$lang" >>"$(dirname $0)/config.properties"
  [[ -n $redisHost ]] && echo "redisHost=$redisHost" >>"$(dirname $0)/config.properties"
  echo "写入成功！"
}

function check_package_manager() {
  if command -v apt-get &>/dev/null; then
    sudo apt-get update
    sudo apt-get install jq software-properties-common -y
  elif command -v yum &>/dev/null; then
    sudo yum update
    sudo yum install jq -y
  elif command -v pacman &>/dev/null; then
    sudo pacman -Syu
    sudo pacman -S jq -y
  else
    echo "不支持的包管理器。"
    exit 1
  fi
}

function test() {
  echo "开始试运行，如果运行没有问题，请使用Ctrl+C终止脚本，并使用：service [刚才设置的服务名] start 启动服务。"
  /usr/bin/java -jar $(dirname $0)/release.jar
}

function init() {
  check_su
  check_package_manager
  download_jar
  check_java
  check_redis
  create_service_file
  write_config
  test
}

function update() {
  check_package_manager
  download_jar
  echo "请重启服务：service [设置的服务名] restart"
}

function config() {
  check_su
  create_service_file
  write_config
  test
}

if [ $# -eq 0 ]; then
  read -p "请输入功能数字（1.安装；2.更新；3.配置）：" option
else
  option="$1"
fi

case "$option" in
1 | init)
  init
  ;;
2 | update)
  update
  ;;
3 | config)
  config
  ;;
*)
  echo "输入不合法"
  exit 1
  ;;
esac
