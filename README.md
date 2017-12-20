Usage
=====

`kafka-config` wrapper script:

```bash
#!/bin/sh

docker run -it --rm --name kafka-config \
 -v `pwd`/conf:/conf \
 -v `pwd`/out:/out \
 smartislav/kafka-tool:0.1 \
 -Dconfig.file=/conf/application.conf \
 $@
```

Be sure to `chmod +x` it.

Ensure that you have *at least* a configured number of replicas:

```bash
kafka-tool repair -o /out/reassignment.json
kafka-reassign-partitions --zookeeper zk1:2181/kafka,zk2:2181/kafka --reassignment-json-file `pwd`/out/reassignment.json --execute
kafka-reassign-partitions --zookeeper zk1:2181/kafka,zk2:2181/kafka --reassignment-json-file `pwd`/out/reassignment.json --verify
```

Remove extra replicas that might have been created by the Kafka built-in partition reassignment tool:

```bash
kafka-tool cleanup -o /out/reassignment.json
# then run kafka-reassign-partitions as in previous example
```
