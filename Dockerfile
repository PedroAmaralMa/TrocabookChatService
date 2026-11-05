# ==========================================
# Estágio 1: Build da aplicação (com Cache)
# ==========================================
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# 1. Copia apenas os arquivos do Maven para baixar dependências primeiro
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw

# 2. Baixa as dependências e armazena em cache na camada do Docker
RUN ./mvnw dependency:go-offline -B

# 3. Copia o código-fonte e compila a aplicação
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ==========================================
# Estágio 2: Execução leve e segura
# ==========================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Criar usuário não-root por segurança
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copia o JAR gerado no estágio anterior
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Parâmetros otimizados de memória para containers (evita estouro de RAM no Render)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]