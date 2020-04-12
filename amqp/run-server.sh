#!/bin/bash
exec docker run -ti --rm -p 5672:5672 rabbitmq:3
