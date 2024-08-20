FROM eclipse-temurin:17-jre-jammy

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
ENV JELU_METADATA_CALIBRE_PATH="/calibre/fetch-ebook-metadata"
ENV LC_ALL=en_US.UTF-8

RUN if [ "$TARGETPLATFORM" = "linux/amd64" ] ; then \
      apt-get update && apt-get install --no-install-recommends --yes wget xz-utils \
      libxcb-xinerama0 \
      libxcb-icccm4 \
      libxcb-image0 \
      libxcb-keysyms1 \
      libxcb-render-util0 \
      libxcb-randr0 \
      libopengl0 \
      libegl1 \
      libglx0 \
      libxkbcommon-x11-0 \
      libxcb-cursor0 \
      python3 \
      python3-xdg \
      binutils \
      poppler-utils \
      libqpdf28 \
      libnss3 \
      && apt-get clean \
      && rm -rf /var/lib/apt/lists/* \
      && wget -nv -O- https://download.calibre-ebook.com/linux-installer.sh | sh /dev/stdin install_dir=/ isolated=y \
      && strip --remove-section=.note.ABI-tag /calibre/lib/libQt6Core.so.6 \
      && curl -L -o goodreads.zip https://github.com/kiwidude68/calibre_plugins/releases/download/goodreads-v1.8.2/goodreads-v1.8.2.zip \
      && /calibre/calibre-customize --add-plugin goodreads.zip \
      && rm goodreads.zip; \
  else \
      apt-get update && apt-get install --no-install-recommends --yes calibre \
      && curl -L -o goodreads.zip https://github.com/kiwidude68/calibre_plugins/releases/download/goodreads-v1.8.2/goodreads-v1.8.2.zip \
      && calibre-customize --add-plugin goodreads.zip \
      && rm goodreads.zip; \
  fi

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher", "--spring.config.additional-location=optional:file:/config/"]
EXPOSE 11111
