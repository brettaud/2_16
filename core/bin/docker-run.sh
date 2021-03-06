#!/bin/bash

# wait for MySql to be available
until mysql -N -s -u $DATABASE_USER -p$DATABASE_PASSWORD -h $DATABASE_HOST $DATABASE_NAME -e "SHOW TABLES;" > /dev/null 2>&1; do sleep 1s; done

# invoke flyway migrate (on first startup only)
if [ ! -f flyway/INIT ]; then
  pushd flyway
  flyway -url=jdbc:mysql://$DATABASE_HOST:$DATABASE_PORT migrate
  touch INIT
  popd
fi

# import db samples (only if database is empty, i.e. has no securities)
if [[ `mysql -N -s -u $DATABASE_USER -p$DATABASE_PASSWORD -h $DATABASE_HOST $DATABASE_NAME -e "SELECT COUNT(id) FROM security;"` == 0 ]]; then
  if [ "$1" = "-i" ]; then
    mysql -u $DATABASE_USER -p$DATABASE_PASSWORD -h $DATABASE_HOST $DATABASE_NAME < samples/db/mysql/mysql-data.sql
	echo "imported mysql sample data"
	if [ ! "$IB_GATEWAY_ACCOUNT" = "" ]; then
	  mysql -N -s -u $DATABASE_USER -p$DATABASE_PASSWORD -h $DATABASE_HOST $DATABASE_NAME -e "UPDATE account SET EXT_ACCOUNT='$IB_GATEWAY_ACCOUNT' WHERE NAME ='IB_NATIVE_TEST';"
	  echo "set IB account"
	fi
  fi
fi

exec java -cp conf:lib/* \
  -DdataSource.url=jdbc:mysql://$DATABASE_HOST:$DATABASE_PORT/$DATABASE_NAME \
  -DdataSource.user=$DATABASE_USER \
  -DdataSource.password=$DATABASE_PASSWORD \
  -Dib.host=$IB_GATEWAY_HOST \
  -Dspring.profiles.active=$SPRING_PROFILES \
  $VM_ARGUMENTS \
  $STARTER_CLASS