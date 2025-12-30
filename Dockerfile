#penjdk:8-jre 为基础镜像，来构建此镜像，可以理解为运行的需要基础环境还可以用 java:8
FROM eclipse-temurin:8-jre-alpine
#WORKDIR指令用于指定容器的一个目录， 容器启动时执行的命令会在该目录下执行(jar包容器内路径)。
WORKDIR /root/ikuai/
#将当前metabase.jar 复制到容器根目录下
ADD ikuaiSimple-1.0-SNAPSHOT.jar ikuaiSimple-1.0-SNAPSHOT.jar
#将依赖包 复制到容器根目录/libs下,metabase.jar已不再需要添加其它jar包
#ADD libs /libs
#暴露容器端口为3000 Docker镜像告知Docker宿主机应用监听了8056端口
EXPOSE 8056
#容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "ikuaiSimple-1.0-SNAPSHOT.jar", "--server.address=0.0.0.0"]
