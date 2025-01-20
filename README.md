# Kafka to Elasticsearch Sink

This application consumes messages from a Kafka topic containing website click events and stores them in Elasticsearch. The messages are processed in order per partition and stored in time-based indices that rotate every 15 minutes.

## Features

- Maintains message ordering within partitions
- Creates time-based indices (format: {topic_name}_{yyyy-MM-dd-HH:mm})
- Index rotation every 15 minutes
- Error handling and logging
- Configuration via HOCON files
- Built with fs2-kafka and elastic4s

## Prerequisites

- Docker and Docker Compose
- Scala 3
- SBT

## Running the Application

1. Start the required services using Docker Compose:

```bash
docker-compose up -d
```

2. Run the application:

```bash
sbt run
```

## Runing unit test

```bash
sbt test
```

## Running it test

```bash
docker-compose up -d
```

```bash
sbt itTest
```

```bash
docker-compose down
```

## Error Handling

I have not added retry policy in case of errors. However, I was planning to:

1. Add retry to the Kafka consumer in case of failure when reading from kafka
2. Add retry in case of failures when writing into ElasticSearch


## Configuration

The application is configured via `application.conf`. Key configuration parameters:

- Kafka bootstrap servers
- Schema registry URL
- Topic name
- Consumer group ID
- Elasticsearch host and port

## Logging

The application uses log4cats with slf4j backend for structured logging. Log levels can be configured in `logback.xml`.