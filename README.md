Setup
=====

Put a `kafka-config` wrapper script somewhere in your `$PATH`:

```bash
#!/bin/sh

docker run -it --rm --name kafka-config \
 -v `pwd`/conf:/conf \
 -v `pwd`/out:/out \
 smartislav/kafka-tool:0.2.0 \
 -Dconfig.file=/conf/application.conf \
 $@
```

Be sure to `chmod +x` it.


Commands
========

### reassign repair

Ensure that you have *at least* a configured number of replicas:

```bash
kafka-tool reassign -o /out/reassignment.json repair
kafka-reassign-partitions --zookeeper zk1:2181/kafka,zk2:2181/kafka --reassignment-json-file `pwd`/out/reassignment.json --execute
kafka-reassign-partitions --zookeeper zk1:2181/kafka,zk2:2181/kafka --reassignment-json-file `pwd`/out/reassignment.json --verify
```

### reassign cleanup

Remove extra replicas that might have been created by the Kafka built-in partition reassignment tool:

```bash
kafka-tool reassign -o /out/reassignment.json cleanup
# then run kafka-reassign-partitions as in previous example
```

### list-superfluous-topics

List topics that are no longer necessary (i.e. not in the config file)

```bash
kafka-tool list-superfluous-topics
```

### update

Update per-topic configuration (but not no. of partitions or replication factor)

```bash
kafka-tool update --alter-if-needed
```

### update-acls

Update per-topic ACL configuration

```bash
kafka-tool update-acls [--dry-run]
```

### list-acls

List ACLS configured on Kafka

```bash
kafka-tool list-acls
```



