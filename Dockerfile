FROM eclipse-temurin:11.0.13_8-jre-focal

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

RUN echo target platform $TARGETPLATFORM

RUN if [ "$TARGETPLATFORM" = "linux/amd64" ] ; then \
      apt-get update && apt-get install --no-install-recommends --yes wget xz-utils \
      xdg-utils \
      libxcb-xinerama0 \
      libxcb-icccm4 \
      libxcb-image0 \
      libxcb-keysyms1 \
      libxcb-render-util0 \
      libxcb-randr0 \
      qt5-default \
      && apt-get clean \
      && rm -rf /var/lib/apt/lists/* \
      && wget -nv -O- https://download.calibre-ebook.com/linux-installer.sh | sh /dev/stdin install_dir=/ isolated=y \
      && curl https://www.mobileread.com/forums/attachment.php\?attachmentid\=182200\&d\=1641360812 > goodreads.zip \
      && /calibre/calibre-customize --add-plugin goodreads.zip \
      && rm goodreads.zip; \
  else \
      apt-get update && apt-get install --no-install-recommends --yes calibre \
      && curl https://www.mobileread.com/forums/attachment.php\?attachmentid\=182200\&d\=1641360812 > goodreads.zip \
      && calibre-customize --add-plugin goodreads.zip \
      && rm goodreads.zip; \
  fi

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher", "--spring.config.additional-location=optional:file:/config/"]
EXPOSE 11111
