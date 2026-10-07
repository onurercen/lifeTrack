#!/bin/sh
# Dumps the database once at start and then every BACKUP_INTERVAL_HOURS, keeping
# BACKUP_KEEP_DAYS days of dumps in /backups. Runs in the postgres image, so
# pg_dump matches the server version. Connection comes from PG* variables.
set -eu

interval_hours="${BACKUP_INTERVAL_HOURS:-24}"
keep_days="${BACKUP_KEEP_DAYS:-14}"

until pg_isready -q; do
	sleep 5
done

while true; do
	file="/backups/lifetrack-$(date +%Y-%m-%d_%H%M).dump"
	if pg_dump --format=custom --file="$file.partial"; then
		mv "$file.partial" "$file"
		echo "backup: wrote $file"
	else
		rm -f "$file.partial"
		echo "backup: pg_dump failed" >&2
	fi
	find /backups -name 'lifetrack-*.dump' -mtime +"$keep_days" -delete
	sleep $((interval_hours * 3600))
done
