#!/bin/sh
# Live reload runner — watch .kt files, rebuild + restart

set -e

KT_SRC="/app/app/src/main/kotlin"
JAR="/app/app/build/libs/app-all.jar"

find_kt_files_checksum() {
	find "$KT_SRC" -type f -name "*.kt" -o -name "*.gradle.kts" | xargs ls -lR | md5sum | awk '{print $1}'
}

rebuild_jar() {
	echo "[$(date '+%H:%M:%S')] Rebuilding..."
	cd /app
	gradle shadowJar -x test --no-daemon 2>&1 | tail -5
}

run_app() {
	cd /app
	exec java -jar "$JAR"
}

echo "[$(date '+%H:%M:%S')] Starting app with live reload..."
rebuild_jar
run_app &
APP_PID=$!

LAST_CHECKSUM=$(find_kt_files_checksum)

while true; do
	sleep 3
	CURRENT_CHECKSUM=$(find_kt_files_checksum)

	if [ "$CURRENT_CHECKSUM" != "$LAST_CHECKSUM" ]; then
		LAST_CHECKSUM="$CURRENT_CHECKSUM"
		echo "[$(date '+%H:%M:%S')] Source changes detected!"

		kill $APP_PID 2>/dev/null || true
		wait $APP_PID 2>/dev/null || true

		rebuild_jar
		run_app &
		APP_PID=$!
	fi
done
