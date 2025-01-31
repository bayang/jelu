FROM eclipse-temurin:17-jre-noble

WORKDIR app

ARG TARGETPLATFORM
ENV TARGETPLATFORM=${TARGETPLATFORM:-linux/amd64}

ARG DEPENDENCY=build/dependency
COPY ${DEPENDENCY}/dependencies/ ./
COPY ${DEPENDENCY}/spring-boot-loader/ ./
COPY ${DEPENDENCY}/snapshot-dependencies/ ./
COPY ${DEPENDENCY}/application/ ./

ENV JELU_DATABASE_PATH="/database/"
ENV JELU_FILES_IMAGES="/files/images/"
ENV JELU_FILES_IMPORTS="/files/imports/"
ENV JELU_METADATA_CALIBRE_PATH="/usr/bin/fetch-ebook-metadata"
ENV LC_ALL=en_US.UTF-8
ENV QTWEBENGINE_DISABLE_SANDBOX="1"

RUN apt-get update && apt-get install --no-install-recommends --yes calibre \
      && curl -L -o goodreads.zip https://github.com/kiwidude68/calibre_plugins/releases/download/goodreads-v1.8.2/goodreads-v1.8.2.zip \
      && calibre-customize --add-plugin goodreads.zip \
      && rm goodreads.zip;

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher", "--spring.config.additional-location=optional:file:/config/"]
EXPOSE 11111
