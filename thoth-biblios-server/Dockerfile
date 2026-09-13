# Builds the native Thoth Biblios Server binary.
#
#   docker build -f thoth-biblios-server/Dockerfile -t thoth-biblios-server .
#
# The native binary is platform-specific; build it on/for the target architecture.
# Production uses the Linux amd64 artifact built by CI.

FROM ghcr.io/graalvm/native-image-community:25 AS build
WORKDIR /build
COPY . .
RUN ./gradlew --no-daemon :thoth-biblios-server:nativeCompile

FROM gcr.io/distroless/base-debian12
COPY --from=build /build/thoth-biblios-server/build/native/nativeCompile/thoth-biblios-server /thoth-biblios-server
EXPOSE 8080
ENTRYPOINT ["/thoth-biblios-server"]
