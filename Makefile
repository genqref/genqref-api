init:
	docker-compose up -d

start:
	docker-compose start

PG_OPTS:=-h localhost -p 5424 -U postgres

create-db:
	#docker-compose exec postgres
	createdb $(PG_OPTS) genqref

migrate:
	lein run :duct.migrator/ragtime

rollback:
	lein run :duct.migrator/ragtime rollback

reset-db:
	#docker-compose exec postgres
	dropdb $(PG_OPTS) genqref
	make create-db
	#make migrate

db-console:
	psql $(PG_OPTS) genqref
