# 使用 eclipse-temurin openjdk 镜像作为 build 工具，用来将 jar 包结构
FROM eclipse-temurin:17-jdk AS builder
# 设置工作目录
WORKDIR /app
# 目标jar文件位置
ARG JAR_FILE=target/*.jar
# 将目标jar复制进docker工作目录
COPY ${JAR_FILE} wechatter.jar
# 使用 spring boot layertools 工具将 jar 包进行结构，可以达到分层效果，加快镜像构建、容器启动效率
RUN java -Djarmode=layertools -jar wechatter.jar extract

# 使用 eclipse-temurin openjdk 作为制作最后镜像的基础
FROM eclipse-temurin:17-jdk
# 设置工作目录
WORKDIR /wechatter
# 将在上一个容器中，结构jar包后得到相关目录文件复制到当前镜像中
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./
# 暴露端口
EXPOSE 8080
# 容器启动命令
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]