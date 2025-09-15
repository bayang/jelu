FROM eclipse-temurin:17-jre-noble

WORKDIR app

ARG TARGETPLATFORM
ENV TARGETPLATFORM=${TARGETPLATFORM:-linux/amd64}

ENV JELU_DATABASE_PATH="/database/"
ENV JELU_FILES_IMAGES="/files/images/"
ENV JELU_FILES_IMPORTS="/files/imports/"
ENV JELU_METADATA_CALIBRE_PATH="/usr/bin/fetch-ebook-metadata"
ENV LC_ALL=en_US.UTF-8
ENV QTWEBENGINE_DISABLE_SANDBOX="1"

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
      python3-xdg \
      binutils \
      poppler-utils \
      libqpdf29t64 \
      libnss3 \
      ttf-wqy-zenhei \
      fonts-wqy-microhei \
      libxcomposite1 \
      libxdamage1 \
      libxfixes3 \
      libxrandr2 \
      libxtst6 \
      libxkbfile1 \
      fcitx-rime && \
      apt-get install -y speech-dispatcher \
      && apt-get clean \
      && rm -rf /var/lib/apt/lists/* && \
      mkdir -p \
        /opt/calibre && \
      CALIBRE_VERSION="8.4.0" && \
      CALIBRE_URL="https://download.calibre-ebook.com/${CALIBRE_VERSION}/calibre-${CALIBRE_VERSION}-x86_64.txz" && \
      curl -o \
        /tmp/calibre-tarball.txz -L \
        "$CALIBRE_URL" && \
      tar xvf /tmp/calibre-tarball.txz -C \
        /opt/calibre && \
      /opt/calibre/calibre_postinstall && \
      strip --remove-section=.note.ABI-tag /opt/calibre/lib/libQt6Core.so.6 \
      && curl -L -o goodreads.zip https://github.com/kiwidude68/calibre_plugins/releases/download/goodreads-v1.8.2/goodreads-v1.8.2.zip \
      && /opt/calibre/calibre-customize --add-plugin goodreads.zip \
      && rm goodreads.zip; \
  elif [ "$TARGETPLATFORM" = "linux/arm64" ]; then \
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
      python3-xdg \
      binutils \
      poppler-utils \
      libqpdf29t64 \
      libnss3 \
      ttf-wqy-zenhei \
      fonts-wqy-microhei \
      libxcomposite1 \
      libxdamage1 \
      libxfixes3 \
      libxrandr2 \
      libxtst6 \
      libxkbfile1 \
      fcitx-rime && \
      apt-get install -y speech-dispatcher \
      && apt-get clean \
      && rm -rf /var/lib/apt/lists/* && \
      mkdir -p \
        /opt/calibre && \
      CALIBRE_VERSION="8.4.0" && \
      CALIBRE_URL="https://download.calibre-ebook.com/${CALIBRE_VERSION}/calibre-${CALIBRE_VERSION}-arm64.txz" && \
      curl -o \
        /tmp/calibre-tarball.txz -L \
        "$CALIBRE_URL" && \
      tar xvf /tmp/calibre-tarball.txz -C \
        /opt/calibre && \
      /opt/calibre/calibre_postinstall && \
      strip --remove-section=.note.ABI-tag /opt/calibre/lib/libQt6Core.so.6 \
      && curl -L -o goodreads.zip https://github.com/kiwidude68/calibre_plugins/releases/download/goodreads-v1.8.2/goodreads-v1.8.2.zip \
      && /opt/calibre/calibre-customize --add-plugin goodreads.zip \
      && rm goodreads.zip; \
  else \
    apt-get update && apt-get install --no-install-recommends --yes calibre \
          && curl -L -o goodreads.zip https://github.com/kiwidude68/calibre_plugins/releases/download/goodreads-v1.8.2/goodreads-v1.8.2.zip \
          && calibre-customize --add-plugin goodreads.zip \
          && rm goodreads.zip; \
  fi

ARG DEPENDENCY=build/dependency
COPY ${DEPENDENCY}/dependencies/ ./
COPY ${DEPENDENCY}/spring-boot-loader/ ./
COPY ${DEPENDENCY}/snapshot-dependencies/ ./
COPY ${DEPENDENCY}/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher", "--spring.config.additional-location=optional:file:/config/"]
EXPOSE 11111
