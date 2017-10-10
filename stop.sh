#!/bin/bash
ENV_FILE=genny.env docker-compose -f dcc.yml  stop $@
ENV_FILE=genny.env docker-compose -f dcc.yml rm -f $@

