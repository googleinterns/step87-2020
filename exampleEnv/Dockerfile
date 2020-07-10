FROM maven

WORKDIR /workspace

ADD . .
RUN chmod +x start.sh
RUN mvn verify --fail-never

CMD ["/workspace/start.sh"]