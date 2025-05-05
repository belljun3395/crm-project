#!/bin/sh

sed -i "s/\$SENTINEL_MONITOR_NAME/$SENTINEL_MONITOR_NAME/g" /redis/sentinel.conf
sed -i "s/\$SENTINEL_QUORUM/$SENTINEL_QUORUM/g" /redis/sentinel.conf
sed -i "s/\$SENTINEL_DOWN_AFTER/$SENTINEL_DOWN_AFTER/g" /redis/sentinel.conf
sed -i "s/\$SENTINEL_FAILOVER/$SENTINEL_FAILOVER/g" /redis/sentinel.conf

# redis environment variable is set via docker-compose.yml
sed -i "s/\$REDIS_MASTER_HOST/$REDIS_MASTER_HOST/g" /redis/sentinel.conf
sed -i "s/\$REDIS_MASTER_PORT/$REDIS_MASTER_PORT/g" /redis/sentinel.conf

redis-server /redis/sentinel.conf --sentinel