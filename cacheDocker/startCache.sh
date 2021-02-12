#!/bin/bash
export LD_LIBRARY_PATH=/usr/local/solace/solclient/lib:/usr/local/solace/SolaceCache/lib:$LD_LIBRARY_PATH
python ./SolaceCache/bin/keepalive ./SolaceCache/bin/SolaceCache -f /tmp/cacheConfig.conf