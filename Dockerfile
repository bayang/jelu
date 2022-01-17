FROM eclipse-temurin:11.0.13_8-jre-focal

WORKDIR app

ARG DEPENDENCY=build/dependency
COPY ${DEPENDENCY}/dependencies/ ./
COPY ${DEPENDENCY}/spring-boot-loader/ ./
COPY ${DEPENDENCY}/snapshot-dependencies/ ./
COPY ${DEPENDENCY}/application/ ./

ENV JELU_DATABASE_PATH="/database/"
ENV JELU_FILES_DIR="/files/"
ENV LC_ALL=en_US.UTF-8

RUN apt-get update \
    && apt-get install --no-install-recommends --yes wget \
    xz-utils \
    xdg-utils \
    libxcb-xinerama0 \
    libxcb-icccm4 \
    libxcb-image0 \
    libxcb-keysyms1 \
    libxcb-render-util0 \
    libxcb-randr0 \
    qt5-default \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN wget -nv -O- https://download.calibre-ebook.com/linux-installer.sh | sh /dev/stdin install_dir=/ isolated=y

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher", "--spring.config.additional-location=optional:file:/config/"]
EXPOSE 11111
