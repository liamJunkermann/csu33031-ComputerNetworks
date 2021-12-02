FROM adoptopenjdk/openjdk14:latest
# WORKDIR /src
ENV DISPLAY=host.docker.internal:0
# COPY ./src ./src
RUN apt-get update -y && apt-get install libxrender1 libxtst6 libxi6 -y
# CMD [ "java -cp . " ]
CMD [ "/bin/bash" ]