from invoke import task

KAFKA_NODES = [('kafka01.internal-service', '9092'),
               ('kafka02.internal-service', '9093'),
               ('kafka03.internal-service', '9094')]

@task
def stage(c):
    """
    Stage apps.
    """
    c.run("sbt clean " +
          " ".join([item + "/docker:stage" for item in ['fileLoader', 'summarizer', 'api']]))


@task
def up(c):
    """
    Start containers.
    """
    stage(c)
    c.run("docker build postgres/ -t postgres:reviews")
    c.run("docker-compose up --no-start --build --force-recreate")
    c.run("docker-compose start postgres")
    c.run("docker-compose start zookeeper")
    for (kafkaNode, port) in KAFKA_NODES:
        c.run("docker-compose start " + kafkaNode)
        print("Waiting for {} to start...".format(kafkaNode))
        c.run("docker-compose exec -T {} cub kafka-ready -b localhost:{} 1 40".
              format(kafkaNode, port))
    c.run("docker-compose exec -T {} kafka-topics --create --if-not-exists --zookeeper zookeeper:2181 --replication-factor 2 --partitions 3 --topic {}"
                .format(KAFKA_NODES[0][0], "reviews"))
    print("Kafka topics:")
    topics(c)
    c.run("docker-compose start summarizer")
    c.run("docker-compose start file-loader")
    c.run("docker-compose start api")
    c.run("docker-compose start frontend")
    ps(c)


@task
def ps(c):
    """
    Lists containers.
    """
    c.run("docker-compose ps")


@task
def down(c):
    """
    Stops containers and removes containers, networks, volumes, and images.
    """
    c.run("docker-compose down")


@task
def topics(c):
    """
    List all topics.
    """
    c.run(
        "docker-compose exec -T {} kafka-topics --list --zookeeper zookeeper:2181"
        .format(KAFKA_NODES[0][0]))


@task(help={'topic': "Topic to consume."})
def consume(c, topic):
    """
    Consume a topic.
    """
    c.run(
        "docker-compose exec -T {} kafka-console-consumer --bootstrap-server localhost:9092 --topic {} --from-beginning"
        .format(KAFKA_NODES[0][0], topic))

@task
def logs(c, name):
    """docker-compose logs"""
    c.run("docker-compose logs -f {}".format(name))
